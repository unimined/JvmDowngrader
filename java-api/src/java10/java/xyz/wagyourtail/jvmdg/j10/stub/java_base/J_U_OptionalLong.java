package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.NoSuchElementException;
import java.util.OptionalLong;

public class J_U_OptionalLong {

    @Stub
    public static long orElseThrow(OptionalLong optional) {
        if (optional.isPresent()) {
            return optional.getAsLong();
        }
        throw new NoSuchElementException("No value present");
    }

}
