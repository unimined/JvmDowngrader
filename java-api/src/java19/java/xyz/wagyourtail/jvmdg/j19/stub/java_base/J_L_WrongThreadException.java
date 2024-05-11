package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

@Adapter("java/lang/WrongThreadException")
public class J_L_WrongThreadException extends RuntimeException {

    public J_L_WrongThreadException() {
        super();
    }

    public J_L_WrongThreadException(String message) {
        super(message);
    }

    public J_L_WrongThreadException(String message, Throwable cause) {
        super(message, cause);
    }

    public J_L_WrongThreadException(Throwable cause) {
        super(cause);
    }

}
