package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiPredicate;
import xyz.wagyourtail.jvmdg.j8.stub.stream.J_U_S_Stream;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class J_N_F_Files {

    @Stub(ref = @Ref("java/nio/file/Files"))
    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }

    @Stub(ref = @Ref("java/nio/file/Files"))
    public static BufferedWriter newBufferedWriter(Path path, OpenOption... options) throws IOException {
        return Files.newBufferedWriter(path, StandardCharsets.UTF_8, options);
    }

    @Stub(ref = @Ref("java/nio/file/Files"))
    public static List<String> readAllLines(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    @Stub(ref = @Ref("java/nio/file/Files"))
    public static void write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options) throws IOException {
        Files.write(path, lines, StandardCharsets.UTF_8, options);
    }

    @Stub(ref = @Ref("java/nio/file/Files"))
    public static J_U_S_Stream<Path> list(Path dir) throws IOException {
        throw MissingStubError.create();
    }

    @Stub(ref = @Ref("java/nio/file/Files"))
    public static J_U_S_Stream<Path> walk(Path start, int maxDepth, FileVisitOption... options) {
        throw MissingStubError.create();
    }

    @Stub(ref = @Ref("java/nio/file/Files"))
    public static J_U_S_Stream<Path> walk(Path start, FileVisitOption... options) {
        return walk(start, Integer.MAX_VALUE, options);
    }

    @Stub(ref = @Ref("java/nio/file/Files"))
    public static J_U_S_Stream<Path> find(Path start, int maxDepth, J_U_F_BiPredicate<Path, BasicFileAttributes> matcher, FileVisitOption... options) {
        throw MissingStubError.create();
    }

    @Stub(ref = @Ref("java/nio/file/Files"))
    public static J_U_S_Stream<String> lines(Path path, Charset charset) throws IOException {
        throw MissingStubError.create();
    }

    @Stub(ref = @Ref("java/nio/file/Files"))
    public static J_U_S_Stream<String> lines(Path path) throws IOException {
        return lines(path, StandardCharsets.UTF_8);
    }


}
