package xyz.wagyourtail.jvmdg.compile.jar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.compile.shade.RefExtractor;
import xyz.wagyourtail.jvmdg.compile.shade.ReferenceIndex;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PipelineJarStep} end-to-end over plain directory trees: the
 * standalone-shade path (what {@code ApiShader.shadeApis} composes), the
 * fused downgrade+shade union run, and the variant channels
 * ({@code versions/<n>/} input entries + {@code multiReleaseOriginal} input
 * captures).
 */
public class TestPipelineJarStep {

    private static final String PREFIX = "shaded/";

    @TempDir
    Path tmp;

    private static Logger logger() {
        return new Logger(TestPipelineJarStep.class, Logger.Level.FATAL, false, System.out);
    }

    private static Flags flags() {
        Flags flags = new Flags();
        flags.classVersion = Opcodes.V1_8;
        flags.shadeInlining = true;
        return flags;
    }

    private static ClassNode plain(String name, int version) {
        ClassNode node = new ClassNode();
        node.version = version;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        return node;
    }

    /** caller → invokestatic api/Util.helper()V */
    private static ClassNode caller(String name, int version) {
        ClassNode node = plain(name, version);
        MethodNode call = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "call", "()V", null, null);
        call.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "api/Util", "helper", "()V", false));
        call.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(call);
        return node;
    }

    private static ClassNode apiUtil() {
        ClassNode node = plain("api/Util", Opcodes.V1_8);
        node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "helper", "()V", null, null));
        node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "unused", "()V", null, null));
        return node;
    }

    private static RefExtractor.Filter apiFilter(final Set<Type> apiClasses) {
        return new RefExtractor.Filter() {
            @Override
            public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
                return apiClasses.contains(member.getOwner());
            }
        };
    }

    private static ReferenceIndex apiIndex(Set<Type> apiClasses) {
        ReferenceIndex index = new ReferenceIndex(logger(), true);
        index.add(apiUtil(), 0, apiFilter(apiClasses));
        return index;
    }

    /** Writes {@code name} (internal form) to {@code root/name.class}. */
    private static void putClass(Path root, String name, ClassNode node) throws IOException {
        Path p = root.resolve(name + ".class");
        Files.createDirectories(p.getParent());
        Files.write(p, ASMUtils.classNodeToBytes(node));
    }

    private static ClassNode readOut(Path out, String rel) throws IOException {
        Path p = out.resolve(rel);
        assertTrue(Files.exists(p), "missing output " + rel);
        return ASMUtils.bytesToClassNode(Files.readAllBytes(p));
    }

    private static MethodInsnNode firstCall(ClassNode node) {
        for (AbstractInsnNode insn : node.methods.get(0).instructions) {
            if (insn instanceof MethodInsnNode) {
                return (MethodInsnNode) insn;
            }
        }
        return null;
    }

    private static StepContext ctx(Flags flags, ClassDowngrader downgrader, Path input) {
        return new StepContext(flags, logger(), downgrader,
            Collections.singletonList(input), new Function<String, byte[]>() {
                @Override
                public byte[] apply(String s) {
                    return null;
                }
            });
    }

    private void run(Path in, Path out, List<JarStep> steps, StepContext ctx) throws IOException {
        FusedJarRun.run(in, steps, ctx, out);
    }

    /** caller → invokevirtual java/lang/String.strip()Ljava/lang/String; (a J11 stub — forces real transforms) */
    private static ClassNode stripCaller(String name, int version) {
        ClassNode node = plain(name, version);
        MethodNode call = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "call", "()V", null, null);
        call.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "strip", "()Ljava/lang/String;", false));
        call.instructions.add(new InsnNode(Opcodes.POP));
        call.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(call);
        return node;
    }

    // --- standalone shade (the ApiShader composition) ---

    @Test
    public void standaloneShadeInlinesSingleUseApi() throws Exception {
        Flags flags = flags();
        Path in = tmp.resolve("in");
        Path out = tmp.resolve("out");
        putClass(in, "a/Caller", caller("a/Caller", Opcodes.V1_8));

        Set<Type> apiClasses = new HashSet<Type>(Collections.singleton(Type.getObjectType("api/Util")));
        run(in, out,
            Collections.<JarStep>singletonList(
                new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses, Collections.<Path>emptyList())),
            ctx(flags, null, in));

        // a member used by exactly one input class inlines into it instead
        // of emitting the api class
        MethodInsnNode call = firstCall(readOut(out, "a/Caller.class"));
        assertEquals("a/Caller", call.owner);
        assertEquals("jvmdg$inlined$helper", call.name);
        assertFalse(Files.exists(out.resolve(PREFIX + "api/Util.class")));
    }

    @Test
    public void standaloneShadePrefixesSharedApi() throws Exception {
        Flags flags = flags();
        Path in = tmp.resolve("in-p");
        Path out = tmp.resolve("out-p");
        putClass(in, "a/CallerA", caller("a/CallerA", Opcodes.V1_8));
        putClass(in, "b/CallerB", caller("b/CallerB", Opcodes.V1_8));

        Set<Type> apiClasses = new HashSet<Type>(Collections.singleton(Type.getObjectType("api/Util")));
        run(in, out,
            Collections.<JarStep>singletonList(
                new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses, Collections.<Path>emptyList())),
            ctx(flags, null, in));

        // two callers can't inline — the api class emits prefixed and the
        // unused member is pruned
        assertEquals(PREFIX + "api/Util", firstCall(readOut(out, "a/CallerA.class")).owner);
        assertEquals(PREFIX + "api/Util", firstCall(readOut(out, "b/CallerB.class")).owner);
        ClassNode emitted = readOut(out, PREFIX + "api/Util.class");
        assertEquals(1, emitted.methods.size());
        assertEquals("helper", emitted.methods.get(0).name);
    }

    @Test
    public void standaloneShadeVersionedInputShadedInPlace() throws Exception {
        Flags flags = flags();
        Path in = tmp.resolve("in-v");
        Path out = tmp.resolve("out-v");
        putClass(in, "a/Caller", caller("a/Caller", Opcodes.V1_8));
        putClass(in, "META-INF/versions/9/a/Variant", caller("a/Variant", Opcodes.V9));

        Set<Type> apiClasses = new HashSet<Type>(Collections.singleton(Type.getObjectType("api/Util")));
        run(in, out,
            Collections.<JarStep>singletonList(
                new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses, Collections.<Path>emptyList())),
            ctx(flags, null, in));

        // the versioned entry stays at its slot, shaded against slot-9's
        // own closure
        assertEquals(PREFIX + "api/Util",
            firstCall(readOut(out, "META-INF/versions/9/a/Variant.class")).owner);
    }

    // --- fused downgrade + shade ---

    @Test
    public void fusedDowngradeThenShade() throws Exception {
        Flags flags = flags();
        Path in = tmp.resolve("in-f");
        Path out = tmp.resolve("out-f");
        // a V11 caller and a V8 caller sharing one run — the union chain
        // must carry the V11 node through its own boundary without touching
        // the already-at-target V8 node
        putClass(in, "a/High", caller("a/High", Opcodes.V11));
        putClass(in, "a/Low", caller("a/Low", Opcodes.V1_8));

        Set<Type> apiClasses = new HashSet<Type>(Collections.singleton(Type.getObjectType("api/Util")));
        try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(flags)) {
            run(in, out,
                Collections.<JarStep>singletonList(
                    new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses, Collections.<Path>emptyList())),
                ctx(flags, downgrader, in));
        }

        ClassNode high = readOut(out, "a/High.class");
        assertEquals(Opcodes.V1_8, high.version, "V11 input not downgraded");
        assertEquals(PREFIX + "api/Util", firstCall(high).owner);
        assertEquals(PREFIX + "api/Util",
            firstCall(readOut(out, "a/Low.class")).owner);
        readOut(out, PREFIX + "api/Util.class");
    }

    @Test
    public void fusedMultiReleaseOriginalSeedsInputVariants() throws Exception {
        Flags flags = flags();
        flags.multiReleaseOriginal = true;
        Path in = tmp.resolve("in-mr");
        Path out = tmp.resolve("out-mr");
        // String.strip is a J11 stub — the class actually transforms, so the
        // input-state variant survives the cross-version byte dedup
        putClass(in, "a/High", stripCaller("a/High", Opcodes.V11));

        Set<Type> apiClasses = new HashSet<Type>(Collections.singleton(Type.getObjectType("api/Util")));
        try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(flags)) {
            run(in, out,
                Collections.<JarStep>singletonList(
                    new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses, Collections.<Path>emptyList())),
                ctx(flags, downgrader, in));
        }

        // input-state capture: V11 original lands under versions/11/
        assertEquals(Opcodes.V11, readOut(out, "META-INF/versions/11/a/High.class").version);
        assertEquals(Opcodes.V1_8, readOut(out, "a/High.class").version);
    }

    // --- preview inputs ---

    @Test
    public void fusedPreviewInputRejectedWithoutFlag() throws Exception {
        Flags flags = flags();
        Path in = tmp.resolve("in-pv");
        Path out = tmp.resolve("out-pv");
        // javac --enable-preview emits minor 0xFFFF — ASM reads it back as
        // minor<<16|major, a negative ClassNode.version
        putClass(in, "a/Prev", stripCaller("a/Prev", (0xFFFF << 16) | Opcodes.V11));

        Set<Type> apiClasses = new HashSet<Type>(Collections.singleton(Type.getObjectType("api/Util")));
        try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(flags)) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                run(in, out,
                    Collections.<JarStep>singletonList(
                        new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses, Collections.<Path>emptyList())),
                    ctx(flags, downgrader, in)));
            assertTrue(e.getMessage().contains("a/Prev"), "error names the preview class");
            assertTrue(e.getMessage().contains("allowPreview"), "error names the opt-in flag");
        }
    }

    @Test
    public void fusedPreviewInputRoutesToMajorBoundary() throws Exception {
        Flags flags = flags();
        flags.allowPreview = true;
        Path in = tmp.resolve("in-pv2");
        Path out = tmp.resolve("out-pv2");
        putClass(in, "a/Prev", stripCaller("a/Prev", (0xFFFF << 16) | Opcodes.V11));

        Set<Type> apiClasses = new HashSet<Type>(Collections.singleton(Type.getObjectType("api/Util")));
        try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(flags)) {
            run(in, out,
                Collections.<JarStep>singletonList(
                    new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses, Collections.<Path>emptyList())),
                ctx(flags, downgrader, in));
        }

        ClassNode prev = readOut(out, "a/Prev.class");
        // routed through the 59→…→52 chain: output is plain V1_8 — no
        // preview minor survives the rewrite
        assertEquals(Opcodes.V1_8, prev.version, "preview input not downgraded to target");
        // the V11-only String.strip callsite must have been rewritten
        // (indy dispatch or invokestatic — either way it no longer
        // invokes the real JDK member)
        for (AbstractInsnNode insn : prev.methods.get(0).instructions) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode m = (MethodInsnNode) insn;
                assertFalse("java/lang/String".equals(m.owner) && "strip".equals(m.name),
                    "callsite still points at the real JDK member");
            }
        }
    }

    // --- META-INF/services shading ---

    private static void putService(Path root, String ifaceDotted, List<String> lines) throws IOException {
        Path p = root.resolve("META-INF/services/" + ifaceDotted);
        Files.createDirectories(p.getParent());
        Files.write(p, lines);
    }

    @Test
    public void shadeRemapsServiceProviderLines() throws Exception {
        Flags flags = flags();
        Path in = tmp.resolve("in-svc");
        Path out = tmp.resolve("out-svc");
        // two callers force api/Util to emit prefixed rather than inline
        putClass(in, "a/CallerA", caller("a/CallerA", Opcodes.V1_8));
        putClass(in, "b/CallerB", caller("b/CallerB", Opcodes.V1_8));
        putService(in, "a.Spi", Arrays.asList(
            "# comment",
            "a.MyImpl",   // input-owned provider — untouched
            "api.Util",   // emitted api provider — prefixed
            "api.Gone")); // api class outside the closure — dropped

        Set<Type> apiClasses = new HashSet<Type>(Arrays.asList(
            Type.getObjectType("api/Util"), Type.getObjectType("api/Gone")));
        run(in, out,
            Collections.<JarStep>singletonList(
                new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses, Collections.<Path>emptyList())),
            ctx(flags, null, in));

        Path outSvc = out.resolve("META-INF/services/a.Spi");
        assertTrue(Files.exists(outSvc));
        List<String> lines = Files.readAllLines(outSvc);
        assertEquals(Arrays.asList("# comment", "a.MyImpl", "shaded.api.Util"), lines);
    }

    @Test
    public void shadeRenamesServiceFileForApiInterface() throws Exception {
        Flags flags = flags();
        Path in = tmp.resolve("in-svc-if");
        Path out = tmp.resolve("out-svc-if");
        putClass(in, "a/CallerA", caller("a/CallerA", Opcodes.V1_8));
        putClass(in, "b/CallerB", caller("b/CallerB", Opcodes.V1_8));
        putService(in, "api.Util", Collections.singletonList("a.MyImpl"));

        Set<Type> apiClasses = new HashSet<Type>(Arrays.asList(
            Type.getObjectType("api/Util"), Type.getObjectType("api/Gone")));
        run(in, out,
            Collections.<JarStep>singletonList(
                new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses, Collections.<Path>emptyList())),
            ctx(flags, null, in));

        // ServiceLoader.load(shaded.api.Util) resolves the prefixed name
        assertFalse(Files.exists(out.resolve("META-INF/services/api.Util")));
        assertEquals(Collections.singletonList("a.MyImpl"),
            Files.readAllLines(out.resolve("META-INF/services/shaded.api.Util")));
    }

    @Test
    public void shadeDropsServiceFileForPrunedApiInterface() throws Exception {
        Flags flags = flags();
        Path in = tmp.resolve("in-svc-pr");
        Path out = tmp.resolve("out-svc-pr");
        putClass(in, "a/CallerA", caller("a/CallerA", Opcodes.V1_8));
        putClass(in, "b/CallerB", caller("b/CallerB", Opcodes.V1_8));
        // the api interface never makes the closure — the file can't resolve
        putService(in, "api.Gone", Collections.singletonList("a.MyImpl"));

        Set<Type> apiClasses = new HashSet<Type>(Arrays.asList(
            Type.getObjectType("api/Util"), Type.getObjectType("api/Gone")));
        run(in, out,
            Collections.<JarStep>singletonList(
                new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses, Collections.<Path>emptyList())),
            ctx(flags, null, in));

        assertFalse(Files.exists(out.resolve("META-INF/services/api.Gone")));
        assertFalse(Files.exists(out.resolve("META-INF/services/shaded.api.Gone")));
    }

    @Test
    public void shadeMergesInputAndApiServiceFiles() throws Exception {
        Flags flags = flags();
        Path in = tmp.resolve("in-svc-m");
        Path out = tmp.resolve("out-svc-m");
        Path apiRoot = tmp.resolve("api-root");
        putClass(in, "a/CallerA", caller("a/CallerA", Opcodes.V1_8));
        putClass(in, "b/CallerB", caller("b/CallerB", Opcodes.V1_8));
        putService(in, "a.Spi", Collections.singletonList("a.MyImpl"));
        putService(apiRoot, "a.Spi", Arrays.asList("api.Util", "api.Gone"));

        Set<Type> apiClasses = new HashSet<Type>(Arrays.asList(
            Type.getObjectType("api/Util"), Type.getObjectType("api/Gone")));
        run(in, out,
            Collections.<JarStep>singletonList(
                new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses,
                    Collections.singletonList(apiRoot))),
            ctx(flags, null, in));

        // one output file per interface — input providers lead, api-side
        // survivors follow prefixed
        assertEquals(Arrays.asList("a.MyImpl", "shaded.api.Util"),
            Files.readAllLines(out.resolve("META-INF/services/a.Spi")));
    }

    @Test
    public void shadeCopiesApiServiceFilesFromRoots() throws Exception {
        Flags flags = flags();
        Path in = tmp.resolve("in-svc-api");
        Path out = tmp.resolve("out-svc-api");
        Path apiRoot = tmp.resolve("api-root2");
        putClass(in, "a/CallerA", caller("a/CallerA", Opcodes.V1_8));
        putClass(in, "b/CallerB", caller("b/CallerB", Opcodes.V1_8));
        // api-side file whose interface is a main (non-api) class — the
        // real shape of TransformContributor/VersionProvider registrations
        putService(apiRoot, "main.Spi", Arrays.asList("api.Util", "api.Gone"));
        // api-side file whose whole provider set prunes away — dropped
        putService(apiRoot, "other.Spi", Collections.singletonList("api.Gone"));

        Set<Type> apiClasses = new HashSet<Type>(Arrays.asList(
            Type.getObjectType("api/Util"), Type.getObjectType("api/Gone")));
        run(in, out,
            Collections.<JarStep>singletonList(
                new PipelineJarStep(PREFIX, apiIndex(apiClasses), apiClasses,
                    Collections.singletonList(apiRoot))),
            ctx(flags, null, in));

        assertEquals(Collections.singletonList("shaded.api.Util"),
            Files.readAllLines(out.resolve("META-INF/services/main.Spi")));
        assertFalse(Files.exists(out.resolve("META-INF/services/other.Spi")));
    }

}
