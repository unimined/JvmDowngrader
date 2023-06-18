package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_Class {

    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/lang/Class;"))
    public static Class<?> forName(String name, J_L_Module module) throws ClassNotFoundException {
        return Class.forName(name, true, module.getClassLoader());
    }

    @Stub(opcVers = Opcodes.V9)
    public static J_L_Module getModule(Class<?> clazz) {
        return new J_L_Module(clazz.getClassLoader());
    }

    @Stub(opcVers = Opcodes.V9)
    public static String getPackageName(Class<?> clazz) {
        String name = clazz.getName();
        int lastDot = name.lastIndexOf('.');
        if (clazz.isPrimitive() || clazz == void.class) {
            return "java.lang";
        }
        if (clazz.isArray()) {
            return getPackageName(clazz.getComponentType());
        }
        if (lastDot == -1) {
            return "";
        }
        return name.substring(0, lastDot);
    }

}
