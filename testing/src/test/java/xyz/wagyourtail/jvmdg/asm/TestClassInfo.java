package xyz.wagyourtail.jvmdg.asm;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.asm.info.ClassInfo;
import xyz.wagyourtail.jvmdg.asm.info.FieldInfo;
import xyz.wagyourtail.jvmdg.asm.info.MethodInfo;
import xyz.wagyourtail.jvmdg.util.IOFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestClassInfo {

    private static ClassNode sampleNode() {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "a/B";
        node.superName = "a/Base";
        node.interfaces = new ArrayList<>();
        node.interfaces.add("java/lang/Cloneable");
        node.methods = new ArrayList<>();
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "foo", "()V", null, null);
        node.methods.add(m);
        node.fields = new ArrayList<>();
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "x", "I", null, null));
        return node;
    }

    @Test
    public void ofClassNodeExposesShapeAndMembers() {
        ClassInfo info = ClassInfo.of(sampleNode());
        assertEquals("a/B", info.getName());
        assertEquals("a/Base", info.getSuperName());
        assertFalse(info.isInterface());
        assertEquals(Opcodes.V1_8, info.getVersion());
        assertEquals(1, info.getInterfaces().size());
        assertEquals("java/lang/Cloneable", info.getInterfaces().get(0));
        assertEquals(1, info.getMethods().size());
        assertEquals(1, info.getFields().size());
    }

    @Test
    public void ofClassReaderReadsVersionAndInterfaceFlag() {
        ClassNode node = sampleNode();
        node.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
        node.superName = "java/lang/Object";
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        ClassInfo info = ClassInfo.of(new ClassReader(writer.toByteArray()));
        assertTrue(info.isInterface());
        assertEquals("a/B", info.getName());
        assertEquals(Opcodes.V1_8, info.getVersion());
        // members are lazy but must still resolve from the reader
        assertTrue(info.getMethods().contains(new MethodInfo(Opcodes.ACC_PUBLIC, "foo", Type.getMethodType("()V"))));
        assertTrue(info.getFields().contains(new FieldInfo(Opcodes.ACC_PRIVATE, "x", Type.getType("I"))));
    }

    @Test
    public void ofClassUsesReflectionMetadata() {
        ClassInfo info = ClassInfo.of(ArrayList.class);
        assertEquals("java/util/ArrayList", info.getName());
        assertEquals("java/util/AbstractList", info.getSuperName());
        assertFalse(info.isInterface());
        assertTrue(info.getInterfaces().contains("java/util/List"));
        assertFalse(info.getMethods().isEmpty());
        assertFalse(info.getFields().isEmpty());
    }

    @Test
    public void equalityIsByNameOnly() {
        ClassInfo a = ClassInfo.of(sampleNode());
        ClassNode other = sampleNode();
        other.methods.clear();
        other.fields.clear();
        other.superName = "java/lang/Object";
        ClassInfo b = ClassInfo.of(other);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void fromClassloaderWithoutLoadingMissesAreNull() throws IOException {
        IOFunction<String, ClassInfo> r = ClassInfo.fromClassloaderWithoutLoading(getClass().getClassLoader());
        assertNull(r.apply("no/such/Clazz"));
        ClassInfo info = r.apply("java/lang/Object");
        assertNotNull(info);
        assertEquals("java/lang/Object", info.getName());
    }

    @Test
    public void cachingRetrieverCallsImplOnce() throws IOException {
        final AtomicInteger calls = new AtomicInteger();
        Map<String, ClassInfo> cache = new HashMap<>();
        IOFunction<String, ClassInfo> r = ClassInfo.caching(new IOFunction<String, ClassInfo>() {
            @Override
            public ClassInfo apply(String s) {
                calls.incrementAndGet();
                return ClassInfo.of(sampleNode());
            }
        }, cache);
        assertSame(r.apply("a/B"), r.apply("a/B"));
        assertEquals(1, calls.get());
    }

    @Test
    public void withFallbackUsesBackupOnNullAndThrow() throws IOException {
        IOFunction<String, ClassInfo> missing = new IOFunction<String, ClassInfo>() {
            @Override
            public ClassInfo apply(String s) {
                return null;
            }
        };
        IOFunction<String, ClassInfo> boom = new IOFunction<String, ClassInfo>() {
            @Override
            public ClassInfo apply(String s) throws IOException {
                throw new IOException("nope");
            }
        };
        IOFunction<String, ClassInfo> good = ClassInfo.fromClassloaderWithoutLoading(getClass().getClassLoader());
        assertNotNull(ClassInfo.withFallback(missing, good).apply("java/lang/Object"));
        assertNotNull(ClassInfo.withFallback(boom, good).apply("java/lang/Object"));
        assertNull(ClassInfo.withFallback(missing, good).apply("no/such/Clazz"));
    }

}
