package xyz.wagyourtail.jvmdg.stub;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.CoreRuntimeDowngrader;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OptionalDependency} resolves the core jar's {@link RuntimeDowngrader}
 * over {@link java.util.ServiceLoader} when the core is on the classpath —
 * which it always is in this test module. The absent-backend half is exercised
 * implicitly by every shaded-jar run (no core jar inside).
 */
public class TestOptionalDependency {

    private static byte[] trivialClass(String name, int version) {
        ClassNode node = new ClassNode();
        node.version = version;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        return ASMUtils.classNodeToBytes(node, 0);
    }

    @Test
    public void downgraderResolvesCoreBackend() {
        Optional<RuntimeDowngrader> backend = OptionalDependency.downgrader();
        assertTrue(backend.isPresent());
        assertTrue(backend.get() instanceof CoreRuntimeDowngrader);
        // cached — same Optional every call
        assertSame(backend, OptionalDependency.downgrader());
    }

    @Test
    public void backendDelegatesToCurrentDowngrader() throws Exception {
        // The shared downgrader targets the running JVM, so a V1_8 class is
        // already at/below target: real delegation returns null (early exit),
        // and bad magic surfaces ClassDowngrader's IllegalClassFormatException.
        RuntimeDowngrader backend = OptionalDependency.downgrader().get();
        AtomicReference<String> name = new AtomicReference<>();
        assertNull(backend.downgrade(name, trivialClass("foo/OptionalDep", Opcodes.V1_8), true, s -> null));
        assertThrows(java.lang.instrument.IllegalClassFormatException.class,
            () -> backend.downgrade(name, new byte[]{1, 2, 3}, true, s -> null));
    }

}
