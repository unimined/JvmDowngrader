package xyz.wagyourtail.jvmdg.version.gen;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.AdapterForwarders;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Emits {@code <adapter>$jvmdg$fwd} — the member-forwarder host for
 * OWN_INTERFACE adapters. Each instance method {@code R m(A…)} on the
 * stand-in interface gets one public static forwarder
 * {@code R m(Object self, A…)} that wraps the receiver via the sibling
 * {@code $jvmdg$retro.wrap} and invokeinterfaces the member — the
 * mechanical shape {@code @Stub static R m(@Coerce Adapter self, A…)} used
 * to be written out by hand per member. {@link AdapterForwarders#registerAll}
 * records the matching entries post-scan (idx rows name this class; the
 * instance-stub {@code invokedynamic} sites resolve them lazily), so only
 * adaptable adapters that actually declare forwardable members get a
 * class, and an authored class of the same name wins via {@code ctx.declared}.
 * <p>
 * Ordinary api classes: V1_8, deterministic member order (name+desc),
 * reference-indexed/shaded/pruned like every other generated sibling.
 */
public final class MemberForwarderGenerator implements ApiGenerator {

    private static final String OBJECT = "java/lang/Object";

    @Override
    public String name() {
        return "member-forwarder";
    }

    @Override
    public void generate(GenContext ctx) throws IOException {
        // stand-in types get wrapped before the member call — the arg may
        // carry a real-JDK member object at runtime (requiresRuntime paths),
        // which the sibling adapter's wrap adapts; already-adapted args pass
        // through its instanceof fast-path
        Set<String> standIns = new HashSet<>();
        for (VersionProvider vp : ctx.providers) {
            for (ClassStubEntry e : vp.classStubs.values()) {
                if (e.mode == Adapter.Mode.OWN_INTERFACE) {
                    standIns.add(e.stubType.getInternalName());
                }
            }
        }
        Set<String> emitted = new HashSet<>();
        for (VersionProvider vp : ctx.providers) {
            for (Map.Entry<Type, ClassStubEntry> e : vp.classStubs.entrySet()) {
                ClassStubEntry stub = e.getValue();
                if (stub.mode != Adapter.Mode.OWN_INTERFACE || !AdapterForwarders.adaptable(stub, vp)) {
                    continue;
                }
                String adapter = stub.stubType.getInternalName();
                String name = adapter + AdapterForwarders.SUFFIX;
                if (!emitted.add(name) || ctx.declared(name)) {
                    continue;
                }
                emit(ctx, adapter, stub.clazz(), name, standIns);
            }
        }
    }

    private void emit(GenContext ctx, String adapter, Class<?> clazz, String name,
                      Set<String> standIns) throws IOException {
        List<Method> forward = AdapterForwarders.forwardableMethods(clazz);
        if (forward.isEmpty()) {
            return;
        }
        ClassNode node = new ClassNode();
        node.visit(Opcodes.V1_8,
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
            name, null, OBJECT, null);
        node.visitSource(name.substring(name.lastIndexOf('/') + 1) + ".java", null);
        MethodNode ctor = new MethodNode(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, OBJECT, "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(1, 1);
        node.methods.add(ctor);
        StringBuilder dump = dumpHead(adapter, name);
        for (Method m : forward) {
            node.methods.add(fwd(adapter, m, standIns));
            dumpFwd(dump, adapter, m, standIns);
        }
        node.visitEnd();
        ctx.emit(name, node, dump.append("}\n").toString());
    }

    private static String simple(String name) {
        return name.substring(name.lastIndexOf('/') + 1);
    }

    /**
     * {@code static R m(Object self, A… args) { return ((A) $retro.wrap(self)).m(args); } }
     * — the receiver arrives unadapted (real JDK member object or impl), the
     * sibling dispatcher's wrap handles every case the authored
     * {@code jvmdg$wrap} did, and the invokeinterface fires the stand-in's
     * default body or the impl's override — preserving {@code X.super.m()}
     * semantics at call sites the noSpecial derivation left alone.
     */
    private static MethodNode fwd(String adapter, Method m, Set<String> standIns) {
        Type desc = AdapterForwarders.handlerDesc(m);
        String[] exceptions = internalNames(m.getExceptionTypes());
        MethodNode mn = new MethodNode(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | (m.isVarArgs() ? Opcodes.ACC_VARARGS : 0),
            m.getName(), desc.getDescriptor(), null, exceptions);
        mn.visitVarInsn(Opcodes.ALOAD, 0);
        mn.visitMethodInsn(Opcodes.INVOKESTATIC, adapter + "$jvmdg$retro", "wrap",
            "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        mn.visitTypeInsn(Opcodes.CHECKCAST, adapter);
        int slot = 1;
        for (Type a : Type.getArgumentTypes(m)) {
            mn.visitVarInsn(a.getOpcode(Opcodes.ILOAD), slot);
            if (a.getSort() == Type.OBJECT && standIns.contains(a.getInternalName())) {
                // adapted-typed arg — the caller may hold a real-JDK member
                // object; wrap it through the arg's own adapter before the call
                mn.visitMethodInsn(Opcodes.INVOKESTATIC,
                    a.getInternalName() + "$jvmdg$retro", "wrap",
                    "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                mn.visitTypeInsn(Opcodes.CHECKCAST, a.getInternalName());
            }
            slot += a.getSize();
        }
        mn.visitMethodInsn(Opcodes.INVOKEINTERFACE, adapter, m.getName(),
            Type.getMethodDescriptor(m), true);
        mn.visitInsn(Type.getReturnType(m).getOpcode(Opcodes.IRETURN));
        // stack peaks at the call depth (wrapped receiver + args) or at the
        // returned value, whichever is deeper — a wide return type alone
        // exceeds the receiver slot for zero-arg members
        mn.visitMaxs(Math.max(slot, Type.getReturnType(m).getSize()), slot);
        return mn;
    }

    private static String[] internalNames(Class<?>[] classes) {
        String[] out = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            out[i] = Type.getInternalName(classes[i]);
        }
        return out;
    }

    // ---- .java inspection dump ----

    private static StringBuilder dumpHead(String adapter, String name) {
        String simple = simple(name);
        StringBuilder sb = new StringBuilder();
        sb.append("// generated by StubIndexWriter — inspection dump; the .class is authoritative\n");
        int pkg = adapter.lastIndexOf('/');
        if (pkg >= 0) {
            sb.append("package ").append(adapter.substring(0, pkg).replace('/', '.')).append(";\n\n");
        }
        sb.append("public final class ").append(simple).append(" {\n");
        sb.append("    private ").append(simple).append("() { }\n\n");
        return sb;
    }

    private static void dumpFwd(StringBuilder sb, String adapter, Method m, Set<String> standIns) {
        String simple = simple(adapter);
        sb.append("    public static ").append(dotted(Type.getReturnType(m)))
            .append(' ').append(m.getName()).append("(Object self");
        Type[] args = Type.getArgumentTypes(m);
        for (int i = 0; i < args.length; i++) {
            sb.append(", ").append(dotted(args[i])).append(" a").append(i);
        }
        sb.append(") {\n        ");
        if (m.getReturnType() != void.class) {
            sb.append("return ");
        }
        sb.append("((").append(simple)
            .append(") ").append(simple).append("$jvmdg$retro.wrap(self)).").append(m.getName()).append("(");
        for (int i = 0; i < args.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            if (args[i].getSort() == Type.OBJECT && standIns.contains(args[i].getInternalName())) {
                sb.append('(').append(dotted(args[i])).append(") ")
                    .append(dotted(args[i].getInternalName())).append("$jvmdg$retro.wrap(a").append(i).append(')');
            } else {
                sb.append('a').append(i);
            }
        }
        sb.append(");\n    }\n\n");
    }

    private static String dotted(String internalName) {
        return internalName.replace('/', '.').replace('$', '.');
    }

    private static String dotted(Type type) {
        if (type.getSort() == Type.ARRAY) {
            StringBuilder sb = new StringBuilder(dotted(type.getElementType()));
            for (int i = 0; i < type.getDimensions(); i++) {
                sb.append("[]");
            }
            return sb.toString();
        }
        if (type.getSort() == Type.OBJECT) {
            return dotted(type.getInternalName());
        }
        return type.getClassName();
    }

}
