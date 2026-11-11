package xyz.wagyourtail.jvmdg.version.map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.junit.jupiter.api.Assertions.*;

public class TestMemberNameAndDesc {

    private static MemberNameAndDesc method(String name, String desc) {
        return new MemberNameAndDesc(name, Type.getMethodType(desc));
    }

    private static MemberNameAndDesc methodExact(String name, String desc) {
        return new MemberNameAndDesc(name, Type.getMethodType(desc), false);
    }

    @Test
    public void defaultKeysIgnoreReturnType() {
        // historical jvmdg semantics: same name + args match regardless of
        // return type (covariant returns, ctor stubs returning the instance)
        MemberNameAndDesc a = method("flip", "()Ljava/nio/Buffer;");
        MemberNameAndDesc b = method("flip", "()Ljava/nio/ByteBuffer;");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differingArgsNeverMatch() {
        MemberNameAndDesc a = method("position", "(I)Ljava/nio/Buffer;");
        MemberNameAndDesc b = method("position", "()Ljava/nio/Buffer;");
        assertNotEquals(a, b);
    }

    @Test
    public void differingNameNeverMatches() {
        assertNotEquals(method("flip", "()V"), method("limit", "()V"));
    }

    @Test
    public void exactKeysDistinguishReturnType() {
        MemberNameAndDesc a = methodExact("flip", "()Ljava/nio/Buffer;");
        MemberNameAndDesc b = methodExact("flip", "()Ljava/nio/ByteBuffer;");
        assertNotEquals(a, b);
        assertEquals(a, methodExact("flip", "()Ljava/nio/Buffer;"));
    }

    @Test
    public void mixedFlagsUseExactCompare() {
        // flagged vs exact: not both opted in, so full desc must match —
        // equal only when the descriptors are identical
        MemberNameAndDesc flagged = method("flip", "()Ljava/nio/Buffer;");
        assertEquals(flagged, methodExact("flip", "()Ljava/nio/Buffer;"));
        assertNotEquals(flagged, methodExact("flip", "()Ljava/nio/ByteBuffer;"));
    }

    @Test
    public void equalMixedFlagKeysHashAlike() {
        // equal objects must hash the same even when only one side is flagged
        MemberNameAndDesc flagged = method("flip", "()Ljava/nio/Buffer;");
        MemberNameAndDesc exact = methodExact("flip", "()Ljava/nio/Buffer;");
        assertEquals(flagged, exact);
        assertEquals(flagged.hashCode(), exact.hashCode());
    }

    @Test
    public void ctorStubsMatchCallSiteVoidReturn() {
        // stub impls return the constructed instance; the call site is
        // <init>(...)V — they must match like they always did
        MemberNameAndDesc stubImpl = method("<init>", "(Ljava/lang/String;)Ljava/io/InvalidClassException;");
        MemberNameAndDesc callSite = method("<init>", "(Ljava/lang/String;)V");
        assertEquals(stubImpl, callSite);
        assertEquals(stubImpl.hashCode(), callSite.hashCode());
    }

    @Test
    public void fieldVsMethodDoesNotThrowOrMatch() {
        MemberNameAndDesc field = new MemberNameAndDesc("x", Type.getType("I"));
        MemberNameAndDesc m = method("x", "()I");
        assertNotEquals(field, m);
        assertNotEquals(m, field);
        assertTrue(field.isField());
        assertFalse(field.isMethod());
    }

    @Test
    public void fieldsUseExactDesc() {
        MemberNameAndDesc a = new MemberNameAndDesc("x", Type.getType("I"));
        MemberNameAndDesc b = new MemberNameAndDesc("x", Type.getType("J"));
        assertNotEquals(a, b);
        assertEquals(a, new MemberNameAndDesc("x", Type.getType("I")));
    }

    @Test
    public void ignoreReturnValueIsIdempotentAndFieldSafe() {
        MemberNameAndDesc exact = methodExact("m", "()V");
        MemberNameAndDesc flagged = exact.ignoreReturnValue();
        assertTrue(flagged.isIgnoreReturnValue());
        assertEquals(flagged, method("m", "()I"));
        // fields can't ignore a return type — flag is a no-op
        MemberNameAndDesc field = new MemberNameAndDesc("x", Type.getType("I"), false);
        assertSame(field, field.ignoreReturnValue());
    }

    @Test
    public void toMemberNameAndDescKeepsDefaultSemantics() {
        FullyQualifiedMemberNameAndDesc fqn = new FullyQualifiedMemberNameAndDesc(
            Type.getObjectType("java/nio/ByteBuffer"), "flip", Type.getMethodType("()Ljava/nio/ByteBuffer;"));
        MemberNameAndDesc m = fqn.toMemberNameAndDesc();
        assertEquals(method("flip", "()Ljava/nio/Buffer;"), m);
    }

}
