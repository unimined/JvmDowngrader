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



            addClass(J_L_CharSequence::class.java)
        }
    }
}