package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class J_L_ClassLoader {
    private static final Map<ClassLoader, String> nameMap = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ClassLoader, J_L_Module> unnamedModuleMap = Collections.synchronizedMap(new WeakHashMap<>());

    @Modify(ref = @Ref(value = "Ljava/lang/ClassLoader;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/ClassLoader;)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: ClassLoader (U), String, ClassLoader
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: ClassLoader (U), ClassLoader, String
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: String, ClassLoader (U), ClassLoader, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: String, ClassLoader (U), ClassLoader
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: String, ClassLoader (U), ClassLoader, ClassLoader (U), ClassLoader
        // init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/ClassLoader", "<init>", "(Ljava/lang/ClassLoader;)V", false));
        // stack: String, ClassLoader, ClassLoader
        list.add(new InsnNode(Opcodes.POP));
        // stack: String, ClassLoader
        // call setName
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(J_L_ClassLoader.class), "setClassloaderName", "(Ljava/lang/String;Ljava/lang/ClassLoader;)V", false));
        // stack:

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    public static void setClassloaderName(String name, ClassLoader classLoader) {
        if (name != null && name.isEmpty()) {
            throw new IllegalArgumentException("name must be non-empty or null");
        }
        nameMap.put(classLoader, name);
    }

    @Stub
    public static String getName(ClassLoader classLoader) {
        // TODO: check if subclass actually overrides this method
        return nameMap.get(classLoader);
    }

    @Stub
    public static J_L_Module getUnnamedModule(ClassLoader classLoader) {
        return unnamedModuleMap.computeIfAbsent(classLoader, J_L_Module::new);
    }

    @Stub(ref = @Ref("Ljava/lang/ClassLoader;"))
    public static ClassLoader getPlatformClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

}
