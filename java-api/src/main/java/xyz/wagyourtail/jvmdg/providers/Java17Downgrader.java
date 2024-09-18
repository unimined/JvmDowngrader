package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.j17.PermittedSubClasses;
import xyz.wagyourtail.jvmdg.j17.stub.java_base.*;
import xyz.wagyourtail.jvmdg.j17.stub.jdk_httpserver.C_S_N_H_Filter;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java17Downgrader extends VersionProvider {
    public Java17Downgrader() {
        super(Opcodes.V17, Opcodes.V16, 0);
    }

    public void init() {
        // -- java.base --
        stub(J_I_Console.class);
        // ObjectInputFilter
        stub(J_L_Class.class);
        stub(J_L_Process.class);
        // SwitchBootstraps
        // InstantSource
        stub(J_U_HexFormat.class);
        stub(J_U_Map$Entry.class);
        // SplittableRandom
        // ForkJoinPool
        stub(J_U_Random.class);
        stub(J_U_SplittableRandom.class);
        stub(J_U_R_RandomGenerator.class);
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
            AnnotationVisitor av = node.visitAnnotation(Type.getType(PermittedSubClasses.class).getDescriptor(), true);
            AnnotationVisitor values = av.visitArray("value");
            for (String s : node.permittedSubclasses) {
                values.visit(null, Type.getObjectType(s));
            }
            values.visitEnd();
            av.visitEnd();

            node.permittedSubclasses = null;
        }
    }
}