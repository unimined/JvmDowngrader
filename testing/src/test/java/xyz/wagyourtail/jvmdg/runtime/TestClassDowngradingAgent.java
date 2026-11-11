package xyz.wagyourtail.jvmdg.runtime;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestClassDowngradingAgent {

    private static byte[] ownClassBytes() throws IOException {
        try (InputStream is = TestClassDowngradingAgent.class.getResourceAsStream(
            "/xyz/wagyourtail/jvmdg/runtime/TestClassDowngradingAgent.class")) {
            assertNotNull(is);
            return is.readAllBytes();
        }
    }

    @Test
    public void malformedClassThrowsNotExits() throws IOException {
        ClassDowngradingAgent agent = new ClassDowngradingAgent();
        ClassLoader loader = getClass().getClassLoader();
        // truncated
        assertThrows(IllegalClassFormatException.class,
            () -> agent.transform(loader, "a/B", null, null, new byte[]{(byte) 0xCA, (byte) 0xFE}));
        // empty
        assertThrows(IllegalClassFormatException.class,
            () -> agent.transform(loader, "a/B", null, null, new byte[0]));
        // wrong magic, valid length
        assertThrows(IllegalClassFormatException.class,
            () -> agent.transform(loader, "a/B", null, null, new byte[]{1, 2, 3, 4, 5, 6, 7, 8}));
    }

    @Test
    public void currentVersionClassIsUntouched() throws IOException, IllegalClassFormatException {
        ClassDowngradingAgent agent = new ClassDowngradingAgent();
        // our own class is compiled at the version this test jvm runs at,
        // so the agent must pass it through (null) rather than transform
        byte[] out = agent.transform(getClass().getClassLoader(),
            "xyz/wagyourtail/jvmdg/runtime/TestClassDowngradingAgent", null, null, ownClassBytes());
        assertNull(out);
    }

    @Test
    public void nullClassNameDoesNotCrash() {
        ClassDowngradingAgent agent = new ClassDowngradingAgent();
        // hidden classes pass a null className — must fail cleanly, not NPE/exit
        assertThrows(IllegalClassFormatException.class,
            () -> agent.transform(getClass().getClassLoader(), null, null, null, new byte[]{1, 2, 3}));
    }

    @Test
    public void tooNewClassFailsCleanly() throws IOException {
        ClassDowngradingAgent agent = new ClassDowngradingAgent();
        byte[] bytes = ownClassBytes();
        // patch major version to something far in the future
        bytes[6] = (byte) 0x03;
        bytes[7] = (byte) 0xE8; // major 1000
        assertThrows(IllegalClassFormatException.class,
            () -> agent.transform(getClass().getClassLoader(),
                "xyz/wagyourtail/jvmdg/runtime/TestClassDowngradingAgent", null, null, bytes));
    }

    @Test
    public void multiReleaseResourceIsPreferred() throws IOException, IllegalClassFormatException {
        // on the current-version downgrader there are no MR versions to probe,
        // so inject a fake version list to exercise the lookup path
        ClassDowngradingAgent agent = new ClassDowngradingAgent() {
            @Override
            protected List<Integer> multiVersions() {
                return Collections.singletonList(53); // probes META-INF/versions/9/
            }
        };
        boolean prev = Bootstrap.flags.downgradeFromMultiReleases;
        Bootstrap.flags.downgradeFromMultiReleases = true;
        try {
            final List<String> requested = new ArrayList<>();
            final byte[] versionedBytes = ownClassBytes();
            ClassLoader fake = new ClassLoader() {
                @Override
                public InputStream getResourceAsStream(String name) {
                    if (name.startsWith("META-INF/versions/") &&
                        name.endsWith("/xyz/wagyourtail/jvmdg/runtime/TestClassDowngradingAgent.class")) {
                        requested.add(name);
                        return new ByteArrayInputStream(versionedBytes);
                    }
                    return null;
                }
            };
            byte[] out = agent.transform(fake,
                "xyz/wagyourtail/jvmdg/runtime/TestClassDowngradingAgent", null, null, new byte[0]);
            assertNull(out); // versioned bytes are at current version -> passthrough
            assertFalse(requested.isEmpty(), "agent should have probed META-INF/versions/ resources");

            // when the versioned copy is garbage it must be used (and fail cleanly)
            ClassLoader fake2 = new ClassLoader() {
                @Override
                public InputStream getResourceAsStream(String name) {
                    if (name.startsWith("META-INF/versions/")) {
                        return new ByteArrayInputStream(new byte[]{9, 9, 9, 9, 9, 9, 9, 9});
                    }
                    return null;
                }
            };
            assertThrows(IllegalClassFormatException.class,
                () -> agent.transform(fake2,
                    "xyz/wagyourtail/jvmdg/runtime/TestClassDowngradingAgent", null, null, ownClassBytes()));
        } finally {
            Bootstrap.flags.downgradeFromMultiReleases = prev;
        }
    }

    @Test
    public void retransformCodeSourceInitsLocalInCtorOnly() throws IOException {
        ClassDowngradingAgent agent = new ClassDowngradingAgent();
        byte[] out = agent.retransformCodeSource(fakeCtorClassBytes());
        ClassNode node = new ClassNode();
        new ClassReader(out).accept(node, 0);
        Map<String, Integer> expected = new HashMap<>();
        // desc -> the ASTORE slot that must be nulled, null = no injection
        expected.put("(Ljava/lang/Object;Ljava/lang/Object;)V", 2);
        expected.put("(JLjava/lang/Object;)V", 3); // long occupies slots 1-2
        expected.put("(Ljava/lang/Object;[Ljava/lang/Object;)V", 2);
        expected.put("()V", null);
        expected.put("(Ljava/lang/Object;)V", null);
        expected.put("(Ljava/lang/Object;I)V", null);
        for (MethodNode method : node.methods) {
            AbstractInsnNode first = method.instructions.getFirst();
            while (first != null && first.getOpcode() == -1) first = first.getNext();
            if (method.name.equals("<init>")) {
                assertTrue(expected.containsKey(method.desc),
                    "unexpected ctor shape in fixture: " + method.desc);
                Integer slot = expected.remove(method.desc);
                if (slot == null) {
                    assertNotEquals(Opcodes.ACONST_NULL, first.getOpcode(),
                        "ctor " + method.desc + " must not get the local init (no 2nd reference arg)");
                } else {
                    assertNotNull(first);
                    assertEquals(Opcodes.ACONST_NULL, first.getOpcode());
                    AbstractInsnNode second = first.getNext();
                    while (second != null && second.getOpcode() == -1) second = second.getNext();
                    assertNotNull(second);
                    assertTrue(second instanceof VarInsnNode);
                    assertEquals(Opcodes.ASTORE, second.getOpcode());
                    assertEquals((int) slot, ((VarInsnNode) second).var);
                }
            } else {
                if (first instanceof InsnNode) {
                    assertNotEquals(Opcodes.ACONST_NULL, first.getOpcode(),
                        "non-ctor method " + method.name + " must not get the local init");
                }
            }
        }
        assertTrue(expected.isEmpty(), "fixture ctors were not all visited: " + expected.keySet());
    }

    /**
     * Builds a class with constructors covering the arg shapes the
     * CodeSource retransform must distinguish — it nulls the 2nd parameter
     * only when it exists and is a reference type.
     */
    private static byte[] fakeCtorClassBytes() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "fake/Cls", null, "java/lang/Object", null);
        String[] ctorDescs = {
            "(Ljava/lang/Object;Ljava/lang/Object;)V",
            "(JLjava/lang/Object;)V",
            "(Ljava/lang/Object;[Ljava/lang/Object;)V",
            "()V",
            "(Ljava/lang/Object;)V",
            "(Ljava/lang/Object;I)V",
        };
        for (String desc : ctorDescs) {
            MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", desc, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 4);
            mv.visitEnd();
        }
        MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC, "foo", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
