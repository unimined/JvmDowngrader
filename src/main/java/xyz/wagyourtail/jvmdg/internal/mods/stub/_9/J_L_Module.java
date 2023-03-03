package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

@Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/Module;")
public class J_L_Module {

    private final ClassLoader classLoader;

    public J_L_Module() {
        // TODO: determine better way to get classloader, caller.getClass().getClassLoader()?
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public J_L_Module(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof J_L_Module) {
            return ((J_L_Module) obj).classLoader == this.classLoader;
        }
        return false;
    }

}
