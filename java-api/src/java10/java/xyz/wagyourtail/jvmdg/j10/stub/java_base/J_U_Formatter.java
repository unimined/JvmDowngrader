package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_U_Formatter {

    @Modify(ref = @Ref(value = "java/util/Formatter", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;Ljava/util/Locale;)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: Formatter, File, Charset, Locale
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: Formatter, File, Locale, Charset
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/charset/Charset", "name", "()Ljava/lang/String;", false));
        // stack: Formatter, File, Locale, String
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: Formatter, File, String, Locale
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Formatter", "<init>", "(Ljava/io/File;Ljava/lang/String;Ljava/util/Locale;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "java/util/Formatter", member = "<init>", desc = "(Ljava/io/OutputStream;Ljava/nio/charset/Charset;Ljava/util/Locale;)V"))
    public static void init2(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: Formatter, OutputStream, Charset, Locale
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: Formatter, OutputStream, Locale, Charset
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/charset/Charset", "name", "()Ljava/lang/String;", false));
        // stack: Formatter, OutputStream, Locale, String
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: Formatter, OutputStream, String, Locale
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Formatter", "<init>", "(Ljava/io/OutputStream;Ljava/lang/String;Ljava/util/Locale;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "java/util/Formatter", member = "<init>", desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;Ljava/util/Locale;)V"))
    public static void init3(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: Formatter, String, Charset, Locale
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: Formatter, String, Locale, Charset
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/charset/Charset", "name", "()Ljava/lang/String;", false));
        // stack: Formatter, String, Locale, String
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: Formatter, String, String, Locale
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Formatter", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/util/Locale;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
