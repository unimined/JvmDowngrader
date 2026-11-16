package xyz.wagyourtail.jvmdg.compile;

import xyz.wagyourtail.jvmdg.cli.Flags;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Shared helpers for jar metadata written/read during downgrade:
 * signature-file detection and jvmdg manifest attributes.
 */
public final class ManifestUtils {

    /** Archive-relative path of the jar manifest. */
    public static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";
    /** Manifest attribute recording the jvmdg version that produced the jar. */
    public static final String JVMDG_VERSION = "JvmDowngrader-Version";
    /** Manifest attribute recording the class-file version the jar was downgraded to. */
    public static final String DOWNGRADE_TARGET = "JvmDowngrader-Downgrade-Target";
    /** Standard Multi-Release attribute. */
    public static final String MULTI_RELEASE = "Multi-Release";

    private ManifestUtils() {
    }

    /**
     * Whether a {@code /}-separated archive-relative path names a jarsigner
     * signature file. Signature files must be stripped from transformed output
     * because the signed digests no longer match the rewritten classes.
     *
     * @param path archive-relative path using {@code /} separators
     * @return true for {@code META-INF/**\/*.SF}, {@code .RSA}, {@code .DSA}, {@code .EC}
     */
    public static boolean isSignatureFile(String path) {
        if (!path.startsWith("META-INF/")) {
            return false;
        }
        return path.endsWith(".SF") || path.endsWith(".RSA") || path.endsWith(".DSA") || path.endsWith(".EC");
    }

    /**
     * Writes jvmdg provenance attributes ({@link #JVMDG_VERSION},
     * {@link #DOWNGRADE_TARGET}) and the {@link #MULTI_RELEASE} flag onto a
     * manifest, and ensures {@code Manifest-Version} is present so synthesized
     * manifests are valid.
     */
    public static void applyDowngradeAttributes(Manifest manifest, Flags flags) {
        Attributes attr = manifest.getMainAttributes();
        if (Flags.jvmdgVersion != null) {
            attr.putValue(JVMDG_VERSION, Flags.jvmdgVersion);
            attr.putValue(DOWNGRADE_TARGET, String.valueOf(flags.classVersion));
        }
        if (flags.multiReleaseOriginal || !flags.multiReleaseVersions.isEmpty()) {
            attr.putValue(MULTI_RELEASE, "true");
        }
        if (attr.getValue(Attributes.Name.MANIFEST_VERSION) == null) {
            attr.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        }
    }

    /**
     * True when the flags request multi-release output, in which case a
     * {@code Multi-Release: true} manifest is required for the versioned
     * entries to be honored.
     */
    public static boolean needsMultiRelease(Flags flags) {
        return flags.multiReleaseOriginal || !flags.multiReleaseVersions.isEmpty();
    }
}
