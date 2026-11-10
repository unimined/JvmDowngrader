package xyz.wagyourtail.jvmdg.compile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.wagyourtail.jvmdg.cli.Flags;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The downgraded-api cache filename must include every flag that changes the
 * produced bytecode — previously only classVersion was keyed, so runs with
 * different flags silently reused each other's artifacts.
 */
public class TestDowngradedApiName {

    private static File apiJar(Path dir) throws Exception {
        Path p = dir.resolve("fake-api.jar");
        Files.write(p, new byte[]{1, 2, 3, 4, 5});
        return p.toFile();
    }

    private static Flags baseFlags() {
        Flags flags = new Flags();
        flags.classVersion = 52;
        flags.multiReleaseOriginal = false;
        flags.multiReleaseVersions = Collections.emptySet();
        flags.downgradeFromMultiReleases = false;
        flags.debugNoSynthetic = false;
        flags.debugSkipStubs = Collections.emptySet();
        flags.debugSkipJeps = Collections.emptySet();
        flags.debugSkipStub = Collections.emptySet();
        return flags;
    }

    @Test
    public void sameFlagsProduceSameName(@TempDir Path dir) throws Exception {
        File f = apiJar(dir);
        assertEquals(ApiShader.downgradedApiName(f, baseFlags()),
            ApiShader.downgradedApiName(f, baseFlags()));
    }

    @Test
    public void outputAffectingFlagsChangeTheName(@TempDir Path dir) throws Exception {
        File f = apiJar(dir);
        String base = ApiShader.downgradedApiName(f, baseFlags());

        Flags f1 = baseFlags();
        f1.multiReleaseOriginal = true;
        assertNotEquals(base, ApiShader.downgradedApiName(f, f1), "multiReleaseOriginal must key the cache");

        Flags f2 = baseFlags();
        f2.multiReleaseVersions = new TreeSet<>(Collections.singleton(9));
        assertNotEquals(base, ApiShader.downgradedApiName(f, f2), "multiReleaseVersions must key the cache");

        Flags f3 = baseFlags();
        f3.downgradeFromMultiReleases = true;
        assertNotEquals(base, ApiShader.downgradedApiName(f, f3), "downgradeFromMultiReleases must key the cache");

        Flags f4 = baseFlags();
        f4.debugNoSynthetic = true;
        assertNotEquals(base, ApiShader.downgradedApiName(f, f4), "debugNoSynthetic must key the cache");

        Flags f5 = baseFlags();
        f5.classVersion = 61;
        assertNotEquals(base, ApiShader.downgradedApiName(f, f5), "classVersion must key the cache");
    }

    @Test
    public void nameContainsVersionAndSha(@TempDir Path dir) throws Exception {
        File f = apiJar(dir);
        String name = ApiShader.downgradedApiName(f, baseFlags());
        assertTrue(name.startsWith("fake-api-"), name);
        assertTrue(name.contains("-downgraded-52-"), name);
        assertTrue(name.endsWith(".jar"), name);
    }

    @Test
    public void fileWithoutExtensionRejected(@TempDir Path dir) throws Exception {
        Path p = dir.resolve("noext");
        Files.write(p, new byte[]{1});
        assertThrows(IllegalArgumentException.class,
            () -> ApiShader.downgradedApiName(p.toFile(), baseFlags()));
    }
}
