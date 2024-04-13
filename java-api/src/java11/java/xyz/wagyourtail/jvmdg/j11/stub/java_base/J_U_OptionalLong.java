package xyz.wagyourtail.jvmdg.j11.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.OptionalLong;

public class J_U_OptionalLong {

    @Stub
    public static boolean isEmpty(OptionalLong optional) {
        return !optional.isPresent();
    }

}
