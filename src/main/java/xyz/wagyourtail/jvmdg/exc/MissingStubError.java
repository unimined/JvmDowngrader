package xyz.wagyourtail.jvmdg.exc;

public class MissingStubError extends Error {

    public MissingStubError(String message) {
        super(message);
    }

    public static MissingStubError create() {
        StackTraceElement prev = new Error().getStackTrace()[1];
        return new MissingStubError("Missing stub body for " + prev.getClassName() + "." + prev.getMethodName() + " at " + prev.getFileName() + ":" + prev.getLineNumber());
    }

}
