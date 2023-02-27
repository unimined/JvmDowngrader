package xyz.wagyourtail.jvmdg.internal.mods.stub._17;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class J_L_Process {

    @Stub(JavaVersion.VERSION_17)
    public static BufferedReader inputReader(Process process) {
        return  new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
    }

    @Stub(JavaVersion.VERSION_17)
    public static BufferedReader inputReader(Process process, Charset charset) {
        return  new BufferedReader(new InputStreamReader(process.getInputStream(), charset));
    }

    @Stub(JavaVersion.VERSION_17)
    public static BufferedReader errorReader(Process process) {
        // get cs field
        return  new BufferedReader(new InputStreamReader(process.getErrorStream(), Charset.defaultCharset()));
    }

    @Stub(JavaVersion.VERSION_17)
    public static BufferedReader errorReader(Process process, Charset charset) {
        // get cs field
        return  new BufferedReader(new InputStreamReader(process.getErrorStream(), charset));
    }

    @Stub(JavaVersion.VERSION_17)
    public static BufferedWriter outputWriter(Process process) {
        // get cs field
        return  new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), Charset.defaultCharset()));
    }

    @Stub(JavaVersion.VERSION_17)
    public static BufferedWriter outputWriter(Process process, Charset charset) {
        // get cs field
        return  new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), charset));
    }


}
