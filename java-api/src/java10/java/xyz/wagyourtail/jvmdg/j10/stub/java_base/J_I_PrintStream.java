package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_I_PrintStream {

    @Modify(ref = @Ref(value = "Ljava/io/PrintStream;", member = "<init>", desc = "(Ljava/io/OutputStream;ZLjava/nio/charset/Charset;)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: PrintStream, OutputStream, boolean, Charset
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/charset/Charset", "name", "()Ljava/lang/String;", false));
        // stack: PrintStream, OutputStream, boolean, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "<init>", "(Ljava/io/OutputStream;ZLjava/lang/String;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "Ljava/io/PrintStream;", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"))
    public static void init2(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: PrintStream, File, Charset
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/charset/Charset", "name", "()Ljava/lang/String;", false));
        // stack: PrintStream, File, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "<init>", "(Ljava/io/File;Ljava/lang/String;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
