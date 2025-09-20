package xyz.wagyourtail.jvmdg.j25.stub;

import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

@Adapter("java/lang/IO")
public class J_L_IO {
    private static volatile BufferedReader inputReader;

    public static void println(Object obj) {
        System.out.println(obj);
    }

    public static void println() {
        System.out.println();
    }

    public static void print(Object obj) {
        var out = System.out;
        out.print(obj);
        out.flush();
    }

    public static String readln() {
        try {
            return getInputReader().readLine();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    public static String readln(String prompt) {
        print(prompt);
        return readln();
    }

    private static BufferedReader getInputReader() {
        if (inputReader == null) {
            synchronized (J_L_IO.class) {
                if (inputReader == null) {
                    String enc = System.getProperty("stdin.encoding", "");
                    Charset cs = Charset.forName(enc);
                    inputReader = new BufferedReader(new InputStreamReader(System.in, cs));
                }
            }
        }
        return inputReader;
    }

}
