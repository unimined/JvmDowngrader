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

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

public class J_I_FileReader {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle sdGetter;
    private static final MethodHandle decSetter;
    static {
        MethodHandle seGet = null;
        MethodHandle decSet = null;
        try {
            Class<?> sd = Class.forName("sun.nio.cs.StreamDecoder");
            seGet = IMPL_LOOKUP.findGetter(FileReader.class, "sd", sd);
            decSet = IMPL_LOOKUP.findSetter(sd, "decoder", CharsetDecoder.class);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        sdGetter = seGet;
        decSetter = decSet;
    }

    public static void setCharset(FileReader fw, Charset charset) {
        try {
            Object sd = sdGetter.invoke(fw);
            decSetter.invoke(sd, charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
        } catch (Throwable t) {
            Utils.sneakyThrow(t);
        }
    }

    @Modify(ref = @Ref(value = "java/io/FileReader", member = "<init>", desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"))
    public static void init1(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        Type firstArgType = Type.getArgumentTypes(node.desc)[0];
        InsnList list = new InsnList();
        // stack: (U) FileReader, String, Charset
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Charset, (U) FileReader, String, Charset
        list.add(new InsnNode(Opcodes.POP));
        // stack: Charset, (U) FileReader, String
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Charset, (U) FileReader, String, (U) FileReader, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileReader", "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, firstArgType), false));
        // stack: Charset, FileReader, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: Charset, FileReader
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: FileReader, Charset
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getType(J_I_FileReader.class).getInternalName(), "setCharset", "(Ljava/io/FileReader;Ljava/nio/charset/Charset;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }


    @Modify(ref = @Ref(value = "java/io/FileReader", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"))
    public static void init3(MethodNode mnode, int i) {
        init1(mnode, i);
    }

}
