package xyz.wagyourtail.jvmdg.j17.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class J_L_Process {

    @Stub
    public static BufferedReader inputReader(Process process) {
        return new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
    }

    @Stub
    public static BufferedReader inputReader(Process process, Charset charset) {
        return new BufferedReader(new InputStreamReader(process.getInputStream(), charset));
    }

    @Stub
    public static BufferedReader errorReader(Process process) {
        // get cs field
        return new BufferedReader(new InputStreamReader(process.getErrorStream(), Charset.defaultCharset()));
    }

    @Stub
    public static BufferedReader errorReader(Process process, Charset charset) {
        // get cs field
        return new BufferedReader(new InputStreamReader(process.getErrorStream(), charset));
    }

    @Stub
    public static BufferedWriter outputWriter(Process process) {
        // get cs field
        return new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), Charset.defaultCharset()));
    }

    @Stub
    public static BufferedWriter outputWriter(Process process, Charset charset) {
        // get cs field
        return new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), charset));
    }


}
