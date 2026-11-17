package xyz.wagyourtail.jvmdg.compile.jar;

import xyz.wagyourtail.jvmdg.compile.ManifestUtils;

import java.io.IOException;

/**
 * Drops jarsigner signature files — transformed class bytes invalidate the
 * signed digests, so {@code META-INF/**\/*.SF|RSA|DSA|EC} entries never reach
 * the output.
 */
public class SignatureStripStep extends JarStep {

    /** {@link StepContext#data} key: set when any signature file was dropped — ManifestStep strips {@code Sealed:} attrs in response. */
    public static final String SIGNATURE_STRIPPED = "jvmdg.signatureStripped";

    @Override
    public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
        if (!ManifestUtils.isSignatureFile(entry.name)) {
            return entry;
        }
        ctx.data.put(SIGNATURE_STRIPPED, Boolean.TRUE);
        return null;
    }

}
