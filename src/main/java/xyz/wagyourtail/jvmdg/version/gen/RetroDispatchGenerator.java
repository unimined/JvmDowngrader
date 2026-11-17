package xyz.wagyourtail.jvmdg.version.gen;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Emits {@code <adapter>$jvmdg$retro} — the per-adapter runtime surface the
 * transform's emitted callsites target: real bytecode for the dispatch and
 * construction paths (wrap's {@code instanceof}/{@code new} chain, the
 * adapter/marker/impl-member instanceofs for {@code instanceof}/{@code
 * checkcast}) that a plain {@code new} makes C2-inlinable and
 * scalar-replaceable, while the name-keyed {@code RetroTypes} tables stay
 * the fallback tier for the bulk JDK-member sets (hundreds-to-thousands of
 * members can't be an {@code instanceof} chain), externally-registered
 * rows, and the missing-on-this-runtime member types the
 * {@code catch (LinkageError)} region defers to.
 * <p>
 * Emitted at {@code V1_8} inside the api jar — an ordinary api class, so
 * reference indexing, prefix shading, inlining, pruning, and runtime
 * downgrade all treat it like every other stub impl. Every emitted callsite
 * is a single {@code invokestatic}; the generator emits a class for
 * <em>every</em> adapter (impl-less OWN_INTERFACE adapters get a
 * fallback-only {@code wrap}) so emission gates on {@code mode} alone and
 * the scan/idx load paths stay identical.
 * <p>
 * Method set per {@link Adapter.Mode}: {@code wrap} is OWN_INTERFACE-only
 * (member adaptation — SUPERTYPE members are already the objects);
 * {@code cast} is SUPERTYPE-only (OWN_INTERFACE checkcast is {@code wrap});
 * {@code isInstance}/{@code isArrayInstance}/{@code castArray}/
 * {@code isAssignableFrom} exist for both. {@code instanceof} on a member
 * type absent at runtime throws {@code NCDFE} — inside {@code catch
 * (LinkageError)} it defers to the table, whose name-set skips the absent
 * name identically.
 */
public final class RetroDispatchGenerator implements ApiGenerator {

    private static final String RETRO_TYPES = "xyz/wagyourtail/jvmdg/stub/RetroTypes";
    private static final String OBJECT = "java/lang/Object";

    @Override
    public String name() {
        return "retro-dispatch";
    }

    @Override
    public void generate(GenContext ctx) throws IOException {
        Map<String, GenContext.FlatWrap> wraps = ctx.flattenedWraps();
        Set<String> emitted = new HashSet<>();
        for (VersionProvider vp : ctx.providers) {
            for (Map.Entry<Type, ClassStubEntry> e : vp.classStubs.entrySet()) {
                ClassStubEntry stub = e.getValue();
                // dispatcher hangs off the api class (stubType): for
                // SUPERTYPE, target is the JDK replacement supertype — not
                // ours to extend; for OWN_INTERFACE they're the same class
                String adapter = stub.stubType.getInternalName();
                String name = adapter + "$jvmdg$retro";
                if (!emitted.add(name) || ctx.declared(name)) {
                    continue;
                }
                emit(ctx, e.getKey().getInternalName(), adapter, stub,
                    wraps.get(e.getKey().getInternalName()));
            }
        }
    }

    private void emit(GenContext ctx, String src, String adapter, ClassStubEntry stub,
                      GenContext.FlatWrap wrap) throws IOException {
        String name = adapter + "$jvmdg$retro";
        String marker = stub.stubType.getInternalName() + "$Marker";
        boolean hasMarker = stub.mode == Adapter.Mode.SUPERTYPE || ctx.declared(marker);
        List<String[]> pairs = wrap != null ? wrap.pairs : java.util.Collections.<String[]>emptyList();

        ClassNode node = new ClassNode();
        node.visit(Opcodes.V1_8,
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
            name, null, OBJECT, null);
        node.visitSource(name.substring(name.lastIndexOf('/') + 1) + ".java", null);
        node.methods.add(ctor(name));
        if (stub.mode == Adapter.Mode.OWN_INTERFACE) {
            node.methods.add(wrap(src, adapter, pairs));
            node.methods.add(isInstance(src, adapter, hasMarker ? marker : null, pairs));
        } else {
            node.methods.add(isInstance(src, adapter, hasMarker ? marker : null,
                java.util.Collections.<String[]>emptyList()));
            node.methods.add(cast(src, adapter, hasMarker ? marker : null));
        }
        node.methods.add(isArrayInstance(src, adapter, hasMarker ? marker : null));
        node.methods.add(castArray(src, adapter, hasMarker ? marker : null));
        node.methods.add(isAssignableFrom(src));
        node.visitEnd();
        ctx.emit(name, node, dump(src, adapter, stub.mode, hasMarker, pairs));
    }

    private static MethodNode ctor(String name) {
        MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, OBJECT, "<init>", "()V", false);
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(1, 1);
        return m;
    }

    private static void frameSame(MethodNode m, Label label) {
        m.visitLabel(label);
        m.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    }

    /**
     * {@code o == null -> null|false} prologue — {@code lBody} is the
     * continuation, framed {@code F_SAME}.
     */
    private static Label nullPrologue(MethodNode m, int nullInsn) {
        Label lBody = new Label();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitJumpInsn(Opcodes.IFNONNULL, lBody);
        if (nullInsn == Opcodes.ICONST_0) {
            m.visitInsn(Opcodes.ICONST_0);
            m.visitInsn(Opcodes.IRETURN);
        } else {
            m.visitInsn(Opcodes.ACONST_NULL);
            m.visitInsn(Opcodes.ARETURN);
        }
        frameSame(m, lBody);
        return lBody;
    }

    /**
     * {@code wrap} — OWN_INTERFACE: null → {@code instanceof adapter}
     * passthrough → ordered {@code instanceof member -> new impl(member)}
     * chain under {@code catch (LinkageError)} → {@code RetroTypes.wrapImpl}
     * tail (non-declared members pass through to the trailing checkcast,
     * non-members CCE, external registrations resolve).
     */
    private static MethodNode wrap(String src, String adapter, List<String[]> pairs) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "wrap", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        Label lTail = new Label();
        Label lChain = pairs.isEmpty() ? lTail : new Label();
        nullPrologue(m, Opcodes.ACONST_NULL);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitTypeInsn(Opcodes.INSTANCEOF, adapter);
        m.visitJumpInsn(Opcodes.IFEQ, lChain);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitInsn(Opcodes.ARETURN);
        if (!pairs.isEmpty()) {
            Label lHandler = new Label();
            frameSame(m, lChain);
            for (int i = 0; i < pairs.size(); i++) {
                String member = pairs.get(i)[0];
                String impl = pairs.get(i)[1];
                Label lNextPair = i + 1 < pairs.size() ? new Label() : lTail;
                m.visitVarInsn(Opcodes.ALOAD, 0);
                m.visitTypeInsn(Opcodes.INSTANCEOF, member);
                m.visitJumpInsn(Opcodes.IFEQ, lNextPair);
                m.visitTypeInsn(Opcodes.NEW, impl);
                m.visitInsn(Opcodes.DUP);
                m.visitVarInsn(Opcodes.ALOAD, 0);
                m.visitTypeInsn(Opcodes.CHECKCAST, member);
                m.visitMethodInsn(Opcodes.INVOKESPECIAL, impl, "<init>",
                    "(L" + member + ";)V", false);
                m.visitInsn(Opcodes.ARETURN);
                if (i + 1 < pairs.size()) {
                    frameSame(m, lNextPair);
                }
            }
            m.visitTryCatchBlock(lChain, lTail, lHandler, "java/lang/LinkageError");
            m.visitLabel(lTail);
            m.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            emitWrapTail(m, src, adapter);
            m.visitLabel(lHandler);
            m.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/LinkageError"});
            m.visitInsn(Opcodes.POP);
            m.visitJumpInsn(Opcodes.GOTO, lTail);
        } else {
            frameSame(m, lTail);
            emitWrapTail(m, src, adapter);
        }
        m.visitMaxs(3, 1);
        return m;
    }

    private static void emitWrapTail(MethodNode m, String src, String adapter) {
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitLdcInsn(src);
        m.visitLdcInsn(Type.getObjectType(adapter));
        m.visitMethodInsn(Opcodes.INVOKESTATIC, RETRO_TYPES, "wrapImpl",
            "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false);
        m.visitInsn(Opcodes.ARETURN);
    }

    /**
     * {@code isInstance} — both modes: null → {@code instanceof adapter}
     * → {@code instanceof marker} (when one exists) → OWN_INTERFACE impl
     * member instanceofs under the LinkageError guard →
     * {@code RetroTypes.isInstance} tail (the bulk JDK-member name set +
     * external registrations).
     */
    private static MethodNode isInstance(String src, String adapter, String marker, List<String[]> memberChecks) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "isInstance", "(Ljava/lang/Object;)Z", null, null);
        nullPrologue(m, Opcodes.ICONST_0);
        membershipBody(m, adapter, marker, memberChecks,
            "isInstance", "(Ljava/lang/Object;Ljava/lang/String;)Z", src, true);
        m.visitMaxs(2, 1);
        return m;
    }

    /**
     * {@code cast} — SUPERTYPE: null → {@code adapter||marker} passthrough
     * → {@code RetroTypes.cast} tail (member passthrough / CCE).
     */
    private static MethodNode cast(String src, String adapter, String marker) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "cast", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        nullPrologue(m, Opcodes.ACONST_NULL);
        membershipBody(m, adapter, marker, java.util.Collections.<String[]>emptyList(),
            "cast", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", src, false);
        m.visitMaxs(2, 1);
        return m;
    }

    /**
     * {@code isArrayInstance} — both modes: null →
     * {@code instanceof adapter[] / marker[]} (downgraded user arrays and
     * marker-component arrays are real array types) →
     * {@code RetroTypes.isArrayInstance} tail (JDK-member components,
     * multi-dim).
     */
    private static MethodNode isArrayInstance(String src, String adapter, String marker) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "isArrayInstance", "(Ljava/lang/Object;)Z", null, null);
        nullPrologue(m, Opcodes.ICONST_0);
        membershipBody(m, "[L" + adapter + ";", marker == null ? null : "[L" + marker + ";",
            java.util.Collections.<String[]>emptyList(),
            "isArrayInstance", "(Ljava/lang/Object;Ljava/lang/String;)Z", src, true);
        m.visitMaxs(2, 1);
        return m;
    }

    /** {@code castArray} — both modes: same fast-path as isArrayInstance, returning the array. */
    private static MethodNode castArray(String src, String adapter, String marker) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "castArray", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        nullPrologue(m, Opcodes.ACONST_NULL);
        membershipBody(m, "[L" + adapter + ";", marker == null ? null : "[L" + marker + ";",
            java.util.Collections.<String[]>emptyList(),
            "castArray", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", src, false);
        m.visitMaxs(2, 1);
        return m;
    }

    /** {@code isAssignableFrom} — both modes: tiny delegate so callsites drop the name ldc. */
    private static MethodNode isAssignableFrom(String src) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "isAssignableFrom", "(Ljava/lang/Class;I)Z", null, null);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitLdcInsn(src);
        m.visitVarInsn(Opcodes.ILOAD, 1);
        m.visitMethodInsn(Opcodes.INVOKESTATIC, RETRO_TYPES, "isAssignableFrom",
            "(Ljava/lang/Class;Ljava/lang/String;I)Z", false);
        m.visitInsn(Opcodes.IRETURN);
        m.visitMaxs(3, 2);
        return m;
    }

    /**
     * The membership-check body shared by {@code isInstance}/{@code cast}/
     * {@code isArrayInstance}/{@code castArray}: {@code instanceof adapter}
     * then {@code instanceof marker} (nullable) then the guarded impl
     * member instanceofs (empty outside OWN_INTERFACE {@code isInstance})
     * — each branch returns {@code iconst_1}/{@code o} for boolean vs
     * passthrough shapes — then the {@code RetroTypes.<retroName>} tail.
     * {@code lTail} is {@code F_SAME}-framed; the LinkageError handler is a
     * separate {@code F_SAME1}-framed label that pops and gotos it.
     */
    private static void membershipBody(MethodNode m, String adapter, String marker,
                                       List<String[]> memberChecks, String retroName, String desc,
                                       String src, boolean bool) {
        Label lTail = new Label();
        Label lNext = marker == null && memberChecks.isEmpty() ? lTail : new Label();
        emitInstanceofCheck(m, adapter, lNext, bool);
        Label cur = lNext;
        if (marker != null) {
            frameSame(m, cur);
            cur = memberChecks.isEmpty() ? lTail : new Label();
            emitInstanceofCheck(m, marker, cur, bool);
        }
        if (!memberChecks.isEmpty()) {
            frameSame(m, cur);
            for (int i = 0; i < memberChecks.size(); i++) {
                Label n = i + 1 < memberChecks.size() ? new Label() : lTail;
                emitInstanceofCheck(m, memberChecks.get(i)[0], n, bool);
                if (i + 1 < memberChecks.size()) {
                    frameSame(m, n);
                }
            }
            Label lHandler = new Label();
            m.visitTryCatchBlock(cur, lTail, lHandler, "java/lang/LinkageError");
            frameSame(m, lTail);
            tailCall(m, src, retroName, desc, bool);
            m.visitLabel(lHandler);
            m.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/LinkageError"});
            m.visitInsn(Opcodes.POP);
            m.visitJumpInsn(Opcodes.GOTO, lTail);
            return;
        }
        frameSame(m, lTail);
        tailCall(m, src, retroName, desc, bool);
    }

    /** {@code instanceof type} → {@code return true|o} on match, else {@code ifeq next} */
    private static void emitInstanceofCheck(MethodNode m, String type, Label next, boolean bool) {
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitTypeInsn(Opcodes.INSTANCEOF, type);
        m.visitJumpInsn(Opcodes.IFEQ, next);
        if (bool) {
            m.visitInsn(Opcodes.ICONST_1);
            m.visitInsn(Opcodes.IRETURN);
        } else {
            m.visitVarInsn(Opcodes.ALOAD, 0);
            m.visitInsn(Opcodes.ARETURN);
        }
    }

    private static void tailCall(MethodNode m, String src, String retroName, String desc, boolean bool) {
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitLdcInsn(src);
        m.visitMethodInsn(Opcodes.INVOKESTATIC, RETRO_TYPES, retroName, desc, false);
        m.visitInsn(bool ? Opcodes.IRETURN : Opcodes.ARETURN);
    }

    // ---- .java inspection dump ----

    private static String dump(String src, String adapter, Adapter.Mode mode, boolean hasMarker,
                               List<String[]> pairs) {
        String simple = adapter.substring(adapter.lastIndexOf('/') + 1);
        String name = simple + "$jvmdg$retro";
        String marker = simple + "$Marker";
        StringBuilder sb = new StringBuilder();
        sb.append("// generated by StubIndexWriter — inspection dump; the .class is authoritative\n");
        int pkg = adapter.lastIndexOf('/');
        if (pkg >= 0) {
            sb.append("package ").append(adapter.substring(0, pkg).replace('/', '.')).append(";\n\n");
        }
        sb.append("public final class ").append(name).append(" {\n");
        sb.append("    private ").append(name).append("() { }\n\n");
        if (mode == Adapter.Mode.OWN_INTERFACE) {
            sb.append("    public static Object wrap(Object o) {\n");
            sb.append("        if (o == null) return null;\n");
            sb.append("        if (o instanceof ").append(simple).append(") return o;\n");
            if (!pairs.isEmpty()) {
                sb.append("        try {\n");
                for (String[] pair : pairs) {
                    sb.append("            if (o instanceof ").append(GenContext.dotted(pair[0]))
                        .append(") return new ").append(GenContext.dotted(pair[1]))
                        .append("((").append(GenContext.dotted(pair[0])).append(") o);\n");
                }
                sb.append("        } catch (LinkageError e) { /* absent member -> table answers identically */ }\n");
            }
            sb.append("        return RetroTypes.wrapImpl(o, \"").append(src).append("\", ")
                .append(simple).append(".class);\n    }\n\n");
            instanceDump(sb, "isInstance", simple, hasMarker ? marker : null, pairs, src);
        } else {
            instanceDump(sb, "isInstance", simple, hasMarker ? marker : null,
                java.util.Collections.<String[]>emptyList(), src);
            castDump(sb, "cast", simple, hasMarker ? marker : null, src);
        }
        arrayInstanceDump(sb, simple, hasMarker ? marker : null, src);
        arrayCastDump(sb, simple, hasMarker ? marker : null, src);
        sb.append("    public static boolean isAssignableFrom(Class<?> cls, int dims) {\n");
        sb.append("        return RetroTypes.isAssignableFrom(cls, \"").append(src).append("\", dims);\n    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void instanceDump(StringBuilder sb, String name, String adapter, String marker,
                                     List<String[]> pairs, String src) {
        sb.append("    public static boolean ").append(name).append("(Object o) {\n");
        sb.append("        if (o == null) return false;\n");
        sb.append("        if (o instanceof ").append(adapter).append(") return true;\n");
        if (marker != null) {
            sb.append("        if (o instanceof ").append(marker).append(") return true;\n");
        }
        if (!pairs.isEmpty()) {
            sb.append("        try {\n");
            for (String[] pair : pairs) {
                sb.append("            if (o instanceof ").append(GenContext.dotted(pair[0])).append(") return true;\n");
            }
            sb.append("        } catch (LinkageError e) { }\n");
        }
        sb.append("        return RetroTypes.isInstance(o, \"").append(src).append("\");\n    }\n\n");
    }

    private static void castDump(StringBuilder sb, String name, String adapter, String marker, String src) {
        sb.append("    public static Object ").append(name).append("(Object o) {\n");
        sb.append("        if (o == null) return null;\n");
        sb.append("        if (o instanceof ").append(adapter).append(") return o;\n");
        if (marker != null) {
            sb.append("        if (o instanceof ").append(marker).append(") return o;\n");
        }
        sb.append("        return RetroTypes.").append(name).append("(o, \"").append(src).append("\");\n    }\n\n");
    }

    private static void arrayInstanceDump(StringBuilder sb, String adapter, String marker, String src) {
        sb.append("    public static boolean isArrayInstance(Object o) {\n");
        sb.append("        if (o == null) return false;\n");
        sb.append("        if (o instanceof ").append(adapter).append("[]) return true;\n");
        if (marker != null) {
            sb.append("        if (o instanceof ").append(marker).append("[]) return true;\n");
        }
        sb.append("        return RetroTypes.isArrayInstance(o, \"").append(src).append("\");\n    }\n\n");
    }

    private static void arrayCastDump(StringBuilder sb, String adapter, String marker, String src) {
        sb.append("    public static Object castArray(Object o) {\n");
        sb.append("        if (o == null) return null;\n");
        sb.append("        if (o instanceof ").append(adapter).append("[]) return o;\n");
        if (marker != null) {
            sb.append("        if (o instanceof ").append(marker).append("[]) return o;\n");
        }
        sb.append("        return RetroTypes.castArray(o, \"").append(src).append("\");\n    }\n\n");
    }

}
