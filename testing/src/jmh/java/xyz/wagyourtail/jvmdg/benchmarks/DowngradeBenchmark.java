package xyz.wagyourtail.jvmdg.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.util.Function;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Transform-time baseline: downgrades the whole feature corpus jar to Java 8
 * (the longest provider chain, so every stage of the pipeline runs).
 * <p>
 * Hierarchy lookups are served from the input jar itself — the same shape as
 * {@code ZipDowngrader} — so this exercises the classpath index, not just the
 * per-class transforms.
 */
@State(Scope.Benchmark)
public class DowngradeBenchmark {

    private Map<String, byte[]> entries;
    private String[] names;
    private ClassDowngrader downgrader;

    @Setup
    public void setup() throws IOException {
        File jar = new File("build/libs/featuretests.jar");
        if (!jar.isFile()) {
            throw new IllegalStateException("corpus jar missing: " + jar.getAbsolutePath() + " (run :testing:featureJar)");
        }
        entries = new HashMap<>();
        JarFile jf = new JarFile(jar);
        try {
            Enumeration<JarEntry> e = jf.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    entries.put(name.substring(0, name.length() - 6), readAll(jf.getInputStream(entry)));
                }
            }
        } finally {
            jf.close();
        }
        names = entries.keySet().toArray(new String[0]);
        Flags flags = new Flags();
        flags.classVersion = Opcodes.V1_8;
        flags.quiet = true;
        downgrader = ClassDowngrader.downgradeTo(flags);
    }

    @TearDown
    public void tearDown() throws IOException {
        downgrader.close();
    }

    @Benchmark
    public void downgradeFeatureCorpusTo8(Blackhole bh) throws Exception {
        for (int i = 0; i < names.length; i++) {
            AtomicReference<String> name = new AtomicReference<>(names[i]);
            bh.consume(downgrader.downgrade(name, entries.get(names[i]), false, new Function<String, byte[]>() {
                @Override
                public byte[] apply(String s) {
                    return entries.get(s);
                }
            }));
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) {
            out.write(buf, 0, r);
        }
        in.close();
        return out.toByteArray();
    }
}
