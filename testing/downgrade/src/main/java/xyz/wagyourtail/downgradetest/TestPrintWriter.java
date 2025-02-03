package xyz.wagyourtail.downgradetest;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class TestPrintWriter {


    public static void main(String[] args) throws IOException {
        PrintWriter out = new PrintWriter(System.out, false, System.out.charset());
        out.println("Hello World!");

        var temp = Files.createTempFile("temp", "txt");
        out = new PrintWriter(temp.toAbsolutePath().toString(), StandardCharsets.UTF_8);
        out.println("Hello World!");
        out.close();

        out = new PrintWriter(temp.toFile(), StandardCharsets.UTF_8);
        out.println("Goodbye World!");
        out.close();

        Files.deleteIfExists(temp);
    }

}
