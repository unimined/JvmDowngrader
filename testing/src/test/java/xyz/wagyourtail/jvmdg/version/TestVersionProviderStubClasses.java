package xyz.wagyourtail.jvmdg.version;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.stub.all.RemovedInterfaces;
import xyz.wagyourtail.jvmdg.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class TestVersionProviderStubClasses {

    private static final String REMOVED_DESC = Type.getType(RemovedInterfaces.class).getDescriptor();

    @Adapter(value = "com/example/RemovedIface", keepInterface = false)
    static class RemovedIfaceStub {
    }

    @Adapter(value = "com/example/KeptIface", keepInterface = true)
    static class KeptIfaceStub {
    }

    static class TestProvider extends VersionProvider {
        TestProvider() {
            super(Opcodes.V11, Opcodes.V1_8, 0);
        }

        @Override
        public void preInit() {
        }

        @Override
        public void init() {
        }
    }

    private static TestProvider provider() {
        TestProvider provider = new TestProvider();
        provider.classStubs.put(
            Type.getObjectType("com/example/RemovedIface"),
            new Pair<Type, Pair<Class<?>, Adapter>>(
                Type.getObjectType("jvmdg/stub/RemovedIface"),
                new Pair<Class<?>, Adapter>(RemovedIfaceStub.class, RemovedIfaceStub.class.getAnnotation(Adapter.class))
            )
        );
        provider.classStubs.put(
            Type.getObjectType("com/example/KeptIface"),
            new Pair<Type, Pair<Class<?>, Adapter>>(
                Type.getObjectType("jvmdg/stub/KeptIface"),
                new Pair<Class<?>, Adapter>(KeptIfaceStub.class, KeptIfaceStub.class.getAnnotation(Adapter.class))
            )
        );
        return provider;
    }

    private static ClassNode classImplementing(String... interfaces) {
        ClassNode node = new ClassNode();
        node.name = "com/example/Target";
        node.superName = "java/lang/Object";
        node.interfaces = new ArrayList<>(Arrays.asList(interfaces));
        return node;
    }

    @Test
    public void removedInterfaceOnUnannotatedClassDoesNotNpe() {
        // ASM leaves visibleAnnotations null for unannotated classes — the removedInterfaces
        // block used to NPE on exactly this shape
        TestProvider provider = provider();
        ClassNode node = classImplementing("com/example/RemovedIface");

        assertDoesNotThrow(() -> provider.stubClasses(node, false, new HashSet<String>()));

        assertTrue(node.interfaces.isEmpty(), "stub-removed interface must be dropped");
        assertNotNull(node.visibleAnnotations, "annotation list must be initialized");
        assertEquals(1, node.visibleAnnotations.size());
        AnnotationNode an = node.visibleAnnotations.get(0);
        assertEquals(REMOVED_DESC, an.desc);
        @SuppressWarnings("unchecked")
        java.util.List<String> removed = (java.util.List<String>) an.values.get(1);
        assertEquals(Collections.singletonList("com.example.RemovedIface"), removed);
    }

    @Test
    public void removedInterfaceMergesIntoExistingAnnotation() {
        TestProvider provider = provider();
        ClassNode node = classImplementing("com/example/RemovedIface");
        AnnotationNode existing = new AnnotationNode(REMOVED_DESC);
        existing.values = new ArrayList<>();
        existing.values.add("value");
        existing.values.add(new ArrayList<>(Collections.singletonList("com.example.AlreadyRemoved")));
        node.visibleAnnotations = new ArrayList<>(Collections.singletonList(existing));

        provider.stubClasses(node, false, new HashSet<String>());

        assertTrue(node.interfaces.isEmpty());
        assertEquals(1, node.visibleAnnotations.size(), "must merge into the existing annotation");
        @SuppressWarnings("unchecked")
        java.util.List<String> removed = (java.util.List<String>) existing.values.get(1);
        assertTrue(removed.contains("com.example.AlreadyRemoved"));
        assertTrue(removed.contains("com.example.RemovedIface"));
        assertEquals(2, removed.size());
    }

    @Test
    public void keptInterfaceIsRemappedNotRemoved() {
        TestProvider provider = provider();
        ClassNode node = classImplementing("com/example/KeptIface");

        provider.stubClasses(node, false, new HashSet<String>());

        assertEquals(Collections.singletonList("jvmdg/stub/KeptIface"), node.interfaces);
        assertNull(node.visibleAnnotations, "no RemovedInterfaces annotation expected");
    }

    @Test
    public void unrelatedInterfacesAreUntouched() {
        TestProvider provider = provider();
        ClassNode node = classImplementing("java/io/Serializable", "com/example/KeptIface", "java/lang/Cloneable");

        provider.stubClasses(node, false, new HashSet<String>());

        assertEquals(Arrays.asList("java/io/Serializable", "jvmdg/stub/KeptIface", "java/lang/Cloneable"),
            node.interfaces, "stub lookup must not disturb unregistered interfaces");
    }
}
