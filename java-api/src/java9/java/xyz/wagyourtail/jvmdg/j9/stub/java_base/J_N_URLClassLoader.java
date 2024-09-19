package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_N_URLClassLoader {

    @Modify(ref = @Ref(value = "java/net/URLClassLoader", member = "<init>", desc = "(Ljava/lang/String;[Ljava/net/URL;Ljava/lang/ClassLoader;)V"))
    public static void init1(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: URLClassLoader (U), String, URL[], ClassLoader
        list.add(new InsnNode(Opcodes.DUP2_X1));
        // stack: URLClassLoader (U), URL[], ClassLoader, String, URL[], ClassLoader
        list.add(new InsnNode(Opcodes.POP2));
        // stack: URLClassLoader (U), URL[], ClassLoader, String
        list.add(new InsnNode(Opcodes.DUP2_X2));
        // stack: ClassLoader, String, URLClassLoader (U), URL[], ClassLoader, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: ClassLoader, String, URLClassLoader (U), URL[], ClassLoader
        list.add(new InsnNode(Opcodes.DUP2_X1));
        // stack: ClassLoader, String, URL[], ClassLoader, URLClassLoader (U), URL[], ClassLoader
        list.add(new InsnNode(Opcodes.POP2));
        // stack: ClassLoader, String, URL[], ClassLoader, URLClassLoader (U)
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: ClassLoader, String, URLClassLoader (U), URL[], ClassLoader, URLClassLoader (U)
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: ClassLoader, String, URLClassLoader (U), URLClassLoader (U), URL[], ClassLoader, URLClassLoader (U)
        list.add(new InsnNode(Opcodes.POP));
        // stack: ClassLoader, String, URLClassLoader (U), URLClassLoader (U), URL[], ClassLoader
        // init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/net/URLClassLoader", "<init>", "([Ljava/net/URL;Ljava/lang/ClassLoader;)V", false));
        // stack: ClassLoader, String, URLClassLoader
        // call setName
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(J_L_ClassLoader.class), "setClassloaderName", "(Ljava/lang/String;Ljava/lang/ClassLoader;)V", false));
        // stack: ClassLoader
        list.add(new InsnNode(Opcodes.POP));
        // stack:

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "java/net/URLClassLoader", member = "<init>", desc = "(Ljava/lang/String;[Ljava/net/URL;Ljava/lang/ClassLoader;Ljava/net/URLStreamHandlerFactory;)V"))
    public static void init2(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: URLClassLoader (U), String, URL[], ClassLoader, URLStreamHandlerFactory

        int var = mnode.maxLocals++;
        list.add(new VarInsnNode(Opcodes.ASTORE, var));

        // stack: URLClassLoader (U), String, URL[], ClassLoader
        list.add(new InsnNode(Opcodes.DUP2_X1));
        // stack: URLClassLoader (U), URL[], ClassLoader, String, URL[], ClassLoader
        list.add(new InsnNode(Opcodes.POP2));
        // stack: URLClassLoader (U), URL[], ClassLoader, String
        list.add(new InsnNode(Opcodes.DUP2_X2));
        // stack: ClassLoader, String, URLClassLoader (U), URL[], ClassLoader, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: ClassLoader, String, URLClassLoader (U), URL[], ClassLoader
        list.add(new InsnNode(Opcodes.DUP2_X1));
        // stack: ClassLoader, String, URL[], ClassLoader, URLClassLoader (U), URL[], ClassLoader
        list.add(new InsnNode(Opcodes.POP2));
        // stack: ClassLoader, String, URL[], ClassLoader, URLClassLoader (U)
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: ClassLoader, String, URLClassLoader (U), URL[], ClassLoader, URLClassLoader (U)
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: ClassLoader, String, URLClassLoader (U), URLClassLoader (U), URL[], ClassLoader, URLClassLoader (U)
        list.add(new InsnNode(Opcodes.POP));
        // stack: ClassLoader, String, URLClassLoader (U), URLClassLoader (U), URL[], ClassLoader
        list.add(new VarInsnNode(Opcodes.ALOAD, var));
        // stack: ClassLoader, String, URLClassLoader (U), URLClassLoader (U), URL[], ClassLoader, URLStreamHandlerFactory
        // init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/net/URLClassLoader", "<init>", "([Ljava/net/URL;Ljava/lang/ClassLoader;Ljava/net/URLStreamHandlerFactory;)V", false));
        // stack: ClassLoader, String, URLClassLoader
        // call setName
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(J_L_ClassLoader.class), "setClassloaderName", "(Ljava/lang/String;Ljava/lang/ClassLoader;)V", false));
        // stack: ClassLoader
        list.add(new InsnNode(Opcodes.POP));
        // stack:

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
