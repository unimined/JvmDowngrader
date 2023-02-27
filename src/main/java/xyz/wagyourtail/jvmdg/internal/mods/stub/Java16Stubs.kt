package xyz.wagyourtail.jvmdg.internal.mods.stub

import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods.stub._16.*

object Java16Stubs {
    fun apply() {
        MethodReplacer.apply {
            // -- java.base --
            addClass(J_L_Class::class.java)
            addClass(J_L_IndexOutOfBoindsException::class.java)
            // Record
            // ElementType
            // MethodHandles
            // VarHandle
            // Reference
            // InvocationHandler
            addClass(J_L_R_RecordComponent::class.java)
            // ObjectMethods (see replacers)
            // StandardProtocolFamily
            // UnixDomainSocketAddress
            // MGF1ParameterSpec
            // DateTimeFormatterBuilder
            // IllegalFormatArgumentIndexException
            addClass(J_U_Objects::class.java)
            addClass(J_U_S_DoubleStream::class.java)
            addClass(J_U_S_DoubleStream.DoubleMapMultiConsumer::class.java)
            addClass(J_U_S_IntStream::class.java)
            addClass(J_U_S_IntStream.IntMapMultiConsumer::class.java)
            addClass(J_U_S_LongStream::class.java)
            addClass(J_U_S_LongStream.LongMapMultiConsumer::class.java)
            addClass(J_U_S_Stream::class.java)
            // ValueBased
            // Preconditions TODO: same exact as J_U_Objects, but it's internal here... jdk package
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
            addClass(J_U_L_LogRecord::class.java)

            // -- java.net.http --
            addClass(J_N_H_HttpRequest::class.java)

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
    }
}