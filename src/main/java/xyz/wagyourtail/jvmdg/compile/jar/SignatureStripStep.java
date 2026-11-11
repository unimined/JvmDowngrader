package xyz.wagyourtail.jvmdg.compile.jar;

import xyz.wagyourtail.jvmdg.compile.ManifestUtils;

import java.io.IOException;

/**
 * Drops jarsigner signature files — transformed class bytes invalidate the
 * signed digests, so {@code META-INF/**\/*.SF|RSA|DSA|EC} entries never reach
 * the output.
 */
public class SignatureStripStep extends JarStep {

    @Override
    public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
        return ManifestUtils.isSignatureFile(entry.name) ? null : entry;
    }

}
