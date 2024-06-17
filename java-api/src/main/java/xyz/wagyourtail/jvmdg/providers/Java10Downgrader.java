package xyz.wagyourtail.jvmdg.providers;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j10.stub.java_base.*;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java10Downgrader extends VersionProvider {
    public Java10Downgrader() {
        super(Opcodes.V10, Opcodes.V9);
    }

    public void init() {
        // -- java.base --
        stub(J_I_ByteArrayOutputStream.class);
        stub(J_I_PrintStream.class);
        stub(J_I_PrintWriter.class);
        stub(J_I_Reader.class);
        stub(J_L_Runtime$Version.class);
        // StackWalker
        stub(J_L_I_MethodType.class);
        stub(J_N_C_Channels.class);
        stub(J_N_URLDecoder.class);
        stub(J_N_URLEncoder.class);
        // FileStore
        // DateTimeFormatter
        // DoubleSummaryStatistics
        // IntSummaryStatistics
        stub(J_U_Formatter.class);
        stub(J_U_List.class);
        // LongSummaryStatistics
        stub(J_U_Map.class);
        stub(J_U_Optional.class);
        stub(J_U_OptionalDouble.class);
        stub(J_U_OptionalInt.class);
        stub(J_U_OptionalLong.class);
        // Properties
        stub(J_U_Scanner.class);
        stub(J_U_Set.class);
        stub(J_U_SplittableRandom.class);
        // StampedLock
        // JarEntry
        // JarFile
        // LocaleNameProvider
        stub(J_U_S_Collectors.class);
        // ZipFile

        // -- java.compiler --
        // SourceVersion
        // TypeKindVisitor6
        // TypeKindVisitor9

        // -- java.desktop --
        // Toolkit
        // Path2D
        // ButtonModel

        // -- java.management --
        // RuntimeMXBean
        // ThreadMXBean

        // -- jdk.compiler --
        // DocCommentTree
        // DocTreeVisitor
        // DocTypeTree
        // SummaryTree
        // DocTreeFactory
        // DocTreeScanner
        // SimpleDocTreeVisitor

        // -- jdk.net --
        // ExtendedSocketOptions

        // -- jdk.unsupported --
        // ExtendedOpenOption
    }
}