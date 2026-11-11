package xyz.wagyourtail.jvmdg.version.map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class TestStubRegistry {

    private static final Type OWNER_A = Type.getObjectType("java/util/Optional");
    private static final Type OWNER_B = Type.getObjectType("java/nio/ByteBuffer");

    @Test
    public void ownerIsNullBeforeRegistration() {
        StubRegistry registry = new StubRegistry();
        assertNull(registry.owner(OWNER_A));
    }

    @Test
    public void forOwnerCreatesAndReusesEntry() {
        StubRegistry registry = new StubRegistry();
        StubRegistry.Owner owner = registry.forOwner(OWNER_A);
        assertNotNull(owner);
        assertSame(owner, registry.forOwner(OWNER_A));
        assertSame(owner, registry.owner(OWNER_A));
    }

    @Test
    public void ownersAreIsolatedPerType() {
        StubRegistry registry = new StubRegistry();
        StubRegistry.Owner a = registry.forOwner(OWNER_A);
        assertNull(registry.owner(OWNER_B));
        StubRegistry.Owner b = registry.forOwner(OWNER_B);
        assertNotSame(a, b);
        assertSame(a, registry.owner(OWNER_A));
    }

    @Test
    public void wildcardModifyUsesNullKey() throws Exception {
        // class-level @Modify (Ref.member unset) lands on the null slot and
        // catches every member insn on the owner — VarHandle is the live user
        StubRegistry registry = new StubRegistry();
        StubRegistry.Owner owner = registry.forOwner(OWNER_A);
        Method m = getClass().getDeclaredMethod("wildcardProbe");
        owner.memberModify.put(null, m);
        assertTrue(owner.memberModify.containsKey(null));
        assertSame(m, owner.memberModify.get(null));
    }

    @SuppressWarnings("unused")
    private static void wildcardProbe() {
    }

    @Test
    public void entriesDoNotLeakAcrossOwners() {
        StubRegistry registry = new StubRegistry();
        registry.forOwner(OWNER_A).methodStub.put(
            new MemberNameAndDesc("get", Type.getMethodType("()Ljava/lang/Object;")),
            new StubEntry(null, false, false, false, false, new String[0]));
        assertTrue(registry.forOwner(OWNER_B).methodStub.isEmpty());
        assertEquals(1, registry.owner(OWNER_A).methodStub.size());
    }

}
