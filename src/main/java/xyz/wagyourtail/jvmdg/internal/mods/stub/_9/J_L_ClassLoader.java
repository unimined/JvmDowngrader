package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_ClassLoader {

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
    public static String getName(ClassLoader classLoader) {
        // TODO: check if subclass actually overrides this method
        return null;
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
    public static J_L_Module getUnnamedModule(ClassLoader classLoader) {
        return new J_L_Module(classLoader);
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/ClassLoader;")
    public static ClassLoader getPlatformClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }
}
