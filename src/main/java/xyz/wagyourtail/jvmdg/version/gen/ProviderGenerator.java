package xyz.wagyourtail.jvmdg.version.gen;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.version.BoundaryTransform;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.util.List;

/**
 * Emits each bundled provider class — {@code
 * xyz.wagyourtail.jvmdg.providers.Java<N>Downgrader} — into the api jar:
 * the constructor passes the version triple, a {@code
 * registerBoundaryTransforms()} override news up every {@link
 * BoundaryTransform} impl the classpath enumeration found under
 * {@code jvmdg/j<N>/transform/} (deferred to the init window — instantiating
 * in {@code <init>} would load transform classes through the
 * DowngradingClassLoader while ServiceLoader is still building the
 * provider chain), and the {@code init()} body calls {@code stub(Class)}
 * for every marker class under {@code jvmdg/j<N>/} — compile-checked
 * registrations instead of replayed resource lists. Providers at or
 * below {@code V1_8} (boundaries whose stub coverage is known-thin) also
 * get the {@code ensureInit} "VERY incomplete" warn the authored classes
 * carried.
 */
public class ProviderGenerator implements ApiGenerator {

    private static final String AVD = "xyz/wagyourtail/jvmdg/providers/AllVersionDowngrader";
    private static final String CLASS_DOWNGRADER = "xyz/wagyourtail/jvmdg/ClassDowngrader";
    private static final String LOGGER = "xyz/wagyourtail/jvmdg/logging/Logger";

    @Override
    public String name() {
        return "providers";
    }

    @Override
    public void generate(GenContext ctx) throws IOException {
        for (VersionProvider vp : ctx.providers) {
            String internal = vp.providerKey().replace('.', '/');
            ctx.emit(internal, node(vp, internal), dump(vp));
        }
    }

    private static ClassNode node(VersionProvider vp, String internal) {
        ClassNode node = new ClassNode();
        node.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internal, null, AVD, null);

        MethodVisitor mv = node.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        iconst(mv, vp.inputVersion);
        iconst(mv, vp.outputVersion);
        iconst(mv, vp.priotity);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, AVD, "<init>", "(III)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(4, 1);
        mv.visitEnd();

        if (!vp.boundaryTransforms.isEmpty()) {
            mv = node.visitMethod(Opcodes.ACC_PUBLIC, "registerBoundaryTransforms", "()V", null, null);
            mv.visitCode();
            for (BoundaryTransform transform : vp.boundaryTransforms) {
                String tName = transform.getClass().getName().replace('.', '/');
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, internal, "boundaryTransforms", "Ljava/util/List;");
                mv.visitTypeInsn(Opcodes.NEW, tName);
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, tName, "<init>", "()V", false);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                mv.visitInsn(Opcodes.POP);
            }
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(4, 1);
            mv.visitEnd();
        }

        mv = node.visitMethod(Opcodes.ACC_PUBLIC, "init", "()V", null, null);
        mv.visitCode();
        if (vp.genStubClasses != null) {
            for (String fqcn : vp.genStubClasses) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitLdcInsn(Type.getObjectType(fqcn.replace('.', '/')));
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internal, "stub", "(Ljava/lang/Class;)V", false);
            }
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();

        if (vp.inputVersion <= Opcodes.V1_8) {
            int in = vp.inputVersion - 44;
            mv = node.visitMethod(Opcodes.ACC_PUBLIC, "ensureInit", "(L" + CLASS_DOWNGRADER + ";)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internal, "isInitialized", "()Z", false);
            Label done = new Label();
            mv.visitJumpInsn(Opcodes.IFNE, done);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.GETFIELD, CLASS_DOWNGRADER, "logger", "L" + LOGGER + ";");
            mv.visitLdcInsn("Java " + in + " -> " + (in - 1) + " Stubs are VERY incomplete!");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, LOGGER, "warn", "(Ljava/lang/String;)V", false);
            // branch target needs a stackmap frame at V1_8 — locals are
            // unchanged from the initial frame ([this, downgrader])
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitLabel(done);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, AVD, "ensureInit", "(L" + CLASS_DOWNGRADER + ";)V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }

        node.visitEnd();
        return node;
    }

    private static void iconst(MethodVisitor mv, int v) {
        if (v >= -1 && v <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + v);
        } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, v);
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, v);
        } else {
            mv.visitLdcInsn(v);
        }
    }

    private static String dump(VersionProvider vp) {
        String fqcn = vp.providerKey();
        int last = fqcn.lastIndexOf('.');
        StringBuilder sb = new StringBuilder();
        sb.append("// generated by StubIndexWriter — inspection dump; the .class is authoritative\n");
        sb.append("package ").append(fqcn, 0, last).append(";\n\n");
        sb.append("public class ").append(fqcn.substring(last + 1)).append(" extends AllVersionDowngrader {\n\n");
        sb.append("    public ").append(fqcn.substring(last + 1)).append("() {\n");
        sb.append("        super(").append(vp.inputVersion).append(", ").append(vp.outputVersion).append(", ").append(vp.priotity).append(");\n");
        sb.append("    }\n");
        if (!vp.boundaryTransforms.isEmpty()) {
            sb.append("\n    @Override\n    public void registerBoundaryTransforms() {\n");
            for (BoundaryTransform transform : vp.boundaryTransforms) {
                sb.append("        boundaryTransforms.add(new ").append(transform.getClass().getName()).append("());\n");
            }
            sb.append("    }\n");
        }
        sb.append("\n    @Override\n    public void init() {\n");
        if (vp.genStubClasses != null) {
            for (String stubClass : vp.genStubClasses) {
                sb.append("        stub(").append(stubClass).append(".class);\n");
            }
        }
        sb.append("    }\n");
        if (vp.inputVersion <= Opcodes.V1_8) {
            int in = vp.inputVersion - 44;
            sb.append("\n    @Override\n    public void ensureInit(xyz.wagyourtail.jvmdg.ClassDowngrader downgrader) {\n");
            sb.append("        if (!isInitialized()) {\n");
            sb.append("            downgrader.logger.warn(\"Java ").append(in).append(" -> ").append(in - 1).append(" Stubs are VERY incomplete!\");\n");
            sb.append("        }\n        super.ensureInit(downgrader);\n    }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

}
