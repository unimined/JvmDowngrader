package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.j8.stub.*;
import xyz.wagyourtail.jvmdg.j8.stub.function.*;
import xyz.wagyourtail.jvmdg.j8.stub.stream.*;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.*;

public class Java8Downgrader extends AllVersionDowngrader {


    public Java8Downgrader() {
        super(Opcodes.V1_8, Opcodes.V1_7, 0);
    }

    @Override
    public void ensureInit(ClassDowngrader downgrader) {
        if (!isInitialized()) {
            downgrader.logger.warn("Java 8 -> 7 Stubs are VERY incomplete!");
        }
        super.ensureInit(downgrader);
    }

    @Override
    public void init() {
        // -- java.base --
        // GaloisCounterMode
        // GCMParameters
        // GCTR
        // GHASH
        stub(J_I_BufferedReader.class);
        stub(J_I_UncheckedIOException.class);
        // AbstractStringBuffer
        stub(J_L_Boolean.class);
        stub(J_L_Byte.class);
        stub(J_L_Character.class);
        stub(J_L_CharSequence.class);
        stub(J_L_Class.class);
        stub(J_L_Double.class);
        stub(J_L_Float.class);
        stub(J_L_FunctionalInterface.class);
        stub(J_L_Integer.class);
        stub(J_L_InternalError.class);
        stub(J_L_Iterable.class);
        stub(J_L_Long.class);
        stub(J_L_Math.class);
        stub(J_L_Package.class);
        stub(J_L_Process.class);
        stub(J_L_Short.class);
        stub(J_L_StrictMath.class);
        stub(J_L_String.class);
        // StringBuffer
        stub(J_L_ThreadLocal.class);
        stub(J_L_VirtualMachineError.class);
        // ElementType
        // Native
        // Repeatable
        stub(J_L_I_LambdaConversionException.class);
        stub(J_L_I_LambdaMetafactory.class);
        // MethodHandleInfo
        // MethodHandles
        // SerializedLambda
        stub(J_L_R_AccessibleObject.class);
        // AnnotatedArrayType
        stub(J_L_R_AnnotatedElement.class);
        // AnnotatedParameterizedType
        // AnnotatedType
        // AnnotatedTypeVariable
        // AnnotatedWildcardType
        // Constructor — view stubs live in stub/all (interpose at every target)
        // Executable
        stub(J_L_R_Field.class);
        stub(J_L_R_MalformedParametersException.class);
        stub(J_L_R_Method.class);
        stub(J_L_R_Modifier.class);
        // Parameter
        stub(J_L_R_Type.class);
        // TypeVariable
        stub(J_M_BigInteger.class);
        // HttpConnectSocketImpl
        // URLPermission
        stub(J_N_F_Files.class);
        // FileTime
        // AccessController
        // DomainLoadStoreParameter
        // KeyStore
        // PKCS12Attribute
        // Principal
        // Provider
        // SecureRandom
        // Certificate
        // CertPathBuilder
        // CertPathBuilderSpi
        // CertPathChecker
        // CertPathValidator
        // CertPathValidatorSpi
        // PKIXRevocationChecker
        // X509Certificate
        // X509CRL
        // DSAGenParameterSpec
        // Clock
        // DateTimeException
        // DayOfWeek
        // Duration
        // Instant
        // LocalDate
        // LocalDateTime
        // LocalTime
        // Month
        // MonthDay
        // OffsetDateTime
        // OffsetTime
        // Period
        // Ser
        // Year
        // YearMonth
        // ZonedDateTime
        // ZoneId
        // ZoneOffset
        // ZoneRegion
        // AbstractChronology
        // ChronoLocalDate
        // ChronoLocalDateImpl
        // ChronoLocalDateTime
        // ChronoLocalDateTimeImpl
        // Chronology
        // ChronoPeriod
        // ChronoPeriodImpl
        // ChronoZonedDateTime
        // ChronoZonedDateTimeImpl
        // Era
        // HijrahChronology
        // HijrahDate
        // IsoChronology
        // IsoEra
        // JapaneseChronology
        // JapaneseDate
        // JapaneseEra
        // MinguoChronology
        // MinguoDate
        // MinguoEra
        // ThaiBuddhistChronology
        // ThaiBuddhistDate
        // ThaiBuddhistEra
        // DateTimeFormatter
        // DateTimeFormatterBuilder
        // DateTimeParseContext
        // DateTimeParseException
        // DateTimePrintContext
        // DateTimeTextProvider
        // DecimalStyle
        // FormatStyle
        // Parsed
        // ResolverStyle
        // SignStyle
        // TextStyle
        // ChronoField
        // ChronoUnit
        // IsoFields
        // JulianFields
        // Temporal
        // TemporalAccessor
        // TemporalAdjuster
        // TemporalAdjusters
        // TemportalAmount
        // TemporalField
        // TemporalQueries
        // TemporalQuery
        // TemporalUnit
        // UnsupportedTemporalTypeException
        // ValueRange
        // WeekFields
        // Ser
        // TzdbZoneRulesProvider
        // ZoneOffsetTransition
        // ZoneOffsetTransitionRule
        // ZoneRules
        // ZoneRulesException
        // ZoneRulesProvider
        // ArrayDeque
        // ArrayList
        // ArrayPrefixHelpers
        stub(J_U_Arrays.class);
        stub(J_U_Base64.class);
        stub(J_U_BitSet.class);
        // Calendar
        stub(J_U_Collection.class);
        stub(J_U_Collections.class);
        // Date
        stub(J_U_DoubleSummaryStatistics.class);
        // GregorianCalendar
        // HashSet -- handled by Collection.spliterator
        stub(J_U_IntSummaryStatistics.class);
        stub(J_U_Iterator.class);
        // LinkedHashSet
        // LinkedList
        stub(J_U_List.class);
        // Locale
        stub(J_U_LongSummaryStatistics.class);
        stub(J_U_Map.class);
        stub(J_U_Objects.class);
        stub(J_U_Optional.class);
        stub(J_U_OptionalDouble.class);
        stub(J_U_OptionalInt.class);
        stub(J_U_OptionalLong.class);
        stub(J_U_PriorityQueue.class);
        // Random
        // ResourceBundle
        // Set
        // SortedSet
        stub(J_U_Spliterator.class);
        stub(J_U_Spliterators.class);
        // SplittableRandom
        stub(J_U_StringJoiner.class);
        // TimeZone
        // TimSort
        // TreeSet -- handled by Collection.spliterator
        // Tripwire
        // Vector -- handled by Collection.spliterator
        // ArrayBlockingQueue
        stub(J_U_C_CompletableFuture.class);
        stub(J_U_C_CompletionException.class);
        stub(J_U_C_CompletionStage.class);
        // ConcurrentHashMap
        // ConcurrentLinkedDeque TODO: concurrent iterator spliterator
        // ConcurrentLinkedQueue
        stub(J_U_C_ConcurrentMap.class);
        // ConcurrentSkipListMap
        // ConcurrentSkipListSet
        // CopyOnWriteArrayList
        // CountedCompleter
        stub(J_U_C_Executors.class);
        stub(J_U_C_ForkJoinPool.class);
        // ForkJoinTask
        // LinkedBlockingDeque
        // LinkedBlockingQueue
        // LinkedTransferQueue
        // PriorityBlockingQueue
        // SynchronousQueue
        // ThreadLocalRandom
        // AtomicInteger
        // AtomicIntegerArray
        // AtomicIntegerFieldUpdater
        // AtomicLong
        // AtomicLongArray
        // AtomicLongFieldUpdater
        // AtomicReference
        // AtomicReferenceArray
        // AtomicReferenceFieldUpdater
        // DoubleAccumulator
        // DoubleAdder
        // LongAccumulator
        // LongAdder
        // StampedLock
        stub(J_U_F_BiConsumer.class);
        stub(J_U_F_BiFunction.class);
        stub(J_U_F_BinaryOperator.class);
        stub(J_U_F_BiPredicate.class);
        stub(J_U_F_BooleanSupplier.class);
        stub(J_U_F_Consumer.class);
        stub(J_U_F_DoubleBinaryOperator.class);
        stub(J_U_F_DoubleConsumer.class);
        stub(J_U_F_DoubleFunction.class);
        stub(J_U_F_DoublePredicate.class);
        stub(J_U_F_DoubleSupplier.class);
        stub(J_U_F_DoubleToIntFunction.class);
        stub(J_U_F_DoubleToLongFunction.class);
        stub(J_U_F_DoubleUnaryOperator.class);
        stub(J_U_F_Function.class);
        stub(J_U_F_IntBinaryOperator.class);
        stub(J_U_F_IntConsumer.class);
        stub(J_U_F_IntFunction.class);
        stub(J_U_F_IntPredicate.class);
        stub(J_U_F_IntSupplier.class);
        stub(J_U_F_IntToDoubleFunction.class);
        stub(J_U_F_IntToLongFunction.class);
        stub(J_U_F_IntUnaryOperator.class);
        stub(J_U_F_LongBinaryOperator.class);
        stub(J_U_F_LongConsumer.class);
        stub(J_U_F_LongFunction.class);
        stub(J_U_F_LongPredicate.class);
        stub(J_U_F_LongSupplier.class);
        stub(J_U_F_LongToDoubleFunction.class);
        stub(J_U_F_LongToIntFunction.class);
        stub(J_U_F_LongUnaryOperator.class);
        stub(J_U_F_ObjDoubleConsumer.class);
        stub(J_U_F_ObjIntConsumer.class);
        stub(J_U_F_ObjLongConsumer.class);
        stub(J_U_F_Predicate.class);
        stub(J_U_F_Supplier.class);
        stub(J_U_F_ToDoubleBiFunction.class);
        stub(J_U_F_ToDoubleFunction.class);
        stub(J_U_F_ToIntBiFunction.class);
        stub(J_U_F_ToIntFunction.class);
        stub(J_U_F_ToLongBiFunction.class);
        stub(J_U_F_ToLongFunction.class);
        stub(J_U_F_UnaryOperator.class);
        // JarFile
        // Matcher
        // Pattern
        // CalendarDateProvider
        // CalendarNameProvider
        // LocaleServiceProvider
        // ResourceBundleControlProvider
        // TimeZoneNameProvider
        stub(J_U_S_BaseStream.class);
        stub(J_U_S_Collector.class);
        stub(J_U_S_Collectors.class);
        stub(J_U_S_DoubleStream.class);
        stub(J_U_S_IntStream.class);
        stub(J_U_S_LongStream.class);
        stub(J_U_S_Stream.class);
    }

}
