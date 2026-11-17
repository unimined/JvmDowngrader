package xyz.wagyourtail.jvmdg.compile.jar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Function;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestFusedJarRun {

    @TempDir
    Path dir;

    private StepContext ctx(Path in) {
        return new StepContext(new Flags(), new Logger("test", Logger.Level.FATAL, false, System.out),
            null, Collections.singletonList(in), new Function<String, byte[]>() {
                @Override
                public byte[] apply(String s) {
                    return null;
                }
            });
    }

    private Path input() throws IOException {
        Path in = dir.resolve("in");
        Files.createDirectories(in.resolve("a/b"));
        Files.write(in.resolve("a/b/c.txt"), "c".getBytes(StandardCharsets.UTF_8));
        Files.write(in.resolve("a/d.txt"), "d".getBytes(StandardCharsets.UTF_8));
        Files.write(in.resolve("e.txt"), "e".getBytes(StandardCharsets.UTF_8));
        return in;
    }

    @Test
    public void passthroughCopiesAndCreatesDirs() throws IOException {
        Path in = input(), out = dir.resolve("out");
        FusedJarRun.run(in, new ArrayList<JarStep>(), ctx(in), out);
        assertEquals("c", new String(Files.readAllBytes(out.resolve("a/b/c.txt")), StandardCharsets.UTF_8));
        assertEquals("d", new String(Files.readAllBytes(out.resolve("a/d.txt")), StandardCharsets.UTF_8));
        assertEquals("e", new String(Files.readAllBytes(out.resolve("e.txt")), StandardCharsets.UTF_8));
        assertTrue(Files.isDirectory(out.resolve("a/b")));
    }

    @Test
    public void nullResultDropsEntry() throws IOException {
        Path in = input(), out = dir.resolve("out");
        List<JarStep> steps = new ArrayList<>();
        steps.add(new JarStep() {
            @Override
            public JarEntry visitEntry(JarEntry entry, StepContext ctx) {
                return entry.name.equals("e.txt") ? null : entry;
            }
        });
        FusedJarRun.run(in, steps, ctx(in), out);
        assertFalse(Files.exists(out.resolve("e.txt")));
        assertTrue(Files.exists(out.resolve("a/d.txt")));
    }

    @Test
    public void rewriteAndRenameFlow() throws IOException {
        Path in = input(), out = dir.resolve("out");
        List<JarStep> steps = new ArrayList<>();
        steps.add(new JarStep() {
            @Override
            public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
                if (entry.name.equals("a/d.txt")) {
                    return entry.renamed("a/renamed.txt").withData("D2".getBytes(StandardCharsets.UTF_8));
                }
                return entry;
            }
        });
        FusedJarRun.run(in, steps, ctx(in), out);
        assertFalse(Files.exists(out.resolve("a/d.txt")));
        assertEquals("D2", new String(Files.readAllBytes(out.resolve("a/renamed.txt")), StandardCharsets.UTF_8));
    }

    @Test
    public void chainedVisitorsSeePreviousOutput() throws IOException {
        Path in = input(), out = dir.resolve("out");
        List<JarStep> steps = new ArrayList<>();
        // step 1 uppercases every txt; step 2 sees the new bytes
        steps.add(new JarStep() {
            @Override
            public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
                return entry.withData(new String(entry.bytes(), StandardCharsets.UTF_8).toUpperCase().getBytes(StandardCharsets.UTF_8));
            }
        });
        steps.add(new JarStep() {
            @Override
            public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
                if (entry.name.equals("e.txt")) {
                    return entry.withData((new String(entry.bytes(), StandardCharsets.UTF_8) + "!").getBytes(StandardCharsets.UTF_8));
                }
                return entry;
            }
        });
        FusedJarRun.run(in, steps, ctx(in), out);
        assertEquals("E!", new String(Files.readAllBytes(out.resolve("e.txt")), StandardCharsets.UTF_8));
        assertEquals("C", new String(Files.readAllBytes(out.resolve("a/b/c.txt")), StandardCharsets.UTF_8));
    }

    @Test
    public void sideEmissionsAndEndFusedWrites() throws IOException {
        Path in = input(), out = dir.resolve("out");
        List<JarStep> steps = new ArrayList<>();
        steps.add(new JarStep() {
            @Override
            public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
                if (entry.name.equals("e.txt")) {
                    ctx.sink.put("side.txt", "side".getBytes(StandardCharsets.UTF_8));
                    return null;
                }
                return entry;
            }

            @Override
            public void endFused(StepContext ctx) {
                ctx.sink.put("end.txt", "end".getBytes(StandardCharsets.UTF_8));
            }
        });
        FusedJarRun.run(in, steps, ctx(in), out);
        assertTrue(Files.exists(out.resolve("side.txt")));
        assertTrue(Files.exists(out.resolve("end.txt")));
    }

    @Test
    public void droppedDirectoryKeepsChildren() throws IOException {
        Path in = input(), out = dir.resolve("out");
        List<JarStep> steps = new ArrayList<>();
        steps.add(new JarStep() {
            @Override
            public JarEntry visitEntry(JarEntry entry, StepContext ctx) {
                return entry;
            }

            @Override
            public boolean visitDirectory(JarEntry entry, StepContext ctx) {
                return !entry.name.equals("a/b");
            }
        });
        FusedJarRun.run(in, steps, ctx(in), out);
        // a/b not explicitly created but children write through (parents
        // materialize implicitly) — the dir exists either way; the contract
        // is only that visitDirectory=false skips the mkdir, not the walk
        assertTrue(Files.exists(out.resolve("a/b/c.txt")));
    }

    @Test
    public void signedSealedJarLosesSealedAttrs() throws IOException {
        Path in = input(), out = dir.resolve("out");
        Files.createDirectories(in.resolve("META-INF"));
        java.util.jar.Manifest mf = new java.util.jar.Manifest();
        mf.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(java.util.jar.Attributes.Name.SEALED, "true");
        java.util.jar.Attributes pkg = new java.util.jar.Attributes();
        pkg.put(java.util.jar.Attributes.Name.SEALED, "true");
        mf.getEntries().put("a/", pkg);
        Files.write(in.resolve("META-INF/MANIFEST.MF"), manifestBytes(mf));
        Files.write(in.resolve("META-INF/TEST.SF"), "sig".getBytes(StandardCharsets.UTF_8));

        List<JarStep> steps = new ArrayList<>();
        steps.add(new SignatureStripStep());
        steps.add(new ManifestStep());
        FusedJarRun.run(in, steps, ctx(in), out);

        assertFalse(Files.exists(out.resolve("META-INF/TEST.SF")));
        java.util.jar.Manifest outMf = new java.util.jar.Manifest(
            Files.newInputStream(out.resolve("META-INF/MANIFEST.MF")));
        assertNull(outMf.getMainAttributes().getValue(java.util.jar.Attributes.Name.SEALED),
            "a dropped signature kills the seal — strip it or injected classes fail to load");
        assertNull(outMf.getEntries().get("a/").getValue(java.util.jar.Attributes.Name.SEALED));
        assertTrue(Files.exists(out.resolve("a/b/c.txt")), "regular entries pass through");
    }

    @Test
    public void unsignedSealedJarKeepsSeal() throws IOException {
        Path in = input(), out = dir.resolve("out");
        Files.createDirectories(in.resolve("META-INF"));
        java.util.jar.Manifest mf = new java.util.jar.Manifest();
        mf.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(java.util.jar.Attributes.Name.SEALED, "true");
        Files.write(in.resolve("META-INF/MANIFEST.MF"), manifestBytes(mf));

        List<JarStep> steps = new ArrayList<>();
        steps.add(new SignatureStripStep());
        steps.add(new ManifestStep());
        FusedJarRun.run(in, steps, ctx(in), out);

        java.util.jar.Manifest outMf = new java.util.jar.Manifest(
            Files.newInputStream(out.resolve("META-INF/MANIFEST.MF")));
        assertEquals("true", outMf.getMainAttributes().getValue(java.util.jar.Attributes.Name.SEALED),
            "unsigned+sealed inputs keep their seal — nothing was stripped");
    }

    private static byte[] manifestBytes(java.util.jar.Manifest mf) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        mf.write(bos);
        return bos.toByteArray();
    }

    @Test
    public void writeShadowsCopyAtSamePath() throws IOException {
        Path in = input(), out = dir.resolve("out");
        List<JarStep> steps = new ArrayList<>();
        steps.add(new JarStep() {
            @Override
            public JarEntry visitEntry(JarEntry entry, StepContext ctx) {
                if (entry.name.equals("e.txt")) {
                    ctx.sink.copy("shared.txt", entry.source);
                    ctx.sink.put("shared.txt", "written".getBytes(StandardCharsets.UTF_8));
                    return null;
                }
                return entry;
            }
        });
        FusedJarRun.run(in, steps, ctx(in), out);
        assertEquals("written", new String(Files.readAllBytes(out.resolve("shared.txt")), StandardCharsets.UTF_8));
    }

}
