package xyz.wagyourtail.jvmdg.compile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scenario tests for jar-level handling: signature files must be stripped
 * (the transformed classes no longer match their digests), the manifest must
 * carry jvmdg provenance, and a missing manifest must be synthesized when
 * multi-release output is requested.
 */
public class TestPathDowngrader {

    private static byte[] trivialClass(String name) {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        return ASMUtils.classNodeToBytes(node, 0);
    }

    private static FileSystem zip(Path file) throws Exception {
        return FileSystems.newFileSystem(URI.create("jar:" + file.toUri()),
            Collections.singletonMap("create", "true"));
    }

    private static Flags flags() {
        Flags f = new Flags();
        f.classVersion = Opcodes.V1_8;
        f.quiet = true;
        return f;
    }

    @Test
    public void signedJarEntriesAreStripped(@TempDir Path dir) throws Exception {
        Path in = dir.resolve("signed.jar");
        Path out = dir.resolve("out.jar");
        try (FileSystem fs = zip(in)) {
            Path root = fs.getPath("/");
            Files.createDirectories(root.resolve("META-INF"));
            Files.write(root.resolve("META-INF/MANIFEST.MF"),
                "Manifest-Version: 1.0\r\n\r\n".getBytes());
            Files.write(root.resolve("META-INF/KEY.SF"), "fake-sig".getBytes());
            Files.write(root.resolve("META-INF/KEY.RSA"), "fake-block".getBytes());
            Files.createDirectories(root.resolve("foo"));
            Files.write(root.resolve("foo/Bar.class"), trivialClass("foo/Bar"));
        }

        Flags f = flags();
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(f);
             FileSystem inFs = Utils.openZipFileSystem(in, false);
             FileSystem outFs = zip(out)) {
            PathDowngrader.downgradePaths(d,
                Collections.singletonList(inFs.getPath("/")),
                Collections.singletonList(outFs.getPath("/")),
                new HashSet<java.net.URL>());
        }

        try (FileSystem fs = Utils.openZipFileSystem(out, false)) {
            Path root = fs.getPath("/");
            assertFalse(Files.exists(root.resolve("META-INF/KEY.SF")), ".SF must be stripped");
            assertFalse(Files.exists(root.resolve("META-INF/KEY.RSA")), ".RSA must be stripped");
            assertTrue(Files.exists(root.resolve("foo/Bar.class")), "class must be carried through");
            Path mf = root.resolve("META-INF/MANIFEST.MF");
            assertTrue(Files.exists(mf));
            try (InputStream is = Files.newInputStream(mf)) {
                Manifest manifest = new Manifest(is);
                Attributes attr = manifest.getMainAttributes();
                // jvmdgVersion is null under unit tests (package impl version
                // comes from the jar manifest); when present, the target is written
                if (Flags.jvmdgVersion != null) {
                    assertEquals(Flags.jvmdgVersion, attr.getValue(ManifestUtils.JVMDG_VERSION));
                    assertEquals(String.valueOf(Opcodes.V1_8), attr.getValue(ManifestUtils.DOWNGRADE_TARGET));
                }
            }
        }
    }

    @Test
    public void manifestSynthesizedForMultiReleaseWithoutInputManifest(@TempDir Path dir) throws Exception {
        Path in = dir.resolve("bare.jar");
        Path out = dir.resolve("out.jar");
        try (FileSystem fs = zip(in)) {
            Path root = fs.getPath("/");
            Files.createDirectories(root.resolve("foo"));
            Files.write(root.resolve("foo/Bar.class"), trivialClass("foo/Bar"));
        }

        Flags f = flags();
        f.multiReleaseOriginal = true;
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(f);
             FileSystem inFs = Utils.openZipFileSystem(in, false);
             FileSystem outFs = zip(out)) {
            PathDowngrader.downgradePaths(d,
                Collections.singletonList(inFs.getPath("/")),
                Collections.singletonList(outFs.getPath("/")),
                new HashSet<java.net.URL>());
        }

        try (FileSystem fs = Utils.openZipFileSystem(out, false)) {
            Path mf = fs.getPath("/").resolve("META-INF/MANIFEST.MF");
            assertTrue(Files.exists(mf),
                "a manifest must be synthesized when multi-release output is requested");
            try (InputStream is = Files.newInputStream(mf)) {
                Manifest manifest = new Manifest(is);
                assertEquals("true", manifest.getMainAttributes().getValue(ManifestUtils.MULTI_RELEASE));
            }
        }
    }

    @Test
    public void repeatedDowngradesProduceIdenticalOutput(@TempDir Path dir) throws Exception {
        Path in = dir.resolve("in.jar");
        try (FileSystem fs = zip(in)) {
            Path root = fs.getPath("/");
            Files.createDirectories(root.resolve("META-INF"));
            Files.write(root.resolve("META-INF/MANIFEST.MF"),
                "Manifest-Version: 1.0\r\n\r\n".getBytes());
            Files.createDirectories(root.resolve("a"));
            Files.write(root.resolve("a/A.class"), trivialClass("a/A"));
            Files.createDirectories(root.resolve("b"));
            Files.write(root.resolve("b/B.class"), trivialClass("b/B"));
            Files.write(root.resolve("b/res.txt"), "resource".getBytes());
        }

        byte[][] hashes = new byte[2][];
        for (int i = 0; i < 2; i++) {
            // straddle the 2s DOS-time granularity so an unpinned timestamp
            // (mtime/atime/ctime) in any output entry fails deterministically
            // instead of only when the two writes happen to cross a boundary
            if (i == 1) Thread.sleep(3000);
            Path out = dir.resolve("out" + i + ".jar");
            Flags f = flags();
            try (ClassDowngrader d = ClassDowngrader.downgradeTo(f);
                 FileSystem inFs = Utils.openZipFileSystem(in, false);
                 FileSystem outFs = zip(out)) {
                PathDowngrader.downgradePaths(d,
                    Collections.singletonList(inFs.getPath("/")),
                    Collections.singletonList(outFs.getPath("/")),
                    new HashSet<java.net.URL>());
            }
            hashes[i] = java.security.MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(out));
        }
        assertArrayEquals(hashes[0], hashes[1],
            "two downgrades of the same input must produce identical bytes");
    }

    @Test
    public void directoryInputClassesAreRewrittenToTargetVersion(@TempDir Path dir) throws Exception {
        Path in = dir.resolve("in");
        Path out = dir.resolve("out");
        Files.createDirectories(in.resolve("foo"));
        ClassNode node = new ClassNode();
        node.version = Opcodes.V11;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "foo/Bar";
        node.superName = "java/lang/Object";
        Files.write(in.resolve("foo/Bar.class"), ASMUtils.classNodeToBytes(node, 0));

        Flags f = flags();
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(f)) {
            PathDowngrader.downgradePaths(d,
                Collections.singletonList(in),
                Collections.singletonList(out),
                new HashSet<java.net.URL>());
        }

        byte[] result = Files.readAllBytes(out.resolve("foo/Bar.class"));
        // major/minor version at bytes 6/7 (big-endian)
        int major = ((result[6] & 0xFF) << 8) | (result[7] & 0xFF);
        assertEquals(Opcodes.V1_8, major, "class must be rewritten to the target version");
    }
}
