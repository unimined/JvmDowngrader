package xyz.wagyourtail.jvmdg.j16.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.ref.Reference;

public class J_L_R_Reference {

    @Stub
    public static boolean refersTo(Reference<?> ref, Object o) {
        return ref.get() == o;
    }

}
