package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

@Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/lang/Module;"))
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
