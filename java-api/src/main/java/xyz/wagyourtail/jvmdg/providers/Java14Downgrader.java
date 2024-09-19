package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j14.stub.java_base.J_I_PrintStream;
import xyz.wagyourtail.jvmdg.j14.stub.java_base.J_L_StrictMath;
import xyz.wagyourtail.jvmdg.j14.stub.java_base.J_U_C_L_LockSupport;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java14Downgrader extends VersionProvider {
    public Java14Downgrader() {
        super(Opcodes.V14, Opcodes.V13, 0);
    }

    public void init() {
        // -- java.base --
        stub(J_I_PrintStream.class);
        // Serial
        stub(J_L_StrictMath.class);
        // MethodHandles
        // CompactNumberFormat
        stub(J_U_C_L_LockSupport.class);
        // PreviewFeature
        // Unsafe

        // -- java.compiler --
        // SourceVersion
        // AbstractAnnotationValueVisitor14
        // AbstractElementVisitor6
        // AbstractTypeVisitor14
        // ElementKindVisitor6
        // SimpleAnnotationValueVisitor14
        // SimpleTypeVisitor14
        // TypeKindVisitor14

        // -- java.xml --
        // ContentHandler

        // -- jdk.compiler --
        // SimpleTreeVisitor
        // TreeScanner

        // -- jdk.jfr --
        // Recording
        // EventStream
        // RecordingStream

        // -- jdk.jshell --
        // Snippet

        // -- jdk.management --
        // OperatingSystemMXBean
        // ThreadMXBean

        // -- jdk.nio.mapmode --
        // ExtendedMapMode
    }

}