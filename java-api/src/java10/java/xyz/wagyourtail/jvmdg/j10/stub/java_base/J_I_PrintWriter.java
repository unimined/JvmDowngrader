package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_I_PrintWriter {

    @Modify(ref = @Ref(value = "Ljava/io/PrintWriter;", member = "<init>", desc = "(Ljava/io/OutputStream;ZLjava/nio/charset/Charset;)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
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
        // stack: PrintWriter, (U) BufferedWriter, boolean, OutputStreamWriter, (U) BufferedWriter
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, (U) BufferedWriter, boolean, (U) BufferedWriter, OutputStreamWriter
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/BufferedWriter", "<init>", "(Ljava/io/Writer;)V", false));
        // stack: PrintWriter, BufferedWriter, boolean
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintWriter", "<init>", "(Ljava/io/Writer;Z)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "Ljava/io/PrintWriter;", member = "<init>", desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"))
    public static void init2(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: PrintWriter, String, Charset
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, Charset, String
        list.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileOutputStream"));
        // stack: PrintWriter, Charset, String, (U) FileOutputStream
        list.add(new InsnNode(Opcodes.DUP_X1));
        // stack: PrintWriter, Charset, (U) FileOutputStream, String, (U) FileOutputStream
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, Charset, (U) FileOutputStream, (U) FileOutputStream, String
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileOutputStream", "<init>", "(Ljava/lang/String;)V", false));
        // stack: PrintWriter, Charset, FileOutputStream
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, FileOutputStream, Charset
        list.add(new TypeInsnNode(Opcodes.NEW, "java/io/OutputStreamWriter"));
        // stack: PrintWriter, FileOutputStream, Charset, (U) OutputStreamWriter
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: PrintWriter, (U) OutputStreamWriter, FileOutputStream, Charset, (U) OutputStreamWriter
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: PrintWriter, (U) OutputStreamWriter, (U) OutputStreamWriter, FileOutputStream, Charset, (U) OutputStreamWriter
        list.add(new InsnNode(Opcodes.POP));
        // stack: PrintWriter, (U) OutputStreamWriter, (U) OutputStreamWriter, FileOutputStream, Charset
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/OutputStreamWriter", "<init>", "(Ljava/io/OutputStream;Ljava/nio/charset/Charset;)V", false));
        // stack: PrintWriter, OutputStreamWriter
        list.add(new TypeInsnNode(Opcodes.NEW, "java/io/BufferedWriter"));
        // stack: PrintWriter, OutputStreamWriter, (U) BufferedWriter
        list.add(new InsnNode(Opcodes.DUP_X1));
        // stack: PrintWriter, (U) BufferedWriter, OutputStreamWriter, (U) BufferedWriter
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, (U) BufferedWriter, (U) BufferedWriter, OutputStreamWriter
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/BufferedWriter", "<init>", "(Ljava/io/Writer;)V", false));
        // stack: PrintWriter, BufferedWriter
        list.add(new LdcInsnNode(1));
        // stack: PrintWriter, BufferedWriter, 1
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintWriter", "<init>", "(Ljava/io/Writer;Z)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "Ljava/io/PrintWriter;", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"))
    public static void init3(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: PrintWriter, File, Charset
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, Charset, File
        list.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileOutputStream"));
        // stack: PrintWriter, Charset, File, (U) FileOutputStream
        list.add(new InsnNode(Opcodes.DUP_X1));
        // stack: PrintWriter, Charset, (U) FileOutputStream, File, (U) FileOutputStream
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, Charset, (U) FileOutputStream, (U) FileOutputStream, File
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileOutputStream", "<init>", "(Ljava/io/File;)V", false));
        // stack: PrintWriter, Charset, FileOutputStream
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, FileOutputStream, Charset
        list.add(new TypeInsnNode(Opcodes.NEW, "java/io/OutputStreamWriter"));
        // stack: PrintWriter, FileOutputStream, Charset, (U) OutputStreamWriter
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: PrintWriter, (U) OutputStreamWriter, FileOutputStream, Charset, (U) OutputStreamWriter
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: PrintWriter, (U) OutputStreamWriter, (U) OutputStreamWriter, FileOutputStream, Charset, (U) OutputStreamWriter
        list.add(new InsnNode(Opcodes.POP));
        // stack: PrintWriter, (U) OutputStreamWriter, (U) OutputStreamWriter, FileOutputStream, Charset
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/OutputStreamWriter", "<init>", "(Ljava/io/OutputStream;Ljava/nio/charset/Charset;)V", false));
        // stack: PrintWriter, OutputStreamWriter
        list.add(new TypeInsnNode(Opcodes.NEW, "java/io/BufferedWriter"));
        // stack: PrintWriter, OutputStreamWriter, (U) BufferedWriter
        list.add(new InsnNode(Opcodes.DUP_X1));
        // stack: PrintWriter, (U) BufferedWriter, OutputStreamWriter, (U) BufferedWriter
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PrintWriter, (U) BufferedWriter, (U) BufferedWriter, OutputStreamWriter
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/BufferedWriter", "<init>", "(Ljava/io/Writer;)V", false));
        // stack: PrintWriter, BufferedWriter
        list.add(new LdcInsnNode(1));
        // stack: PrintWriter, BufferedWriter, 1
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintWriter", "<init>", "(Ljava/io/Writer;Z)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
