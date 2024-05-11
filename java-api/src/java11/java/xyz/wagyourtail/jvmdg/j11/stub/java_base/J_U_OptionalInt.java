package xyz.wagyourtail.jvmdg.j11.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.OptionalInt;

public class J_U_OptionalInt {

    @Stub
    public static boolean isEmpty(OptionalInt optional) {
        return !optional.isPresent();
    }

}
