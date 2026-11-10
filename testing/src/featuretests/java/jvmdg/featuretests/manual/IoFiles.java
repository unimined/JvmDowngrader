package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * File I/O: temp-file create/write/read/delete, FileWriter append, FileReader
 * charset ctor, zipfs via FileSystems.newFileSystem, PrintWriter overloads.
 * All writes land in java.io.tmpdir — nothing touches the working dir.
 */
public class IoFiles {

    public static void main(String[] args) {
        Support.run(IoFiles.class);
    }

    @FeatureTest
    static void readWrite() throws IOException {
        // FileWriter/Reader charset ctors + append mode — real tmpdir round-trip
        var temp = Files.createTempFile("jvmdg", "txt");
        Files.deleteIfExists(temp);
        try (FileWriter writer = new FileWriter(temp.toAbsolutePath().toString(), StandardCharsets.UTF_8)) {
            writer.write("Hello World!\n");
        }

        try (FileWriter writer2 = new FileWriter(temp.toFile(), StandardCharsets.UTF_8, true)) {
            writer2.write("Goodbye World!");
        }

        try (FileReader reader = new FileReader(temp.toAbsolutePath().toString(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            System.out.println(sb.toString());
        }

        Files.deleteIfExists(temp);
    }

    @FeatureTest
    static void zipfs() throws IOException {
        // FileSystems.newFileSystem (zipfs) — a provider-backed FS rooted
        // inside a jar entry, exercised through the downgrading classloader
        var temp2 = Files.createTempFile("jvmdg", ".zip");
        Files.deleteIfExists(temp2);
        try (FileSystem fs = FileSystems.newFileSystem(temp2, Map.of("create", "true"))) {
            Files.writeString(fs.getPath("/test.txt"), "Hello World!", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println(Files.readString(fs.getPath("/test.txt")));
        }
        Files.deleteIfExists(temp2);
    }

    @FeatureTest
    static void printWriter() throws IOException {
        // PrintWriter charset/File/String ctors + autoflush overloads
        PrintWriter out = new PrintWriter(System.out, false, System.out.charset());
        out.println("Hello World!");
        out.flush();

        var temp = Files.createTempFile("jvmdg", "txt");
        try (PrintWriter pw = new PrintWriter(temp.toAbsolutePath().toString(), StandardCharsets.UTF_8)) {
            pw.println("Hello World!");
        }
        try (PrintWriter pw = new PrintWriter(temp.toFile(), StandardCharsets.UTF_8)) {
            pw.println("Goodbye World!");
        }
        System.out.println(Files.readString(temp).replace("\n", "|").replace("\r", ""));
        Files.deleteIfExists(temp);
    }
}
