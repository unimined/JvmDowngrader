package xyz.wagyourtail.jvmdg.compile;

import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipDowngrader {

    public static void main(String[] args) throws IOException {
        int target = Integer.parseInt(args[0]);
        File input = new File(args[1]);
        File output = new File(args[2]);
        downgradeZip(target, input, output);
    }

    public static void downgradeZip(int opcVersion, File input, File output) throws IOException {
        downgradeZip(ClassDowngrader.downgradeTo(opcVersion), input.toPath(), output.toPath());
    }

    public static void downgradeZip(int opcVersion, Path input, Path output) throws IOException {
        downgradeZip(ClassDowngrader.downgradeTo(opcVersion), input, output);
    }

    public static void downgradeZip(final ClassDowngrader downgrader, Path zip, final Path output) throws IOException {
        try (final FileSystem zipfs = Utils.openZipFileSystem(zip, new HashMap<String, Object>())) {
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(output))) {
                Files.walkFileTree(zipfs.getPath("/"), new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (dir.startsWith("META-INF"))
                            return FileVisitResult.SKIP_SUBTREE;
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        byte[] bytes = Utils.readAllBytes(Files.newInputStream(file));
                        if (file.getFileName().toString().endsWith(".class")) {
                            try {
                                String internalName = file.toString().substring(1, file.toString().length() - 6);
                                Map<String, byte[]> outputs = downgrader.downgrade(internalName, bytes, new Function<String, byte[]>() {
                                    @Override
                                    public byte[] apply(String s) {
                                        try {
                                            Path path = zipfs.getPath(s + ".class");
                                            if (Files.exists(path)) {
                                                return Utils.readAllBytes(Files.newInputStream(path));
                                            }
                                            return null;
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                                if (outputs == null || outputs.isEmpty()) {
                                    ZipEntry newEntry = new ZipEntry(internalName + ".class");
                                    newEntry.setTime(attrs.lastModifiedTime().toMillis());
                                    zos.putNextEntry(newEntry);
                                    zos.write(bytes);
                                    zos.flush();
                                    zos.closeEntry();
                                    return FileVisitResult.CONTINUE;
                                }
                                for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                                    ZipEntry newEntry = new ZipEntry(entry.getKey() + ".class");
                                    newEntry.setTime(attrs.lastModifiedTime().toMillis());
                                    zos.putNextEntry(newEntry);
                                    zos.write(entry.getValue());
                                    zos.flush();
                                    zos.closeEntry();
                                }
                            } catch (IllegalClassFormatException e) {
                                throw new IOException("Failed to downgrade class", e);
                            }
                        } else {
                            String fileName = file.getFileName().toString();
                            // remove leading /
                            if (fileName.startsWith("/")) {
                                fileName = fileName.substring(1);
                            }
                            ZipEntry newEntry = new ZipEntry(fileName);
                            newEntry.setTime(attrs.lastModifiedTime().toMillis());
                            zos.putNextEntry(newEntry);
                            zos.write(bytes);
                            zos.flush();
                            zos.closeEntry();
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        throw new IOException("Failed to visit file", exc);
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

}
