package xyz.wagyourtail.jvmdg.internal.mods.stub

import org.gradle.api.JavaVersion
import xyz.wagyourtail.jvmdg.internal.mods.ClassReplacer
import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods.stub._9.*

object Java9Stubs {
    fun apply() {
        MethodReplacer.apply {
            // -- java.base --
            addClass(J_I_InputStream::class.java)
            // ObjectInputFilter
            // ObjectInputStream
            // ObjectStreamConstants
            // AbstractStringBuilder -- handled by CharSequence
            addClass(J_L_Byte::class.java)
            // Character -- LTR/RTL and unicode spaces
            addClass(J_L_Class::class.java)
            addClass(J_L_ClassLoader::class.java)
            // Deprecated
            // IllegalCallerException
            addClass(J_L_IndexOutOfBoundsException::class.java)
            // LayeredInstantiationException
            addClass(J_L_Math::class.java)
            addClass(J_L_Module::class.java)
            // ModuleLayer
            // Process
            // ProcessBuilder
            // ProcessHandle
            // ProcessHandleImpl
            addClass(J_L_Short::class.java)
            // StackTraceElement
            // StackWalker
            addClass(J_L_StrictMath::class.java)
            // String -- handled by CharSequence
            addClass(J_L_System::class.java)
            ClassReplacer.registerReplace(JavaVersion.VERSION_1_9, "Ljava/lang/System\$Logger;", "Ljava/util/logging/Logger;")
            ClassReplacer.registerReplace(JavaVersion.VERSION_1_9, "Ljava/lang/System\$Logger\$Level;", "Ljava/util/logging/Level;")
            // System$LoggerFinder
            addClass(J_L_Thread::class.java)
            // ElementType
            // MethodHandle
            // MethodHandles
            // StringConcatException
            // StringConcatFactory
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
            // Cleaner
            // Reference
            addClass(J_L_R_AccessibleObject::class.java)
            // AnnotatedArrayType
            // AnnotatedParameterizedType
            // AnnotatedType
            // AnnotatedTypeVariable
            // AnnotatedWildcardType
            // InaccessibleObjectException
            addClass(J_M_BigInteger::class.java)
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
            addClass(J_N_Buffer::class.java)
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
            addClass(J_T_Clock::class.java)
            addClass(J_T_Duration::class.java)
            addClass(J_T_LocalDate::class.java)
            addClass(J_T_LocalTime::class.java)
            addClass(J_T_OffsetTime::class.java)
            addClass(J_T_C_Chronology::class.java)
            addClass(J_T_C_IsoChronology::class.java)
            // DateTimeFormatterBuilder
            addClass(J_U_Arrays::class.java)
            addClass(J_U_Currency::class.java)
            addClass(J_U_Enumeration::class.java)
            // ImmutableCollections
            // KeyValueHolder
            addClass(J_U_List::class.java)
            // Locale
            addClass(J_U_Map::class.java)
            addClass(J_U_Objects::class.java)
            addClass(J_U_Optional::class.java)
            addClass(J_U_OptionalDouble::class.java)
            addClass(J_U_OptionalInt::class.java)
            addClass(J_U_OptionalLong::class.java)
            addClass(J_U_ResourceBundle::class.java)
            addClass(J_U_Scanner::class.java)
            addClass(J_U_ServiceLoader::class.java)
            addClass(J_U_ServiceLoader.Provider::class.java)
            addClass(J_U_Set::class.java)
            addClass(J_U_C_CompletableFuture::class.java)
            // Flow
            // ForkJoinPool
            // ForkJoinTask
            // SubmissionPublisher
            addClass(J_U_C_TimeUnit::class.java)
            // AtomicBoolean
            // AtomicInteger
            // AtomicIntegerArray
            // AtomicLong
            // AtomicLongArray
            // AtomicReference
            // AtomicReferenceArray
            // JarFile
            addClass(J_U_R_Matcher::class.java)
            // AbstractResourceBundleProvider
            // ResourceBundleProvider
            // ToolProvider
            addClass(J_U_S_Collectors::class.java)
            addClass(J_U_S_DoubleStream::class.java)
            addClass(J_U_S_IntStream::class.java)
            addClass(J_U_S_LongStream::class.java)
            addClass(J_U_S_Stream::class.java)
            // WhileOps
            addClass(J_U_Z_Checksum::class.java)
            // CRC32C
            // ZipEntry
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
    }
}