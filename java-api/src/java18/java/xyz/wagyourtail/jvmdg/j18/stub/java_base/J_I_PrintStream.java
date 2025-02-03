package xyz.wagyourtail.jvmdg.j18.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class J_I_PrintStream {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle getCharOut;

    static {
        try {
            getCharOut = IMPL_LOOKUP.findGetter(PrintStream.class, "charOut", OutputStreamWriter.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Stub
    public static Charset charset(PrintStream printStream) {
        try {
            OutputStreamWriter writer = (OutputStreamWriter) getCharOut.invokeExact(printStream);
            String encoding = writer.getEncoding();
            return Charset.forName(encoding);
        } catch (Throwable e) {
            Utils.sneakyThrow(e);
        }
        return null;
    }

}
