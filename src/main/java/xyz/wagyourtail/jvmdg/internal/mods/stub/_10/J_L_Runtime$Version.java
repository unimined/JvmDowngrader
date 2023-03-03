package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_Runtime$Version {

    @Stub(JavaVersion.VERSION_1_10)
    public static int feature(Runtime.Version version) {
        return version.version().get(0);
    }

    @Stub(JavaVersion.VERSION_1_10)
    public static int interim(Runtime.Version version) {
        return version.version().size() > 1 ? version.version().get(1) : 0;
    }

    @Stub(JavaVersion.VERSION_1_10)
    public static int update(Runtime.Version version) {
        return version.version().size() > 2 ? version.version().get(2) : 0;
    }

    @Stub(JavaVersion.VERSION_1_10)
    public static int patch(Runtime.Version version) {
        return version.version().size() > 3 ? version.version().get(3) : 0;
    }

}
