package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.RecordComponentNode;
import xyz.wagyourtail.jvmdg.j16.RecordComponents;
import xyz.wagyourtail.jvmdg.j16.stub.J_U_L_LogRecord;
import xyz.wagyourtail.jvmdg.j16.stub.java_base.*;
import xyz.wagyourtail.jvmdg.j16.stub.java_net_http.J_N_H_HttpRequest;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java16Downgrader extends VersionProvider {
    public Java16Downgrader() {
        super(Opcodes.V16, Opcodes.V15, 0);
    }

    public void init() {
        // -- java.base --
        stub(J_L_Class.class);
        stub(J_L_IndexOutOfBoundsException.class);
        stub(J_L_Record.class);
        // ElementType
        // MethodHandles
        // VarHandle
        stub(J_L_R_Reference.class);
        // InvocationHandler
        stub(J_L_R_RecordComponent.class);
        stub(J_N_ByteBuffer.class);
        stub(J_L_R_ObjectMethods.class);
        // StandardProtocolFamily
        // UnixDomainSocketAddress
        // MGF1ParameterSpec
        // DateTimeFormatterBuilder
        // IllegalFormatArgumentIndexException
        stub(J_U_Objects.class);
        stub(J_U_S_DoubleStream.class);
        stub(J_U_S_DoubleStream.DoubleMapMultiConsumer.class);
        stub(J_U_S_IntStream.class);
        stub(J_U_S_IntStream.IntMapMultiConsumer.class);
        stub(J_U_S_LongStream.class);
        stub(J_U_S_LongStream.LongMapMultiConsumer.class);
        stub(J_U_S_Stream.class);
        // ValueBased
        // Preconditions // same exact as J_U_Objects, but it's internal here... jdk package
        // IntrinsicCandidate
        // TypeAnnotation

        // -- java.compiler --
        // SourceVersion
        // ElementKind
        // ElementVisitor
        // RecordComponentElement
        // TypeElement
        // AbstractElementVisitor14
        // ElementFilter
        // ElementKindVisitor14
        // ElementKindVisitor6
        // Elements
        // ElementScanner14
        // SimpleElementVisitor14

        // -- java.logging --
        stub(J_U_L_LogRecord.class);

        // -- java.net.http --
        stub(J_N_H_HttpRequest.class);

        // -- jdk.compiler --
        // ReturnTree
        // BindingPatternTree
        // InstanceOfTree
        // PatternTree
        // Tree
        // TreeVisitor
        // DocTreeFactory

        // -- jdk.jfr --
        // MetadataEvent
        // Throttle

        // -- jdk.jshell --
        // Selector

        // -- jdk.management.jfr --
        // RemoteRecordingStream

        // -- jdk.net --
        // ExtendedSocketOptions
        // UnixDomainPrincipal
    }

    @Override
    public ClassNode otherTransforms(ClassNode clazz) {
        recordRemover(clazz);
        return clazz;
    }

    public void recordRemover(ClassNode node) {
        if ((node.access & Opcodes.ACC_RECORD) != 0) {
            node.access &= ~Opcodes.ACC_RECORD;
//            StringBuilder value = new StringBuilder();
//            for (int i = 0; i < node.recordComponents.size(); i++) {
//                RecordComponentNode recordComponent = node.recordComponents.get(i);
//                value.append(recordComponent.name).append(' ').append(recordComponent.signature).append(';');
//            }
//            value.deleteCharAt(value.length() - 1);
//            node.recordComponents = null;
//            node.visitField(
//                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
//                "jvmdowngrader$recordComponents",
//                "Ljava/lang/String;",
//                null,
//                value.toString()
//            ).visitEnd();
        }
        if (node.recordComponents != null) {
            AnnotationVisitor av = node.visitAnnotation(Type.getType(RecordComponents.class).getDescriptor(), true);
            AnnotationVisitor values = av.visitArray("value");

            for (RecordComponentNode rcn : node.recordComponents) {
                AnnotationVisitor value = values.visitAnnotation(null, Type.getType(RecordComponents.Value.class).getDescriptor());
                value.visit("name", rcn.name);
                value.visit("type", Type.getType(rcn.descriptor));
                value.visitEnd();
            }
            values.visitEnd();
            av.visitEnd();

            node.recordComponents = null;
        }
    }
}