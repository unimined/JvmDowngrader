package xyz.wagyourtail.jvmdg.version;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.pipeline.Stage;
import xyz.wagyourtail.jvmdg.pipeline.TransformContributor;
import xyz.wagyourtail.jvmdg.providers.Java10Downgrader;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two CG-3d SPIs: {@link TransformContributor} (pipeline stage
 * contribution at fixed anchors, ServiceLoader-discovered, deterministic
 * order) and {@link ReflectionContributor} (single declaration point for
 * the reflection-stub surface providers apply at every boundary).
 */
public class TestContributors {

    /**
     * Records anchor invocations; registered through the test classpath's
     * {@code META-INF/services} so every pipeline built in this suite also
     * exercises discovery. Contributes nothing — observing only.
     */
    public static class RecordingContributor implements TransformContributor {
        public static final List<Anchor> SEEN = new CopyOnWriteArrayList<>();

        @Override
        public void contribute(Anchor anchor, ClassDowngrader downgrader, List<Stage> out) {
            SEEN.add(anchor);
        }
    }

    @Test
    public void transformContributorAnchorsAroundVersionChain() {
        RecordingContributor.SEEN.clear();
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            DowngradeRegistrar reg = new DowngradeRegistrar(d, false, null);
            List<Stage> stages = reg.stages(Opcodes.V9);
            assertFalse(stages.isEmpty(), "expected at least the V9->V8 stage");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertTrue(RecordingContributor.SEEN.contains(TransformContributor.Anchor.BEFORE_VERSION_CHAIN),
            "BEFORE anchor never invoked");
        assertTrue(RecordingContributor.SEEN.contains(TransformContributor.Anchor.AFTER_VERSION_CHAIN),
            "AFTER anchor never invoked");
        assertTrue(
            RecordingContributor.SEEN.indexOf(TransformContributor.Anchor.BEFORE_VERSION_CHAIN)
                < RecordingContributor.SEEN.lastIndexOf(TransformContributor.Anchor.AFTER_VERSION_CHAIN),
            "BEFORE must precede AFTER");
    }

    @Test
    public void reflectionContributorRegistersStubSurface() {
        boolean found = false;
        for (ReflectionContributor c : ServiceLoader.load(ReflectionContributor.class)) {
            if (c.getClass().getName().equals(
                "xyz.wagyourtail.jvmdg.providers.CoreReflectionContributor")) {
                found = true;
            }
        }
        assertTrue(found, "CoreReflectionContributor not discovered via ServiceLoader");
        // preInit routes through the contributor set — the reflection
        // surface lands without a provider-inline stub() list. Forcing the
        // scan path so preInit actually runs (idx load would skip it).
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            d.flags.debugNoStubIdx = true;
            Java10Downgrader p = new Java10Downgrader();
            p.ensureInit(d);
            assertTrue(p.stubRegistry.owners().containsKey("java/lang/Class"),
                "reflection stubs absent after contributor-driven preInit");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
