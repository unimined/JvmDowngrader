package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_ClassLoader {

    // TODO: stub init's...

    @Stub
    public static String getName(ClassLoader classLoader) {
        // TODO: check if subclass actually overrides this method
        return null;
    }

    @Stub
    public static J_L_Module getUnnamedModule(ClassLoader classLoader) {
        return new J_L_Module(classLoader);
    }

    @Stub(ref = @Ref("Ljava/lang/ClassLoader;"))
    public static ClassLoader getPlatformClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

}
