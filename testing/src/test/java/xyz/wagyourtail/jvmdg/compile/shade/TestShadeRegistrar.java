package xyz.wagyourtail.jvmdg.compile.shade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.config.ShadeReflection;
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
    public void inlinedMemberStillShipsItsResources(@TempDir Path apiRoot) throws Exception {
        Files.write(apiRoot.resolve("data.txt"), "payload".getBytes("UTF-8"));

        ClassNode util = apiUtil();
        MethodNode resourceful = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "resourceful", "()V", null, null);
        AnnotationNode requires = new AnnotationNode("Lxyz/wagyourtail/jvmdg/version/RequiresResource;");
        requires.values = Arrays.<Object>asList("value", Arrays.<Object>asList("data.txt"));
        resourceful.invisibleAnnotations = new ArrayList<AnnotationNode>(1);
        resourceful.invisibleAnnotations.add(requires);
        util.methods.add(resourceful);

        Fixture f = new Fixture();
        f.apiRoots.add(apiRoot);
        f.addApi(util);
        // one caller → `resourceful` is an inline candidate and leaves the
        // required set — resources are captured at closure time, before
        // inline candidates are subtracted, so the table can't be stranded
        TransformContext ctx = f.ctx();
        new TransformPipeline().addStages(f.registrar.stages())
            .run(Collections.singletonList(
                caller("in/A", Opcodes.V1_8, "api/Util", "resourceful", "()V")), ctx);

        assertTrue(f.registrar.context().emitted.isEmpty(), "member inlined — no api class emitted");
        assertArrayEquals("payload".getBytes("UTF-8"), ctx.resources().get("data.txt"),
            "inlined member's resources still ship");
    }

    @Test
    public void generatedDispatcherRidesThePrefixedClosure(@TempDir Path apiRoot) throws Exception {
        // the CG-1 shape: caller → invokestatic <adapter>$jvmdg$retro.wrap;
        // the dispatcher holds new-impl + ldc-adapter + invokestatic
        // RetroTypes edges; RetroTypes carries class-level @RequiresResource
        // for both runtime tables
        Files.createDirectories(apiRoot.resolve("META-INF/jvmdg"));
        Files.write(apiRoot.resolve("META-INF/jvmdg/retrotypes"), "retro\tmembers".getBytes("UTF-8"));
        Files.write(apiRoot.resolve("META-INF/jvmdg/wraps"), "retro\tstub\tmember=impl".getBytes("UTF-8"));

        ClassNode dispatcher = apiClass("api/Adapter$jvmdg$retro", Opcodes.V1_8);
        MethodNode wrap = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "wrap", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        wrap.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        wrap.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "api/RetroTypes", "wrapImpl", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
        wrap.instructions.add(new InsnNode(Opcodes.ARETURN));
        dispatcher.methods.add(wrap);
        MethodNode newImpl = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
            "newImpl", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        newImpl.instructions.add(new TypeInsnNode(Opcodes.NEW, "api/Impl"));
        newImpl.instructions.add(new InsnNode(Opcodes.DUP));
        newImpl.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        newImpl.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
            "api/Impl", "<init>", "(Ljava/lang/Object;)V", false));
        newImpl.instructions.add(new InsnNode(Opcodes.ARETURN));
        dispatcher.methods.add(newImpl);

        ClassNode retroTypes = apiClass("api/RetroTypes", Opcodes.V1_8);
        retroTypes.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "wrapImpl", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null));
        AnnotationNode requires = new AnnotationNode("Lxyz/wagyourtail/jvmdg/version/RequiresResource;");
        requires.values = Arrays.<Object>asList("value",
            Arrays.<Object>asList("META-INF/jvmdg/retrotypes", "META-INF/jvmdg/wraps"));
        retroTypes.invisibleAnnotations = new ArrayList<AnnotationNode>(1);
        retroTypes.invisibleAnnotations.add(requires);

        ClassNode impl = apiClass("api/Impl", Opcodes.V1_8);
        impl.methods.add(new MethodNode(Opcodes.ACC_PUBLIC,
            "<init>", "(Ljava/lang/Object;)V", null, null));

        Fixture f = new Fixture();
        f.apiRoots.add(apiRoot);
        f.apiClasses.add(Type.getObjectType("api/Adapter$jvmdg$retro"));
        f.apiClasses.add(Type.getObjectType("api/RetroTypes"));
        f.apiClasses.add(Type.getObjectType("api/Impl"));
        f.addApi(dispatcher);
        f.addApi(retroTypes);
        f.addApi(impl);

        // two callers → prefix path (shared member can't inline)
        TransformContext ctx = f.ctx();
        List<ClassNode> out = new TransformPipeline().addStages(f.registrar.stages())
            .run(Arrays.asList(
                caller("in/A", Opcodes.V1_8, "api/Adapter$jvmdg$retro", "wrap",
                    "(Ljava/lang/Object;)Ljava/lang/Object;"),
                caller("in/B", Opcodes.V1_8, "api/Adapter$jvmdg$retro", "wrap",
                    "(Ljava/lang/Object;)Ljava/lang/Object;")), ctx);

        for (ClassNode node : out) {
            assertEquals(PREFIX + "api/Adapter$jvmdg$retro", findCall(node).owner);
        }
        // dispatcher emitted under the prefix; its internal api edges
        // (RetroTypes wrapImpl, Impl construction) prefixed with it
        ClassNode emitted = f.registrar.context().emitted.get(PREFIX + "api/Adapter$jvmdg$retro.class");
        assertNotNull(emitted, "dispatcher lands in the api closure");
        for (MethodNode m : emitted.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof MethodInsnNode) {
                    assertTrue(((MethodInsnNode) insn).owner.startsWith(PREFIX),
                        "api ref inside emitted dispatcher is prefixed");
                }
                if (insn instanceof TypeInsnNode) {
                    assertTrue(((TypeInsnNode) insn).desc.startsWith(PREFIX),
                        "new target inside emitted dispatcher is prefixed");
                }
            }
        }
        assertNotNull(f.registrar.context().emitted.get(PREFIX + "api/RetroTypes.class"));
        // class-level @RequiresResource pulled both runtime tables
        assertArrayEquals("retro\tmembers".getBytes("UTF-8"),
            ctx.resources().get("META-INF/jvmdg/retrotypes"));
        assertArrayEquals("retro\tstub\tmember=impl".getBytes("UTF-8"),
            ctx.resources().get("META-INF/jvmdg/wraps"));
    }

    @Test
    public void gainedCallsShipWithPrefixedStubSites(@TempDir Path apiRoot) throws Exception {
        // the CG-2 gained shape: caller → invokestatic gained dispatcher's
        // member method; the dispatcher tail-calls RetroTypes.gainedCall
        // (bytecode edge → class-level @RequiresResource ships the table)
        // and declares its reflective stub sites via @ReflectionReferences —
        // the only edge that pulls J_U_C_ExecutorService.close into the
        // closure — while the shipped row's site owner gets prefixed
        String row = "java/lang/AutoCloseable\tclose ()V\t" +
            "java/util/concurrent/ExecutorService=63:java/util/concurrent/ExecutorService>" +
            "api/J_U_C_ExecutorService.close(Ljava/util/concurrent/ExecutorService;)V," +
            "java/lang/Process=70:java/lang/Process>" +
            "D:api/J_L_Process.close(Ljava/lang/Process;)V," +
            "java/io/InputStream=64:java/io/InputStream;67:-";
        Files.createDirectories(apiRoot.resolve("META-INF/jvmdg"));
        Files.write(apiRoot.resolve("META-INF/jvmdg/gained-calls"), row.getBytes("UTF-8"));

        ClassNode dispatcher = apiClass("api/gained/J_L_AutoCloseable", Opcodes.V1_8);
        MethodNode close = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "close$()V", "(Ljava/lang/Object;I)V", null, null);
        close.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "api/RetroTypes", "gainedCall", "(Ljava/lang/String;)Ljava/lang/Object;", false));
        close.instructions.add(new InsnNode(Opcodes.POP));
        close.instructions.add(new InsnNode(Opcodes.RETURN));
        AnnotationNode ref = new AnnotationNode("Lxyz/wagyourtail/jvmdg/version/Ref;");
        ref.values = Arrays.<Object>asList(
            "value", "Lapi/J_U_C_ExecutorService;",
            "member", "close",
            "desc", "(Ljava/util/concurrent/ExecutorService;)V");
        AnnotationNode dRef = new AnnotationNode("Lxyz/wagyourtail/jvmdg/version/Ref;");
        dRef.values = Arrays.<Object>asList(
            "value", "Lapi/J_L_Process;",
            "member", "close",
            "desc", "(Ljava/lang/Process;)V");
        AnnotationNode refs = new AnnotationNode("Lxyz/wagyourtail/jvmdg/version/ReflectionReferences;");
        refs.values = Arrays.<Object>asList("value",
            Arrays.<Object>asList(ref, dRef));
        close.invisibleAnnotations = new ArrayList<AnnotationNode>(1);
        close.invisibleAnnotations.add(refs);
        dispatcher.methods.add(close);

        ClassNode stub = apiClass("api/J_U_C_ExecutorService", Opcodes.V1_8);
        stub.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "close", "(Ljava/util/concurrent/ExecutorService;)V", null, null));
        stub.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "other", "()V", null, null));

        ClassNode dStub = apiClass("api/J_L_Process", Opcodes.V1_8);
        dStub.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "close", "(Ljava/lang/Process;)V", null, null));

        ClassNode retroTypes = apiClass("api/RetroTypes", Opcodes.V1_8);
        retroTypes.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "gainedCall", "(Ljava/lang/String;)Ljava/lang/Object;", null, null));
        AnnotationNode requires = new AnnotationNode("Lxyz/wagyourtail/jvmdg/version/RequiresResource;");
        requires.values = Arrays.<Object>asList("value",
            Arrays.<Object>asList("META-INF/jvmdg/gained-calls"));
        retroTypes.invisibleAnnotations = new ArrayList<AnnotationNode>(1);
        retroTypes.invisibleAnnotations.add(requires);

        Fixture f = new Fixture();
        f.apiRoots.add(apiRoot);
        f.apiClasses.add(Type.getObjectType("api/gained/J_L_AutoCloseable"));
        f.apiClasses.add(Type.getObjectType("api/J_U_C_ExecutorService"));
        f.apiClasses.add(Type.getObjectType("api/J_L_Process"));
        f.apiClasses.add(Type.getObjectType("api/RetroTypes"));
        f.addApi(dispatcher);
        f.addApi(stub);
        f.addApi(dStub);
        f.addApi(retroTypes);

        TransformContext ctx = f.ctx();
        new TransformPipeline().addStages(f.registrar.stages())
            .run(Arrays.asList(
                caller("in/A", Opcodes.V1_8, "api/gained/J_L_AutoCloseable", "close$()V",
                    "(Ljava/lang/Object;I)V"),
                caller("in/B", Opcodes.V1_8, "api/gained/J_L_AutoCloseable", "close$()V",
                    "(Ljava/lang/Object;I)V")), ctx);

        // the declared ref pulled the stub member into the prefixed closure;
        // its sibling member is pruned
        ClassNode emitted = f.registrar.context().emitted.get(PREFIX + "api/J_U_C_ExecutorService.class");
        assertNotNull(emitted, "@ReflectionReferences pulls the reflective stub site");
        assertEquals(1, emitted.methods.size(), "unreferenced stub members prune");
        assertEquals("close", emitted.methods.get(0).name);

        byte[] shipped = ctx.resources().get("META-INF/jvmdg/gained-calls");
        assertNotNull(shipped, "gained-calls table must ride the closure");
        String out = new String(shipped, "UTF-8");
        assertTrue(out.contains(">" + PREFIX + "api/J_U_C_ExecutorService.close("),
            "stub site owner carries the prefix: " + out);
        assertTrue(out.contains(">D:" + PREFIX + "api/J_L_Process.close("),
            "a D: dispatch site keeps its marker and prefixes the impl owner: " + out);
        assertTrue(out.contains("java/util/concurrent/ExecutorService=63:"),
            "JDK declarer names stay unprefixed: " + out);
        assertTrue(out.contains(";67:-"), "unresolvable windows survive verbatim: " + out);
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

    // --- shadeReflection ---

    private static final String REFL_RES = "xyz/wagyourtail/jvmdg/stub/reflection.idx";

    private static void writeRefl(Path apiRoot, String... rows) throws IOException {
        Path p = apiRoot.resolve(REFL_RES);
        Files.createDirectories(p.getParent());
        Files.write(p, String.join("\n", Arrays.asList(rows)).getBytes("UTF-8"));
    }

    /** stub class carrying the impl member + class-level RequiresResource for the table */
    private static ClassNode scannerStub() {
        ClassNode node = apiClass("api/J_U_Scanner", Opcodes.V1_8);
        node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "useDelimiter", "(Ljava/util/Scanner;Ljava/lang/String;)Ljava/util/Scanner;", null, null));
        AnnotationNode requires = new AnnotationNode("Lxyz/wagyourtail/jvmdg/version/RequiresResource;");
        requires.values = Arrays.<Object>asList("value", Arrays.<Object>asList(REFL_RES));
        node.invisibleAnnotations = new ArrayList<AnnotationNode>(1);
        node.invisibleAnnotations.add(requires);
        return node;
    }

    /** M row: java/util/Scanner.useDelimiter emulated by api/J_U_Scanner.useDelimiter */
    private static final String SCANNER_ROW =
        "M\tjava/util/Scanner\tuseDelimiter\t(Ljava/lang/String;)Ljava/util/Scanner;\ti\t" +
        "api/J_U_Scanner\tuseDelimiter\t(Ljava/util/Scanner;Ljava/lang/String;)Ljava/util/Scanner;\t55\t1";

    private static Fixture reflFixture(Path apiRoot) throws IOException {
        // second row: a gained-supertype T row whose "impl" is a real JDK
        // type — it must never root into the api closure
        writeRefl(apiRoot, SCANNER_ROW,
            "T\tjava/util/SequencedCollection\tjava/util/Collection\t65");
        Fixture f = new Fixture();
        f.apiRoots.add(apiRoot);
        f.apiClasses.add(Type.getObjectType("api/J_U_Scanner"));
        f.addApi(scannerStub());
        return f;
    }

    @Test
    public void usedOwnersRootsReflectionImpls(@TempDir Path apiRoot) throws Exception {
        Fixture f = reflFixture(apiRoot);
        // caller touches java/util/Scanner — the owner ref roots the row's impl
        TransformContext ctx = f.ctx();
        new TransformPipeline().addStages(f.registrar.stages())
            .run(Arrays.asList(
                caller("in/A", Opcodes.V1_8, "java/util/Scanner", "hasNext", "()Z"),
                // keeps the JDK-target T row — its impl must not root
                caller("in/B", Opcodes.V1_8, "java/util/SequencedCollection", "reversed",
                    "()Ljava/util/SequencedCollection;")), ctx);

        ClassNode emitted = f.registrar.context().emitted.get(PREFIX + "api/J_U_Scanner.class");
        assertNotNull(emitted, "reflection-rooted impl lands in the closure");
        assertFalse(f.registrar.context().emitted.containsKey(PREFIX + "java/util/Collection.class"),
            "JDK-target rows never enter the api closure");
        // the shipped table keeps the row, impl owner prefixed
        byte[] shipped = ctx.resources().get(PREFIX + REFL_RES);
        assertNotNull(shipped, "table rides the closure");
        String out = new String(shipped, "UTF-8");
        assertTrue(out.contains(PREFIX + "api/J_U_Scanner"), "impl owner prefixed: " + out);
        assertTrue(out.contains("java/util/Scanner"), "JDK owner unprefixed: " + out);
    }

    @Test
    public void unreferencedOwnersPruneReflectionRows(@TempDir Path apiRoot) throws Exception {
        Fixture f = reflFixture(apiRoot);
        // no Scanner ref → the row's impl isn't rooted → row prunes
        TransformContext ctx = f.ctx();
        new TransformPipeline().addStages(f.registrar.stages())
            .run(Collections.singletonList(
                caller("in/A", Opcodes.V1_8, "java/lang/System", "gc", "()V")), ctx);

        assertFalse(f.registrar.context().emitted.containsKey(PREFIX + "api/J_U_Scanner.class"),
            "unrooted impl stays out of the closure");
    }

    @Test
    public void shadeReflectionNoneDropsEverything(@TempDir Path apiRoot) throws Exception {
        Fixture f = reflFixture(apiRoot);
        f.flags.shadeReflection = ShadeReflection.NONE;
        TransformContext ctx = f.ctx();
        new TransformPipeline().addStages(f.registrar.stages())
            .run(Collections.singletonList(
                caller("in/A", Opcodes.V1_8, "java/util/Scanner", "hasNext", "()Z")), ctx);

        assertFalse(f.registrar.context().emitted.containsKey(PREFIX + "api/J_U_Scanner.class"),
            "none roots no impls");
    }

    @Test
    public void shadeKeepReflectionUnionsExtraRows(@TempDir Path apiRoot) throws Exception {
        Fixture f = reflFixture(apiRoot);
        f.flags.shadeKeepReflection.put("java/util/Scanner.", Flags.WildcardType.SINGLE);
        // no Scanner bytecode ref at all — the keep pattern alone roots the row
        TransformContext ctx = f.ctx();
        new TransformPipeline().addStages(f.registrar.stages())
            .run(Collections.singletonList(
                caller("in/A", Opcodes.V1_8, "java/lang/System", "gc", "()V")), ctx);

        assertNotNull(f.registrar.context().emitted.get(PREFIX + "api/J_U_Scanner.class"),
            "keep pattern roots the impl without a bytecode ref");
        byte[] shipped = ctx.resources().get(PREFIX + REFL_RES);
        assertNotNull(shipped);
        assertTrue(new String(shipped, "UTF-8").contains(PREFIX + "api/J_U_Scanner"));
    }

    @Test
    public void dynamicSitesWarnUnderUsedOwners(@TempDir Path apiRoot) throws Exception {
        Fixture f = reflFixture(apiRoot);
        // Class.forName is a dynamic entry point — the site warns under usedOwners
        TransformContext ctx = f.ctx();
        new TransformPipeline().addStages(f.registrar.stages())
            .run(Collections.singletonList(
                caller("in/A", Opcodes.V1_8, "java/lang/Class", "forName",
                    "(Ljava/lang/String;)Ljava/lang/Class;")), ctx);

        Set<String> warns = ctx.diagnostics.drain("shade");
        assertFalse(warns.isEmpty(), "a dynamic site warns under usedOwners");
        assertTrue(warns.iterator().next().contains("in/A"), warns.toString());

        // ALL silences the warning — nothing is limited
        Fixture g = reflFixture(apiRoot);
        g.flags.shadeReflection = ShadeReflection.ALL;
        TransformContext gctx = g.ctx();
        new TransformPipeline().addStages(g.registrar.stages())
            .run(Collections.singletonList(
                caller("in/B", Opcodes.V1_8, "java/lang/Class", "forName",
                    "(Ljava/lang/String;)Ljava/lang/Class;")), gctx);
        assertTrue(gctx.diagnostics.drain("shade").isEmpty());
    }

    // --- reflection const-fold ---

    /** T row: java/io/UncheckedIOException → api/J_I_UncheckedIOException */
    private static final String T_ROW =
        "T\tjava/io/UncheckedIOException\tapi/J_I_UncheckedIOException\t52";

    private static ClassNode forNameCaller(String literal) {
        ClassNode node = apiClass("in/A", Opcodes.V1_8);
        MethodNode call = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "call", "()V", null, null);
        // a real owner ref (checkcast) so usedOwners roots the T row
        call.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        call.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/io/UncheckedIOException"));
        call.instructions.add(new InsnNode(Opcodes.POP));
        call.instructions.add(new LdcInsnNode(literal));
        call.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false));
        call.instructions.add(new InsnNode(Opcodes.POP));
        call.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(call);
        return node;
    }

    private static String ldcOf(ClassNode node) {
        for (AbstractInsnNode insn : node.methods.get(0).instructions) {
            if (insn instanceof LdcInsnNode) {
                return String.valueOf(((LdcInsnNode) insn).cst);
            }
        }
        return null;
    }

    @Test
    public void forNameLiteralFoldsToShadedTarget(@TempDir Path apiRoot) throws Exception {
        writeRefl(apiRoot, T_ROW);
        Fixture f = new Fixture();
        f.apiRoots.add(apiRoot);
        f.apiClasses.add(Type.getObjectType("api/J_I_UncheckedIOException"));
        f.addApi(apiClass("api/J_I_UncheckedIOException", Opcodes.V1_8));

        List<ClassNode> out = new TransformPipeline().addStages(f.registrar.stages())
            .run(Collections.singletonList(forNameCaller("java.io.UncheckedIOException")), f.ctx());

        assertEquals(PREFIX.replace('/', '.') + "api.J_I_UncheckedIOException",
            ldcOf(out.get(0)), "forName arg folds to the prefixed target name");
    }

    @Test
    public void literalWithoutReflCallIsLeftAlone(@TempDir Path apiRoot) throws Exception {
        writeRefl(apiRoot, T_ROW);
        Fixture f = new Fixture();
        f.apiRoots.add(apiRoot);
        f.apiClasses.add(Type.getObjectType("api/J_I_UncheckedIOException"));
        f.addApi(apiClass("api/J_I_UncheckedIOException", Opcodes.V1_8));

        // same literal but the consuming call is println, not forName
        ClassNode caller = apiClass("in/A", Opcodes.V1_8);
        MethodNode call = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "call", "()V", null, null);
        call.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        call.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/io/UncheckedIOException"));
        call.instructions.add(new InsnNode(Opcodes.POP));
        call.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
            "java/lang/System", "out", "Ljava/io/PrintStream;"));
        call.instructions.add(new LdcInsnNode("java.io.UncheckedIOException"));
        call.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
        call.instructions.add(new InsnNode(Opcodes.RETURN));
        caller.methods.add(call);

        List<ClassNode> out = new TransformPipeline().addStages(f.registrar.stages())
            .run(Collections.singletonList(caller), f.ctx());
        assertEquals("java.io.UncheckedIOException", ldcOf(out.get(0)),
            "a literal not feeding forName stays literal");
    }

    @Test
    public void foldSkipsWhenImplNotInClosure(@TempDir Path apiRoot) throws Exception {
        writeRefl(apiRoot, T_ROW);
        Fixture f = new Fixture();
        f.apiRoots.add(apiRoot);
        f.apiClasses.add(Type.getObjectType("api/J_I_UncheckedIOException"));
        f.addApi(apiClass("api/J_I_UncheckedIOException", Opcodes.V1_8));
        // no owner ref at all → the T row's impl isn't rooted → nothing to fold to
        List<ClassNode> out = new TransformPipeline().addStages(f.registrar.stages())
            .run(Collections.singletonList(
                caller("in/A", Opcodes.V1_8, "java/lang/System", "gc", "()V")), f.ctx());
        // fold map resolves nothing; the un-rooted target can't be named
        assertTrue(f.registrar.context().emitted.isEmpty());
    }

}
