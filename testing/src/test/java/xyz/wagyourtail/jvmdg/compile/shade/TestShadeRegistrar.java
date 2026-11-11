package xyz.wagyourtail.jvmdg.compile.shade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.pipeline.Stage;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.pipeline.TransformPipeline;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * In-pipeline shading: the registrar's scan → closure → inline/prefix/
 * module-info stage chain against crafted ClassNodes. The api side is
 * indexed directly (what {@code ApiShader.scanApis} produces for jars).
 */
public class TestShadeRegistrar {

    private static final String PREFIX = "shaded/";

    private static Logger logger() {
        return new Logger(TestShadeRegistrar.class, Logger.Level.FATAL, false, System.out);
    }

    private static Flags flags() {
        Flags flags = new Flags();
        flags.classVersion = Opcodes.V1_8;
        flags.shadeInlining = true;
        return flags;
    }

    private static ClassNode apiClass(String name, int version) {
        ClassNode node = new ClassNode();
        node.version = version;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        return node;
    }

    /** caller → invokestatic owner.name desc */
    private static ClassNode caller(String name, int version, String owner, String target, String desc) {
        ClassNode node = apiClass(name, version);
        MethodNode call = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "call", "()V", null, null);
        call.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, target, desc, false));
        call.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(call);
        return node;
    }

    /** api class with a static method + an unused member to verify pruning. */
    private static ClassNode apiUtil() {
        ClassNode node = apiClass("api/Util", Opcodes.V1_8);
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

    private static class Fixture {
        final Flags flags = flags();
        final ReferenceIndex apiIndex = new ReferenceIndex(logger(), true);
        final ReferenceIndex inputIndex = new ReferenceIndex(logger(), true, flags.shadeInlining);
        final Set<Type> apiClasses = new HashSet<Type>();
        final List<Path> apiRoots = new ArrayList<Path>();
        final ShadeRegistrar registrar;

        Fixture() {
            apiClasses.add(Type.getObjectType("api/Util"));
            registrar = new ShadeRegistrar(flags, logger(), PREFIX, apiIndex, apiClasses, apiRoots, inputIndex);
        }

        void addApi(ClassNode node) {
            apiIndex.add(node, 0, apiFilter(apiClasses));
        }

        TransformContext ctx() {
            TransformContext ctx = new TransformContext(null, flags, logger());
            registrar.install(ctx);
            return ctx;
        }

        List<ClassNode> run(TransformPipeline pipeline, List<ClassNode> input, TransformContext ctx) throws IOException {
            return pipeline.run(input, ctx);
        }
    }

    private static MethodInsnNode findCall(ClassNode node) {
        for (AbstractInsnNode insn : node.methods.get(0).instructions) {
            if (insn instanceof MethodInsnNode) {
                return (MethodInsnNode) insn;
            }
        }
        return null;
    }

    // --- inline ---

    @Test
    public void singleCallerInlinesApiMethod() throws Exception {
        Fixture f = new Fixture();
        f.addApi(apiUtil());
        ClassNode caller = caller("in/Caller", Opcodes.V1_8, "api/Util", "helper", "()V");
        TransformPipeline p = new TransformPipeline().addStages(f.registrar.stages());
        List<ClassNode> out = p.run(Collections.singletonList(caller), f.ctx());

        ClassNode shaded = out.get(0);
        MethodInsnNode insn = findCall(shaded);
        // retargeted to the caller's own appended copy
        assertEquals("in/Caller", insn.owner);
        assertEquals("jvmdg$inlined$helper", insn.name);
        assertEquals(2, shaded.methods.size(), "call + inlined helper");
        assertEquals("jvmdg$inlined$helper", shaded.methods.get(1).name);
        assertTrue((shaded.methods.get(1).access & Opcodes.ACC_PRIVATE) != 0);
        // inlined members leave required entirely — no api class emitted
        assertTrue(f.registrar.context().emitted.isEmpty());
    }

    @Test
    public void inliningDisabledPrefixesInstead() throws Exception {
        Fixture f = new Fixture();
        f.flags.shadeInlining = false;
        f.addApi(apiUtil());
        ClassNode caller = caller("in/Caller", Opcodes.V1_8, "api/Util", "helper", "()V");
        TransformPipeline p = new TransformPipeline().addStages(f.registrar.stages());
        List<ClassNode> out = p.run(Collections.singletonList(caller), f.ctx());

        assertEquals(PREFIX + "api/Util", findCall(out.get(0)).owner);
        assertEquals("helper", findCall(out.get(0)).name);
        assertEquals(1, out.get(0).methods.size(), "no inlined copy appended");
        assertTrue(f.registrar.context().emitted.containsKey(PREFIX + "api/Util.class"));
    }

    // --- prefix / emission ---

    @Test
    public void sharedApiMemberPrefixesCallersAndEmitsPruned() throws Exception {
        Fixture f = new Fixture();
        f.addApi(apiUtil());
        List<ClassNode> out = new TransformPipeline().addStages(f.registrar.stages())
            .run(Arrays.asList(
                caller("in/CallerA", Opcodes.V1_8, "api/Util", "helper", "()V"),
                caller("in/CallerB", Opcodes.V1_8, "api/Util", "helper", "()V")), f.ctx());

        for (ClassNode node : out) {
            MethodInsnNode insn = findCall(node);
            assertEquals(PREFIX + "api/Util", insn.owner);
            assertEquals("helper", insn.name, "member name is not prefixed");
        }
        // emitted api class carries only the used member
        ClassNode emitted = f.registrar.context().emitted.get(PREFIX + "api/Util.class");
        assertNotNull(emitted);
        assertEquals(PREFIX + "api/Util", emitted.name);
        assertEquals(1, emitted.methods.size());
        assertEquals("helper", emitted.methods.get(0).name);
    }

    @Test
    public void nonApiRefsAreUntouched() throws Exception {
        Fixture f = new Fixture();
        f.addApi(apiUtil());
        ClassNode caller = caller("in/Caller", Opcodes.V1_8, "java/lang/System", "gc", "()V");
        List<ClassNode> out = new TransformPipeline().addStages(f.registrar.stages())
            .run(Collections.singletonList(caller), f.ctx());
        assertEquals("java/lang/System", findCall(out.get(0)).owner);
        assertTrue(f.registrar.context().emitted.isEmpty());
    }

    // --- variants ---

    @Test
    public void capturedVariantsAreShadedInPlace() throws Exception {
        Fixture f = new Fixture();
        f.addApi(apiUtil());
        // shared by caller + its variant → not an inline candidate at slot 0;
        // at the variant's slot it is the sole user → inlined there instead
        List<Stage> stages = new ArrayList<Stage>();
        stages.add(new Stage("capture").capture(Opcodes.V11));
        stages.addAll(f.registrar.stages());
        TransformContext ctx = f.ctx();
        List<ClassNode> out = new TransformPipeline().addStages(stages)
            .run(Arrays.asList(
                caller("in/Caller", Opcodes.V11, "api/Util", "helper", "()V"),
                caller("in/Other", Opcodes.V11, "api/Util", "helper", "()V")), ctx);

        // primaries flow at slot 0 (their version stamps V11 > target → variant slot 11)…
        Map<Object, List<ClassNode>> variants = ctx.variants;
        assertFalse(variants.isEmpty());
        for (List<ClassNode> variantSet : variants.values()) {
            for (ClassNode v : variantSet) {
                assertNotNull(findCall(v), "variant kept its invokestatic");
            }
        }
        // emitted api class covers the required member
        assertTrue(f.registrar.context().emitted.containsKey(PREFIX + "api/Util.class"));
        for (ClassNode node : out) {
            assertEquals(PREFIX + "api/Util", findCall(node).owner);
        }
    }

    // --- resources + module-info ---

    @Test
    public void apiResourcesAreEmittedButNotSignatures(@TempDir Path apiRoot) throws Exception {
        Files.write(apiRoot.resolve("data.txt"), "payload".getBytes("UTF-8"));
        Files.createDirectories(apiRoot.resolve("META-INF"));
        Files.write(apiRoot.resolve("META-INF/SIG.SF"), "sig".getBytes("UTF-8"));

        ClassNode util = apiUtil();
        MethodNode resourceful = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "resourceful", "()V", null, null);
        AnnotationNode requires = new AnnotationNode("Lxyz/wagyourtail/jvmdg/version/RequiresResource;");
        requires.values = Arrays.<Object>asList("value", Arrays.<Object>asList("data.txt", "META-INF/SIG.SF"));
        // CLASS retention → invisibleAnnotations
        resourceful.invisibleAnnotations = new ArrayList<AnnotationNode>(1);
        resourceful.invisibleAnnotations.add(requires);
        util.methods.add(resourceful);

        Fixture f = new Fixture();
        f.apiRoots.add(apiRoot);
        f.addApi(util);
        // two callers → prefix path keeps the member required, pulling its resources
        TransformContext ctx = f.ctx();
        new TransformPipeline().addStages(f.registrar.stages())
            .run(Arrays.asList(
                caller("in/A", Opcodes.V1_8, "api/Util", "resourceful", "()V"),
                caller("in/B", Opcodes.V1_8, "api/Util", "resourceful", "()V")), ctx);

        assertArrayEquals("payload".getBytes("UTF-8"), ctx.resources().get("data.txt"));
        assertNull(ctx.resources().get("META-INF/SIG.SF"), "signature files never carried");
    }

    @Test
    public void moduleInfoDropsApiRequires() throws Exception {
        Fixture f = new Fixture();
        ClassNode modInfo = apiClass("module-info", Opcodes.V9);
        modInfo.module = new ModuleNode("in.module", Opcodes.ACC_OPEN, null);
        modInfo.module.requires = new ArrayList<ModuleRequireNode>();
        modInfo.module.requires.add(new ModuleRequireNode("java.base", Opcodes.ACC_MANDATED, null));
        modInfo.module.requires.add(new ModuleRequireNode(ShadeModuleInfoTransform.API_MODULE, 0, null));

        List<ClassNode> out = new TransformPipeline().addStages(f.registrar.stages())
            .run(Collections.singletonList(modInfo), f.ctx());

        assertEquals(1, out.get(0).module.requires.size());
        assertEquals("java.base", out.get(0).module.requires.get(0).module);
    }

}
