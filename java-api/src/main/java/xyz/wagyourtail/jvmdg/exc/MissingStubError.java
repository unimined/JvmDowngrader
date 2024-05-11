package xyz.wagyourtail.jvmdg.exc;

import org.objectweb.asm.Type;

public class MissingStubError extends Error {

    public MissingStubError(String message) {
        super(message);
    }

    public static MissingStubError create() {
        StackTraceElement prev = new Error().getStackTrace()[1];
        return new MissingStubError("Missing stub body for " + prev.getClassName() + "." + prev.getMethodName() + " at " + prev.getFileName() + ":" + prev.getLineNumber());
    }

    public static MissingStubError create(Type type, String name, Type desc) {
        StackTraceElement prev = new Error().getStackTrace()[1];
        if (name == null) {
            return new MissingStubError("Missing stub for " + type.getClassName() + " at " + prev.getFileName() + ":" + prev.getLineNumber());
        } else {
            return new MissingStubError("Missing stub for " + type.getClassName() + "." + name + ";" + desc + " at " + prev.getFileName() + ":" + prev.getLineNumber());
        }
    }

}
