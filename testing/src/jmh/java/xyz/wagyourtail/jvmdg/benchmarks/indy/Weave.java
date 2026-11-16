package xyz.wagyourtail.jvmdg.benchmarks.indy;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * ASM fixture emitter — classes javac can't produce, faithful to the shapes
 * the transform/generators emit into downgraded jars:
 *
 * <ul>
 *   <li>{@code bench/M*} — receiver classes implementing {@code Member}
 *       (and {@code bench/N*} additionally {@code NewI} for the newer-JVM
 *       fast path)</li>
 *   <li>{@code bench/R*} — retro member classes implementing {@code Marker}</li>
 *   <li>{@code bench/DispGained*} — the generated {@code gen/gained/<I>}
 *       dispatcher shape: {@code try { instanceof I → invokeinterface } catch
 *       (LinkageError) { reflective gainedCall tail }}</li>
 *   <li>{@code bench/Indy*} — the candidate indy sites bootstrapped by
 *       {@link Bsms} — same calls the M-2 design would emit</li>
 *   <li>{@code bench/NoIndy*} — the {@code jvmdg$indy$} lowering shape:
 *       static CallSite field resolved once in {@code <clinit>} + synthetic
 *       method doing {@code getTarget().invokeExact}</li>
 *   <li>{@code bench/DispRetro*}/{@code DispXClass} — the generated
 *       {@code $jvmdg$retro} dispatcher shapes</li>
 * </ul>
 *
 * Member classes emit at V1_6 (no StackMapTable needed); dispatch fixtures at
 * V1_8 with hand frames — same as the real generators.
 */
final class Weave implements Opcodes {

    private Weave() {
    }

    static final String FIX = "xyz/wagyourtail/jvmdg/benchmarks/indy/Fix$";
    static final String MEMBER = FIX + "Member";
    static final String NEWI = FIX + "NewI";
    static final String MARKER = FIX + "Marker";
    static final String ADAPTER = FIX + "Adapter";
    static final String WRAPIMPL = FIX + "WrapImpl";
    static final String SITE = FIX + "Site";
    static final String BOOLSITE = FIX + "BoolSite";
    static final String BSMS = "xyz/wagyourtail/jvmdg/benchmarks/indy/Bsms";
    static final String RETRO = "xyz/wagyourtail/jvmdg/stub/RetroTypes";
    /** the gained interface for the old-JVM scenario — deliberately unresolvable */
    static final String PHANTOM = "java/util/bench/GainedIface";
    static final String RETRO_SRC = "bench/Retro";
    /** registered gained-call i-key (fake namespace, never collides with real rows) */
    static final String GAINED_I = "bench/NewI";

    /** fixture classloader — parents to the bench loader so shared classes resolve */
    static final class FixLoader extends ClassLoader {
        FixLoader() {
            super(Weave.class.getClassLoader());
        }

