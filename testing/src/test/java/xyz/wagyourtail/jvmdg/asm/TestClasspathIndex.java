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
        assertEquals(1, resourceReads.get(), "miss must be read at most once");
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
