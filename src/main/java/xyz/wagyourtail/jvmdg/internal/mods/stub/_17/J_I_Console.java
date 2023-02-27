package xyz.wagyourtail.jvmdg.internal.mods.stub._17;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.Console;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class J_I_Console {
    @Stub(JavaVersion.VERSION_17)
    public static Charset charset(Console console) throws NoSuchFieldException, IllegalAccessException {
        // get cs field
        Field f = Console.class.getDeclaredField("cs");
        f.setAccessible(true);
        return (Charset) f.get(console);
    }
}