        Class<?> def(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    static class Out {
        final FixLoader loader = new FixLoader();
        Class<?>[] members;
        Class<?>[] membersNew;
        Class<?>[] retroMembers;
        Class<?>[] empty;
        Object dispGainedMissing;
        Object dispGainedReal;
        Object indyGained;
        Object noindyGained;
        Object dispRetroIs;
        Object indyRetroIs;
        Object noindyRetroIs;
        Object dispRetroWrap;
        Object indyRetroWrap;
        Object dispXClass;
        Object indyXClass;
        Object indyMember;
        Object noindyMember;

        @SuppressWarnings("unchecked")
        <T> T site(String name) {
            try {
                return (T) loader.loadClass(name.replace('/', '.')).getDeclaredConstructor().newInstance();
            } catch (Throwable t) {
                throw new IllegalStateException("fixture " + name, t);
            }
        }

        Class<?> cls(String name) {
            try {
                return loader.loadClass(name.replace('/', '.'));
            } catch (Throwable t) {
                throw new IllegalStateException("fixture " + name, t);
            }
        }
    }

    static Out emit() {
        Out out = new Out();
        // receivers — op(int) returns x + idx so calls can't fold away
        for (int i = 0; i < 16; i++) {
            out.loader.def("bench.M" + i, member("bench/M" + i, i, false));
            out.loader.def("bench.N" + i, member("bench/N" + i, i, true));
            out.loader.def("bench.R" + i, plain("bench/R" + i, MARKER));
            out.loader.def("bench.E" + i, plain("bench/E" + i, null));
        }
        out.members = new Class<?>[16];
        out.membersNew = new Class<?>[16];
        out.retroMembers = new Class<?>[16];
        out.empty = new Class<?>[16];
        for (int i = 0; i < 16; i++) {
            out.members[i] = out.cls("bench/M" + i);
            out.membersNew[i] = out.cls("bench/N" + i);
            out.retroMembers[i] = out.cls("bench/R" + i);
            out.empty[i] = out.cls("bench/E" + i);
        }
        // dispatch fixtures
        out.loader.def("bench.DispGainedMissing",
            dispGained("bench/DispGainedMissing", PHANTOM));
        out.loader.def("bench.DispGainedReal",
            dispGained("bench/DispGainedReal", NEWI));
        out.dispGainedMissing = out.site("bench/DispGainedMissing");
        out.dispGainedReal = out.site("bench/DispGainedReal");
        out.loader.def("bench.IndyGained", indyGained("bench/IndyGained", NEWI));
        out.indyGained = out.site("bench/IndyGained");
        out.loader.def("bench.NoIndyGained", noindyGained("bench/NoIndyGained", NEWI));
        out.noindyGained = out.site("bench/NoIndyGained");
        out.loader.def("bench.DispRetroIs", dispRetroIs("bench/DispRetroIs"));
        out.dispRetroIs = out.site("bench/DispRetroIs");
        out.loader.def("bench.IndyRetroIs", indyRetroIs("bench/IndyRetroIs"));
        out.indyRetroIs = out.site("bench/IndyRetroIs");
        out.loader.def("bench.NoIndyRetroIs", noindyRetroIs("bench/NoIndyRetroIs"));
        out.noindyRetroIs = out.site("bench/NoIndyRetroIs");
        out.loader.def("bench.DispRetroWrap", dispRetroWrap("bench/DispRetroWrap"));
        out.dispRetroWrap = out.site("bench/DispRetroWrap");
        out.loader.def("bench.IndyRetroWrap", indyRetroWrap("bench/IndyRetroWrap"));
        out.indyRetroWrap = out.site("bench/IndyRetroWrap");
        out.loader.def("bench.DispXClass", dispXClass("bench/DispXClass"));
        out.dispXClass = out.site("bench/DispXClass");
        out.loader.def("bench.IndyXClass", indyXClass("bench/IndyXClass"));
        out.indyXClass = out.site("bench/IndyXClass");
        out.loader.def("bench.IndyMember", indyMember("bench/IndyMember"));
        out.indyMember = out.site("bench/IndyMember");
        out.loader.def("bench.NoIndyMember", noindyMember("bench/NoIndyMember"));
        out.noindyMember = out.site("bench/NoIndyMember");
        return out;
    }

    // ---------- receivers ----------

    /** {@code class N implements Member[, NewI] { public int op(int x){return x+k;} } } at V1_6 */
    private static byte[] member(String name, int k, boolean newer) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, name, null, "java/lang/Object",
            newer ? new String[]{MEMBER, NEWI} : new String[]{MEMBER});
        MethodVisitor ctor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(1, 1);
        ctor.visitEnd();
        MethodVisitor op = cw.visitMethod(ACC_PUBLIC, "op", "(I)I", null, null);
        op.visitCode();
        op.visitVarInsn(ILOAD, 1);
        op.visitIntInsn(BIPUSH, k);
        op.visitInsn(IADD);
        op.visitInsn(IRETURN);
        op.visitMaxs(2, 2);
        op.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** {@code class N implements [iface] {} } at V1_6 — retro marker members / empty receivers */
    private static byte[] plain(String name, String iface) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, name, null, "java/lang/Object",
            iface == null ? null : new String[]{iface});
        MethodVisitor ctor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(1, 1);
        ctor.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static ClassWriter classOut(String name, String iface) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_8, ACC_PUBLIC | ACC_SUPER, name, null, "java/lang/Object",
            new String[]{iface});
        MethodVisitor ctor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(1, 1);
        ctor.visitEnd();
        return cw;
    }

    private static void frameSame(MethodVisitor m) {
        m.visitFrame(F_SAME, 0, null, 0, null);
    }

    // ---------- gained member dispatch ----------

    /**
     * The generated {@code gen/gained/<I>.<name>$<desc>} shape verbatim:
     * {@code try { self instanceof I → ((I)self).op(arg) } catch
     * (LinkageError) { RetroTypes.gainedCall(i, "op (I)I", self, args,
     * ceiling, This.class) }} — boxing included.
     */
    private static byte[] dispGained(String name, String iface) {
        ClassWriter cw = classOut(name, SITE);
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "call",
            "(Ljava/lang/Object;II)Ljava/lang/Object;", null,
            new String[]{"java/lang/Throwable"});
        m.visitCode();
        Label tryStart = new Label(), handler = new Label(), member = new Label();
        m.visitTryCatchBlock(tryStart, handler, handler, "java/lang/LinkageError");
        m.visitLabel(tryStart);
        m.visitVarInsn(ALOAD, 1);
        m.visitTypeInsn(INSTANCEOF, iface);
        m.visitJumpInsn(IFEQ, member);
        m.visitVarInsn(ALOAD, 1);
        m.visitTypeInsn(CHECKCAST, iface);
        m.visitVarInsn(ILOAD, 2);
        m.visitMethodInsn(INVOKEINTERFACE, iface, "op", "(I)I", true);
        m.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
            "(I)Ljava/lang/Integer;", false);
        m.visitInsn(ARETURN);
        m.visitLabel(handler);
        m.visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/lang/LinkageError"});
        m.visitInsn(POP);
        m.visitLabel(member);
        frameSame(m);
        m.visitLdcInsn(GAINED_I);
        m.visitLdcInsn("op (I)I");
        m.visitVarInsn(ALOAD, 1);
        m.visitInsn(ICONST_1);
        m.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        m.visitInsn(DUP);
        m.visitInsn(ICONST_0);
        m.visitVarInsn(ILOAD, 2);
        m.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
            "(I)Ljava/lang/Integer;", false);
        m.visitInsn(AASTORE);
        m.visitVarInsn(ILOAD, 3);
        m.visitLdcInsn(Type.getObjectType(name));
        m.visitMethodInsn(INVOKESTATIC, RETRO, "gainedCall",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;ILjava/lang/Class;)Ljava/lang/Object;",
            false);
        m.visitInsn(ARETURN);
        m.visitMaxs(8, 4);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * The M-2a candidate: {@code call} body is a single
     * {@code indy op (Object,I)Object} — bsm {@link Bsms#gained} with args
     * {@code (iName, "op (I)I", ceiling-bsmArg 61)}. The ceiling is a bsmArg
     * in the new design, so the site desc carries no trailing int — but the
     * fixture keeps the {@code Site} signature and passes arg through.
     */
    private static byte[] indyGained(String name, String iface) {
        ClassWriter cw = classOut(name, SITE);
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "call",
            "(Ljava/lang/Object;II)Ljava/lang/Object;", null,
            new String[]{"java/lang/Throwable"});
        m.visitCode();
        m.visitVarInsn(ALOAD, 1);
        m.visitVarInsn(ILOAD, 2);
        Handle bsm = new Handle(H_INVOKESTATIC, BSMS, "gained",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/invoke/CallSite;",
            false);
        m.visitInvokeDynamicInsn("op", "(Ljava/lang/Object;I)Ljava/lang/Object;", bsm,
            iface, "op (I)I", 61);
        m.visitInsn(ARETURN);
        m.visitMaxs(2, 4);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * The {@code jvmdg$indy$} lowering shape: a static {@code CallSite}
     * field resolved once in {@code <clinit>} (the generic lowering calls
     * the bsm reflectively — same effect as calling it here), and
     * {@code call} does {@code CS.getTarget().invokeExact}.
     */
    private static byte[] noindyGained(String name, String iface) {
        ClassWriter cw = classOut(name, SITE);
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "jvmdg$indy$0",
            "Ljava/lang/invoke/CallSite;", null, null).visitEnd();
        MethodVisitor clinit = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.visitCode();
        clinit.visitLdcInsn(iface);
        clinit.visitLdcInsn("op (I)I");
        clinit.visitIntInsn(BIPUSH, 61);
        clinit.visitMethodInsn(INVOKESTATIC, BSMS,
            "lowerGained", "(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/invoke/CallSite;",
            false);
        clinit.visitFieldInsn(PUTSTATIC, name, "jvmdg$indy$0",
            "Ljava/lang/invoke/CallSite;");
        clinit.visitInsn(RETURN);
        clinit.visitMaxs(3, 0);
        clinit.visitEnd();
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "call",
            "(Ljava/lang/Object;II)Ljava/lang/Object;", null,
            new String[]{"java/lang/Throwable"});
        m.visitCode();
        m.visitFieldInsn(GETSTATIC, name, "jvmdg$indy$0", "Ljava/lang/invoke/CallSite;");
        m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "getTarget",
            "()Ljava/lang/invoke/MethodHandle;", false);
        m.visitVarInsn(ALOAD, 1);
        m.visitVarInsn(ILOAD, 2);
        m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
            "(Ljava/lang/Object;I)Ljava/lang/Object;", false);
        m.visitInsn(ARETURN);
        m.visitMaxs(3, 4);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    // ---------- retro isInstance / cast ----------

    /**
     * The generated {@code $jvmdg$retro.isInstance} shape: ordered
     * {@code instanceof adapter || marker || impl-member…} chain then a
     * name-keyed {@code RetroTypes.isInstance} table tail.
     */
    private static byte[] dispRetroIs(String name) {
        ClassWriter cw = classOut(name, BOOLSITE);
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "test", "(Ljava/lang/Object;)Z", null, null);
        m.visitCode();
        Label next = new Label(), t = new Label();
        m.visitVarInsn(ALOAD, 1);
        m.visitTypeInsn(INSTANCEOF, ADAPTER);
        m.visitJumpInsn(IFNE, t);
        m.visitVarInsn(ALOAD, 1);
        m.visitTypeInsn(INSTANCEOF, MARKER);
        m.visitJumpInsn(IFNE, t);
        for (int i = 0; i < 4; i++) {
            m.visitVarInsn(ALOAD, 1);
            m.visitTypeInsn(INSTANCEOF, "bench/R" + i);
            m.visitJumpInsn(IFNE, t);
        }
        m.visitLabel(next);
        frameSame(m);
        m.visitVarInsn(ALOAD, 1);
        m.visitLdcInsn(RETRO_SRC);
        m.visitMethodInsn(INVOKESTATIC, RETRO, "isInstance",
            "(Ljava/lang/Object;Ljava/lang/String;)Z", false);
        m.visitInsn(IRETURN);
        m.visitLabel(t);
        frameSame(m);
        m.visitInsn(ICONST_1);
        m.visitInsn(IRETURN);
        m.visitMaxs(2, 2);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * M-2b candidate — {@code indy isInstance (Object)Z} bsmed by
     * {@link Bsms#retroIsInstance}: per-rc {@code constant(boolean)}.
     */
    private static byte[] indyRetroIs(String name) {
        ClassWriter cw = classOut(name, BOOLSITE);
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "test", "(Ljava/lang/Object;)Z", null, null);
        m.visitCode();
        m.visitVarInsn(ALOAD, 1);
        Handle bsm = new Handle(H_INVOKESTATIC, BSMS, "retroIsInstance",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/invoke/CallSite;",
            false);
        m.visitInvokeDynamicInsn("isInstance", "(Ljava/lang/Object;)Z", bsm,
            RETRO_SRC, Type.getType("L" + ADAPTER + ";"), Type.getType("L" + MARKER + ";"),
            "bench/R0;bench/R1;bench/R2;bench/R3");
        m.visitInsn(IRETURN);
        m.visitMaxs(1, 2);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** noIndy-lowered retro isInstance — CallSite field + getTarget().invokeExact */
    private static byte[] noindyRetroIs(String name) {
        ClassWriter cw = classOut(name, BOOLSITE);
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "jvmdg$indy$0",
            "Ljava/lang/invoke/CallSite;", null, null).visitEnd();
        MethodVisitor clinit = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.visitCode();
        clinit.visitMethodInsn(INVOKESTATIC, BSMS,
            "lowerRetroIs", "()Ljava/lang/invoke/CallSite;", false);
        clinit.visitFieldInsn(PUTSTATIC, name, "jvmdg$indy$0",
            "Ljava/lang/invoke/CallSite;");
        clinit.visitInsn(RETURN);
        clinit.visitMaxs(1, 0);
        clinit.visitEnd();
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "test", "(Ljava/lang/Object;)Z", null, null);
        m.visitCode();
        m.visitFieldInsn(GETSTATIC, name, "jvmdg$indy$0", "Ljava/lang/invoke/CallSite;");
        m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "getTarget",
            "()Ljava/lang/invoke/MethodHandle;", false);
        m.visitVarInsn(ALOAD, 1);
        m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
            "(Ljava/lang/Object;)Z", false);
        m.visitInsn(IRETURN);
        m.visitMaxs(2, 2);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    // ---------- retro wrap ----------

    /**
     * The generated {@code $jvmdg$retro.wrap} shape:
     * {@code o instanceof adapter → o; member-chain → new Impl(o); else
     * RetroTypes.wrapImpl} — 4-member impl chain then the table.
     */
    private static byte[] dispRetroWrap(String name) {
        ClassWriter cw = classOut(name, SITE);
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "call",
            "(Ljava/lang/Object;II)Ljava/lang/Object;", null, null);
        m.visitCode();
        // adapter passthrough under catch(LinkageError) — the generated shape
        Label tryS = new Label(), handler = new Label(), chain = new Label();
        m.visitTryCatchBlock(tryS, handler, handler, "java/lang/LinkageError");
        m.visitLabel(tryS);
        m.visitVarInsn(ALOAD, 1);
        m.visitTypeInsn(INSTANCEOF, ADAPTER);
        m.visitJumpInsn(IFEQ, chain);
        m.visitVarInsn(ALOAD, 1);
        m.visitInsn(ARETURN);
        m.visitLabel(handler);
        m.visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/lang/LinkageError"});
        m.visitInsn(POP);
        m.visitLabel(chain);
        frameSame(m);
        Label tail = new Label();
        for (int i = 0; i < 4; i++) {
            Label next = new Label();
            m.visitVarInsn(ALOAD, 1);
            m.visitTypeInsn(INSTANCEOF, "bench/R" + i);
            m.visitJumpInsn(IFEQ, next);
            m.visitTypeInsn(NEW, WRAPIMPL);
            m.visitInsn(DUP);
            m.visitVarInsn(ALOAD, 1);
            m.visitMethodInsn(INVOKESPECIAL, WRAPIMPL, "<init>", "(Ljava/lang/Object;)V", false);
            m.visitInsn(ARETURN);
            m.visitLabel(next);
            frameSame(m);
        }
        m.visitLabel(tail);
        frameSame(m);
        m.visitVarInsn(ALOAD, 1);
        m.visitLdcInsn(RETRO_SRC);
        m.visitLdcInsn(Type.getObjectType(name));
        m.visitMethodInsn(INVOKESTATIC, RETRO, "wrapImpl",
            "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false);
        m.visitInsn(ARETURN);
        m.visitMaxs(4, 4);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** M-2b candidate — {@code indy wrap (Object)Object} → per-rc impl-ctor handle */
    private static byte[] indyRetroWrap(String name) {
        ClassWriter cw = classOut(name, SITE);
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "call",
            "(Ljava/lang/Object;II)Ljava/lang/Object;", null, null);
        m.visitCode();
        m.visitVarInsn(ALOAD, 1);
        Handle bsm = new Handle(H_INVOKESTATIC, BSMS, "retroWrap",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/CallSite;",
            false);
        m.visitInvokeDynamicInsn("wrap", "(Ljava/lang/Object;)Ljava/lang/Object;", bsm,
            Type.getType("L" + ADAPTER + ";"), Type.getType("L" + MARKER + ";"),
            Type.getType("L" + WRAPIMPL + ";"));
        m.visitInsn(ARETURN);
        m.visitMaxs(1, 4);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    // ---------- X.class queries ----------

    /**
     * {@code Adapter.class.isInstance(o) || Marker.class.isInstance(o) ||
     * impl-instanceof chain || RetroTypes.isInstance} — the dispatcher shape
     * {@code remapClassCalls} emits for ldc'd retro Class receivers.
     */
    private static byte[] dispXClass(String name) {
        ClassWriter cw = classOut(name, BOOLSITE);
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "test", "(Ljava/lang/Object;)Z", null, null);
        m.visitCode();
        Label tail = new Label(), t = new Label();
        m.visitLdcInsn(Type.getObjectType(ADAPTER));
        m.visitVarInsn(ALOAD, 1);
        m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "isInstance",
            "(Ljava/lang/Object;)Z", false);
        m.visitJumpInsn(IFNE, t);
        m.visitLdcInsn(Type.getObjectType(MARKER));
        m.visitVarInsn(ALOAD, 1);
        m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "isInstance",
            "(Ljava/lang/Object;)Z", false);
        m.visitJumpInsn(IFNE, t);
        for (int i = 0; i < 4; i++) {
            m.visitVarInsn(ALOAD, 1);
            m.visitTypeInsn(INSTANCEOF, "bench/R" + i);
            m.visitJumpInsn(IFNE, t);
        }
        m.visitVarInsn(ALOAD, 1);
        m.visitLdcInsn(RETRO_SRC);
        m.visitMethodInsn(INVOKESTATIC, RETRO, "isInstance",
            "(Ljava/lang/Object;Ljava/lang/String;)Z", false);
        m.visitInsn(IRETURN);
        m.visitLabel(t);
        frameSame(m);
        m.visitInsn(ICONST_1);
        m.visitInsn(IRETURN);
        m.visitMaxs(2, 2);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** M-2c — X.class.isInstance → same PIC family keyed on the arg's class */
    private static byte[] indyXClass(String name) {
        ClassWriter cw = classOut(name, BOOLSITE);
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "test", "(Ljava/lang/Object;)Z", null, null);
        m.visitCode();
        m.visitVarInsn(ALOAD, 1);
        Handle bsm = new Handle(H_INVOKESTATIC, BSMS, "retroIsInstance",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/invoke/CallSite;",
            false);
        m.visitInvokeDynamicInsn("isInstance", "(Ljava/lang/Object;)Z", bsm,
            RETRO_SRC, Type.getType("L" + ADAPTER + ";"), Type.getType("L" + MARKER + ";"),
            "bench/R0;bench/R1;bench/R2;bench/R3");
        m.visitInsn(IRETURN);
        m.visitMaxs(1, 2);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    // ---------- instance-stub dispatch (today's indy — baseline) ----------

    /**
     * Today's emitted shape verbatim: {@code indy op (Object,I)I} bsmed by
     * {@link DispatchRuntime#bootstrap} — the real PIC.
     */
    private static byte[] indyMember(String name) {
        ClassWriter cw = classOut(name, SITE);
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "call",
            "(Ljava/lang/Object;II)Ljava/lang/Object;", null,
            new String[]{"java/lang/Throwable"});
        m.visitCode();
        m.visitVarInsn(ALOAD, 1);
        m.visitVarInsn(ILOAD, 2);
        Handle bsm = new Handle(H_INVOKESTATIC, BSMS, "member",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/invoke/CallSite;",
            false);
        m.visitInvokeDynamicInsn("op", "(Ljava/lang/Object;I)I", bsm,
            Type.getMethodType("(I)I"), Type.getType("L" + FIX + "Impl;"),
            "op (Ljava/lang/Object;I)I");
        m.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
            "(I)Ljava/lang/Integer;", false);
        m.visitInsn(ARETURN);
        m.visitMaxs(2, 4);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** lowered instance-stub site — CallSite field + getTarget().invokeExact */
    private static byte[] noindyMember(String name) {
        ClassWriter cw = classOut(name, SITE);
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "jvmdg$indy$0",
            "Ljava/lang/invoke/CallSite;", null, null).visitEnd();
        MethodVisitor clinit = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.visitCode();
        clinit.visitMethodInsn(INVOKESTATIC, BSMS,
            "lowerMember", "()Ljava/lang/invoke/CallSite;", false);
        clinit.visitFieldInsn(PUTSTATIC, name, "jvmdg$indy$0",
            "Ljava/lang/invoke/CallSite;");
        clinit.visitInsn(RETURN);
        clinit.visitMaxs(1, 0);
        clinit.visitEnd();
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "call",
            "(Ljava/lang/Object;II)Ljava/lang/Object;", null,
            new String[]{"java/lang/Throwable"});
        m.visitCode();
        m.visitFieldInsn(GETSTATIC, name, "jvmdg$indy$0", "Ljava/lang/invoke/CallSite;");
        m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "getTarget",
            "()Ljava/lang/invoke/MethodHandle;", false);
        m.visitVarInsn(ALOAD, 1);
        m.visitVarInsn(ILOAD, 2);
        m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
            "(Ljava/lang/Object;I)I", false);
        m.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
            "(I)Ljava/lang/Integer;", false);
        m.visitInsn(ARETURN);
        m.visitMaxs(3, 4);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
