package xyz.wagyourtail.downgradetest;

import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TestWasAbstract {

    public static void main(String[] args) {
        TestPath t = new TestPath("a/");
        System.out.println(t);
        System.out.println(t.resolveSibling("test"));
        System.out.println(t.resolve("other"));
        Function<String, Path> test = t::resolve;
        Path test2 = test.apply("test");
        System.out.println(test2);
    }

    public static class TestPath implements Path {
        String path;

        public TestPath(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return path;
        }

        @NotNull
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

        
        @NotNull
        @Override
        public Path getName(int index) {
            return null;
        }

        
        @NotNull
        @Override
        public Path subpath(int beginIndex, int endIndex) {
            return null;
        }

        
        @Override
        public boolean startsWith(@NotNull Path other) {
            return false;
        }

        
        @Override
        public boolean endsWith(@NotNull Path other) {
            return false;
        }

        
        @NotNull
        @Override
        public Path normalize() {
            return null;
        }

        
        @NotNull
        @Override
        public Path resolve(@NotNull Path other) {
            return new TestPath(this.path + "/" + other + "/");
        }

        @NotNull
        @Override
        public Path resolveSibling(@NotNull String other) {
            return Path.super.resolveSibling(other);
        }

        
        @NotNull
        @Override
        public Path relativize(@NotNull Path other) {
            return null;
        }

        
        @NotNull
        @Override
        public URI toUri() {
            return null;
        }

        
        @NotNull
        @Override
        public Path toAbsolutePath() {
            return null;
        }

        
        @NotNull
        @Override
        public Path toRealPath(@NotNull LinkOption... options) throws IOException {
            return null;
        }

        
        @NotNull
        @Override
        public WatchKey register(@NotNull WatchService watcher, @NotNull WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
            return null;
        }

        
        @Override
        public int compareTo(@NotNull Path other) {
            return 0;
        }

        public Function<String, Path> test() {
            return this::resolveSibling;
        }

        private static class TestFS extends FileSystem {
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

            @NotNull
            @Override
            public Path getPath(@NotNull String first, @NotNull String... more) {
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

}
