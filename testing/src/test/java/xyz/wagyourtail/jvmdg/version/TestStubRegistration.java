package xyz.wagyourtail.jvmdg.version;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stub registration must be resilient: a broken member logs an aggregated
 * error summary but does not abort registration of the remaining members.
 */
public class TestStubRegistration {

    @Adapter(value = "com/example/Target")
    static class MixedStub {
        @Stub(ref = @Ref(value = "Lcom/example/Target;", member = "size", desc = "()I"))
        public static int size(Object target) {
            return 0;
        }

        @Modify(ref = @Ref(value = "Lcom/example/Target;", member = "broken", desc = "()V"))
        public static void broken(String invalidParam) {
            // String is not in Modify.MODIFY_SIG — must fail registration
        }
    }

    @Adapter(value = "com/example/Iface")
    static class DefaultImplStub {
        @DefaultImpl(ref = @Ref(value = "Lcom/example/Iface;", member = "size", desc = "()I"))
        public static int size(Object self) {
            return 0;
        }

        @DefaultImpl(ref = @Ref(value = "Lcom/example/Iface;", member = "<init>", desc = "()V"))
        public static void badCtor(Object self) {
            // <init> default impls are meaningless — must fail registration
        }
    }

    @Adapter(value = "com/example/Iface")
    static class ExplicitRefDefaultImplStub {
        @DefaultImpl(ref = @Ref(value = "com/example/Iface"))
        public static void doThing(Object self, String arg) {
            // self is the receiver — an explicit-owner auto-derived desc must
            // not register it as a real parameter
        }
    }

    @Adapter("")
    static class EmptyAdapter {
    }

    @Adapter(value = "com/example/Target")
    static class ValidModifyStub {
        @Modify(ref = @Ref(value = "Lcom/example/Target;", member = "hook", desc = "()V"))
        public static void hook(MethodNode method, int index, ClassNode classNode, Set extra, boolean noSynthetic) {
        }
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

    private static TestProvider provider(ClassDowngrader downgrader) {
        TestProvider provider = new TestProvider();
        provider.ensureInit(downgrader);
        return provider;
    }

    @Test
    public void brokenMemberDoesNotAbortOtherRegistrations() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider provider = provider(d);
            assertDoesNotThrow(() -> provider.stub(MixedStub.class),
                "one bad member must not fail the whole stub class");

            // the valid member is still registered
            assertTrue(provider.stubMappings.containsKey(Type.getObjectType("com/example/Target")));
            assertTrue(provider.stubMappings.get(Type.getObjectType("com/example/Target"))
                    .getMethodStubMap().containsKey(new MemberNameAndDesc("size", Type.getMethodType("()I"))),
                "valid @Stub member should register despite the broken @Modify");
        }
    }

    @Test
    public void defaultImplRegistersAsAbstractDefaultEntry() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider provider = provider(d);
            provider.stub(DefaultImplStub.class);
            Map<MemberNameAndDesc, StubEntry> stubs = provider.stubMappings
                .get(Type.getObjectType("com/example/Iface")).getMethodStubMap();
            MemberNameAndDesc key = new MemberNameAndDesc("size", Type.getMethodType("()I"));
            assertTrue(stubs.containsKey(key), "@DefaultImpl must register the interface member");
            assertTrue(stubs.get(key).abstractDefault,
                "@DefaultImpl entries must carry abstractDefault=true for insertAbstractMethods");
            assertFalse(stubs.containsKey(new MemberNameAndDesc("<init>", Type.getMethodType("()V"))),
                "@DefaultImpl on <init> must be rejected");
        }
    }

    @Test
    public void explicitRefDefaultImplInsertsCorrectArityBridge() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider provider = provider(d);
            provider.stub(ExplicitRefDefaultImplStub.class);

            Type iface = Type.getObjectType("com/example/Iface");
            MemberNameAndDesc callsiteKey = new MemberNameAndDesc("doThing", Type.getMethodType("(Ljava/lang/String;)V"));
            assertTrue(provider.stubMappings.get(iface).getMethodStubMap().containsKey(callsiteKey),
                "explicit-owner @DefaultImpl must register the callsite desc, receiver stripped");
            assertFalse(provider.stubMappings.get(iface).getMethodStubMap()
                    .containsKey(new MemberNameAndDesc("doThing", Type.getMethodType("(Ljava/lang/Object;Ljava/lang/String;)V"))),
                "the handler signature (receiver in arg0) must not become the member desc");

            // an implementor lacking the method gets a bridge that actually
            // implements the interface member, not a wrong-arity overload
            ClassNode impl = new ClassNode(Opcodes.ASM9);
            impl.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "com/example/Impl",
                null, "java/lang/Object", new String[]{"com/example/Iface"});

            IOFunction<Type, Set<MemberNameAndDesc>> memberResolver = new IOFunction<Type, Set<MemberNameAndDesc>>() {
                @Override
                public Set<MemberNameAndDesc> apply(Type t) {
                    return new HashSet<>();
                }
            };
            IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver = new IOFunction<Type, List<Pair<Type, Boolean>>>() {
                @Override
                public List<Pair<Type, Boolean>> apply(Type t) {
                    return t.equals(Type.getObjectType("com/example/Impl"))
                        ? Collections.singletonList(new Pair<>(iface, true))
                        : Collections.<Pair<Type, Boolean>>emptyList();
                }
            };
            provider.insertAbstractMethods(impl, new HashSet<ClassNode>(), memberResolver, superTypeResolver);

            MethodNode bridge = null;
            for (MethodNode m : impl.methods) {
                if (m.name.equals("doThing")) bridge = m;
            }
            assertNotNull(bridge, "implementor must receive the default bridge");
            assertEquals("(Ljava/lang/String;)V", bridge.desc,
                "bridge must implement the interface signature — receiver-in-desc produced an uncallable overload");

            MethodInsnNode call = null;
            for (AbstractInsnNode insn : bridge.instructions) {
                if (insn instanceof MethodInsnNode) call = (MethodInsnNode) insn;
            }
            assertNotNull(call, "bridge body must invokestatic the stub handler");
            assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
            assertEquals("(Ljava/lang/Object;Ljava/lang/String;)V", call.desc,
                "invokestatic must use the handler signature — receiver as arg0");
        }
    }

    @Test
    public void classLevelFailureIsCollectedNotThrown() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider provider = provider(d);
            assertDoesNotThrow(() -> provider.stub(EmptyAdapter.class),
                "a failing @Adapter must log an error summary, not propagate");
        }
    }

    @Test
    public void validModifySignatureStillRegisters() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider provider = provider(d);
            provider.stub(ValidModifyStub.class);
            assertTrue(provider.stubMappings.get(Type.getObjectType("com/example/Target"))
                    .getMethodModifyMap().containsKey(new MemberNameAndDesc("hook", Type.getMethodType("()V"))),
                "a well-formed @Modify must register");
        }
    }
}
