package xyz.wagyourtail.jvmdg.version.map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link DispatchSites} shape predicates + the emitted callsite:
 * instance-stub detection gates the {@code invokedynamic} route, and the
 * emitted site carries the member's MethodType + impl Handle as bootstrap
 * args the {@code DispatchRuntime} PIC consumes.
 */
public class TestDispatchSites {

    private static StubEntry entry(String owner, String name, String desc,
                                   boolean downgradeVersion, boolean abstractDefault) {
        return entry(owner, name, desc, downgradeVersion, false, abstractDefault);
    }

    private static StubEntry entry(String owner, String name, String desc,
                                   boolean downgradeVersion, boolean callerClass, boolean abstractDefault) {
        return new StubEntry(
            new MethodRef(
                new FullyQualifiedMemberNameAndDesc(
                    Type.getObjectType(owner), name, Type.getMethodType(desc)),
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, false, new int[0]),
            downgradeVersion, callerClass, false, abstractDefault, false, new String[0]);
    }

    private static MemberNameAndDesc member(String name, String desc) {
        return new MemberNameAndDesc(name, Type.getMethodType(desc));
    }

    @Test
    public void instanceShapeRequiresSelfSlot() {
        // self-first handler: (Owner, args...) with ref arg0
        assertTrue(DispatchSites.isInstanceStub(
            entry("x/S", "close", "(Lcom/e/Exec;)V", false, false),
            member("close", "()V")));
        assertTrue(DispatchSites.isInstanceStub(
            entry("x/S", "m", "(Lcom/e/O;I)Ljava/lang/Object;", false, false),
            member("m", "(I)Ljava/lang/Object;")));
        // downgradeVersion trailing int counts as the extra arg
        assertTrue(DispatchSites.isInstanceStub(
            entry("x/S", "m", "(Lcom/e/O;I)V", true, false),
            member("m", "()V")));
        // callerClass trailing Class counts the same way — both extra slots
        // sit between the member args and the end of the handler signature
        assertTrue(DispatchSites.isInstanceStub(
            entry("x/S", "m", "(Lcom/e/O;ILjava/lang/Class;I)V", true, true, false),
            member("m", "(I)V")));
        assertTrue(DispatchSites.isInstanceStub(
            entry("x/S", "m", "(Lcom/e/O;Ljava/lang/Class;)V", false, true, false),
            member("m", "()V")));
    }

    @Test
    public void nonInstanceShapesRejected() {
        // static stub: handler args == member args
        assertFalse(DispatchSites.isInstanceStub(
            entry("x/S", "of", "(Ljava/util/Collection;)Ljava/util/List;", false, false),
            member("of", "(Ljava/util/Collection;)Ljava/util/List;")));
        // abstractDefault is the @DefaultImpl machinery, not a callsite stub
        assertFalse(DispatchSites.isInstanceStub(
            entry("x/S", "close", "(Lcom/e/Exec;)V", false, true),
            member("close", "()V")));
        // arg0 primitive can't be a receiver
        assertFalse(DispatchSites.isInstanceStub(
            entry("x/S", "m", "(II)V", false, false),
            member("m", "(I)V")));
        // handler arg count unrelated to member shape
        assertFalse(DispatchSites.isInstanceStub(
            entry("x/S", "m", "(Lcom/e/O;II)V", false, false),
            member("m", "()V")));
    }

    @Test
    public void emittedSiteCarriesMemberAndImpl() {
        MemberNameAndDesc member = member("close", "()V");
        MethodRef handler = entry("x/S", "close", "(Lcom/e/Exec;)V", false, false).handler;
        TypeRemap identity = new TypeRemap() {
            @Override
            public Type remap(Type type, java.util.Set<String> warnings) {
                return type;
            }

            @Override
            public ClassStubEntry stubFor(Type type) {
                return null;
            }

            @Override
            public void warnUnstubbed(Type type, java.util.Set<String> warnings) {
            }
        };
        InvokeDynamicInsnNode indy = DispatchSites.insn(member, handler, identity,
            new java.util.HashSet<>());

        assertEquals("close", indy.name, "the indy name is the member name");
        assertEquals("(Lcom/e/Exec;)V", indy.desc,
            "the callsite desc is the self-first handler desc");
        assertSame(DispatchSites.DISPATCH_BSM, indy.bsm);
        assertEquals(3, indy.bsmArgs.length);
        assertEquals(Type.getMethodType("()V"), indy.bsmArgs[0],
            "bsm arg 0 is the member's own MethodType");
        assertEquals(Type.getObjectType("x/S"), indy.bsmArgs[1],
            "bsm arg 1 is the impl owner's class");
        assertEquals("close (Lcom/e/Exec;)V", indy.bsmArgs[2],
            "bsm arg 2 is the impl 'name desc' in registered vocabulary");
    }
}
