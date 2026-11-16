package xyz.wagyourtail.jvmdg.version;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

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
