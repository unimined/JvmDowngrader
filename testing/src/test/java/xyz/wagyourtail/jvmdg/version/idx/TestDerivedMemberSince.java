package xyz.wagyourtail.jvmdg.version.idx;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scan-vs-idx parity for {@code memberSince}: the idx carries it inline while
 * the scan path loads the flattened {@code META-INF/jvmdg/member-since}
 * resource ({@link DerivedMemberSince}). Both load paths must populate
 * identical maps — the map drives BEHAVIOR vs MISSING_API classification, so
 * divergence emits different stubs for the same input.
 */
public class TestDerivedMemberSince {

    private static Flags flags(boolean noStubIdx) {
        Flags f = new Flags();
        f.classVersion = Opcodes.V1_8;
        f.quiet = true;
        f.debugNoStubIdx = noStubIdx;
        return f;
    }

    @Test
    public void scanPathMemberSinceMatchesIndex() throws Exception {
        try (ClassDowngrader dIdx = ClassDowngrader.downgradeTo(flags(false));
             ClassDowngrader dScan = ClassDowngrader.downgradeTo(flags(true))) {
            Map<String, VersionProvider> scanned = new HashMap<>();
            for (VersionProvider vp : ServiceLoader.load(VersionProvider.class)) {
                vp.ensureInit(dScan);
                scanned.put(vp.getClass().getName(), vp);
            }
            int compared = 0;
            for (VersionProvider vp : ServiceLoader.load(VersionProvider.class)) {
                vp.ensureInit(dIdx);
                VersionProvider scan = scanned.get(vp.getClass().getName());
                assertNotNull(scan, "scan run found no provider for " + vp.getClass().getName());
                assertEquals(vp.stubRegistry.memberSince, scan.stubRegistry.memberSince,
                    vp.getClass().getName() + ": memberSince differs between idx and scan load paths");
                compared++;
            }
            assertTrue(compared > 0, "no VersionProviders on test classpath");
            // sanity: the data actually populated somewhere — an empty-vs-empty
            // compare would pass vacuously on an api jar missing the resource
            VersionProvider some = scanned.values().iterator().next();
            assertFalse(some.stubRegistry.memberSince.isEmpty(),
                "scan path populated no memberSince — is META-INF/jvmdg/member-since on the api classpath?");
        }
    }
}
