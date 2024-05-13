package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_I_PrintWriter {

    @Modify(ref = @Ref(value = "Ljava/io/PrintWriter;", member = "<init>", desc = "(Ljava/io/OutputStream;ZLjava/nio/charset/Charset;)V"))
    public static void init(MethodNode mnode, int i) {
        AbstractInsnNode node = mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: PrintWriter, OutputStream, boolean, Charset
        list.add(new InsnNode(Opcodes.DUP2_X1));
        // stack: PrintWriter, boolean, Charset, OutputStream, boolean, Charset
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, boolean, Charset, OutputStream, Charset, boolean
        list.add(new InsnNode(Opcodes.POP));
        // stack: PrintWriter, boolean, Charset, OutputStream, Charset
        list.add(new TypeInsnNode(Opcodes.NEW, "java/io/OutputStreamWriter"));
        // stack: PrintWriter, boolean, Charset, OutputStream, Charset, (U) OutputStreamWriter
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: PrintWriter, boolean, Charset, (U) OutputStreamWriter, OutputStream, Charset, (U) OutputStreamWriter
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: PrintWriter, boolean, Charset, (U) OutputStreamWriter, (U) OutputStreamWriter, OutputStream, Charset, (U) OutputStreamWriter
        list.add(new InsnNode(Opcodes.POP));
        // stack: PrintWriter, boolean, Charset, (U) OutputStreamWriter, (U) OutputStreamWriter, OutputStream, Charset
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/OutputStreamWriter", "<init>", "(Ljava/io/OutputStream;Ljava/nio/charset/Charset;)V", false));
        // stack: PrintWriter, boolean, Charset, OutputStreamWriter
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, boolean, OutputStreamWriter, Charset
        list.add(new InsnNode(Opcodes.POP));
        // stack: PrintWriter, boolean, OutputStreamWriter
        list.add(new TypeInsnNode(Opcodes.NEW, "java/io/BufferedWriter"));
        // stack: PrintWriter, boolean, OutputStreamWriter, (U) BufferedWriter
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: PrintWriter, boolean, (U) BufferedWriter, OutputStreamWriter, (U) BufferedWriter
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, boolean, (U) BufferedWriter, (U) BufferedWriter, OutputStreamWriter
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/BufferedWriter", "<init>", "(Ljava/io/Writer;)V", false));
        // stack: PrintWriter, boolean, BufferedWriter
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, BufferedWriter, boolean
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintWriter", "<init>", "(Ljava/io/Writer;Z)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
