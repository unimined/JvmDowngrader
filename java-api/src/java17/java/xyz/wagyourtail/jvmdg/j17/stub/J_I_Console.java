package xyz.wagyourtail.jvmdg.j17.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.io.Console;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class J_I_Console {
    @Stub(javaVersion = Opcodes.V17)
    public static Charset charset(Console console) throws NoSuchFieldException, IllegalAccessException {
        // get cs field
        Field f = Console.class.getDeclaredField("cs");
        f.setAccessible(true);
        return (Charset) f.get(console);
    }

}
