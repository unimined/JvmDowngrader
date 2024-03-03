package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Runtime$Version {

    @Stub
    public static int feature(Runtime.Version version) {
        return version.version().get(0);
    }

    @Stub
    public static int interim(Runtime.Version version) {
        return version.version().size() > 1 ? version.version().get(1) : 0;
    }

    @Stub
    public static int update(Runtime.Version version) {
        return version.version().size() > 2 ? version.version().get(2) : 0;
    }

    @Stub
    public static int patch(Runtime.Version version) {
        return version.version().size() > 3 ? version.version().get(3) : 0;
    }

}
