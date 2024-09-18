package xyz.wagyourtail.jvmdg.j23.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.Console;
import java.io.PrintWriter;
import java.util.Locale;

public class J_I_Console {

    @Stub
    public Console format(Console console, Locale locale, String format, Object... args) {
        PrintWriter pw = console.writer();
        pw.format(locale, format, args);
        pw.flush();
        return console;
    }

    @Stub
    public Console printf(Console console, Locale locale, String format, Object... args) {
        PrintWriter pw = console.writer();
        pw.printf(locale, format, args);
        pw.flush();
        return console;
    }

    @Stub
    public String readLine(Console console, Locale locale, String format, Object... args) {
        PrintWriter pw = console.writer();
        pw.printf(locale, format, args);
        pw.flush();
        return console.readLine();
    }

    @Stub
    public char[] readPassword(Console console, Locale locale, String format, Object... args) {
        PrintWriter pw = console.writer();
        pw.printf(locale, format, args);
        pw.flush();
        return  console.readPassword();
    }

}
