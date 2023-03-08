package xyz.wagyourtail.jvmdg.j13;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.VersionProvider;
import xyz.wagyourtail.jvmdg.j13.stub.J_N_Buffer;
import xyz.wagyourtail.jvmdg.j13.stub.J_N_F_FileSystems;

public class Java13Downgrader extends VersionProvider {
    public Java13Downgrader() {
        super(Opcodes.V13);
    }

    public void init() {
            // -- java.base --
            // Character$UnicodeBlock (more unicode spaces);
            stub(J_N_Buffer.class);
            // MappedByteBuffer
            stub(J_N_F_FileSystems.class);
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