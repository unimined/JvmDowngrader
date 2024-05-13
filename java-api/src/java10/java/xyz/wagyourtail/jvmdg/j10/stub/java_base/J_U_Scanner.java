package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_U_Scanner {

    @Modify(ref = @Ref(value = "Ljava/util/Scanner;", member = "<init>", desc = "(Ljava/nio/channels/ReadableByteChannel;Ljava/nio/charset/Charset;)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: Scanner, ReadableByteChannel, Charset
        // call:J_N_C_Channels.newReader(ReadableByteChannel, Charset)
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getType(J_N_C_Channels.class).getInternalName(), "newReader", "(Ljava/nio/channels/ReadableByteChannel;Ljava/nio/charset/Charset;)Ljava/io/Reader;", false));
        // stack: Scanner, Reader
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/lang/Readable;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
