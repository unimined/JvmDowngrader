package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_ClassLoader {

    @Stub(javaVersion = Opcodes.V9, subtypes = true)
    public static String getName(ClassLoader classLoader) {
        // TODO: check if subclass actually overrides this method
        return null;
    }

    @Stub(javaVersion = Opcodes.V9, subtypes = true)
    public static J_L_Module getUnnamedModule(ClassLoader classLoader) {
        return new J_L_Module(classLoader);
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/lang/ClassLoader;"))
    public static ClassLoader getPlatformClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

}
