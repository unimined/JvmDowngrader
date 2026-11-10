package jvmdg.stubtests.manual;

import jvmdg.stubtests.StubTest;
import jvmdg.stubtests.Support;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Parity tests that need real filesystem fixtures: charset-aware IO
 * constructors, {@code Files.*} operations on a controlled temp tree, and
 * {@code FileSystems.newFileSystem} over a synthesized zip. The generator
 * denies these wholesale because synthesized receivers would hit the real
 * filesystem with uncontrolled paths — each test here owns its temp fixture
 * and only prints deterministic content (never temp paths).
 */
public final class IoTests {

    private IoTests() {
    }

    private static Path dir() throws Exception {
        return java.nio.file.Files.createTempDirectory("jvmdg-stub");
    }

    private static Path file(String content) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        write(p, content);
        return p;
    }

    // fixtures deliberately use unstubbed primitives so a stub failure lands
    // on its own test rather than cascading through setup
    private static void write(Path p, String content) throws Exception {
        try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(p)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String read(Path p) throws Exception {
        return new String(java.nio.file.Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private static Path zip() throws Exception {
        Path z = java.nio.file.Files.createTempFile("jvmdg-stub", ".zip");
        try (java.util.zip.ZipOutputStream zos =
                 new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(z))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("a.txt"));
            zos.write("inside".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return z;
    }

    private static void rm(Path p) {
        try {
            if (java.nio.file.Files.isDirectory(p)) {
                try (java.util.stream.Stream<Path> s = java.nio.file.Files.list(p)) {
                    s.forEach(f -> {
                        try {
                            java.nio.file.Files.delete(f);
                        } catch (java.io.IOException ignored) {
                        }
                    });
                }
            }
            java.nio.file.Files.deleteIfExists(p);
        } catch (java.io.IOException ignored) {
        }
    }

    /** FileReader(File, Charset) is denied — a synthesized path hits real fs. */
    @StubTest(member = @Ref(value = "java/io/FileReader", member = "<init>",
        desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"))
    public static void fileReaderFile(String id) throws Exception {
        Path p = file("café");
        try (java.io.FileReader r = new java.io.FileReader(p.toFile(),
            StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            for (int c = r.read(); c != -1; c = r.read()) {
                sb.append((char) c);
            }
            Support.print(id, sb.toString());
        } finally {
            rm(p);
        }
    }

    /** FileReader(String, Charset) is denied — same fixture via path string. */
    @StubTest(member = @Ref(value = "java/io/FileReader", member = "<init>",
        desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"))
    public static void fileReaderString(String id) throws Exception {
        Path p = file("café");
        try (java.io.FileReader r = new java.io.FileReader(p.toString(),
            StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            for (int c = r.read(); c != -1; c = r.read()) {
                sb.append((char) c);
            }
            Support.print(id, sb.toString());
        } finally {
            rm(p);
        }
    }

    /** FileWriter(File, Charset) is denied — synthesized path would write fs. */
    @StubTest(member = @Ref(value = "java/io/FileWriter", member = "<init>",
        desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"))
    public static void fileWriterFile(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try (java.io.FileWriter w = new java.io.FileWriter(p.toFile(),
            StandardCharsets.UTF_8)) {
            w.write("wé");
            w.close();
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** FileWriter(File, Charset, boolean) — append variant of the above. */
    @StubTest(member = @Ref(value = "java/io/FileWriter", member = "<init>",
        desc = "(Ljava/io/File;Ljava/nio/charset/Charset;Z)V"))
    public static void fileWriterFileAppend(String id) throws Exception {
        Path p = file("a");
        try (java.io.FileWriter w = new java.io.FileWriter(p.toFile(),
            StandardCharsets.UTF_8, true)) {
            w.write("b");
            w.close();
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** FileWriter(String, Charset) — denied like the File overload. */
    @StubTest(member = @Ref(value = "java/io/FileWriter", member = "<init>",
        desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"))
    public static void fileWriterString(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try (java.io.FileWriter w = new java.io.FileWriter(p.toString(),
            StandardCharsets.UTF_8)) {
            w.write("sé");
            w.close();
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** FileWriter(String, Charset, boolean) — append variant. */
    @StubTest(member = @Ref(value = "java/io/FileWriter", member = "<init>",
        desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;Z)V"))
    public static void fileWriterStringAppend(String id) throws Exception {
        Path p = file("x");
        try (java.io.FileWriter w = new java.io.FileWriter(p.toString(),
            StandardCharsets.UTF_8, true)) {
            w.write("y");
            w.close();
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** PrintStream(File, Charset) is denied — synthesized path writes fs. */
    @StubTest(member = @Ref(value = "java/io/PrintStream", member = "<init>",
        desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"))
    public static void printStreamFile(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try (java.io.PrintStream ps = new java.io.PrintStream(p.toFile(),
            StandardCharsets.UTF_8)) {
            ps.print("pé");
            ps.close();
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** PrintStream(String, Charset) — path-string variant. */
    @StubTest(member = @Ref(value = "java/io/PrintStream", member = "<init>",
        desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"))
    public static void printStreamString(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try (java.io.PrintStream ps = new java.io.PrintStream(p.toString(),
            StandardCharsets.UTF_8)) {
            ps.print("ps");
            ps.close();
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** writeBytes shares the java/io write* name deny — in-memory receiver. */
    @StubTest(member = @Ref(value = "java/io/PrintStream", member = "writeBytes",
        desc = "([B)V"))
    public static void printStreamWriteBytes(String id) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(baos);
        ps.writeBytes("wb".getBytes(StandardCharsets.UTF_8));
        ps.flush();
        Support.print(id, baos.toString("UTF-8"));
    }

    /** PrintWriter(File, Charset) is denied — synthesized path writes fs. */
    @StubTest(member = @Ref(value = "java/io/PrintWriter", member = "<init>",
        desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"))
    public static void printWriterFile(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try (java.io.PrintWriter w = new java.io.PrintWriter(p.toFile(),
            StandardCharsets.UTF_8)) {
            w.print("wŕ");
            w.close();
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** PrintWriter(String, Charset) — path-string variant. */
    @StubTest(member = @Ref(value = "java/io/PrintWriter", member = "<init>",
        desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"))
    public static void printWriterString(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try (java.io.PrintWriter w = new java.io.PrintWriter(p.toString(),
            StandardCharsets.UTF_8)) {
            w.print("ws");
            w.close();
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** writeBytes on the in-memory stream is denied by the io write* rule. */
    @StubTest(member = @Ref(value = "java/io/ByteArrayOutputStream",
        member = "writeBytes", desc = "([B)V"))
    public static void baosWriteBytes(String id) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        baos.writeBytes("ba".getBytes(StandardCharsets.UTF_8));
        Support.print(id, baos.toString("UTF-8"));
    }

    /** Formatter(File, Charset, Locale) is denied — synthesized path writes fs. */
    @StubTest(member = @Ref(value = "java/util/Formatter", member = "<init>",
        desc = "(Ljava/io/File;Ljava/nio/charset/Charset;Ljava/util/Locale;)V"))
    public static void formatterFile(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try (java.util.Formatter f = new java.util.Formatter(p.toFile(),
            StandardCharsets.UTF_8, java.util.Locale.ROOT)) {
            f.format("v%d", 7);
            f.close();
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** Formatter(String, Charset, Locale) — path-string variant. */
    @StubTest(member = @Ref(value = "java/util/Formatter", member = "<init>",
        desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;Ljava/util/Locale;)V"))
    public static void formatterString(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try (java.util.Formatter f = new java.util.Formatter(p.toString(),
            StandardCharsets.UTF_8, java.util.Locale.ROOT)) {
            f.format("s%d", 3);
            f.close();
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** Scanner(File, Charset) is denied — real fixture content is read back. */
    @StubTest(member = @Ref(value = "java/util/Scanner", member = "<init>",
        desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"))
    public static void scannerFile(String id) throws Exception {
        Path p = file("12 34");
        try (java.util.Scanner s = new java.util.Scanner(p.toFile(),
            StandardCharsets.UTF_8)) {
            Support.print(id, s.nextInt() + s.nextInt());
        } finally {
            rm(p);
        }
    }

    /** Files.* is owner-denied (every member takes a real Path) — controlled
     * temp tree fixture instead. */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "find",
        desc = "(Ljava/nio/file/Path;ILjava/util/function/BiPredicate;"
            + "[Ljava/nio/file/FileVisitOption;)"
            + "Ljava/util/stream/Stream;"))
    public static void filesFind(String id) throws Exception {
        Path d = dir();
        try {
            write(d.resolve("one.txt"), "1");
            write(d.resolve("two.txt"), "2");
            try (java.util.stream.Stream<Path> s = java.nio.file.Files.find(d, 1,
                (p, a) -> true)) {
                Support.print(id, s.count());
            }
        } finally {
            rm(d);
        }
    }

    /** Files.lines(Path) — stream of lines from a controlled temp file. */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "lines",
        desc = "(Ljava/nio/file/Path;)"
            + "Ljava/util/stream/Stream;"))
    public static void filesLines(String id) throws Exception {
        Path p = file("l1\nl2");
        try (java.util.stream.Stream<String> s = java.nio.file.Files.lines(p)) {
            Support.print(id, s.count());
        } finally {
            rm(p);
        }
    }

    /** Files.lines(Path, Charset) — charset variant. */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "lines",
        desc = "(Ljava/nio/file/Path;Ljava/nio/charset/Charset;)"
            + "Ljava/util/stream/Stream;"))
    public static void filesLinesCharset(String id) throws Exception {
        Path p = file("a\nb\nc");
        try (java.util.stream.Stream<String> s = java.nio.file.Files.lines(p,
            StandardCharsets.UTF_8)) {
            Support.print(id, s.count());
        } finally {
            rm(p);
        }
    }

    /** Files.list(Path) — dir listing over the controlled temp dir. */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "list",
        desc = "(Ljava/nio/file/Path;)"
            + "Ljava/util/stream/Stream;"))
    public static void filesList(String id) throws Exception {
        Path d = dir();
        try {
            write(d.resolve("a"), "");
            write(d.resolve("b"), "");
            try (java.util.stream.Stream<Path> s = java.nio.file.Files.list(d)) {
                Support.print(id, s.count());
            }
        } finally {
            rm(d);
        }
    }

    /** Files.mismatch(Path, Path) — byte compare of two fixture files. */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "mismatch",
        desc = "(Ljava/nio/file/Path;Ljava/nio/file/Path;)J"))
    public static void filesMismatch(String id) throws Exception {
        Path a = file("same"), b = file("diff!");
        try {
            Support.print(id, java.nio.file.Files.mismatch(a, b));
        } finally {
            rm(a);
            rm(b);
        }
    }

    /** Files.newBufferedReader(Path) — reads the fixture back. */
    @StubTest(member = @Ref(value = "java/nio/file/Files",
        member = "newBufferedReader", desc = "(Ljava/nio/file/Path;)Ljava/io/BufferedReader;"))
    public static void filesNewBufferedReader(String id) throws Exception {
        Path p = file("br-line");
        try (java.io.BufferedReader r = java.nio.file.Files.newBufferedReader(p)) {
            Support.print(id, r.readLine());
        } finally {
            rm(p);
        }
    }

    /** Files.newBufferedWriter(Path, OpenOption...) — write + readback. */
    @StubTest(member = @Ref(value = "java/nio/file/Files",
        member = "newBufferedWriter",
        desc = "(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/io/BufferedWriter;"))
    public static void filesNewBufferedWriter(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try (java.io.BufferedWriter w = java.nio.file.Files.newBufferedWriter(p)) {
            w.write("bw");
            w.close();
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** Files.readAllLines(Path) — fixture lines as a list. */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "readAllLines",
        desc = "(Ljava/nio/file/Path;)Ljava/util/List;"))
    public static void filesReadAllLines(String id) throws Exception {
        Path p = file("x\ny");
        try {
            Support.print(id, java.nio.file.Files.readAllLines(p));
        } finally {
            rm(p);
        }
    }

    /** Files.readString(Path) — whole fixture content. */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "readString",
        desc = "(Ljava/nio/file/Path;)Ljava/lang/String;"))
    public static void filesReadString(String id) throws Exception {
        Path p = file("content");
        try {
            Support.print(id, java.nio.file.Files.readString(p));
        } finally {
            rm(p);
        }
    }

    /** Files.readString(Path, Charset) — charset variant. */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "readString",
        desc = "(Ljava/nio/file/Path;Ljava/nio/charset/Charset;)Ljava/lang/String;"))
    public static void filesReadStringCharset(String id) throws Exception {
        Path p = file("chârset");
        try {
            Support.print(id, java.nio.file.Files.readString(p, StandardCharsets.UTF_8));
        } finally {
            rm(p);
        }
    }

    /** Files.walk(Path, int, FileVisitOption...) — depth-limited walk. */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "walk",
        desc = "(Ljava/nio/file/Path;I[Ljava/nio/file/FileVisitOption;)"
            + "Ljava/util/stream/Stream;"))
    public static void filesWalkDepth(String id) throws Exception {
        Path d = dir();
        try {
            write(d.resolve("f"), "");
            try (java.util.stream.Stream<Path> s = java.nio.file.Files.walk(d, 1)) {
                Support.print(id, s.count());
            }
        } finally {
            rm(d);
        }
    }

    /** Files.walk(Path, FileVisitOption...) — unbounded walk. */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "walk",
        desc = "(Ljava/nio/file/Path;[Ljava/nio/file/FileVisitOption;)"
            + "Ljava/util/stream/Stream;"))
    public static void filesWalk(String id) throws Exception {
        Path d = dir();
        try {
            write(d.resolve("f"), "");
            try (java.util.stream.Stream<Path> s = java.nio.file.Files.walk(d)) {
                Support.print(id, s.count());
            }
        } finally {
            rm(d);
        }
    }

    /** Files.write(Path, Iterable, OpenOption...) — write lines + readback. */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "write",
        desc = "(Ljava/nio/file/Path;Ljava/lang/Iterable;[Ljava/nio/file/OpenOption;)V"))
    public static void filesWriteLines(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try {
            java.nio.file.Files.write(p, java.util.Arrays.asList("a", "b"));
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** Files.writeString(Path, CharSequence, Charset, OpenOption...). */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "writeString",
        desc = "(Ljava/nio/file/Path;Ljava/lang/CharSequence;Ljava/nio/charset/Charset;"
            + "[Ljava/nio/file/OpenOption;)Ljava/nio/file/Path;"))
    public static void filesWriteStringCharset(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try {
            java.nio.file.Files.writeString(p, "cs", StandardCharsets.UTF_8);
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** Files.writeString(Path, CharSequence, OpenOption...). */
    @StubTest(member = @Ref(value = "java/nio/file/Files", member = "writeString",
        desc = "(Ljava/nio/file/Path;Ljava/lang/CharSequence;"
            + "[Ljava/nio/file/OpenOption;)Ljava/nio/file/Path;"))
    public static void filesWriteString(String id) throws Exception {
        Path p = java.nio.file.Files.createTempFile("jvmdg-stub", ".txt");
        try {
            java.nio.file.Files.writeString(p, "plain");
            Support.print(id, read(p));
        } finally {
            rm(p);
        }
    }

    /** newFileSystem(URI, Map) — denied: a synthesized URI would probe installed
     * providers with a random scheme; a real jar: URI opens the zipfs. */
    @StubTest(member = @Ref(value = "java/nio/file/FileSystems",
        member = "newFileSystem",
        desc = "(Ljava/net/URI;Ljava/util/Map;)Ljava/nio/file/FileSystem;"))
    public static void newFileSystemUri(String id) throws Exception {
        Path z = zip();
        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(
            java.net.URI.create("jar:" + z.toUri()),
            java.util.Collections.emptyMap())) {
            Support.print(id, read(fs.getPath("a.txt")));
        } finally {
            rm(z);
        }
    }

    /** newFileSystem(URI, Map, ClassLoader) — loader-scoped variant. */
    @StubTest(member = @Ref(value = "java/nio/file/FileSystems",
        member = "newFileSystem",
        desc = "(Ljava/net/URI;Ljava/util/Map;Ljava/lang/ClassLoader;)"
            + "Ljava/nio/file/FileSystem;"))
    public static void newFileSystemUriLoader(String id) throws Exception {
        Path z = zip();
        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(
            java.net.URI.create("jar:" + z.toUri()),
            java.util.Collections.emptyMap(), IoTests.class.getClassLoader())) {
            Support.print(id, read(fs.getPath("a.txt")));
        } finally {
            rm(z);
        }
    }

    /** newFileSystem(Path) — auto-detect provider over a real zip file. */
    @StubTest(member = @Ref(value = "java/nio/file/FileSystems",
        member = "newFileSystem", desc = "(Ljava/nio/file/Path;)Ljava/nio/file/FileSystem;"))
    public static void newFileSystemPath(String id) throws Exception {
        Path z = zip();
        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(z)) {
            Support.print(id, read(fs.getPath("a.txt")));
        } finally {
            rm(z);
        }
    }

    /** newFileSystem(Path, Map) — env map variant. */
    @StubTest(member = @Ref(value = "java/nio/file/FileSystems",
        member = "newFileSystem",
        desc = "(Ljava/nio/file/Path;Ljava/util/Map;)Ljava/nio/file/FileSystem;"))
    public static void newFileSystemPathEnv(String id) throws Exception {
        Path z = zip();
        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(z,
            java.util.Collections.emptyMap())) {
            Support.print(id, read(fs.getPath("a.txt")));
        } finally {
            rm(z);
        }
    }

    /** newFileSystem(Path, Map, ClassLoader) — loader-scoped variant. */
    @StubTest(member = @Ref(value = "java/nio/file/FileSystems",
        member = "newFileSystem",
        desc = "(Ljava/nio/file/Path;Ljava/util/Map;Ljava/lang/ClassLoader;)"
            + "Ljava/nio/file/FileSystem;"))
    public static void newFileSystemPathEnvLoader(String id) throws Exception {
        Path z = zip();
        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(z,
            java.util.Collections.emptyMap(), IoTests.class.getClassLoader())) {
            Support.print(id, read(fs.getPath("a.txt")));
        } finally {
            rm(z);
        }
    }

    /** FileSystemProvider.exists is denied (real fs probe) — temp file fixture. */
    @StubTest(member = @Ref(value = "java/nio/file/spi/FileSystemProvider",
        member = "exists", desc = "(Ljava/nio/file/Path;[Ljava/nio/file/LinkOption;)Z"))
    public static void providerExists(String id) throws Exception {
        Path p = file("");
        try {
            Support.print(id, java.nio.file.FileSystems.getDefault().provider().exists(p));
        } finally {
            rm(p);
        }
    }

    /** readAttributesIfExists on the real fixture returns non-null attrs. */
    @StubTest(member = @Ref(value = "java/nio/file/spi/FileSystemProvider",
        member = "readAttributesIfExists",
        desc = "(Ljava/nio/file/Path;Ljava/lang/Class;[Ljava/nio/file/LinkOption;)"
            + "Ljava/nio/file/attribute/BasicFileAttributes;"))
    public static void providerReadAttributes(String id) throws Exception {
        Path p = file("five!");
        try {
            java.nio.file.attribute.BasicFileAttributes a =
                java.nio.file.FileSystems.getDefault().provider()
                    .readAttributesIfExists(p, java.nio.file.attribute.BasicFileAttributes.class);
            Support.print(id, a != null && a.size() == 5);
        } finally {
            rm(p);
        }
    }
}
