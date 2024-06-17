package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.j9.stub.java_base.*;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.util.ArrayList;
import java.util.List;

public class Java9Downgrader extends VersionProvider {


    public Java9Downgrader() {
        super(Opcodes.V9, Opcodes.V1_8);
    }

    @Override
    public void init() {
        // -- java.base --
        stub(J_I_InputStream.class);
        // ObjectInputFilter
        // ObjectInputStream
        // ObjectStreamConstants
        // AbstractStringBuilder -- handled by CharSequence
        stub(J_L_Byte.class);
        // Character -- LTR/RTL and unicode spaces
        stub(J_L_Class.class);
        stub(J_L_ClassLoader.class);
        // Deprecated
        // IllegalCallerException
        stub(J_L_IndexOutOfBoundsException.class);
        stub(J_L_Integer.class);
        // LayeredInstantiationException
        stub(J_L_Math.class);
        stub(J_L_Module.class);
        stub(J_L_ModuleLayer.class);
        // Process
        // ProcessBuilder
        // ProcessHandle
        // ProcessHandleImpl
        stub(J_L_Runtime.class);
        stub(J_L_Runtime.Version.class);
        stub(J_L_Short.class);
        // StackTraceElement
        stub(J_L_StackWalker.class);
        stub(J_L_StrictMath.class);
        // String -- handled by CharSequence
        stub(J_L_System.class);
        // TODO: this properly
//        ClassReplacer.registerReplace(
//            JavaVersion.VERSION_1_9,
//            "Ljava/lang/System\$Logger;",
//            "Ljava/util/logging/Logger;"
//        )
//        ClassReplacer.registerReplace(
//            JavaVersion.VERSION_1_9,
//            "Ljava/lang/System\$Logger\$Level;",
//            "Ljava/util/logging/Level;"
//        )
        // System$LoggerFinder
        stub(J_L_Thread.class);
        // ElementType
        // MethodHandle
        stub(J_L_I_MethodHandles.class);
        // MethodHandles$Lookup
        // StringConcatException
        stub(J_L_I_StringConcatFactory.class);
        // VarHandle
        // Configuration
        // FindException
        // InvalidModuleDescriptorException
        // ModuleDescriptor
        // ModuleFinder
        // ModuleReader
        // ModuleReference
        // ResolutionException
        // ResolvedModule
        stub(J_L_R_Cleaner.class);
        // Reference
        stub(J_L_R_AccessibleObject.class);
        // AnnotatedArrayType
        // AnnotatedParameterizedType
        // AnnotatedType
        // AnnotatedTypeVariable
        // AnnotatedWildcardType
        // InaccessibleObjectException
        stub(J_M_BigDecimal.class);
        stub(J_M_BigInteger.class);
        // MutableBigInteger
        // Authenticator
        // DatagramSocket
        // DatagramSocketImpl
        // HttpUrlConnection
        // InetAddress
        // NetworkInterface
        // ProxySelector
        // ServerSocket
        // Socket
        // SocketImpl
        // SocketOptions
        // StandardSocketOptions
        // URLClassLoader
        // URLConnection
        // URLStreamHandlerProvider
        stub(J_N_Buffer.class);
        stub(J_N_F_Path.class);
        // AuthProvider
        // DrbgParameters
        // KeyStore
        // KeyStoreSpi
        // PermissionCollection
        // ProtectionDomain
        // Provider
        // SecureClassLoader
        // SecureRandom
        // SecureRandomParameters
        // SecureRandomSpi
        // EncodeKeySpec
        // PKCS8EncodedKeySpec
        // X509EncodedKeySpec
        // URICertStoreParameters
        stub(J_T_Clock.class);
        stub(J_T_Duration.class);
        stub(J_T_LocalDate.class);
        stub(J_T_LocalTime.class);
        stub(J_T_OffsetTime.class);
        stub(J_T_C_Chronology.class);
        stub(J_T_C_IsoChronology.class);
        // DateTimeFormatterBuilder
        stub(J_U_Arrays.class);
        stub(J_U_Currency.class);
        stub(J_U_Enumeration.class);
        // ImmutableCollections
        // KeyValueHolder
        stub(J_U_List.class);
        // Locale
        stub(J_U_Map.class);
        stub(J_U_Objects.class);
        stub(J_U_Optional.class);
        stub(J_U_OptionalDouble.class);
        stub(J_U_OptionalInt.class);
        stub(J_U_OptionalLong.class);
        stub(J_U_ResourceBundle.class);
        stub(J_U_Scanner.class);
        stub(J_U_ServiceLoader.class);
        stub(J_U_ServiceLoader.Provider.class);
        stub(J_U_Set.class);
        stub(J_U_C_A_AtomicReference.class);
        stub(J_U_C_CompletableFuture.class);
        stub(J_U_C_Flow.class);
        stub(J_U_C_Flow.Publisher.class);
        stub(J_U_C_Flow.Subscriber.class);
        stub(J_U_C_Flow.Subscriber.class);
        stub(J_U_C_Flow.Subscription.class);
        stub(J_U_C_Flow.Processor.class);
        // ForkJoinPool
        // ForkJoinTask
        // SubmissionPublisher
        stub(J_U_C_TimeUnit.class);
        // AtomicBoolean
        // AtomicInteger
        // AtomicIntegerArray
        // AtomicLong
        // AtomicLongArray
        // AtomicReference
        // AtomicReferenceArray
        // Attributes
        // JarFile
        stub(J_U_R_Matcher.class);
        // AbstractResourceBundleProvider
        // ResourceBundleProvider
        // ToolProvider
        stub(J_U_S_Collectors.class);
        stub(J_U_S_DoubleStream.class);
        stub(J_U_S_IntStream.class);
        stub(J_U_S_LongStream.class);
        stub(J_U_S_Stream.class);
        // WhileOps
        stub(J_U_Z_Checksum.class);
        // CRC32C
        stub(J_U_Z_ZipEntry.class);
        // ExtendedSSLSession
        // SSLEngine
        // SSLParameters
        // SSLSocket
        // ClassLoaderValue
        // Preconditions
        // InvalidJarIndexError
        // ReservedStackAccess
        // EntropySource
        // ResponderId
        // KeyStoreDelegator
        // JavaTimeDateTimePatternProvider
        // SSLEngineResult
        // Signal
        // SHA3
        // ClassFileTransformer

        // -- java.naming --
        // JdkLDAP
        // LDAPCertStoreImpl

        // -- java.compiler --
        // Generated
        // RoundEnvironment
        // SourceVersion
        // ElementKind
        // ElementVisitor
        // ModuleElement
        // UnknownDirectiveException
        // TypeKind
        // AbstractAnnotationValueVisitor9
        // AbstractElementVisitor6
        // AbstractElementVisitor9
        // ElementFilter
        // ElementKindVisitor9
        // SimpleAnnotationValueVisitor9
        // SimpleElementVisitor9
        // SimpleTypeVisitor9
        // TypeKindVisitor9
        // DocumentationTool
        // ForwardingJavaFileManager
        // JavaCompiler
        // JavaFileManager
        // StandardJavaFileManager
        // StandardLocation
        // Tool

        // -- java.datatransfer --
        // DataFlavorUtil
        // DesktopDatatransferService
        // Component
        // Desktop
        // Font
        // RenderingHints
        // Robot
        // Taskbar
        // AboutEvent
        // AboutHandler
        // AppEvent
        // AppForegroundEvent
        // AppForegroundListener
        // AppHiddenEvent
        // AppHiddenListener
        // AppReopenedEvent
        // AppReopenedListener
        // FilesEvent
        // OpenFilesEvent
        // OpenFilesHandler
        // OpenURIEvent
        // OpenURIHandler
        // PreferencesEvent
        // PreferencesHandler
        // PrintFilesEvent
        // PrintFilesHandler
        // QuitEvent
        // QuitHandler
        // QuitResponse
        // QuitStrategy
        // ScreenSleepEvent
        // ScreenSleepListener
        // SystemEventListener
        // UserSessionEvent
        // UserSessionListener
        // FocusEvent
        // NumericShaper
        // AbstractMultiResolutionImage
        // BaseMultiResolutionImage
        // MultiResolutionImage
        // BeanProperty
        // IndexedPropertyDescriptor
        // JavaBean
        // PropertyDescriptor
        // AccessibilityProvider
        // BaselineTIFFTagSet
        // ExifGPSTagSet
        // ExifInteroperabilityTagSet
        // ExifParentTIFFTagSet
        // FaxTIFFTagSet
        // GeoTIFFTagSet
        // TIFFDirectory
        // TIFFField
        // TIFFImageReadParam
        // TIFFTag
        // TIFFTagSet
        // InputVerifier
        // JComponent
        // JList
        // JToggleButton
        // SwingContainer
        // UIClientPropertyKey
        // UIDefaults
        // UIManager
        // FileSystemView
        // TextUI
        // BasicGraphicsUtils
        // AbstractDocument
        // JTextComponent
        // PasswordView
        // PlainView
        // Utilities
        // WrappedPlainView
        // ComponentFactory
        // LightweightFrame
        // DesktopDatatransferServiceImpl
        // LightweightContent
        // UndoableEditLockSupport

        // -- java.instrument --
        // Instrumentation
        // UnmodifiableModuleException

        // -- java.logging --
        // FileHandler
        // Logger
        // LogManager
        // LogRecord

        // -- java.management --
        // ThreadInfo
        // ConstructorParameters

        // -- java.net.http --
        // Decoder
        // DecodingCallback
        // Encoder
        // Huffman
        // NaiveHuffman
        // RawChannel

        // -- java.rmi --
        // UnicastRemoteObject
        // RegistryImpl

        // -- java.security --
        // EncryptionKey
        // KerberosCredMessage

        // -- java.sql --
        // Connection
        // ConnectionBuilder
        // DatabaseMetaData
        // DriverManager
        // ShardingKey
        // ShardingKeyBuilder
        // Statement
        // CommonDataSource
        // ConnectionPoolDataSource
        // DataSource
        // PooledConnectionBuilder
        // XAConnectionBuilder
        // XADataSource

        // -- java.xml --
        // XMLConstants
        // AltCatalog
        // BaseEntry
        // Catalog
        // CatalogEntry
        // CatalogException
        // CatalogFeatures
        // CatalogImpl
        // CatalogManager
        // CatalogMessages
        // CatalogReader
        // CatalogResolver
        // CatalogResolverImpl
        // DelegatePublic
        // DelegateSystem
        // DelegateUri
        // GroupEntry
        // NextCatalog
        // Normalizer
        // PublicEntry
        // RewriteSystem
        // RewriteUri
        // SystemEntry
        // SystemSuffix
        // UriEntry
        // UriSuffix
        // Util
        // DatatypeFactory
        // DocumentBuilderFactory
        // SAParserFactory
        // XMLEventFactory
        // XMLInputFactory
        // XMLOutputFactory
        // TransformerFactory
        // SchemaFactory
        // XPath
        // XPathEvaluationResult
        // XPathExpression
        // XPathFactory
        // XPathNodes
        // ElementTraversal
        // DocumentRange
        // Range
        // RangeException
        // DocumentTraversal
        // NodeFilter
        // NodeIterator
        // TreeWalker

        // -- jdk.attach --
        // AttachOperationFailedException

        // -- jdk.compiler --
        // DocComponentTree
        // DocTreeVisitor
        // HiddenTree
        // IndexTree
        // ProvidesTree
        // UsesTree
        // CompilationUnitTree
        // ExportsTree
        // ModuleTree
        // OpensTree
        // PackageTree
        // ProvidesTree
        // RequiresTree
        // Tree
        // UsesTree
        // DocTreeFactory
        // DocTrees
        // SimpleDocTreeVisitor
        // TaskEvent
        // JavacToolProvider

        // -- jdk.jartoool --
        // JarSigner
        // JarSignerException

        // -- jdk.javadoc --
        // Doclet
        // DocletEnvironment
        // Reporter
        // Taglet
        // JavadocToolProvider
        // ReferenceType
        // VirtualMachine

        // -- jdk.jdeps --
        // MultiReleaseException

        // -- jdk.jdi --
        // InvalidModuleException
        // ModuleReference

        // -- jdk.jfr --
        // AnnotationElement
        // BooleanFlag
        // Category
        // Configuration
        // ContentType
        // DataAmount
        // Description
        // Enabled
        // Event
        // EventFactory
        // EventSettings
        // EventType
        // Experimental
        // FlightRecorder
        // FlightRecorderListener
        // FlightRecorderPermission
        // Frequency
        // Label
        // MemoryAddress
        // MetadataDefinition
        // Name
        // Percentage
        // Period
        // Recording
        // RecordingState
        // Registered
        // Relational
        // SettingControl
        // SettingDefinition
        // SettingDescriptor
        // StackTrace
        // Threshold
        // Timespan
        // Timestamp
        // TransitionFrom
        // TransitionTo
        // Unsigned
        // ValueDescriptor
        // RecordedClass
        // RecordedClassLoader
        // RecordedEvent
        // RecordedFrame
        // RecordedMethod
        // RecordedObject
        // RecordedStackTrace
        // RecordedThread
        // RecordedGroup
        // RecordingFile
        // Cutoff

        // -- jdk.jshell --
        // *

        // -- jdk.management.jfr --
        // ConfigurationInfo
        // EventTypeInfo
        // FlightRecorderMXBean
        // RecordingInfo
        // SettingDescriptorInfo

        // -- jdk.management --
        // VMOption

        // -- jdk.security.jgss --
        // InquireType

        // -- jdk.unsupported --
        // Unsafe
    }

