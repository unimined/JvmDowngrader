package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

@Adapter("java/lang/MatchException")
public final class J_L_MatchException extends RuntimeException {

    public J_L_MatchException(String message, Throwable cause) {
        super(message, cause);
    }

}

