package xyz.wagyourtail.jvmdg.compile;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipOutputStream;

public class ZipDowngrader {

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        int target = Integer.parseInt(args[0]);
        File input = new File(args[1]);
        File output = new File(args[2]);
        Set<File> classpath = new HashSet<>();
        if (args.length > 3) {
            for (String s : args[3].split(File.pathSeparator)) {
                classpath.add(new File(s));
            }
        }
        downgradeZip(target, input, classpath, output);
        System.out.println("Downgraded in " + (System.currentTimeMillis() - start) + "ms");
    }

    public static void downgradeZip(int opcVersion, File input, Set<File> classpath, File output) throws IOException {
        Set<URL> classpathPaths = new HashSet<>();
        for (File file : classpath) {
            classpathPaths.add(file.toURI().toURL());
        }
        try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(opcVersion)) {
            downgradeZip(downgrader, input.toPath(), classpathPaths, output.toPath());
        }
    }

    public static void downgradeZip(int opcVersion, Path input, Set<URL> classpath, Path output) throws IOException {
        try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(opcVersion)) {
            downgradeZip(downgrader, input, classpath, output);
        }
    }

    public static void downgradeZip(final ClassDowngrader downgrader, Path zip, Set<URL> classpath, final Path output) throws IOException {
        try (final FileSystem zipfs = Utils.openZipFileSystem(zip, false)) {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = parent != null ? Files.createTempFile(parent, output.getFileName().toString(), ".zip") : Files.createTempFile(output.getFileName().toString(), ".zip");
            try {
                new ZipOutputStream(Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)).close();
                try (final FileSystem outputZipFs = Utils.openZipFileSystem(tmp, true)) {
                    PathDowngrader.downgradePaths(downgrader, Collections.singletonList(zipfs.getPath("/")), Collections.singletonList(outputZipFs.getPath("/")), classpath);
                }
                Files.move(tmp, output, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                // a failed downgrade leaves the staging jar behind — delete it
                // unless the move consumed it
                Files.deleteIfExists(tmp);
            }
        }
    }

}
