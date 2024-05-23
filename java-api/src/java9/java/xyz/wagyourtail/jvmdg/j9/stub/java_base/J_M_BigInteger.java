package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.math.BigInteger;


public class J_M_BigInteger {


    @Stub(ref = @Ref("java/lang/Integer"))
    public static final BigInteger TWO = BigInteger.valueOf(2);
    static final long LONG_MASK = 0xffffffffL;

    @Modify(ref = @Ref(value = "Ljava/math/BigInteger;", member = "<init>", desc = "([BII)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();

        // stack: BigInteger, byte[], (start) int, (len) int
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: BigInteger, byte[], (start) int, (len) int, (start) int, (len) int
        list.add(new InsnNode(Opcodes.IADD));
        // stack: BigInteger, byte[],(start) int, (len) int, (start+len) int
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: BigInteger, byte[], (start) int, (start+len) int, (len) int
        list.add(new InsnNode(Opcodes.POP));
        // stack: BigInteger, byte[], (start) int, (start+len) int
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Arrays", "copyOfRange", "([BII)[B", false));
        // stack: BigInteger, byte[]
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/math/BigInteger", "<init>", "([B)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "Ljava/math/BigInteger;", member = "<init>", desc = "(I[BII)V"))
    public static void init2(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();

        // stack: BigInteger, int, byte[], (start) int, (len) int
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: BigInteger, int, byte[], (start) int, (len) int, (start) int, (len) int
        list.add(new InsnNode(Opcodes.IADD));
        // stack: BigInteger, int, byte[], (start) int, (len) int, (start+len) int
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: BigInteger, int, byte[], (start) int, (start+len) int, (len) int
        list.add(new InsnNode(Opcodes.POP));
        // stack: BigInteger, int, byte[], (start) int, (start+len) int
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Arrays", "copyOfRange", "([BII)[B", false));
        // stack: BigInteger, int, byte[]
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/math/BigInteger", "<init>", "(I[B)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Stub
    public static BigInteger sqrt(BigInteger x) {
        // Special cases.
        // is zero
        if (x.signum() == 0) {
            return BigInteger.ZERO;
        } else if (x.signum() < 0) {
            throw new ArithmeticException("square root of negative number");
        } else if (x.equals(BigInteger.ONE)) {
            return BigInteger.ONE;
        }

        BigInteger div = BigInteger.ZERO.setBit(x.bitLength() / 2);
        BigInteger div2 = div;
        // Loop until we hit the same value twice in a row, or wind
        // up alternating.
        for (; ; ) {
            BigInteger y = div.add(x.divide(div)).shiftRight(1);
            if (y.equals(div) || y.equals(div2))
                return y;
            div2 = div;
            div = y;
        }
    }

    @Stub
    public static BigInteger[] sqrtAndRemainder(BigInteger x) {
        BigInteger s = sqrt(x);
        BigInteger r = x.subtract(s.multiply(s));
        return new BigInteger[]{s, r};
    }

}
