package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

@Adapter("java/lang/reflect/InaccessibleObjectException")
public class J_L_R_InaccessibleObjectException extends RuntimeException {
    public J_L_R_InaccessibleObjectException() {}

    public J_L_R_InaccessibleObjectException(String msg) {
        super(msg);
    }
}
