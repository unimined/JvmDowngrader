package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

@Adapter("java/lang/IllegalCallerException")
public class J_L_IllegalCallerException extends RuntimeException {
    public J_L_IllegalCallerException() {
        super();
    }

    public J_L_IllegalCallerException(String s) {
        super(s);
    }

    public J_L_IllegalCallerException(Throwable cause) {
        super(cause);
    }

    public J_L_IllegalCallerException(String message, Throwable cause) {
        super(message, cause);
    }

}
