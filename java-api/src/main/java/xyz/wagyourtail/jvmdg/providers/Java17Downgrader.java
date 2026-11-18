package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j17.stub.java_base.*;
import xyz.wagyourtail.jvmdg.j17.stub.jdk_httpserver.C_S_N_H_Filter;

public class Java17Downgrader extends AllVersionDowngrader {
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

}