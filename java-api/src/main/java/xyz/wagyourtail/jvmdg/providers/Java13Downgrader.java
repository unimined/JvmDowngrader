package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j13.stub.java_base.*;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java13Downgrader extends VersionProvider {
    public Java13Downgrader() {
        super(Opcodes.V13, Opcodes.V12, 0);
    }

    public void init() {
        // -- java.base --
        // Character$UnicodeBlock (more unicode spaces);
        stub(J_N_Buffer.class);
        stub(J_N_ByteBuffer.class);
        stub(J_N_CharBuffer.class);
        stub(J_N_DoubleBuffer.class);
        stub(J_N_F_FileSystems.class);
        stub(J_N_FloatBuffer.class);
        stub(J_N_IntBuffer.class);
        stub(J_N_LongBuffer.class);
        // MappedByteBuffer
        stub(J_N_ShortBuffer.class);
        // Signature
        // DecimalFormatSymbols
        // JapaneseEra
        // Unsafe
        // UnsafeConstants
        // GCMParameters

        // -- java.compiler --
        // ProcessingEnvironment
        // SourceVersion
        // StandardJavaFileManager

        // -- java.security --
        // KerberosPrincipal

        // -- java.xml.crypto --
        // CanonicalizationMethod
        // DocumentBuilderFactory
        // SAXParserFactory

        // -- jdk.compiler --
        // Tree
        // TreeVisitor
        // YieldTree
        // JavacTask
        // ParameterNameProvider
    }

}