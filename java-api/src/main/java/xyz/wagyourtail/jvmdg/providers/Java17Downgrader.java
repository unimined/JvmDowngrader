package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.j17.stub.*;

public class Java17Downgrader extends VersionProvider {
    public Java17Downgrader() {
        super(Opcodes.V17, Opcodes.V16);
    }

    public void init() {
            // -- java.base --
            stub(J_I_Console.class);
            // ObjectInputFilter
            stub(J_L_Class.class);
            stub(J_L_Process.class);
            // SwitchBootstraps
            // InstantSource
            // HexFormat
            stub(J_U_Map$Entry.class);
            // SplittableRandom
            // ForkJoinPool
            // RandomGenerator
            // RandomGeneratorFactory

            // -- java.compiler --
            // RandomSupport
            // SourceVersion
            // Modifier
            // TypeElement
            // Elements

            // -- java.desktop --
            // FileSystemView
            // DMarlinRenderingEngine
            // RenderingEngine

            // -- java.xml.crypto --
            // SignatureMethod
            // RSAPSSParameterSpec

            // -- jdk.compiler --
            // CaseLabelTree
            // CaseTree
            // ClassTree
            // CompilationUnitTree
            // DefaultCaseLabelTree
            // GuardedPatternTree
            // ParenthesizedPatternTree
            // Tree
            // TreeVisitor
            // SimpleTreeVisitor
            // TreeScanner

            // -- jdk.httpserver --
            stub(C_S_N_H_Filter.class);

            // -- jdk.javadoc --
            // Reporter
            // StandardDoclet

            // -- jdk.jfr --
            // RecordingStream

            // -- jdk.jshell --
            // JavaShellToolBuilder

            // -- jdk.management.jfr --
            // RemoteRecordingStream
    }

    @Override
    public ClassNode otherTransforms(ClassNode clazz) {
        unseal(clazz);
        return clazz;
    }

    // TODO: add bytecode to check the seal and throw
    public void unseal(ClassNode node) {
        if (node.permittedSubclasses != null) {
            StringBuilder sb = new StringBuilder();
            for (String s : node.permittedSubclasses) {
                sb.append(s).append(";");
            }
            sb.deleteCharAt(sb.length() - 1);
            node.permittedSubclasses = null;
            node.visitField(
                Constants.synthetic(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL),
                "permittedSubclasses$jvmdowngrader",
                "Ljava/lang/String;",
                null,
                sb.toString()
            ).visitEnd();
        }
    }
}