package xyz.wagyourtail.jvmdg.version;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.stub.all.RemovedInterfaces;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestVersionProviderStubClasses {

    private static final String REMOVED_DESC = Type.getType(RemovedInterfaces.class).getDescriptor();

    @Adapter(value = "com/example/RemovedIface", keepInterface = false)
    static class RemovedIfaceStub {
    }

    @Adapter(value = "com/example/KeptIface", keepInterface = true)
    static class KeptIfaceStub {
    }

    static class TestProvider extends VersionProvider {
        TestProvider() {
            super(Opcodes.V11, Opcodes.V1_8, 0);
        }

        @Override
        public void preInit() {
        }

        @Override
        public void init() {
        }
    }

    private static TestProvider provider() {
        TestProvider provider = new TestProvider();
        // OWN_INTERFACE pins the interface-remap bookkeeping under test —
        // SUPERTYPE would additionally inject retro markers
        provider.classStubs.put(
            Type.getObjectType("com/example/RemovedIface"),
            new ClassStubEntry(
                Type.getObjectType("jvmdg/stub/RemovedIface"),
                RemovedIfaceStub.class, RemovedIfaceStub.class.getAnnotation(Adapter.class).keepInterface(),
                Adapter.Mode.OWN_INTERFACE, null, null, null
            )
        );
        provider.classStubs.put(
            Type.getObjectType("com/example/KeptIface"),
            new ClassStubEntry(
                Type.getObjectType("jvmdg/stub/KeptIface"),
                KeptIfaceStub.class, KeptIfaceStub.class.getAnnotation(Adapter.class).keepInterface(),
                Adapter.Mode.OWN_INTERFACE, null, null, null
            )
        );
        return provider;
    }

    private static ClassNode classImplementing(String... interfaces) {
        ClassNode node = new ClassNode();
        node.name = "com/example/Target";
        node.superName = "java/lang/Object";
        node.interfaces = new ArrayList<>(Arrays.asList(interfaces));
        return node;
    }

    @Test
    public void removedInterfaceOnUnannotatedClassDoesNotNpe() {
        // ASM leaves visibleAnnotations null for unannotated classes — the removedInterfaces
        // block used to NPE on exactly this shape
        TestProvider provider = provider();
        ClassNode node = classImplementing("com/example/RemovedIface");

        assertDoesNotThrow(() -> provider.stubClasses(node, false, new HashSet<String>()));

        assertTrue(node.interfaces.isEmpty(), "stub-removed interface must be dropped");
        assertNotNull(node.visibleAnnotations, "annotation list must be initialized");
        assertEquals(1, node.visibleAnnotations.size());
        AnnotationNode an = node.visibleAnnotations.get(0);
        assertEquals(REMOVED_DESC, an.desc);
        @SuppressWarnings("unchecked")
        java.util.List<String> removed = (java.util.List<String>) an.values.get(1);
        assertEquals(Collections.singletonList("com.example.RemovedIface"), removed);
    }

    @Test
    public void removedInterfaceMergesIntoExistingAnnotation() {
        TestProvider provider = provider();
        ClassNode node = classImplementing("com/example/RemovedIface");
        AnnotationNode existing = new AnnotationNode(REMOVED_DESC);
        existing.values = new ArrayList<>();
        existing.values.add("value");
        existing.values.add(new ArrayList<>(Collections.singletonList("com.example.AlreadyRemoved")));
        node.visibleAnnotations = new ArrayList<>(Collections.singletonList(existing));

        provider.stubClasses(node, false, new HashSet<String>());

        assertTrue(node.interfaces.isEmpty());
        assertEquals(1, node.visibleAnnotations.size(), "must merge into the existing annotation");
        @SuppressWarnings("unchecked")
        java.util.List<String> removed = (java.util.List<String>) existing.values.get(1);
        assertTrue(removed.contains("com.example.AlreadyRemoved"));
        assertTrue(removed.contains("com.example.RemovedIface"));
        assertEquals(2, removed.size());
    }

    @Test
    public void keptInterfaceIsRemappedNotRemoved() {
        TestProvider provider = provider();
        ClassNode node = classImplementing("com/example/KeptIface");

        provider.stubClasses(node, false, new HashSet<String>());

        assertEquals(Collections.singletonList("jvmdg/stub/KeptIface"), node.interfaces);
        assertNull(node.visibleAnnotations, "no RemovedInterfaces annotation expected");
    }

    @Test
    public void unrelatedInterfacesAreUntouched() {
        TestProvider provider = provider();
        ClassNode node = classImplementing("java/io/Serializable", "com/example/KeptIface", "java/lang/Cloneable");

        provider.stubClasses(node, false, new HashSet<String>());

        assertEquals(Arrays.asList("java/io/Serializable", "jvmdg/stub/KeptIface", "java/lang/Cloneable"),
            node.interfaces, "stub lookup must not disturb unregistered interfaces");
    }

    /** SUPERTYPE adapter — its source type remaps away, so the marker restores membership. */
    private static ClassStubEntry supertypeStub(String source, String target, boolean keepInterface) {
        return new ClassStubEntry(
            Type.getObjectType(target), Type.getObjectType("jvmdg/stub/" + source.substring(source.lastIndexOf('/') + 1)),
            keepInterface, true, null, null, new int[0],
            Adapter.Mode.SUPERTYPE, null, null, null, null);
    }

    @Test
    public void supertypeInterfaceInjectsMarker() {
        TestProvider provider = provider();
        provider.classStubs.put(Type.getObjectType("com/example/RetroIface"),
            supertypeStub("com/example/RetroIface", "java/util/Collection", true));
        ClassNode node = classImplementing("com/example/RetroIface");

        provider.stubClasses(node, false, new HashSet<String>());

        assertEquals(Arrays.asList("java/util/Collection", "jvmdg/stub/RetroIface$Marker"),
            node.interfaces, "remapped interface plus injected marker restores instanceof");
    }

    @Test
    public void supertypeRemovedInterfaceStillInjectsMarker() {
        TestProvider provider = provider();
        provider.classStubs.put(Type.getObjectType("com/example/RetroIface"),
            supertypeStub("com/example/RetroIface", "java/util/Collection", false));
        ClassNode node = classImplementing("com/example/RetroIface");

        provider.stubClasses(node, false, new HashSet<String>());

        assertEquals(Collections.singletonList("jvmdg/stub/RetroIface$Marker"), node.interfaces,
            "interface removed but membership must survive via the marker");
    }

    @Test
    public void supertypeMarkerNotDuplicatedWhenAlreadyDeclared() {
        TestProvider provider = provider();
        provider.classStubs.put(Type.getObjectType("com/example/RetroIface"),
            supertypeStub("com/example/RetroIface", "java/util/Collection", true));
        ClassNode node = classImplementing("com/example/RetroIface", "jvmdg/stub/RetroIface$Marker");

        provider.stubClasses(node, false, new HashSet<String>());

        assertEquals(Arrays.asList("java/util/Collection", "jvmdg/stub/RetroIface$Marker"), node.interfaces);
    }

    @Test
    public void supertypeSuperNameInjectsMarker() {
        TestProvider provider = provider();
        provider.classStubs.put(Type.getObjectType("com/example/RetroBase"),
            supertypeStub("com/example/RetroBase", "java/util/AbstractList", true));
        ClassNode node = classImplementing("java/io/Serializable");
        node.superName = "com/example/RetroBase";

        provider.stubClasses(node, false, new HashSet<String>());

        assertEquals("java/util/AbstractList", node.superName);
        assertTrue(node.interfaces.contains("jvmdg/stub/RetroBase$Marker"),
            "superclass remap still leaves the instanceof hole the marker covers");
    }

    @Test
    public void collidingMethodDescsAfterRemapThrow() {
        TestProvider provider = provider();
        provider.classStubs.put(Type.getObjectType("com/example/RetroIface"),
            supertypeStub("com/example/RetroIface", "java/util/Collection", true));
        ClassNode node = classImplementing();
        node.methods = new ArrayList<>(Arrays.asList(
            method("m", "(Lcom/example/RetroIface;)V"),
            method("m", "(Ljava/util/Collection;)V")));

        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> provider.stubClasses(node, false, new HashSet<String>()));
        assertTrue(e.getMessage().contains("com/example/Target.m"), e.getMessage());
    }

    @Test
    public void collidingFieldDescsAfterRemapThrow() {
        TestProvider provider = provider();
        provider.classStubs.put(Type.getObjectType("com/example/RetroIface"),
            supertypeStub("com/example/RetroIface", "java/util/Collection", true));
        ClassNode node = classImplementing();
        node.fields = new ArrayList<>(Arrays.asList(
            new org.objectweb.asm.tree.FieldNode(Opcodes.ACC_PUBLIC, "f", "Lcom/example/RetroIface;", null, null),
            new org.objectweb.asm.tree.FieldNode(Opcodes.ACC_PUBLIC, "f", "Ljava/util/Collection;", null, null)));

        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> provider.stubClasses(node, false, new HashSet<String>()));
        assertTrue(e.getMessage().contains("com/example/Target.f"), e.getMessage());
    }

    @Test
    public void distinctSourceMembersRemapDistinctly() {
        TestProvider provider = provider();
        provider.classStubs.put(Type.getObjectType("com/example/RetroIface"),
            supertypeStub("com/example/RetroIface", "java/util/Collection", true));
        ClassNode node = classImplementing();
        node.methods = new ArrayList<>(Arrays.asList(
            method("m", "(Lcom/example/RetroIface;)V"),
            method("n", "(Ljava/util/Collection;)V")));

        assertDoesNotThrow(() -> provider.stubClasses(node, false, new HashSet<String>()));
        assertEquals("(Ljava/util/Collection;)V", node.methods.get(0).desc);
    }

    private static org.objectweb.asm.tree.MethodNode method(String name, String desc) {
        return new org.objectweb.asm.tree.MethodNode(Opcodes.ACC_PUBLIC, name, desc, null, null);
    }

    // ---- gained-supertype interface injection ------------------------------

    /** registers {@code JdkExec.close ()V} as a dispatchable instance stub on {@code jvmdg/stub/Exec} */
    private static TestProvider providerWithExecStub() {
        TestProvider provider = provider();
        provider.stubRegistry.forOwner(Type.getObjectType("com/example/JdkExec")).methodStub.put(
            new xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc("close", Type.getMethodType("()V")),
            new xyz.wagyourtail.jvmdg.version.map.StubEntry(
                new xyz.wagyourtail.jvmdg.version.map.MethodRef(
                    new xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc(
                        Type.getObjectType("jvmdg/stub/Exec"), "close",
                        Type.getMethodType("(Lcom/example/JdkExec;)V")),
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, false, new int[0]),
                false, false, false, false, new String[0]));
        return provider;
    }

    /** Target's supertypes: JdkExec → Object; JdkExec's: Object; Object's: none */
    private static xyz.wagyourtail.jvmdg.util.IOFunction<Type, java.util.List<xyz.wagyourtail.jvmdg.util.Pair<Type, Boolean>>> supers() {
        return t -> {
            String n = t.getInternalName();
            if (n.equals("com/example/Target")) {
                return java.util.Collections.singletonList(
                    new xyz.wagyourtail.jvmdg.util.Pair<>(Type.getObjectType("com/example/JdkExec"), false));
            }
            if (n.equals("com/example/JdkExec")) {
                return java.util.Collections.singletonList(
                    new xyz.wagyourtail.jvmdg.util.Pair<>(Type.getObjectType("java/lang/Object"), false));
            }
            return java.util.Collections.emptyList();
        };
    }

    private static xyz.wagyourtail.jvmdg.util.IOFunction<Type, java.util.Set<xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc>> noMembers() {
        return t -> java.util.Collections.emptySet();
    }

    @Test
    public void noMarkerInjectionOnDeclaredMember() throws Exception {
        // dispatch is runtime-side now (the indy callsite resolves the
        // receiver's declared member) — no interface is injected for
        // override-preserving dispatch
        TestProvider provider = providerWithExecStub();
        ClassNode node = classImplementing();
        node.superName = "com/example/JdkExec";
        node.methods = new ArrayList<>(java.util.Collections.singletonList(method("close", "()V")));

        provider.stubClasses(node, false, noMembers(), supers(), new HashSet<String>());

        assertTrue(node.interfaces == null || node.interfaces.isEmpty(),
            "a declared override must not inject any marker interface");
    }

    // ---- gained-supertype interface injection ------------------------------

    /** seeds {@code com/example/FakeIface ← com/example/JdkExec} as a gained row crossing this boundary */
    private static TestProvider providerWithGained() {
        TestProvider provider = providerWithExecStub();
        provider.gained.computeIfAbsent("com/example/FakeIface", k -> new HashSet<String>())
            .add("com/example/JdkExec");
        return provider;
    }

    @Test
    public void gainedInterfaceInjectsOnClosureMember() throws Exception {
        TestProvider provider = providerWithGained();
        ClassNode node = classImplementing();
        node.superName = "com/example/JdkExec";

        provider.stubClasses(node, false, noMembers(), supers(), new HashSet<String>());

        assertTrue(node.interfaces.contains("com/example/FakeIface"),
            "a class whose supertype gained the interface must implement it");
    }

    @Test
    public void gainedInterfaceSkippedWithoutClosureMember() throws Exception {
        TestProvider provider = providerWithExecStub();
        // the gained row names a member not in Target's supertype closure
        provider.gained.computeIfAbsent("com/example/FakeIface", k -> new HashSet<String>())
            .add("com/example/Other");
        ClassNode node = classImplementing();
        node.superName = "com/example/JdkExec";

        provider.stubClasses(node, false, noMembers(), supers(), new HashSet<String>());

        assertFalse(node.interfaces.contains("com/example/FakeIface"),
            "no gained member in the closure means no injection");
    }

    @Test
    public void gainedInterfaceNotDuplicated() throws Exception {
        TestProvider provider = providerWithGained();
        ClassNode node = classImplementing("com/example/FakeIface");
        node.superName = "com/example/JdkExec";

        provider.stubClasses(node, false, noMembers(), supers(), new HashSet<String>());

        assertEquals(1, java.util.Collections.frequency(node.interfaces, "com/example/FakeIface"),
            "an already-implemented interface must not be injected twice");
    }

    @Test
    public void gainedInterfaceInjectsEvenWhenPresentInClosure() throws Exception {
        // the resolver reports the source-version hierarchy — the gained
        // interface may already appear in walked through the gained edge
        // itself (JdkExec "is" FakeIface at the authored version) and must
        // still be injected for the output where that edge doesn't exist
        TestProvider provider = providerWithGained();
        ClassNode node = classImplementing();
        node.superName = "com/example/JdkExec";

        xyz.wagyourtail.jvmdg.util.IOFunction<Type, java.util.List<xyz.wagyourtail.jvmdg.util.Pair<Type, Boolean>>> supersWithIface =
            t -> {
                String n = t.getInternalName();
                if (n.equals("com/example/Target")) {
                    return java.util.Collections.singletonList(
                        new xyz.wagyourtail.jvmdg.util.Pair<>(Type.getObjectType("com/example/JdkExec"), false));
                }
                if (n.equals("com/example/JdkExec")) {
                    return Arrays.asList(
                        new xyz.wagyourtail.jvmdg.util.Pair<>(Type.getObjectType("com/example/FakeIface"), true),
                        new xyz.wagyourtail.jvmdg.util.Pair<>(Type.getObjectType("java/lang/Object"), false));
                }
                return java.util.Collections.emptyList();
            };
        provider.stubClasses(node, false, noMembers(), supersWithIface, new HashSet<String>());

        assertTrue(node.interfaces.contains("com/example/FakeIface"),
            "the interface being in walked via the gained edge must not suppress injection");
    }

    // ---- derived noSpecial --------------------------------------------------

    private static StubEntry memberStub(String handlerOwner, String name, String desc, boolean noSpecial) {
        return new StubEntry(
            new MethodRef(
                new FullyQualifiedMemberNameAndDesc(
                    Type.getObjectType(handlerOwner), name, Type.getMethodType(desc)),
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, false, new int[0]),
            false, false, StubCondition.ALWAYS, false, noSpecial, new String[0]);
    }

    @Test
    public void ownInterfaceAdapterMemberSuppressesInvokespecial() throws Exception {
        // own-interface stand-ins carry real default bodies — X.super.m()
        // must keep native super-dispatch, so member stubs under an
        // OWN_INTERFACE adapter's stand-in owner suppress invokespecial
        // without a declared noSpecial flag
        TestProvider provider = provider();
        // the member owner is the stand-in type — the adapter class itself,
        // matching how real own-interface member stubs register
        Type owner = Type.getType(KeptIfaceStub.class);
        MemberNameAndDesc m = new MemberNameAndDesc("m", Type.getMethodType("()V"));
        provider.stubRegistry.forOwner(owner).methodStub.put(m,
            memberStub(Type.getInternalName(KeptIfaceStub.class), "m", "(Ljava/lang/Object;)V", false));
        ClassMapping mapping = provider.getStubMapper(owner, true, noMembers(), supers());

        assertNull(mapping.getStubFor(m, false, Opcodes.INVOKESPECIAL, new HashSet<String>()),
            "OWN_INTERFACE member keeps invokespecial native");
        assertNull(mapping.getStubFor(m, false, Opcodes.H_INVOKESPECIAL, new HashSet<String>()),
            "handle spelling takes the same path after tag normalization");
        assertNotNull(mapping.getStubFor(m, false, Opcodes.INVOKEVIRTUAL, new HashSet<String>()));
        assertNotNull(mapping.getStubFor(m, false, Opcodes.INVOKEINTERFACE, new HashSet<String>()));
    }

    @Test
    public void supertypeAdapterMemberDoesNotSuppressInvokespecial() throws Exception {
        TestProvider provider = provider();
        provider.classStubs.put(Type.getObjectType("com/example/RetroIface"),
            supertypeStub("com/example/RetroIface", "java/util/Collection", true));
        // SUPERTYPE members register under their adapter class too — same
        // shape as the own-interface case, distinguished only by mode
        Type owner = Type.getObjectType("jvmdg/stub/RetroIface");
        MemberNameAndDesc m = new MemberNameAndDesc("m", Type.getMethodType("()V"));
        provider.stubRegistry.forOwner(owner).methodStub.put(m,
            memberStub("jvmdg/stub/RetroIface", "m", "(Ljava/lang/Object;)V", false));
        ClassMapping mapping = provider.getStubMapper(owner, true, noMembers(), supers());

        assertNotNull(mapping.getStubFor(m, false, Opcodes.INVOKESPECIAL, new HashSet<String>()),
            "SUPERTYPE member reroutes invokespecial through the stub");

        // the explicit flag remains as the escape hatch for non-own-interface owners
        provider = provider();
        provider.stubRegistry.forOwner(owner).methodStub.put(m,
            memberStub("jvmdg/stub/RetroIface", "m", "(Ljava/lang/Object;)V", true));
        mapping = provider.getStubMapper(owner, true, noMembers(), supers());
        assertNull(mapping.getStubFor(m, false, Opcodes.INVOKESPECIAL, new HashSet<String>()),
            "declared noSpecial still suppresses");
    }
}
