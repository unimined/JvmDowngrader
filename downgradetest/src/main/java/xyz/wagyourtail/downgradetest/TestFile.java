package xyz.wagyourtail.downgradetest;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class TestFile {

    public static void main(String[] args) throws IOException {
        FileWriter writer = new FileWriter("build/test/test.txt", StandardCharsets.UTF_8);
        writer.write("Hello World!");
        writer.close();

        FileReader reader = new FileReader(new File("build/test/test.txt"), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            sb.append((char) c);
        }
        reader.close();
        System.out.println(sb.toString());
    }

}
