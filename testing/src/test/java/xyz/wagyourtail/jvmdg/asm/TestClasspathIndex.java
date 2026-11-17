package xyz.wagyourtail.jvmdg.asm;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.asm.index.ClasspathIndex;
import xyz.wagyourtail.jvmdg.asm.info.ClassInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestClasspathIndex {

    @Test
    public void loaderInfoResolvesRealClasses() throws IOException {
        ClasspathIndex index = new ClasspathIndex(getClass().getClassLoader());
        ClassInfo info = index.loaderInfo("java/lang/Object");
        assertNotNull(info);
        assertEquals("java/lang/Object", info.getName());
        assertFalse(info.isInterface());
        assertNull(index.loaderInfo("no/such/Clazz"));
    }

    @Test
    public void loaderInfoMemoizesHits() throws IOException {
        ClasspathIndex index = new ClasspathIndex(getClass().getClassLoader());
        assertSame(index.loaderInfo("java/util/ArrayList"), index.loaderInfo("java/util/ArrayList"));
    }

    @Test
    public void loaderInfoMemoizesMisses() throws IOException {
        final AtomicInteger resourceReads = new AtomicInteger();
        ClassLoader counting = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public InputStream getResourceAsStream(String name) {
                if (name.startsWith("no/such/")) {
                    resourceReads.incrementAndGet();
                    return null;
                }
                return super.getResourceAsStream(name);
            }
        };
        ClasspathIndex index = new ClasspathIndex(counting);
        assertNull(index.loaderInfo("no/such/Clazz"));
        assertNull(index.loaderInfo("no/such/Clazz"));
        // misses re-probe by design — permanently caching a null result
        // poisoned stub lookup for the downgrader's lifetime whenever a
        // transient resource failure raced the lookup — 2 calls x 1 probe
        assertEquals(2, resourceReads.get(), "miss must re-probe on each lookup");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void loaderInfoReplacesDeadSoftReference() throws Exception {
        ClasspathIndex index = new ClasspathIndex(getClass().getClassLoader());
        // plant a GC-cleared ref the way heap pressure leaves one behind —
        // the old putIfAbsent install kept serving the dead slot as a miss
        java.lang.reflect.Field f = ClasspathIndex.class.getDeclaredField("loaderInfos");
        f.setAccessible(true);
        java.util.Map<String, java.lang.ref.SoftReference<ClassInfo>> map =
            (java.util.Map<String, java.lang.ref.SoftReference<ClassInfo>>) f.get(index);
        java.lang.ref.SoftReference<ClassInfo> dead = new java.lang.ref.SoftReference<>(null);
        dead.clear();
        map.put("java/lang/String", dead);
        ClassInfo info = index.loaderInfo("java/lang/String");
        assertNotNull(info, "dead soft-reference slot must be replaced, not served as a miss");
        assertEquals("java/lang/String", info.getName());
        assertSame(info, index.loaderInfo("java/lang/String"));
    }

    @Test
    public void workingLayerIndexesByName() {
        ClasspathIndex index = new ClasspathIndex(getClass().getClassLoader());
        assertNull(index.workingAt("a/B"));
        ClassNode node = new ClassNode();
        node.name = "a/B";
        index.registerWorking(node);
        assertSame(node, index.workingAt("a/B"));
        index.clearWorking();
        assertNull(index.workingAt("a/B"));
    }

}