    @Override
    public ClassNode otherTransforms(ClassNode clazz) {
        fixPrivateMethodsInInterfaces(clazz);
        if (clazz.name.equals("module-info")) {
            return null;
        }
        return clazz;
    }

    public void fixPrivateMethodsInInterfaces(ClassNode node) {
        if ((node.access & Opcodes.ACC_INTERFACE) == 0) return;

        List<String> privateMethods = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_PRIVATE) != 0) {
                privateMethods.add(method.name + method.desc);
            }
        }

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    if (min.getOpcode() == Opcodes.INVOKEINTERFACE && min.owner.equals(node.name) && privateMethods.contains(min.name + min.desc)) {
                        min.setOpcode(Opcodes.INVOKESPECIAL);
                    }
                } else if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                    if (indy.bsmArgs[1] instanceof Handle) {
                        Handle lambda = (Handle) indy.bsmArgs[1];
                        if (lambda.getOwner().equals(node.name) &&
                                lambda.getTag() == Opcodes.H_INVOKEINTERFACE &&
                                privateMethods.contains(lambda.getName() + lambda.getDesc())
                        ) {
                            indy.bsmArgs[1] = new Handle(Opcodes.H_INVOKESPECIAL, lambda.getOwner(), lambda.getName(), lambda.getDesc(), lambda.isInterface());
                        }
                    }
                }
            }
        }
    }
}
