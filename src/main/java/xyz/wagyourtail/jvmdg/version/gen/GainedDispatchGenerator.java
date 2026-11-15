package xyz.wagyourtail.jvmdg.version.gen;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import xyz.wagyourtail.jvmdg.version.map.GainedSites;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Emits {@code xyz/wagyourtail/jvmdg/gen/gained/<mangled-I>} — one dispatcher
 * per gained-supertype interface plus a {@code $Wrap} sibling implementing
 * it. A gained supertype is an existing JDK class that acquired an interface
 * in a later release ({@code HttpClient}→{@code AutoCloseable} @65,
 * {@code ExecutorService}→{@code AutoCloseable} @63): at the downgrade
 * target the member doesn't implement the interface, so
 * {@code invokeinterface}/{@code instanceof}/{@code checkcast} sites on it
 * route through here.
 * <p>
 * Every entry point carries a trailing {@code int ceiling} — the calling
 * class's authored class-file version — because the binding is
 * version-scoped: a V20-authored site must see {@code HttpClient instanceof
 * AutoCloseable} as {@code false} even when the same jar is processed by a
 * later toolchain. {@code instanceof}-form answers
 * {@code o instanceof I || RetroTypes.gainedMember}; {@code cast} produces a
 * real {@code I}-typed result — the raw member can't satisfy the verifier —
 * via {@code $Wrap}, an {@code I}-implementing delegator (identity diverges
 * like OWN_INTERFACE wrap). Member calls fast-path {@code self instanceof
 * I → I.m}, else tail-call {@link
 * xyz.wagyourtail.jvmdg.stub.RetroTypes#gainedCall} whose table resolves the
 * declarer — package-private members included, which a bytecode chain can't
 * reach — and falls back to the registered member stub on linkage failure.
 * <p>
 * Emitted at {@code V1_8} inside the api jar — ordinary api classes for
 * indexing/shading/pruning. The interface itself always exists at the
 * target (the binding's {@code since} postdates both types), so hard
 * {@code instanceof I}/invokeinterface references are verifiable.
 */
public final class GainedDispatchGenerator implements ApiGenerator {

    private static final String RETRO_TYPES = "xyz/wagyourtail/jvmdg/stub/RetroTypes";
    private static final String OBJECT = "java/lang/Object";

    @Override
    public String name() {
        return "gained-dispatch";
    }

    @Override
    public void generate(GenContext ctx) throws IOException {
        for (Map.Entry<String, Set<String>> e : new TreeMap<>(ctx.gainedMethods).entrySet()) {
            String iface = e.getKey();
            String name = GainedSites.dispatchOwner(iface);
            String wrapName = name + "$Wrap";
            if (ctx.declared(name) || ctx.declared(wrapName)) {
                continue; // hand-declared wins — both or neither
            }
            emit(ctx, iface, name, e.getValue());
        }
    }

    private void emit(GenContext ctx, String iface, String name, Set<String> methods) throws IOException {
        Type iType = Type.getObjectType(iface);
        String iDesc = iType.getDescriptor();
        String wrapName = name + "$Wrap";

        ClassNode node = new ClassNode();
        node.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
            name, null, OBJECT, null);
        node.visitInnerClass(wrapName, name, "Wrap",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);
        node.visitSource(name.substring(name.lastIndexOf('/') + 1) + ".java", null);
        node.methods.add(ctor(name));
        node.methods.add(isInstance(iface));
        node.methods.add(cast(iface, iDesc, wrapName));
        node.methods.add(isAssignableFrom(iface, iDesc));
        node.methods.add(isArrayInstance(iface));
        node.methods.add(isArrayAssignableFrom(iface));
        Map<String, Set<MethodRef>> sites = ctx.gainedSites.get(iface);
        for (String key : methods) {
            int sp = key.indexOf(' ');
            node.methods.add(memberDispatch(ctx, iface, name, key.substring(0, sp), key.substring(sp + 1), key,
                sites == null ? null : sites.get(key)));
        }
        node.visitEnd();

        ClassNode wrap = new ClassNode();
        wrap.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
            wrapName, null, OBJECT, new String[]{iface});
        wrap.visitInnerClass(wrapName, name, "Wrap",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);
        wrap.visitSource(wrapName.substring(wrapName.lastIndexOf('/') + 1) + ".java", null);
        wrap.fields.add(new FieldNode(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "t", "Ljava/lang/Object;", null, null));
        wrap.fields.add(new FieldNode(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "ceiling", "I", null, null));
        wrap.methods.add(wrapCtor(wrapName));
        for (String key : methods) {
            int sp = key.indexOf(' ');
            wrap.methods.add(wrapMember(name, iType, key.substring(0, sp), key.substring(sp + 1)));
        }
        wrap.visitEnd();

        ctx.emit(name, node, dump(iface, name, methods));
        ctx.emit(wrapName, wrap, dumpWrap(iface, wrapName, methods));
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
     * {@code jvmdg$isInstance(Object o, int ceiling) Z} —
     * {@code o instanceof I || gainedMember(I, o, ceiling)}; null-safe by
     * both halves.
     */
    private static MethodNode isInstance(String iface) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "jvmdg$isInstance", "(Ljava/lang/Object;I)Z", null, null);
        Label member = new Label();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitTypeInsn(Opcodes.INSTANCEOF, iface);
        m.visitJumpInsn(Opcodes.IFEQ, member);
        m.visitInsn(Opcodes.ICONST_1);
        m.visitInsn(Opcodes.IRETURN);
        frameSame(m, member);
        m.visitLdcInsn(iface);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitVarInsn(Opcodes.ILOAD, 1);
        m.visitMethodInsn(Opcodes.INVOKESTATIC, RETRO_TYPES, "gainedMember",
            "(Ljava/lang/String;Ljava/lang/Object;I)Z", false);
        m.visitInsn(Opcodes.IRETURN);
        m.visitMaxs(3, 2);
        return m;
    }

    /**
     * {@code jvmdg$cast(Object o, int ceiling) LI;} —
     * {@code o instanceof I → o; gainedMember → new Wrap(o, ceiling); else
     * ClassCastException}. The result is genuinely {@code I}-typed, which is
     * what a real {@code checkcast} result's stack slot requires.
     */
    private static MethodNode cast(String iface, String iDesc, String wrapName) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "jvmdg$cast", "(Ljava/lang/Object;I)" + iDesc, null, null);
        Label member = new Label(), fail = new Label(), nonNull = new Label();
        // checkcast passes null through — no instanceof/membership question
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitJumpInsn(Opcodes.IFNONNULL, nonNull);
        m.visitInsn(Opcodes.ACONST_NULL);
        m.visitInsn(Opcodes.ARETURN);
        frameSame(m, nonNull);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitTypeInsn(Opcodes.INSTANCEOF, iface);
        m.visitJumpInsn(Opcodes.IFEQ, member);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitTypeInsn(Opcodes.CHECKCAST, iface);
        m.visitInsn(Opcodes.ARETURN);
        frameSame(m, member);
        m.visitLdcInsn(iface);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitVarInsn(Opcodes.ILOAD, 1);
        m.visitMethodInsn(Opcodes.INVOKESTATIC, RETRO_TYPES, "gainedMember",
            "(Ljava/lang/String;Ljava/lang/Object;I)Z", false);
        m.visitJumpInsn(Opcodes.IFEQ, fail);
        m.visitTypeInsn(Opcodes.NEW, wrapName);
        m.visitInsn(Opcodes.DUP);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitVarInsn(Opcodes.ILOAD, 1);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, wrapName, "<init>", "(Ljava/lang/Object;I)V", false);
        m.visitInsn(Opcodes.ARETURN);
        frameSame(m, fail);
        m.visitTypeInsn(Opcodes.NEW, "java/lang/ClassCastException");
        m.visitInsn(Opcodes.DUP);
        m.visitLdcInsn("Object cannot be cast to " + iface.replace('/', '.'));
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/ClassCastException",
            "<init>", "(Ljava/lang/String;)V", false);
        m.visitInsn(Opcodes.ATHROW);
        m.visitMaxs(4, 2);
        return m;
    }

    /**
     * {@code jvmdg$isAssignableFrom(Class cls, int ceiling) Z} —
     * {@code I.isAssignableFrom(cls) || gainedAssignable(I, cls, ceiling)}.
     */
    private static MethodNode isAssignableFrom(String iface, String iDesc) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "jvmdg$isAssignableFrom", "(Ljava/lang/Class;I)Z", null, null);
        Label member = new Label();
        m.visitLdcInsn(iType(iface));
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "isAssignableFrom",
            "(Ljava/lang/Class;)Z", false);
        m.visitJumpInsn(Opcodes.IFEQ, member);
        m.visitInsn(Opcodes.ICONST_1);
        m.visitInsn(Opcodes.IRETURN);
        frameSame(m, member);
        m.visitLdcInsn(iface);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitVarInsn(Opcodes.ILOAD, 1);
        m.visitMethodInsn(Opcodes.INVOKESTATIC, RETRO_TYPES, "gainedAssignable",
            "(Ljava/lang/String;Ljava/lang/Class;I)Z", false);
        m.visitInsn(Opcodes.IRETURN);
        m.visitMaxs(3, 2);
        return m;
    }

    /**
     * {@code jvmdg$isArrayInstance(Object o, int ceiling) Z} —
     * {@code instanceof I[k]} on a member array: the array covariance
     * check reduces to the base element type — {@code ExecutorService[][]}
     * answers true iff {@code ExecutorService} is a member. Member arrays
     * stay unwrappable ({@code checkcast I[]} keeps its native CCE —
     * array covariance can't be fabricated).
     */
    private static MethodNode isArrayInstance(String iface) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "jvmdg$isArrayInstance", "(Ljava/lang/Object;I)Z", null, null);
        Label notArr = new Label(), loop = new Label(), check = new Label(), tail = new Label();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitTypeInsn(Opcodes.INSTANCEOF, "[Ljava/lang/Object;");
        m.visitJumpInsn(Opcodes.IFEQ, notArr);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass",
            "()Ljava/lang/Class;", false);
        m.visitVarInsn(Opcodes.ASTORE, 2);
        // loop head — the extra Class local is an F_APPEND over the args frame
        m.visitLabel(loop);
        m.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/Class"}, 0, null);
        m.visitVarInsn(Opcodes.ALOAD, 2);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "isArray", "()Z", false);
        m.visitJumpInsn(Opcodes.IFEQ, check);
        m.visitVarInsn(Opcodes.ALOAD, 2);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getComponentType",
            "()Ljava/lang/Class;", false);
        m.visitVarInsn(Opcodes.ASTORE, 2);
        m.visitJumpInsn(Opcodes.GOTO, loop);
        frameSame(m, check);
        m.visitLdcInsn(iType(iface));
        m.visitVarInsn(Opcodes.ALOAD, 2);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "isAssignableFrom",
            "(Ljava/lang/Class;)Z", false);
        m.visitJumpInsn(Opcodes.IFEQ, tail);
        m.visitInsn(Opcodes.ICONST_1);
        m.visitInsn(Opcodes.IRETURN);
        frameSame(m, tail);
        m.visitLdcInsn(iface);
        m.visitVarInsn(Opcodes.ALOAD, 2);
        m.visitVarInsn(Opcodes.ILOAD, 1);
        m.visitMethodInsn(Opcodes.INVOKESTATIC, RETRO_TYPES, "gainedAssignable",
            "(Ljava/lang/String;Ljava/lang/Class;I)Z", false);
        m.visitInsn(Opcodes.IRETURN);
        // back to the 2-arg frame — drop the Class local
        m.visitLabel(notArr);
        m.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        m.visitInsn(Opcodes.ICONST_0);
        m.visitInsn(Opcodes.IRETURN);
        m.visitMaxs(3, 3);
        return m;
    }

    private static Type iType(String iface) {
        return Type.getObjectType(iface);
    }

    /**
     * {@code jvmdg$isArrayAssignableFrom(Class cls, int dims, int ceiling) Z}
     * — {@code I[dims].class.isAssignableFrom(cls)} for the gained members:
     * strip {@code dims} components off {@code cls} (a shorter or non-array
     * operand is false), then the same {@code I.isAssignableFrom ||
     * gainedAssignable} element check the scalar form does.
     */
    private static MethodNode isArrayAssignableFrom(String iface) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "jvmdg$isArrayAssignableFrom", "(Ljava/lang/Class;II)Z", null, null);
        Label loop = new Label(), done = new Label(), tail = new Label(), fail = new Label();
        m.visitLabel(loop);
        m.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        m.visitVarInsn(Opcodes.ILOAD, 1);
        m.visitIincInsn(1, -1);
        m.visitJumpInsn(Opcodes.IFLE, done);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "isArray", "()Z", false);
        m.visitJumpInsn(Opcodes.IFEQ, fail);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getComponentType",
            "()Ljava/lang/Class;", false);
        m.visitVarInsn(Opcodes.ASTORE, 0);
        m.visitJumpInsn(Opcodes.GOTO, loop);
        frameSame(m, done);
        m.visitLdcInsn(iType(iface));
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "isAssignableFrom",
            "(Ljava/lang/Class;)Z", false);
        m.visitJumpInsn(Opcodes.IFEQ, tail);
        m.visitInsn(Opcodes.ICONST_1);
        m.visitInsn(Opcodes.IRETURN);
        frameSame(m, tail);
        m.visitLdcInsn(iface);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitVarInsn(Opcodes.ILOAD, 2);
        m.visitMethodInsn(Opcodes.INVOKESTATIC, RETRO_TYPES, "gainedAssignable",
            "(Ljava/lang/String;Ljava/lang/Class;I)Z", false);
        m.visitInsn(Opcodes.IRETURN);
        frameSame(m, fail);
        m.visitInsn(Opcodes.ICONST_0);
        m.visitInsn(Opcodes.IRETURN);
        m.visitMaxs(3, 3);
        return m;
    }

    /**
     * Per-member dispatch, {@code <name>$<mangled-desc>}:
     * {@code <ret′> m$desc(Object self, <args′>, int ceiling)} where
     * {@code ′} erases every reference slot to {@code Object} — sites
     * compute the same erased desc from their post-remap signature, so the
     * verifier only ever sees Object/what's-already-pushed. Fast-path
     * {@code self instanceof I → ((I)self).m(args)} (ref args checkcast to
     * the real decl type — adapter-typed args honestly CCE), else
     * {@code RetroTypes.gainedCall(I, "name desc", self, args, ceiling,
     * This.class)} — the dispatcher class is the context whose loader
     * resolves the reflective sites — with boxing/unboxing on the
     * boundary. Checked throws propagate raw — the bytecode needs no
     * catch.
     */
    private static MethodNode memberDispatch(GenContext ctx, String iface, String own, String name, String desc, String key,
                                             Set<MethodRef> sites) {
        Type mType = Type.getMethodType(desc);
        Type ret = mType.getReturnType();
        Type[] args = mType.getArgumentTypes();
        StringBuilder sig = new StringBuilder("(Ljava/lang/Object;");
        for (Type a : args) {
            sig.append(GainedSites.isRef(a) ? "Ljava/lang/Object;" : a.getDescriptor());
        }
        sig.append("I)").append(GainedSites.isRef(ret) ? "Ljava/lang/Object;" : ret.getDescriptor());
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            GainedSites.dispatchName(name, desc), sig.toString(), null,
            new String[]{"java/lang/Throwable"});
        if (sites != null && !sites.isEmpty()) {
            // the gainedCall fallback reaches these reflectively — no
            // bytecode edge exists for shading's closure to follow, so the
            // member declares them like an authored @ReflectionReferences —
            // descs carry the chain remap since the api index keys members
            // in post-remap (stand-in) vocabulary
            Set<String> warnings = new HashSet<>();
            List<Object> refs = new ArrayList<>(sites.size());
            for (MethodRef site : sites) {
                Type rDesc = site.member.getDesc();
                for (xyz.wagyourtail.jvmdg.version.VersionProvider provider : ctx.providers) {
                    rDesc = provider.remap(rDesc, warnings);
                }
                AnnotationNode ref = new AnnotationNode("Lxyz/wagyourtail/jvmdg/version/Ref;");
                ref.values = java.util.Arrays.<Object>asList(
                    "value", "L" + site.member.getOwner().getInternalName() + ";",
                    "member", site.member.getName(),
                    "desc", rDesc.getDescriptor());
                refs.add(ref);
            }
            AnnotationNode rr = new AnnotationNode("Lxyz/wagyourtail/jvmdg/version/ReflectionReferences;");
            rr.values = java.util.Arrays.<Object>asList("value", refs);
            m.invisibleAnnotations = new ArrayList<AnnotationNode>(1);
            m.invisibleAnnotations.add(rr);
        }

        Label member = new Label();
        Label tryStart = new Label(), handler = new Label();
        // the fast path's invokeinterface throws LinkageError (NSME when
        // the member is version-gated above the running JDK, AME when the
        // receiver's class can't serve it) — same catch-and-fallthrough
        // shape the $jvmdg$retro dispatchers use for impls: the reflective
        // path then reaches the declarer or the registered stub site
        m.visitTryCatchBlock(tryStart, handler, handler, "java/lang/LinkageError");
        m.visitLabel(tryStart);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitTypeInsn(Opcodes.INSTANCEOF, iface);
        m.visitJumpInsn(Opcodes.IFEQ, member);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitTypeInsn(Opcodes.CHECKCAST, iface);
        int slot = 1;
        for (Type a : args) {
            m.visitVarInsn(a.getOpcode(Opcodes.ILOAD), slot);
            if (GainedSites.isRef(a)) {
                // the erased param is Object — the real member needs the
                // decl type (adapter-typed args honestly CCE here)
                m.visitTypeInsn(Opcodes.CHECKCAST, a.getInternalName());
            }
            slot += a.getSize();
        }
        m.visitMethodInsn(Opcodes.INVOKEINTERFACE, iface, name, desc, true);
        m.visitInsn(ret.getOpcode(Opcodes.IRETURN));
        m.visitLabel(handler);
        m.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/LinkageError"});
        m.visitInsn(Opcodes.POP);
        frameSame(m, member);
        // RetroTypes.gainedCall(I, "name desc", self, args, ceiling, This.class)
        m.visitLdcInsn(iface);
        m.visitLdcInsn(key);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        intInsn(m, args.length);
        m.visitTypeInsn(Opcodes.ANEWARRAY, OBJECT);
        slot = 1;
        for (int i = 0; i < args.length; i++) {
            m.visitInsn(Opcodes.DUP);
            intInsn(m, i);
            boxArg(m, args[i], slot);
            slot += args[i].getSize();
            m.visitInsn(Opcodes.AASTORE);
        }
        m.visitVarInsn(Opcodes.ILOAD, slot); // ceiling is the last slot
        m.visitLdcInsn(Type.getObjectType(own));
        m.visitMethodInsn(Opcodes.INVOKESTATIC, RETRO_TYPES, "gainedCall",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;ILjava/lang/Class;)Ljava/lang/Object;", false);
        if (ret.getSort() == Type.VOID) {
            m.visitInsn(Opcodes.POP);
            m.visitInsn(Opcodes.RETURN);
        } else if (GainedSites.isRef(ret)) {
            m.visitInsn(Opcodes.ARETURN);
        } else {
            unboxRet(m, ret);
            m.visitInsn(ret.getOpcode(Opcodes.IRETURN));
        }
        // stack peak is the Object[] fill: base 4 + dup + index + value(≤2) ≈ 8
        m.visitMaxs(Math.max(9, slot + 3), slot + 1);
        return m;
    }

    private static void intInsn(MethodNode m, int v) {
        if (v >= 0 && v <= 5) {
            m.visitInsn(Opcodes.ICONST_0 + v);
        } else {
            m.visitIntInsn(v <= Byte.MAX_VALUE ? Opcodes.BIPUSH : Opcodes.SIPUSH, v);
        }
    }

    /** load arg slot {@code slot} of type {@code t}, boxing primitives for the {@code Object[]} */
    private static void boxArg(MethodNode m, Type t, int slot) {
        m.visitVarInsn(t.getOpcode(Opcodes.ILOAD), slot);
        switch (t.getSort()) {
            case Type.BOOLEAN:
                m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case Type.BYTE:
                m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case Type.CHAR:
                m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case Type.SHORT:
                m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
            case Type.INT:
                m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case Type.LONG:
                m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case Type.FLOAT:
                m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case Type.DOUBLE:
                m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            default: // ref types pass through
        }
    }

    /** {@code Object} → the method's real return type: cast for refs, unbox for primitives */
    private static void unboxRet(MethodNode m, Type ret) {
        switch (ret.getSort()) {
            case Type.BOOLEAN:
                m.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                break;
            case Type.BYTE:
                m.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                break;
            case Type.CHAR:
                m.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                break;
            case Type.SHORT:
                m.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                break;
            case Type.INT:
                m.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                break;
            case Type.LONG:
                m.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                break;
            case Type.FLOAT:
                m.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                break;
            case Type.DOUBLE:
                m.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                break;
            case Type.ARRAY:
            case Type.OBJECT:
                m.visitTypeInsn(Opcodes.CHECKCAST, ret.getInternalName());
                break;
            default:
        }
    }

    private static MethodNode wrapCtor(String wrapName) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/Object;I)V", null, null);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, OBJECT, "<init>", "()V", false);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitVarInsn(Opcodes.ALOAD, 1);
        m.visitFieldInsn(Opcodes.PUTFIELD, wrapName, "t", "Ljava/lang/Object;");
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitVarInsn(Opcodes.ILOAD, 2);
        m.visitFieldInsn(Opcodes.PUTFIELD, wrapName, "ceiling", "I");
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(2, 3);
        return m;
    }

    /**
     * {@code <ret> m(<args>) → dispatch.m$desc(this.t, args, this.ceiling)} —
     * the wrapper's instance methods carry the target + baked ceiling; the
     * dispatch signature is the erased shape (ref slots widen to Object),
     * with a checkcast back to the real return for reference returns.
     */
    private static MethodNode wrapMember(String dispatchName, Type iType, String name, String desc) {
        Type mType = Type.getMethodType(desc);
        Type ret = mType.getReturnType();
        Type[] args = mType.getArgumentTypes();
        StringBuilder sig = new StringBuilder("(Ljava/lang/Object;");
        for (Type a : args) {
            sig.append(GainedSites.isRef(a) ? "Ljava/lang/Object;" : a.getDescriptor());
        }
        sig.append("I)").append(GainedSites.isRef(ret) ? "Ljava/lang/Object;" : ret.getDescriptor());
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, name, desc, null, null);
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitFieldInsn(Opcodes.GETFIELD, dispatchName + "$Wrap", "t", "Ljava/lang/Object;");
        int slot = 1;
        for (Type a : args) {
            m.visitVarInsn(a.getOpcode(Opcodes.ILOAD), slot);
            slot += a.getSize();
        }
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitFieldInsn(Opcodes.GETFIELD, dispatchName + "$Wrap", "ceiling", "I");
        m.visitMethodInsn(Opcodes.INVOKESTATIC, dispatchName,
            GainedSites.dispatchName(name, desc), sig.toString(), false);
        if (GainedSites.isRef(ret)) {
            m.visitTypeInsn(Opcodes.CHECKCAST, ret.getInternalName());
        }
        m.visitInsn(ret.getOpcode(Opcodes.IRETURN));
        m.visitMaxs(slot + 1, slot);
        return m;
    }

    private static String dump(String iface, String name, Set<String> methods) {
        StringBuilder sb = new StringBuilder();
        sb.append("// generated dispatcher for gained supertype ").append(iface).append('\n');
        sb.append("public final class ").append(name.substring(name.lastIndexOf('/') + 1)).append(" {\n");
        sb.append("  public static boolean jvmdg$isInstance(Object o, int ceiling)\n");
        sb.append("  public static ").append(iface.replace('/', '.')).append(" jvmdg$cast(Object o, int ceiling)\n");
        sb.append("  public static boolean jvmdg$isAssignableFrom(Class<?> cls, int ceiling)\n");
        for (String key : methods) {
            sb.append("  // dispatch: ").append(key).append(" — instanceof fast-path, RetroTypes.gainedCall tail\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String dumpWrap(String iface, String wrapName, Set<String> methods) {
        StringBuilder sb = new StringBuilder();
        sb.append("// (T,I) wrapper — carries the target + authored ceiling\n");
        sb.append("public final class ").append(wrapName.substring(wrapName.lastIndexOf('/') + 1))
            .append(" implements ").append(iface.replace('/', '.')).append(" {\n");
        sb.append("  final Object t; final int ceiling;\n");
        for (String key : methods) {
            sb.append("  // ").append(key).append(" -> dispatcher\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
