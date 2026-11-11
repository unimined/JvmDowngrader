package xyz.wagyourtail.jvmdg.compile.jar;

import xyz.wagyourtail.jvmdg.compile.ManifestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Manifest;

/**
 * Rewrites {@code META-INF/MANIFEST.MF} with jvmdg downgrade attributes
 * ({@link ManifestUtils#applyDowngradeAttributes}), and when the flags request
 * multi-release output synthesizes one at {@link #endFused} if the input
 * carried none — the JVM ignores {@code META-INF/versions} without a
 * {@code Multi-Release: true} manifest.
 */
public class ManifestStep extends JarStep {

    private final AtomicBoolean sawManifest = new AtomicBoolean();

    @Override
    public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
        if (!ManifestUtils.MANIFEST_PATH.equals(entry.name)) {
            return entry;
        }
        sawManifest.set(true);
        Manifest manifest = new Manifest(new ByteArrayInputStream(entry.bytes()));
        ManifestUtils.applyDowngradeAttributes(manifest, ctx.flags);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        manifest.write(bos);
        return entry.withData(bos.toByteArray());
    }

    @Override
    public void endFused(StepContext ctx) throws IOException {
        if (sawManifest.get() || !ManifestUtils.needsMultiRelease(ctx.flags)) {
            return;
        }
        Manifest manifest = new Manifest();
        ManifestUtils.applyDowngradeAttributes(manifest, ctx.flags);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        manifest.write(bos);
        ctx.sink.put(ManifestUtils.MANIFEST_PATH, bos.toByteArray());
        ctx.sink.mkdir("META-INF");
    }

}
