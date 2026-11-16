package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;
import java.util.function.Function;

/**
 * Interface method forms: default, static, private, private-static, fields,
 * anonymous impls, super-interface default dispatch ({@code I.super.m()}),
 * conflict resolution. On a <=j8 target these are desugared into
 * {@code X$jvmdg$StaticDefaults} helper classes.
 */
public interface Interfaces {

    String test5 = "test";

    static void main(String[] args) {
        Support.run(Interfaces.class);
    }

    @FeatureTest
    static void defaults() {
        // default-method dispatch through an anonymous impl + interface field
        new Interfaces() {
        }.test2();
        System.out.println(Interfaces.test5);
    }

    @FeatureTest
    static void statics() {
        // interface static method containing a ctor method-ref
        test3();
    }

    @FeatureTest
    static void defaultSuper() {
        // invokeinterface-super: Path.super.resolveSibling calls the interface
        // default through the implementor — exercises invokespecial-on-itf
        TestPath t = new TestPath("a/");
        System.out.println(t);
        System.out.println(t.resolveSibling("test"));
        System.out.println(t.resolve("other"));
        Function<String, Path> test = t::resolve;
        Path test2 = test.apply("test");
        System.out.println(test2);
        System.out.println(t.test().apply("test").getClass().getSimpleName());
    }

    @FeatureTest
    static void conflict() {
        // two interfaces with same default -> impl must override
        new ABImpl().who();
    }

    @FeatureTest
    static void chainedDefaults() {
        // default method calling private + private static members
        new Chained() {
        }.go();
    }

    static void test3() {
        System.out.println("test3" + 4);

        Function<Long, IndexOutOfBoundsException> IOOBE = IndexOutOfBoundsException::new;
        IOOBE.apply(0L);
    }

    default void test2() {
        test();
    }

    private void test() {
        System.out.println("test");
    }

    private static String helper() {
        return "privStatic";
    }

    interface Chained {
        default void go() {
            System.out.println(inner() + ":" + stat());
        }

        private String inner() {
            return "inner";
        }

        private static String stat() {
            return "stat";
        }
    }

    interface A {
        default void who() {
            System.out.println("A");
        }
    }

    interface B {
        default void who() {
            System.out.println("B");
        }
    }

    class ABImpl implements A, B {
        @Override
        public void who() {
            A.super.who();
            B.super.who();
            System.out.println("AB");
        }
    }

    class TestPath implements Path {
        String path;

        public TestPath(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return path;
        }

        @Override
        public FileSystem getFileSystem() {
            return new TestFS();
        }

        @Override
        public boolean isAbsolute() {
            return false;
        }

        @Override
        public Path getRoot() {
            return null;
        }

        @Override
        public Path getFileName() {
            return null;
        }

        @Override
        public Path getParent() {
            return null;
        }

        @Override
        public int getNameCount() {
            return 0;
        }

        @Override
        public Path getName(int index) {
            return null;
        }

        @Override
        public Path subpath(int beginIndex, int endIndex) {
            return null;
        }

        @Override
        public boolean startsWith(Path other) {
            return false;
        }

        @Override
        public boolean endsWith(Path other) {
            return false;
        }

        @Override
        public Path normalize() {
            return null;
        }

        @Override
        public Path resolve(Path other) {
            return new TestPath(this.path + "/" + other + "/");
        }

        @Override
        public Path resolveSibling(String other) {
            return Path.super.resolveSibling(other);
        }

        @Override
        public Path relativize(Path other) {
            return null;
        }

        @Override
        public URI toUri() {
            return null;
        }

        @Override
        public Path toAbsolutePath() {
            return null;
        }

        @Override
        public Path toRealPath(LinkOption... options) throws IOException {
            return null;
        }

        @Override
        public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
            return null;
        }

        @Override
        public int compareTo(Path other) {
            return 0;
        }

        public Function<String, Path> test() {
            return this::resolveSibling;
        }
    }

    class TestFS extends FileSystem {
        @Override
        public FileSystemProvider provider() {
            return null;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public String getSeparator() {
            return null;
        }

        @Override
        public Iterable<Path> getRootDirectories() {
            return null;
        }

        @Override
        public Iterable<FileStore> getFileStores() {
            return null;
        }

        @Override
        public Set<String> supportedFileAttributeViews() {
            return null;
        }

        @Override
        public Path getPath(String first, String... more) {
            return new TestPath(first + "/" + String.join(File.separator, more) + "/");
        }

        @Override
        public PathMatcher getPathMatcher(String syntaxAndPattern) {
            return null;
        }

        @Override
        public UserPrincipalLookupService getUserPrincipalLookupService() {
            return null;
        }

        @Override
        public WatchService newWatchService() throws IOException {
            return null;
        }

    }

}
