package xyz.wagyourtail.jvmdg.test.corpusgen;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Emits corpus fixture classes that javac cannot produce — ldc of a
 * ConstantDynamic, and a nested condy inside bootstrap-method args — directly
 * as {@code .class} files into {@code build/generated/featureClasses}. The
 * output dir is on the {@code featuretests} compile classpath (so javac
 * resolves references to them) and packaged into {@code featuretests.jar}.
 * This replaces the annotationasm plugin weaving previously used for
 * {@code Condy}'s {@code @MethodASM} methods.
 *
 * <p>{@code CondyWoven} mirrors the bytecode {@code @MethodASM} produced:
 * {@code ldc <condy>} + call sites exercising the j11 {@code ldcCondyToIndy}
 * and nested-condy rewrite paths.
 */
public final class FeatureCorpusGenerator implements Opcodes {

    private static final String CONDY_WOVEN = "jvmdg/featuretests/manual/CondyWoven";

    /** {@code ConstantBootstraps.invoke(Lookup, String, Class, MethodHandle, Object[])Object} */
    private static Handle invokeBsm() {
        return new Handle(H_INVOKESTATIC, "java/lang/invoke/ConstantBootstraps", "invoke",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;"
                + "Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/Object;", false);
    }

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args[0]);
        int major = Integer.parseInt(args[1]) + 44;
        writeClass(out, CONDY_WOVEN + ".class", condyWoven(major));
    }

    private static void writeClass(Path out, String name, byte[] bytes) throws IOException {
        Path target = out.resolve(name);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
    }

    private static byte[] condyWoven(int major) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(major, ACC_PUBLIC | ACC_FINAL, CONDY_WOVEN, null, "java/lang/Object", null);
        explicitCast(cw);
        nestedCondy(cw);
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** {@code System.out.println((String) ldc<condy explicitCast("test")>);} */
    private static void explicitCast(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "testExplicitCast", "()V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(new ConstantDynamic("_", "Ljava/lang/CharSequence;",
            new Handle(H_INVOKESTATIC, "java/lang/invoke/ConstantBootstraps", "explicitCast",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;"
                    + "Ljava/lang/Object;)Ljava/lang/Object;", false),
            "test"));
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * {@code System.out.println(ldc<condy EnumDesc.of(condy ClassDesc.of(
     * "jvmdg.featuretests.manual.Condy$Type"), "VALUE_A")>);} — a condy whose
     * bsmArgs contain another condy, exercising nestedCondyInIndy.
     */
    private static void nestedCondy(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "testNestedCondy", "()V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        ConstantDynamic classDesc = new ConstantDynamic("_", "Ljava/lang/constant/ClassDesc;", invokeBsm(),
            new Handle(H_INVOKESTATIC, "java/lang/constant/ClassDesc", "of",
                "(Ljava/lang/String;)Ljava/lang/constant/ClassDesc;", true),
            "jvmdg.featuretests.manual.Condy$Type");
        mv.visitLdcInsn(new ConstantDynamic("_", "Ljava/lang/Enum$EnumDesc;", invokeBsm(),
            new Handle(H_INVOKESTATIC, "java/lang/Enum$EnumDesc", "of",
                "(Ljava/lang/constant/ClassDesc;Ljava/lang/String;)Ljava/lang/Enum$EnumDesc;", false),
            classDesc,
            "VALUE_A"));
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private FeatureCorpusGenerator() {
    }
}
