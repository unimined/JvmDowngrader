package xyz.wagyourtail.jvmdg.compile;

import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jvmdg.cli.Flags;

import java.util.Collections;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the shared jar-metadata helpers: signature-file stripping and
 * jvmdg manifest attributes (previously inlined in PathDowngrader; also adds
 * .EC signature blocks and manifest synthesis for inputs lacking one).
 * <p>
 * Note: {@code Flags.jvmdgVersion} is final and only populated from the jar
 * manifest at runtime, so the provenance attributes are exercised
 * structurally (they must be absent when it is null).
 */
public class TestManifestUtils {

    @Test
    public void signatureFilesDetected() {
        assertTrue(ManifestUtils.isSignatureFile("META-INF/TEST.SF"));
        assertTrue(ManifestUtils.isSignatureFile("META-INF/TEST.RSA"));
        assertTrue(ManifestUtils.isSignatureFile("META-INF/TEST.DSA"));
        assertTrue(ManifestUtils.isSignatureFile("META-INF/TEST.EC"), ".EC signature blocks must strip too");
        assertTrue(ManifestUtils.isSignatureFile("META-INF/sub/TEST.SF"));
    }

    @Test
    public void nonSignatureFilesPassThrough() {
        assertFalse(ManifestUtils.isSignatureFile("META-INF/MANIFEST.MF"));
        assertFalse(ManifestUtils.isSignatureFile("META-INF/versions/9/foo/Bar.class"));
        assertFalse(ManifestUtils.isSignatureFile("foo/Bar.SF"), "only under META-INF/");
        assertFalse(ManifestUtils.isSignatureFile("META-INF/TEST.SFX"));
        assertFalse(ManifestUtils.isSignatureFile("META-INF/services/foo.Service"));
    }

    @Test
    public void multiReleaseAndVersionAttrsWritten() {
        Flags flags = new Flags();
        flags.classVersion = 61;
        flags.multiReleaseOriginal = true;

        Manifest manifest = new Manifest();
        ManifestUtils.applyDowngradeAttributes(manifest, flags);
        Attributes attr = manifest.getMainAttributes();

        assertEquals("true", attr.getValue(ManifestUtils.MULTI_RELEASE));
        assertEquals("1.0", attr.getValue(Attributes.Name.MANIFEST_VERSION),
            "synthesized manifests need Manifest-Version to be valid");
        // jvmdgVersion is null under unit tests (no jar manifest) — when set,
        // both provenance attrs are written with these values.
        if (Flags.jvmdgVersion != null) {
            assertEquals(Flags.jvmdgVersion, attr.getValue(ManifestUtils.JVMDG_VERSION));
            assertEquals("61", attr.getValue(ManifestUtils.DOWNGRADE_TARGET));
        }
    }

    @Test
    public void noAttributesWithoutVersionOrMultiRelease() {
        Flags flags = new Flags();
        flags.multiReleaseOriginal = false;
        flags.multiReleaseVersions = Collections.emptySet();

        Manifest manifest = new Manifest();
        ManifestUtils.applyDowngradeAttributes(manifest, flags);
        Attributes attr = manifest.getMainAttributes();

        assertNull(attr.getValue(ManifestUtils.JVMDG_VERSION));
        assertNull(attr.getValue(ManifestUtils.DOWNGRADE_TARGET));
        assertNull(attr.getValue(ManifestUtils.MULTI_RELEASE));
        assertEquals("1.0", attr.getValue(Attributes.Name.MANIFEST_VERSION));
    }

    @Test
    public void needsMultiReleaseMatrix() {
        Flags flags = new Flags();
        flags.multiReleaseOriginal = false;
        flags.multiReleaseVersions = Collections.emptySet();
        assertFalse(ManifestUtils.needsMultiRelease(flags));

        flags.multiReleaseOriginal = true;
        assertTrue(ManifestUtils.needsMultiRelease(flags));

        flags.multiReleaseOriginal = false;
        flags.multiReleaseVersions = Collections.singleton(52);
        assertTrue(ManifestUtils.needsMultiRelease(flags));
    }
}
