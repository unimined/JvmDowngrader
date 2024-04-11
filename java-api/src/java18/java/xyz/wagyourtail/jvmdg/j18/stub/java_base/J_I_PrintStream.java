package xyz.wagyourtail.jvmdg.j18.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class J_I_PrintStream {

    @Stub
    public static Charset charset(PrintStream printStream) throws NoSuchFieldException, IllegalAccessException {
        Field charOut = printStream.getClass().getField("charOut");
        charOut.setAccessible(true);
        OutputStreamWriter writer = (OutputStreamWriter) charOut.get(printStream);
        String encoding = writer.getEncoding();
        return Charset.forName(encoding);
    }

}
