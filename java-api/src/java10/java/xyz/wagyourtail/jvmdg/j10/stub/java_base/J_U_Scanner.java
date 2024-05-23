package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_U_Scanner {

    @Modify(ref = @Ref(value = "Ljava/util/Scanner;", member = "<init>", desc = "(Ljava/nio/channels/ReadableByteChannel;Ljava/nio/charset/Charset;)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: Scanner, ReadableByteChannel, Charset
        // charset.name
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/charset/Charset", "name", "()Ljava/lang/String;", false));
        // stack: Scanner, ReadableByteChannel, String
        // init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/nio/channels/ReadableByteChannel;Ljava/lang/String;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "Ljava/util/Scanner;", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"))
    public static void init2(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: Scanner, File, Charset
        // charset.name
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/charset/Charset", "name", "()Ljava/lang/String;", false));
        // stack: Scanner, File, String
        // init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/File;Ljava/lang/String;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "Ljava/util/Scanner;", member = "<init>", desc = "(Ljava/io/InputStream;Ljava/nio/charset/Charset;)V"))
    public static void init3(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: Scanner, InputStream, Charset
        // charset.name
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/charset/Charset", "name", "()Ljava/lang/String;", false));
        // stack: Scanner, InputStream, String
        // init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;Ljava/lang/String;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
