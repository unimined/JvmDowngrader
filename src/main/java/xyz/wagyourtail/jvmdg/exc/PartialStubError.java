package xyz.wagyourtail.jvmdg.exc;

import org.objectweb.asm.Type;

public class PartialStubError extends Error {

    public PartialStubError(String message) {
        super(message);
    }

    public static PartialStubError create() {
        StackTraceElement prev = new Error().getStackTrace()[1];
        return new PartialStubError("Partial stub body for " + prev.getClassName() + "." + prev.getMethodName() + " at " + prev.getFileName() + ":" + prev.getLineNumber());
    }

    public static PartialStubError create(Type type, String name, Type desc) {
        StackTraceElement prev = new Error().getStackTrace()[1];
        if (name == null) {
            return new PartialStubError("Partial stub for " + type.getClassName() + " at " + prev.getFileName() + ":" + prev.getLineNumber());
        } else {
            return new PartialStubError("Partial stub for " + type.getClassName() + "." + name + ";" + desc + " at " + prev.getFileName() + ":" + prev.getLineNumber());
        }
    }

}
