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
    private final java.util.concurrent.atomic.AtomicReference<byte[]> manifestBytes =
        new java.util.concurrent.atomic.AtomicReference<>();

    @Override
    public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
        if (!ManifestUtils.MANIFEST_PATH.equals(entry.name)) {
            return entry;
        }
        // defer emission to endFused: whether Sealed must be stripped depends
        // on whether signature files were dropped anywhere in the jar, and
        // entry order doesn't guarantee they precede the manifest
        sawManifest.set(true);
        manifestBytes.set(entry.bytes());
        return null;
    }

    @Override
    public void endFused(StepContext ctx) throws IOException {
        byte[] raw = manifestBytes.get();
        if (raw == null && !ManifestUtils.needsMultiRelease(ctx.flags)) {
            return;
        }
        Manifest manifest = raw == null ? new Manifest()
            : new Manifest(new ByteArrayInputStream(raw));
        if (ctx.data.containsKey(SignatureStripStep.SIGNATURE_STRIPPED)) {
            ManifestUtils.stripSealedAttributes(manifest);
        }
        ManifestUtils.applyDowngradeAttributes(manifest, ctx.flags);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        manifest.write(bos);
        ctx.sink.put(ManifestUtils.MANIFEST_PATH, bos.toByteArray());
        ctx.sink.mkdir("META-INF");
    }

}
