package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.io.FileWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

public class J_I_FileWriter {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle seGetter;
    private static final MethodHandle encSetter;
    static {
        MethodHandle seGet = null;
        MethodHandle encSet = null;
        try {
            Class<?> se = Class.forName("sun.nio.cs.StreamEncoder");
            seGet = IMPL_LOOKUP.findGetter(FileWriter.class, "se", se);
            encSet = IMPL_LOOKUP.findSetter(se, "encoder", CharsetEncoder.class);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        seGetter = seGet;
        encSetter = encSet;
    }

    public static void setCharset(FileWriter fw, Charset charset) {
        try {
            Object se = seGetter.invoke(fw);
            encSetter.invoke(se, charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
        } catch (Throwable t) {
            Utils.sneakyThrow(t);
        }
    }

    @Modify(ref = @Ref(value = "java/io/FileWriter", member = "<init>", desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"))
    public static void init1(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        Type firstArgType = Type.getArgumentTypes(node.desc)[0];
        InsnList list = new InsnList();
        // stack: (U) FileWriter, String, Charset
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Charset, (U) FileWriter, String, Charset
        list.add(new InsnNode(Opcodes.POP));
        // stack: Charset, (U) FileWriter, String
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Charset, (U) FileWriter, String, (U) FileWriter, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileWriter", "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, firstArgType), false));
        // stack: Charset, FileWriter, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: Charset, FileWriter
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: FileWriter, Charset
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getType(J_I_FileWriter.class).getInternalName(), "setCharset", "(Ljava/io/FileWriter;Ljava/nio/charset/Charset;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "java/io/FileWriter", member = "<init>", desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;Z)V"))
    public static void init2(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        Type firstArgType = Type.getArgumentTypes(node.desc)[0];
        InsnList list = new InsnList();
        // stack: (U) FileWriter, String, Charset, boolean
        list.add(new InsnNode(Opcodes.DUP2_X2));
        // stack: Charset, boolean, (U) FileWriter, String, Charset, boolean
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: Charset, boolean, (U) FileWriter, String, boolean, Charset
        list.add(new InsnNode(Opcodes.POP));
        // stack: Charset, boolean, (U) FileWriter, String, boolean
        list.add(new InsnNode(Opcodes.DUP2_X1));
        // stack: Charset, boolean, String, boolean, (U) FileWriter, String, boolean
        list.add(new InsnNode(Opcodes.POP2));
        // stack: Charset, boolean, String, boolean, (U) FileWriter
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Charset, boolean, (U) FileWriter, String, boolean, (U) FileWriter
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Charset, boolean, (U) FileWriter, (U) FileWriter, String, boolean, (U) FileWriter
        list.add(new InsnNode(Opcodes.POP));
        // stack: Charset, boolean, (U) FileWriter, (U) FileWriter, String, boolean
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileWriter", "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, firstArgType, Type.BOOLEAN_TYPE), false));
        // stack: Charset, boolean, FileWriter
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: Charset, FileWriter, boolean
        list.add(new InsnNode(Opcodes.POP));
        // stack: Charset, FileWriter
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: FileWriter, Charset
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getType(J_I_FileWriter.class).getInternalName(), "setCharset", "(Ljava/io/FileWriter;Ljava/nio/charset/Charset;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "java/io/FileWriter", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"))
    public static void init3(MethodNode mnode, int i) {
        init1(mnode, i);
    }

    @Modify(ref = @Ref(value = "java/io/FileWriter", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;Z)V"))
    public static void init4(MethodNode mnode, int i) {
        init2(mnode, i);
    }

}
