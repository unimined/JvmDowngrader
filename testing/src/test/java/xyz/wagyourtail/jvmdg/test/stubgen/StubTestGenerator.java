package xyz.wagyourtail.jvmdg.test.stubgen;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

/**
 * Generates the stub-parity corpus: for every stub member registered by every
 * {@link VersionProvider} it can synthesize arguments for, emits real Java call
 * sites into per-owner classes under {@code jvmdg.stubtests.*} plus an
 * {@code Index} dispatcher. Run via the {@code generateStubTests} Gradle task;
 * output is compiled by {@code compileStubTests} into {@code stubtests.jar} for
 * {@code StubParityTests}.
 *
 * <p>Members are probed with a three-step value ladder (trivial, meaningful,
 * boundary) — up to three call sites per member, deduplicated on the emitted
 * expression. Site ids are {@code m####} with a {@code b}/{@code c} suffix on
 * the second/third variant, so bare ids stay stable across regenerations.
 * Hand-written tests live in {@code jvmdg.stubtests.manual.*}, annotated
 * {@code jvmdg.stubtests.StubTest}; they are compiled by
 * {@code compileStubTestsManual}, reflected here, and invoked from {@code Index}
 * with {@code x####} ids.</p>
 *
 * <p>Each call site prints one line: {@code <id> RESULT:<norm>} or
 * {@code <id> EXC:<exception-binary-name>} via {@code jvmdg.stubtests.Support}.
 * Call sites must be real bytecode invocations — the downgrader rewrites
 * INVOKE* instructions, so reflection is not usable.</p>
 *
 * <p>Must run on a JVM whose class files ASM can parse and whose JDK still
 * contains every emitted member, since generated sources compile at that
 * release.</p>
 */
public class StubTestGenerator {

    private static final int CHUNK = 150;

    // whole-owner denies: process/JVM lifecycle, entropy sources
    private static final Set<String> DENIED_OWNERS = new HashSet<>();
    static {
        DENIED_OWNERS.add("java/lang/Runtime");
        DENIED_OWNERS.add("java/lang/Process");
        DENIED_OWNERS.add("java/lang/ProcessBuilder");
        DENIED_OWNERS.add("java/lang/Shutdown");
        DENIED_OWNERS.add("java/lang/SecurityManager");
        DENIED_OWNERS.add("java/util/Random");
        DENIED_OWNERS.add("java/security/SecureRandom");
        DENIED_OWNERS.add("java/util/concurrent/ThreadLocalRandom");
        DENIED_OWNERS.add("java/util/Timer"); // ctor spawns a non-daemon thread
        // every member reports live process state
        DENIED_OWNERS.add("java/lang/ProcessHandle");
    }

    // name denies regardless of owner: blocking, lifecycle, nondeterministic
    private static final Set<String> DENIED_NAMES = new HashSet<>();
    static {
        DENIED_NAMES.add("exit");
        DENIED_NAMES.add("halt");
        DENIED_NAMES.add("exec");
        DENIED_NAMES.add("stop");
        DENIED_NAMES.add("suspend");
        DENIED_NAMES.add("resume");
        DENIED_NAMES.add("destroy");
        DENIED_NAMES.add("destroyForcibly");
        DENIED_NAMES.add("shutdown");
        DENIED_NAMES.add("shutdownNow");
        DENIED_NAMES.add("load");
        DENIED_NAMES.add("loadLibrary");
        DENIED_NAMES.add("runFinalization");
        DENIED_NAMES.add("gc");
        DENIED_NAMES.add("join");
        // quietlyJoin on an unforked ForkJoinTask parks forever (unlike get(),
        // it never submits the task to the common pool)
        DENIED_NAMES.add("quietlyJoin");
        DENIED_NAMES.add("park");
        DENIED_NAMES.add("parkNanos");
        DENIED_NAMES.add("parkUntil");
        DENIED_NAMES.add("await");
        DENIED_NAMES.add("take");
        DENIED_NAMES.add("sleep");
        // nondeterministic results
        DENIED_NAMES.add("now");
        DENIED_NAMES.add("nanoTime");
        DENIED_NAMES.add("currentTimeMillis");
        DENIED_NAMES.add("identityHashCode");
        DENIED_NAMES.add("random");
        DENIED_NAMES.add("randomUUID");
        DENIED_NAMES.add("nextInt");
        DENIED_NAMES.add("nextLong");
        DENIED_NAMES.add("nextFloat");
        DENIED_NAMES.add("nextDouble");
        DENIED_NAMES.add("nextBoolean");
        DENIED_NAMES.add("nextBytes");
        DENIED_NAMES.add("nextGaussian");
        DENIED_NAMES.add("nextExponential");
        // process handle ids are per-process
        DENIED_NAMES.add("pid");
    }

    // name denies scoped to an owner prefix: blocking on synthesized instances
    private static final Map<String, Set<String>> DENIED_BY_OWNER = new HashMap<>();
    static {
        denyOn("java/util/concurrent/CompletableFuture", "get", "join", "obtrudeValue", "obtrudeException");
        denyOn("java/util/concurrent/FutureTask", "get", "join", "run");
        // *Async returns a future that completes off-thread — its printed state
        // races with the executor
        denyOn("java/util/concurrent/CompletableFuture", "supplyAsync", "runAsync",
            "thenApplyAsync", "thenAcceptAsync", "thenRunAsync", "thenCombineAsync",
            "thenAcceptBothAsync", "applyToEitherAsync", "acceptEitherAsync",
            "runAfterBothAsync", "runAfterEitherAsync", "handleAsync",
            "whenCompleteAsync", "thenComposeAsync");
        denyOn("java/util/concurrent/CompletionStage", "thenApplyAsync", "thenAcceptAsync",
            "thenRunAsync", "thenCombineAsync", "thenAcceptBothAsync", "applyToEitherAsync",
            "acceptEitherAsync", "runAfterBothAsync", "runAfterEitherAsync", "handleAsync",
            "whenCompleteAsync", "thenComposeAsync", "exceptionallyAsync",
            "exceptionallyComposeAsync");
        // submit/fork return tasks whose toString includes completion state
        denyOn("java/util/concurrent/ForkJoinPool", "submit", "fork");
        // quietlyJoinUninterruptibly parks until the unforked task completes or
        // the timeout elapses — a DAYS unit would park the corpus for a day
        denyOn("java/util/concurrent/ForkJoinTask", "fork", "quietlyJoinUninterruptibly");
        // consume returns a future that races the internal task
        denyOn("java/util/concurrent/SubmissionPublisher", "consume");
        denyOn("java/util/concurrent/Exchanger", "exchange");
        denyOn("java/util/concurrent/SynchronousQueue", "put", "take", "offer", "poll", "remove", "add");
        denyOn("java/util/concurrent/LinkedTransferQueue", "transfer", "put");
        denyOn("java/util/concurrent/Phaser", "awaitAdvance", "arriveAndAwaitAdvance", "awaitAdvanceInterruptibly");
        denyOn("java/util/concurrent/Executors", "newFixedThreadPool", "newCachedThreadPool",
            "newSingleThreadExecutor", "newSingleThreadScheduledExecutor", "newScheduledThreadPool");
        denyOn("java/nio/channels/Selector", "select");
        // real network round-trips
        denyOn("java/net/http/HttpClient", "send", "sendAsync");
        denyOn("java/net/http/WebSocket$Builder", "buildAsync");
        denyOn("java/io/PipedInputStream", "read", "receive");
        denyOn("java/io/PipedReader", "read");
        denyOn("java/io/PipedOutputStream", "connect");
        denyOn("java/io/PipedWriter", "connect");
        // invokedynamic bootstrap factories: their @Modify handlers rewrite the
        // indy instruction, so a direct INVOKESTATIC call site is not valid
        // coverage (juggling expects InvokeDynamicInsnNode context)
        denyOn("java/lang/invoke/StringConcatFactory");
        denyOn("java/lang/invoke/LambdaMetafactory");
        denyOn("java/lang/invoke/ConstantBootstraps");
        denyOn("java/lang/runtime/SwitchBootstraps");
        denyOn("java/lang/runtime/ObjectMethods");
        // MXBean values are per-JVM/per-moment (pid, uptime, memory)
        denyOn("java/lang/management/RuntimeMXBean");
        // every Files member takes a Path — all probes hit the real filesystem
        // (missing-path exception types and directory listings differ across
        // JDK providers, not the stubs)
        denyOn("java/nio/file/Files");
        denyOn("java/nio/file/FileSystems", "newFileSystem");
        denyOn("java/nio/file/spi/FileSystemProvider", "exists", "readAttributesIfExists");
    }

    private static void denyOn(String ownerPrefix, String... names) {
        DENIED_BY_OWNER.computeIfAbsent(ownerPrefix, k -> new HashSet<>()).addAll(java.util.Arrays.asList(names));
    }

    // exact-member denies: nondeterministic or thread-id dependent
    private static final Set<String> DENIED_MEMBERS = new HashSet<>();
    static {
        DENIED_MEMBERS.add("java/lang/Object hashCode ()I");
        DENIED_MEMBERS.add("java/lang/Thread currentThread ()Ljava/lang/Thread;");
        DENIED_MEMBERS.add("java/lang/Thread getId ()J");
        DENIED_MEMBERS.add("java/lang/Thread getName ()Ljava/lang/String;");
        // monotonic ids / environment contents are stable in shape only
        DENIED_MEMBERS.add("java/lang/Thread threadId ()J");
        DENIED_MEMBERS.add("java/util/logging/LogRecord getLongThreadID ()J");
        DENIED_MEMBERS.add("java/lang/System getProperties ()Ljava/util/Properties;");
        // getMethods order is spec-unspecified — differs across runtimes
        DENIED_MEMBERS.add("java/lang/Class getMethods ()[Ljava/lang/reflect/Method;");
        DENIED_MEMBERS.add("java/lang/System getenv ()Ljava/util/Map;");
        DENIED_MEMBERS.add("java/lang/System getenv (Ljava/lang/String;)Ljava/lang/String;");
        // async/scheduled completion state races with the executor/delayer —
        // even a zero-delay timeout may not have fired when the result prints
        DENIED_MEMBERS.add("java/util/concurrent/CompletableFuture completeOnTimeout (Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)Ljava/util/concurrent/CompletableFuture;");
        DENIED_MEMBERS.add("java/util/concurrent/CompletableFuture orTimeout (JLjava/util/concurrent/TimeUnit;)Ljava/util/concurrent/CompletableFuture;");
        DENIED_MEMBERS.add("java/util/concurrent/CompletableFuture completeAsync (Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;");
        DENIED_MEMBERS.add("java/util/concurrent/CompletableFuture completeAsync (Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;");
        DENIED_MEMBERS.add("java/util/concurrent/CompletionStage exceptionallyAsync (Ljava/util/function/Function;)Ljava/util/concurrent/CompletionStage;");
        DENIED_MEMBERS.add("java/util/concurrent/CompletionStage exceptionallyAsync (Ljava/util/function/Function;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletionStage;");
        DENIED_MEMBERS.add("java/util/concurrent/CompletionStage exceptionallyComposeAsync (Ljava/util/function/Function;)Ljava/util/concurrent/CompletionStage;");
        DENIED_MEMBERS.add("java/util/concurrent/CompletionStage exceptionallyComposeAsync (Ljava/util/function/Function;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletionStage;");
        // receivers synthesized with wall-clock state make these per-run values
        DENIED_MEMBERS.add("java/util/logging/LogRecord getInstant ()Ljava/time/Instant;");
        DENIED_MEMBERS.add("java/util/Date toInstant ()Ljava/time/Instant;");
        DENIED_MEMBERS.add("java/util/Calendar toInstant ()Ljava/time/Instant;");
        DENIED_MEMBERS.add("java/util/GregorianCalendar toZonedDateTime ()Ljava/time/ZonedDateTime;");
        // probes that open/create real files are environment-dependent:
        // FileWriter("a") actually writes, and reads depend on cwd contents
        DENIED_MEMBERS.add("java/io/FileReader <init> (Ljava/io/File;Ljava/nio/charset/Charset;)V");
        DENIED_MEMBERS.add("java/io/FileReader <init> (Ljava/lang/String;Ljava/nio/charset/Charset;)V");
        DENIED_MEMBERS.add("java/io/FileWriter <init> (Ljava/io/File;Ljava/nio/charset/Charset;)V");
        DENIED_MEMBERS.add("java/io/FileWriter <init> (Ljava/io/File;Ljava/nio/charset/Charset;Z)V");
        DENIED_MEMBERS.add("java/io/FileWriter <init> (Ljava/lang/String;Ljava/nio/charset/Charset;)V");
        DENIED_MEMBERS.add("java/io/FileWriter <init> (Ljava/lang/String;Ljava/nio/charset/Charset;Z)V");
        DENIED_MEMBERS.add("java/io/PrintStream <init> (Ljava/io/File;Ljava/nio/charset/Charset;)V");
        DENIED_MEMBERS.add("java/io/PrintStream <init> (Ljava/lang/String;Ljava/nio/charset/Charset;)V");
        DENIED_MEMBERS.add("java/io/PrintWriter <init> (Ljava/io/File;Ljava/nio/charset/Charset;)V");
        DENIED_MEMBERS.add("java/io/PrintWriter <init> (Ljava/lang/String;Ljava/nio/charset/Charset;)V");
        DENIED_MEMBERS.add("java/util/Formatter <init> (Ljava/io/File;Ljava/nio/charset/Charset;Ljava/util/Locale;)V");
        // String-arg Formatter writes a file literally named by the arg
        DENIED_MEMBERS.add("java/util/Formatter <init> (Ljava/lang/String;Ljava/nio/charset/Charset;Ljava/util/Locale;)V");
        DENIED_MEMBERS.add("java/util/Scanner <init> (Ljava/io/File;Ljava/nio/charset/Charset;)V");
    }

    // concrete defaults for interface/abstract params and receivers. Each entry
    // is a profile ladder {trivial, meaningful, boundary}: later profiles probe
    // populated/boundary states; missing steps reuse the last value so
    // single-entry aliases collapse to one site after dedup. Lambdas sit behind
    // raw-type casts at the call site, so generic instantiations don't matter.
    private static final Map<String, String[]> ALIASES = new HashMap<>();
    static {
        al("java.lang.Object", "new java.lang.Object()", "\"a\"", "Integer.valueOf(1)");
        al("java.lang.CharSequence", "\"\"", "\"a\"", "\"\\u0000\"");
        al("java.lang.Appendable", "new java.lang.StringBuilder()",
            "new java.lang.StringBuilder(\"a\")", "new java.lang.StringBuilder(\"\\u0000\")");
        al("java.lang.Comparable", "\"\"", "\"a\"", "\"\\u0000\"");
        al("java.lang.Iterable", "new java.util.ArrayList()",
            "new java.util.ArrayList(java.util.Arrays.asList(\"a\", \"b\"))",
            "java.util.Collections.emptyList()");
        al("java.lang.Cloneable", "new int[0]", "new int[]{1}");
        al("java.lang.Runnable", "() -> {}", "() -> {}",
            "() -> { throw new java.lang.RuntimeException(); }");
        al("java.lang.ClassLoader", "java.lang.ClassLoader.getSystemClassLoader()",
            "java.lang.String.class.getClassLoader()");
        al("java.lang.Class", "java.lang.String.class", "java.lang.Integer.class",
            "java.lang.Void.TYPE");
        al("java.lang.Module", "java.lang.String.class.getModule()");
        al("java.lang.ModuleLayer", "java.lang.ModuleLayer.boot()");
        al("java.lang.ref.Reference", "new java.lang.ref.WeakReference<Object>(\"a\")");
        // stable members, not getMethods()[0]: reflection order and the String
        // API surface differ across JDKs, which would print different members
        al("java.lang.reflect.Method", "java.lang.String.class.getMethod(\"length\")");
        al("java.lang.reflect.Constructor", "java.lang.String.class.getConstructor()");
        al("java.lang.reflect.Field", "java.lang.String.class.getField(\"CASE_INSENSITIVE_ORDER\")");
        al("java.lang.reflect.Parameter",
            "java.lang.String.class.getMethod(\"charAt\", int.class).getParameters()[0]");
        al("java.lang.reflect.Executable", "java.lang.String.class.getMethod(\"length\")");
        al("java.lang.reflect.AccessibleObject", "java.lang.String.class.getMethod(\"length\")");
        al("java.lang.reflect.Member", "java.lang.String.class.getMethod(\"length\")");
        al("java.lang.reflect.InvocationHandler", "(p, m, a) -> null");
        al("java.lang.reflect.TypeVariable", "java.util.List.class.getTypeParameters()[0]");
        al("java.lang.reflect.AnnotatedType", "java.lang.String.class.getAnnotatedSuperclass()");
        al("java.lang.reflect.GenericDeclaration", "java.lang.String.class");
        al("java.lang.reflect.ParameterizedType",
            "java.util.AbstractList.class.getGenericInterfaces()[0]");
        al("java.lang.reflect.GenericArrayType",
            "java.util.ArrayList.class.getMethod(\"toArray\", java.util.function.IntFunction.class)"
                + ".getGenericReturnType()");
        al("java.lang.reflect.Type", "java.lang.String.class", "java.lang.Integer.class", "int.class");
        al("java.lang.invoke.MethodHandle",
            "java.lang.invoke.MethodHandles.constant(java.lang.String.class, \"a\")");
        al("java.lang.invoke.VarHandle",
            "java.lang.invoke.MethodHandles.arrayElementVarHandle(int[].class)");
        al("java.util.Collection", "new java.util.ArrayList()",
            "new java.util.ArrayList(java.util.Arrays.asList(\"a\", \"b\"))",
            "java.util.Collections.emptyList()");
        al("java.util.List", "new java.util.ArrayList()",
            "new java.util.ArrayList(java.util.Arrays.asList(\"a\", \"b\"))",
            "java.util.Collections.emptyList()");
        al("java.util.AbstractList", "new java.util.ArrayList()",
            "new java.util.ArrayList(java.util.Arrays.asList(\"a\", \"b\"))",
            "java.util.Collections.emptyList()");
        al("java.util.AbstractCollection", "new java.util.ArrayList()",
            "new java.util.ArrayList(java.util.Arrays.asList(\"a\", \"b\"))",
            "java.util.Collections.emptyList()");
        al("java.util.SequencedCollection", "new java.util.LinkedList()",
            "new java.util.LinkedList(java.util.Arrays.asList(\"a\", \"b\"))",
            "java.util.Collections.emptyList()");
        al("java.util.Set", "new java.util.HashSet()",
            "new java.util.HashSet(java.util.Arrays.asList(\"a\", \"b\"))",
            "java.util.Collections.emptySet()");
        al("java.util.AbstractSet", "new java.util.HashSet()",
            "new java.util.HashSet(java.util.Arrays.asList(\"a\", \"b\"))",
            "java.util.Collections.emptySet()");
        al("java.util.SequencedSet", "new java.util.LinkedHashSet()",
            "new java.util.LinkedHashSet(java.util.Arrays.asList(\"a\", \"b\"))",
            "java.util.Collections.emptySet()");
        al("java.util.SortedSet", "new java.util.TreeSet()",
            "new java.util.TreeSet(java.util.Arrays.asList(\"a\", \"b\"))",
            "java.util.Collections.emptySortedSet()");
        al("java.util.NavigableSet", "new java.util.TreeSet()",
            "new java.util.TreeSet(java.util.Arrays.asList(\"a\", \"b\"))",
            "java.util.Collections.emptyNavigableSet()");
        al("java.util.Queue", "new java.util.LinkedList()",
            "new java.util.LinkedList(java.util.Arrays.asList(\"a\", \"b\"))",
            "new java.util.ArrayDeque()");
        al("java.util.Deque", "new java.util.LinkedList()",
            "new java.util.LinkedList(java.util.Arrays.asList(\"a\", \"b\"))",
            "new java.util.ArrayDeque()");
        al("java.util.AbstractQueue", "new java.util.LinkedList()",
            "new java.util.LinkedList(java.util.Arrays.asList(\"a\", \"b\"))",
            "new java.util.ArrayDeque()");
        al("java.util.Map", "new java.util.HashMap()",
            "new java.util.HashMap(java.util.Collections.singletonMap(\"a\", \"b\"))",
            "java.util.Collections.emptyMap()");
        al("java.util.AbstractMap", "new java.util.HashMap()",
            "new java.util.HashMap(java.util.Collections.singletonMap(\"a\", \"b\"))",
            "java.util.Collections.emptyMap()");
        al("java.util.SequencedMap", "new java.util.LinkedHashMap()",
            "new java.util.LinkedHashMap(java.util.Collections.singletonMap(\"a\", \"b\"))",
            "java.util.Collections.emptySortedMap()");
        al("java.util.SortedMap", "new java.util.TreeMap()",
            "new java.util.TreeMap(java.util.Collections.singletonMap(\"a\", \"b\"))",
            "java.util.Collections.emptySortedMap()");
        al("java.util.NavigableMap", "new java.util.TreeMap()",
            "new java.util.TreeMap(java.util.Collections.singletonMap(\"a\", \"b\"))",
            "java.util.Collections.emptyNavigableMap()");
        al("java.util.Iterator", "java.util.Collections.emptyIterator()",
            "java.util.Arrays.asList(\"a\", \"b\").iterator()");
        al("java.util.ListIterator", "java.util.Collections.emptyListIterator()",
            "java.util.Arrays.asList(\"a\", \"b\").listIterator()");
        al("java.util.Enumeration", "java.util.Collections.emptyEnumeration()",
            "java.util.Collections.enumeration(java.util.Arrays.asList(\"a\", \"b\"))");
        al("java.util.Spliterator", "java.util.Spliterators.emptySpliterator()",
            "java.util.Arrays.asList(\"a\", \"b\").spliterator()");
        al("java.util.concurrent.Callable", "() -> null", "() -> \"a\"",
            "() -> { throw new java.io.IOException(); }");
        al("java.util.BitSet", "new java.util.BitSet()",
            "java.util.BitSet.valueOf(new long[]{0b1010L})");
        al("java.util.concurrent.ExecutorService",
            "java.util.concurrent.Executors.newSingleThreadExecutor()");
        al("java.util.concurrent.ForkJoinWorkerThread",
            "java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(new java.util.concurrent.ForkJoinPool())");
        al("java.util.stream.Stream$Builder", "java.util.stream.Stream.builder()");
        al("java.util.Comparator", "(a, b) -> 0", "java.util.Comparator.naturalOrder()",
            "java.util.Comparator.reverseOrder()");
        al("java.util.stream.Stream", "java.util.stream.Stream.empty()",
            "java.util.stream.Stream.of(\"a\", \"b\")");
        al("java.util.stream.IntStream", "java.util.stream.IntStream.empty()",
            "java.util.stream.IntStream.of(1, 2)");
        al("java.util.stream.LongStream", "java.util.stream.LongStream.empty()",
            "java.util.stream.LongStream.of(1L, 2L)");
        al("java.util.stream.DoubleStream", "java.util.stream.DoubleStream.empty()",
            "java.util.stream.DoubleStream.of(0.5d, 1.5d)");
        al("java.util.function.Function", "x -> x", "x -> x",
            "x -> { throw new java.lang.IllegalStateException(); }");
        al("java.util.function.Supplier", "() -> null", "() -> \"a\"");
        al("java.util.function.Consumer", "x -> {}");
        al("java.util.function.Predicate", "x -> true", "x -> false");
        al("java.util.function.UnaryOperator", "x -> x");
        al("java.util.function.BiFunction", "(a, b) -> null", "(a, b) -> a");
        al("java.util.function.BiConsumer", "(a, b) -> {}");
        al("java.util.function.BiPredicate", "(a, b) -> true", "(a, b) -> false");
        al("java.util.function.BinaryOperator", "(a, b) -> a", "(a, b) -> b");
        al("java.util.function.LongBinaryOperator", "(a, b) -> a", "(a, b) -> a + b");
        al("java.util.function.DoubleBinaryOperator", "(a, b) -> a", "(a, b) -> a + b");
        al("java.util.function.IntToLongFunction", "x -> (long) x", "x -> (long) -x");
        al("java.util.function.IntToDoubleFunction", "x -> (double) x", "x -> (double) -x");
        al("java.util.stream.IntStream$IntMapMultiConsumer", "(v, c) -> c.accept(v)");
        al("java.util.stream.LongStream$LongMapMultiConsumer", "(v, c) -> c.accept(v)");
        al("java.util.stream.DoubleStream$DoubleMapMultiConsumer", "(v, c) -> c.accept(v)");
        al("java.time.temporal.TemporalAmount", "java.time.Duration.ofSeconds(0L)",
            "java.time.Duration.ofSeconds(90L)", "java.time.Period.ofDays(1)");
        // java.time.Duration.MAX/MIN only exist on 18+, and field-constant
        // discovery would pick them for the non-trivial profiles
        al("java.time.Duration", "java.time.Duration.ofSeconds(0L)",
            "java.time.Duration.ofSeconds(90L)", "java.time.Duration.ofMillis(-1L)");
        al("java.time.temporal.Temporal", "java.time.Instant.EPOCH",
            "java.time.Instant.ofEpochSecond(1L)", "java.time.LocalDate.of(2000, 1, 2)");
        al("java.time.temporal.TemporalUnit", "java.time.temporal.ChronoUnit.NANOS",
            "java.time.temporal.ChronoUnit.DAYS", "java.time.temporal.ChronoUnit.FOREVER");
        al("java.time.chrono.Era", "java.time.chrono.IsoEra.CE", "java.time.chrono.IsoEra.BCE");
        al("java.lang.reflect.AnnotatedElement", "java.lang.String.class", "java.lang.Integer.class");
        al("java.lang.invoke.MethodHandles$Lookup", "java.lang.invoke.MethodHandles.lookup()");
        al("java.lang.invoke.TypeDescriptor", "java.lang.String.class", "java.lang.Integer.class",
            "int.class");
        al("java.lang.invoke.TypeDescriptor$OfField", "java.lang.String.class",
            "java.lang.Integer.class", "int.class");
        al("java.lang.invoke.TypeDescriptor$OfMethod",
            "java.lang.invoke.MethodType.methodType(void.class)",
            "java.lang.invoke.MethodType.methodType(java.lang.String.class)",
            "java.lang.invoke.MethodType.methodType(void.class, java.lang.String.class)");
        al("java.lang.invoke.CallSite",
            "new java.lang.invoke.ConstantCallSite("
                + "java.lang.invoke.MethodHandles.constant(java.lang.String.class, \"a\"))");
        al("java.lang.Enum", "java.lang.Thread.State.NEW", "java.lang.Thread.State.RUNNABLE",
            "java.lang.Thread.State.BLOCKED");
        al("java.lang.Package", "java.lang.String.class.getPackage()");
        al("java.lang.Number", "Integer.valueOf(1)", "Long.valueOf(1L)", "Double.valueOf(0.5d)");
        al("java.lang.constant.Constable", "\"a\"");
        al("java.lang.constant.ConstantDesc", "\"a\"");
        al("java.lang.constant.ClassDesc", "java.lang.String.class.describeConstable().get()",
            "java.lang.Integer.class.describeConstable().get()",
            "java.lang.Integer.TYPE.describeConstable().get()");
        al("java.lang.constant.PrimitiveClassDesc", "java.lang.Integer.TYPE.describeConstable().get()");
        al("java.lang.constant.MethodTypeDesc",
            "java.lang.constant.MethodTypeDesc.ofDescriptor(\"()V\")",
            "java.lang.constant.MethodTypeDesc.ofDescriptor(\"()I\")",
            "java.lang.constant.MethodTypeDesc.ofDescriptor(\"(I)V\")");
        al("java.lang.Runtime$Version", "java.lang.Runtime.Version.parse(\"1\")",
            "java.lang.Runtime.Version.parse(\"1.2\")", "java.lang.Runtime.Version.parse(\"1.2.3\")");
        al("java.lang.ModuleLayer$Controller",
            "java.lang.ModuleLayer.empty().defineModules(java.lang.module.Configuration.empty(),"
                + " java.util.Collections.emptyList(), (s) -> null)");
        al("java.lang.module.ModuleDescriptor",
            "java.lang.module.ModuleDescriptor.newModule(\"m\").build()");
        al("java.lang.module.ModuleDescriptor$Builder",
            "java.lang.module.ModuleDescriptor.newModule(\"m\")");
        al("java.lang.module.ModuleDescriptor$Exports",
            "java.lang.module.ModuleDescriptor.newModule(\"m\").exports(\"p\")"
                + ".build().exports().iterator().next()");
        al("java.lang.module.ModuleDescriptor$Opens",
            "java.lang.module.ModuleDescriptor.newModule(\"m\").opens(\"p\")"
                + ".build().opens().iterator().next()");
        al("java.lang.module.ModuleDescriptor$Provides",
            "java.lang.module.ModuleDescriptor.newModule(\"m\")"
                + ".provides(\"a\", java.util.Collections.singletonList(\"b\"))"
                + ".build().provides().iterator().next()");
        // requires() also contains the implicit java.base edge — iterate the
        // plain-module build so the element is deterministic
        al("java.lang.module.ModuleDescriptor$Requires",
            "java.lang.module.ModuleDescriptor.newModule(\"m\").build().requires().iterator().next()");
        al("java.lang.module.ModuleDescriptor$Version",
            "java.lang.module.ModuleDescriptor.newModule(\"m\").version(\"1\")"
                + ".build().version().get()");
        al("java.lang.module.Configuration", "java.lang.ModuleLayer.boot().configuration()");
        al("java.lang.module.ModuleFinder",
            "java.lang.module.ModuleFinder.of(new java.nio.file.Path[0])");
        al("java.lang.System$Logger", "java.lang.System.getLogger(\"a\")");
        al("java.lang.StackWalker$StackFrame",
            "java.lang.StackWalker.getInstance().walk(s -> s.findFirst().get())");
        al("java.lang.Readable", "new java.io.StringReader(\"\")", "new java.io.StringReader(\"a\")");
        al("java.lang.AutoCloseable", "new java.io.StringReader(\"\")");
        al("java.lang.Thread$Builder", "java.lang.Thread.ofVirtual()",
            "java.lang.Thread.ofPlatform()");
        al("java.lang.Thread$Builder$OfVirtual", "java.lang.Thread.ofVirtual()");
        al("java.lang.Thread$Builder$OfPlatform", "java.lang.Thread.ofPlatform()");
        al("java.lang.Thread$UncaughtExceptionHandler", "(t, e) -> {}");
        al("java.lang.ref.Cleaner$Cleanable",
            "java.lang.ref.Cleaner.create().register(new java.lang.Object(), () -> {})");
        al("java.lang.ScopedValue$Carrier",
            "java.lang.ScopedValue.where(java.lang.ScopedValue.newInstance(), Integer.valueOf(1))");
        al("java.io.Reader", "new java.io.StringReader(\"\")", "new java.io.StringReader(\"a\")",
            "new java.io.StringReader(\"\\u0000\")");
        al("java.io.Writer", "new java.io.StringWriter()");
        al("java.io.InputStream", "new java.io.ByteArrayInputStream(new byte[0])",
            "new java.io.ByteArrayInputStream(new byte[]{1})",
            "new java.io.ByteArrayInputStream(new byte[]{-1})");
        al("java.io.OutputStream", "new java.io.ByteArrayOutputStream()");
        al("java.io.Console", "java.lang.System.console()");
        al("java.nio.Buffer", "java.nio.ByteBuffer.allocate(0)", "java.nio.ByteBuffer.allocate(1)");
        al("java.nio.channels.Selector", "java.nio.channels.Selector.open()");
        al("java.nio.channels.ReadableByteChannel",
            "java.nio.channels.Channels.newChannel(new java.io.ByteArrayInputStream(new byte[0]))");
        al("java.nio.channels.WritableByteChannel",
            "java.nio.channels.Channels.newChannel(new java.io.ByteArrayOutputStream())");
        al("java.nio.charset.Charset", "java.nio.charset.StandardCharsets.UTF_8",
            "java.nio.charset.StandardCharsets.ISO_8859_1", "java.nio.charset.StandardCharsets.UTF_16");
        al("java.nio.file.Path", "java.nio.file.Paths.get(\"\")",
            "java.nio.file.Paths.get(\"x\")", "java.nio.file.Paths.get(\"a\", \"b\")");
        al("java.nio.file.spi.FileSystemProvider",
            "java.nio.file.FileSystems.getDefault().provider()");
        al("java.util.regex.Matcher", "java.util.regex.Pattern.compile(\"\").matcher(\"\")",
            "java.util.regex.Pattern.compile(\"a\").matcher(\"a\")",
            "java.util.regex.Pattern.compile(\"(.*)\").matcher(\"x\")");
        al("java.util.regex.MatchResult", "java.util.regex.Pattern.compile(\"\").matcher(\"\")",
            "java.util.regex.Pattern.compile(\"a\").matcher(\"a\")",
            "java.util.regex.Pattern.compile(\"(.*)\").matcher(\"x\")");
        // deterministic sources — a File/Path-backed Scanner reads the cwd
        al("java.util.Scanner", "new java.util.Scanner(\"\")",
            "new java.util.Scanner(\"a b\")",
            "new java.util.Scanner(new java.io.ByteArrayInputStream(new byte[]{1}))");
        al("java.util.concurrent.ConcurrentMap", "new java.util.concurrent.ConcurrentHashMap()",
            "new java.util.concurrent.ConcurrentHashMap(java.util.Collections.singletonMap(\"a\", \"b\"))",
            "new java.util.concurrent.ConcurrentSkipListMap()");
        al("java.util.concurrent.CompletionStage",
            "java.util.concurrent.CompletableFuture.completedStage(null)",
            "java.util.concurrent.CompletableFuture.completedStage(\"a\")",
            "java.util.concurrent.CompletableFuture.failedStage(new java.lang.IllegalStateException())");
        al("java.util.concurrent.Future",
            "java.util.concurrent.CompletableFuture.completedFuture(null)",
            "java.util.concurrent.CompletableFuture.completedFuture(\"a\")",
            "java.util.concurrent.CompletableFuture.failedFuture(new java.lang.IllegalStateException())");
        al("java.util.concurrent.Executor", "(r) -> r.run()");
        al("java.util.concurrent.CompletableFuture", "new java.util.concurrent.CompletableFuture()",
            "java.util.concurrent.CompletableFuture.completedFuture(null)",
            "java.util.concurrent.CompletableFuture.failedFuture(new java.lang.IllegalStateException())");
        al("java.util.concurrent.locks.Lock", "new java.util.concurrent.locks.ReentrantLock()");
        al("java.util.concurrent.locks.ReadWriteLock",
            "new java.util.concurrent.locks.ReentrantReadWriteLock()");
        al("java.util.concurrent.locks.Condition",
            "new java.util.concurrent.locks.ReentrantLock().newCondition()");
        al("java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock",
            "new java.util.concurrent.locks.ReentrantReadWriteLock().readLock()");
        al("java.util.concurrent.locks.ReentrantReadWriteLock$WriteLock",
            "new java.util.concurrent.locks.ReentrantReadWriteLock().writeLock()");
        al("java.util.concurrent.RecursiveAction",
            "new java.util.concurrent.RecursiveAction() { protected void compute() {} }");
        al("java.util.concurrent.CountedCompleter",
            "new java.util.concurrent.CountedCompleter<Object>() { protected void exec() {} }");
        al("java.util.concurrent.RejectedExecutionHandler",
            "new java.util.concurrent.ThreadPoolExecutor.AbortPolicy()",
            "new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()",
            "new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy()");
        al("java.util.Base64$Encoder", "java.util.Base64.getEncoder()",
            "java.util.Base64.getUrlEncoder()", "java.util.Base64.getMimeEncoder()");
        al("java.util.Base64$Decoder", "java.util.Base64.getDecoder()",
            "java.util.Base64.getUrlDecoder()", "java.util.Base64.getMimeDecoder()");
        al("java.lang.StringTemplate$Processor", "java.lang.StringTemplate.STR",
            "java.lang.StringTemplate.RAW", "java.lang.StringTemplate.FMT");
        al("java.util.zip.Checksum", "new java.util.zip.CRC32()", "new java.util.zip.CRC32C()",
            "new java.util.zip.Adler32()");
        al("java.util.Map$Entry", "java.util.Map.entry(\"\", \"\")", "java.util.Map.entry(\"a\", \"b\")");
        al("java.math.MathContext", "java.math.MathContext.UNLIMITED",
            "java.math.MathContext.DECIMAL64", "java.math.MathContext.DECIMAL128");
        al("java.net.URL", "new java.net.URL(\"http://localhost/\")",
            "new java.net.URL(\"http://localhost/a\")");
        al("java.net.URLStreamHandlerFactory", "(p) -> null");
        al("java.net.http.HttpRequest",
            "java.net.http.HttpRequest.newBuilder(java.net.URI.create(\"http://localhost/\")).build()");
        al("java.net.http.HttpResponse$BodyHandler",
            "java.net.http.HttpResponse.BodyHandlers.discarding()");
        al("java.net.http.HttpResponse$BodySubscriber",
            "java.net.http.HttpResponse.BodySubscribers.discarding()");
        al("java.util.function.IntPredicate", "x -> true", "x -> false");
        al("java.util.function.LongPredicate", "x -> true", "x -> false");
        al("java.util.function.DoublePredicate", "x -> true", "x -> false");
        al("java.util.function.IntConsumer", "x -> {}");
        al("java.util.function.LongConsumer", "x -> {}");
        al("java.util.function.DoubleConsumer", "x -> {}");
        al("java.util.function.IntFunction", "x -> new java.lang.Object[x]", "x -> \"a\"",
            "x -> null");
        al("java.util.function.LongFunction", "x -> null", "x -> \"a\"");
        al("java.util.function.DoubleFunction", "x -> null", "x -> \"a\"");
        al("java.util.function.IntSupplier", "() -> 0", "() -> 1", "() -> -1");
        al("java.util.function.LongSupplier", "() -> 0L", "() -> 1L", "() -> -1L");
        al("java.util.function.DoubleSupplier", "() -> 0.0d", "() -> 0.5d", "() -> java.lang.Double.NaN");
        al("java.util.function.BooleanSupplier", "() -> true", "() -> false");
        al("java.util.function.IntUnaryOperator", "x -> x", "x -> x + 1", "x -> -x");
        al("java.util.function.LongUnaryOperator", "x -> x", "x -> x + 1L", "x -> -x");
        al("java.util.function.DoubleUnaryOperator", "x -> x", "x -> x + 1.0d", "x -> -x");
        al("java.util.function.ToIntFunction", "x -> 0", "x -> 1", "x -> -1");
        al("java.util.function.ToLongFunction", "x -> 0L", "x -> 1L", "x -> -1L");
        al("java.util.function.ToDoubleFunction", "x -> 0.0d", "x -> 0.5d");
        al("java.util.function.IntBinaryOperator", "(a, b) -> a", "(a, b) -> a + b");
        al("java.util.function.ObjIntConsumer", "(o, i) -> {}");
        al("java.util.function.ObjLongConsumer", "(o, l) -> {}");
        al("java.util.function.ObjDoubleConsumer", "(o, d) -> {}");
        al("java.util.function.ToIntBiFunction", "(a, b) -> 0", "(a, b) -> 1");
        al("java.util.function.ToLongBiFunction", "(a, b) -> 0L", "(a, b) -> 1L");
        al("java.util.function.ToDoubleBiFunction", "(a, b) -> 0.0d", "(a, b) -> 0.5d");
        al("java.util.function.LongToIntFunction", "x -> (int) x");
        al("java.util.function.DoubleToIntFunction", "x -> (int) x");
        al("java.util.function.LongToDoubleFunction", "x -> (double) x");
        al("java.util.function.DoubleToLongFunction", "x -> (long) x");
        al("java.util.stream.BaseStream", "java.util.stream.Stream.empty()");
        al("java.util.stream.Collector", "java.util.stream.Collectors.toList()");
        al("java.util.PrimitiveIterator$OfInt", "java.util.stream.IntStream.of(1).iterator()");
        al("java.util.PrimitiveIterator$OfLong", "java.util.stream.LongStream.of(1L).iterator()");
        al("java.util.PrimitiveIterator$OfDouble", "java.util.stream.DoubleStream.of(0.5d).iterator()");
        al("java.util.Spliterator$OfInt", "java.util.stream.IntStream.of(1).spliterator()");
        al("java.util.Spliterator$OfLong", "java.util.stream.LongStream.of(1L).spliterator()");
        al("java.util.Spliterator$OfDouble", "java.util.stream.DoubleStream.of(0.5d).spliterator()");
        al("java.util.Spliterator$OfPrimitive", "java.util.stream.IntStream.of(1).spliterator()");
        al("java.util.concurrent.ThreadFactory",
            "java.util.concurrent.Executors.defaultThreadFactory()");
        al("java.util.concurrent.BlockingQueue", "new java.util.concurrent.LinkedBlockingQueue()",
            "new java.util.concurrent.LinkedBlockingQueue(java.util.Arrays.asList(\"a\", \"b\"))",
            "new java.util.concurrent.ArrayBlockingQueue(1)");
        al("java.util.concurrent.BlockingDeque", "new java.util.concurrent.LinkedBlockingDeque()",
            "new java.util.concurrent.LinkedBlockingDeque(java.util.Arrays.asList(\"a\", \"b\"))");
        al("java.util.concurrent.TransferQueue", "new java.util.concurrent.LinkedTransferQueue()");
        al("java.util.concurrent.Delayed", "new java.util.concurrent.ScheduledThreadPoolExecutor(0)");
        al("java.util.concurrent.RunnableFuture",
            "new java.util.concurrent.FutureTask(() -> null)");
        al("java.util.concurrent.CompletionService",
            "new java.util.concurrent.ExecutorCompletionService((r) -> r.run())");
        al("java.util.concurrent.ForkJoinPool$ManagedBlocker", "() -> true");
        al("java.util.concurrent.ForkJoinPool$ForkJoinWorkerThreadFactory",
            "java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory");
        al("java.util.concurrent.ConcurrentNavigableMap",
            "new java.util.concurrent.ConcurrentSkipListMap()",
            "new java.util.concurrent.ConcurrentSkipListMap("
                + "java.util.Collections.singletonMap(\"a\", \"b\"))");
        al("java.util.AbstractSequentialList", "new java.util.LinkedList()",
            "new java.util.LinkedList(java.util.Arrays.asList(\"a\", \"b\"))");
        al("java.util.Dictionary", "new java.util.Hashtable()");
        al("java.util.EventListener", "new java.util.EventListener() {}");
        al("java.util.Observer", "(o, a) -> {}");
        al("java.util.Formattable", "(a, b, c, d) -> {}");
        al("java.util.logging.Filter", "(r) -> true");
        al("java.util.logging.Formatter", "new java.util.logging.SimpleFormatter()");
        al("java.io.Closeable", "new java.io.StringReader(\"\")");
        al("java.io.Flushable", "new java.io.StringWriter()");
        al("java.io.FileFilter", "(f) -> true");
        al("java.io.FilenameFilter", "(d, n) -> true");
        al("java.io.Serializable", "\"a\"");
        al("java.io.DataInput",
            "new java.io.DataInputStream(new java.io.ByteArrayInputStream(new byte[0]))");
        al("java.io.DataOutput",
            "new java.io.DataOutputStream(new java.io.ByteArrayOutputStream())");
        al("java.nio.ByteBuffer", "java.nio.ByteBuffer.allocate(0)",
            "java.nio.ByteBuffer.allocate(1)", "java.nio.ByteBuffer.wrap(new byte[]{-1})");
        al("java.nio.CharBuffer", "java.nio.CharBuffer.allocate(0)",
            "java.nio.CharBuffer.wrap(\"a\")", "java.nio.CharBuffer.wrap(\"\\u0000\")");
        al("java.nio.IntBuffer", "java.nio.IntBuffer.allocate(0)",
            "java.nio.IntBuffer.wrap(new int[]{1})");
        al("java.nio.LongBuffer", "java.nio.LongBuffer.allocate(0)",
            "java.nio.LongBuffer.wrap(new long[]{1L})");
        al("java.nio.ShortBuffer", "java.nio.ShortBuffer.allocate(0)",
            "java.nio.ShortBuffer.wrap(new short[]{1})");
        al("java.nio.FloatBuffer", "java.nio.FloatBuffer.allocate(0)",
            "java.nio.FloatBuffer.wrap(new float[]{0.5f})");
        al("java.nio.DoubleBuffer", "java.nio.DoubleBuffer.allocate(0)",
            "java.nio.DoubleBuffer.wrap(new double[]{0.5d})");
        al("java.nio.charset.CharsetDecoder", "java.nio.charset.StandardCharsets.UTF_8.newDecoder()");
        al("java.nio.charset.CharsetEncoder", "java.nio.charset.StandardCharsets.UTF_8.newEncoder()");
        al("java.nio.channels.ByteChannel", "java.nio.channels.Pipe.open().source()");
        al("java.nio.channels.Pipe$SourceChannel", "java.nio.channels.Pipe.open().source()");
        al("java.nio.channels.Pipe$SinkChannel", "java.nio.channels.Pipe.open().sink()");
        al("java.nio.channels.InterruptibleChannel", "java.nio.channels.Pipe.open().source()");
        al("java.nio.file.WatchEvent$Kind", "java.nio.file.StandardWatchEventKinds.ENTRY_CREATE",
            "java.nio.file.StandardWatchEventKinds.ENTRY_DELETE",
            "java.nio.file.StandardWatchEventKinds.OVERFLOW");
        al("java.nio.file.OpenOption", "java.nio.file.StandardOpenOption.READ",
            "java.nio.file.StandardOpenOption.WRITE", "java.nio.file.StandardOpenOption.APPEND");
        al("java.nio.file.LinkOption", "java.nio.file.LinkOption.NOFOLLOW_LINKS");
        al("java.nio.file.CopyOption", "java.nio.file.StandardCopyOption.REPLACE_EXISTING",
            "java.nio.file.StandardCopyOption.COPY_ATTRIBUTES",
            "java.nio.file.LinkOption.NOFOLLOW_LINKS");
        al("java.nio.file.AccessMode", "java.nio.file.AccessMode.READ",
            "java.nio.file.AccessMode.WRITE", "java.nio.file.AccessMode.EXECUTE");
        al("java.nio.file.DirectoryStream$Filter", "(e) -> true");
        al("java.nio.file.attribute.FileAttribute",
            "java.nio.file.attribute.PosixFilePermissions.asFileAttribute("
                + "java.util.Collections.emptySet())");
        al("java.nio.file.attribute.UserPrincipalLookupService",
            "java.nio.file.FileSystems.getDefault().getUserPrincipalLookupService()");
        al("java.net.SocketAddress", "new java.net.InetSocketAddress(0)");
        al("java.net.SocketOption", "java.net.StandardSocketOptions.TCP_NODELAY",
            "java.net.StandardSocketOptions.SO_KEEPALIVE", "java.net.StandardSocketOptions.IP_TOS");
        al("java.net.ProtocolFamily", "java.net.StandardProtocolFamily.INET",
            "java.net.StandardProtocolFamily.INET6", "java.net.StandardProtocolFamily.UNIX");
        al("java.net.InetAddress", "java.net.InetAddress.getLoopbackAddress()");
        al("java.net.NetworkInterface",
            "java.net.NetworkInterface.getByInetAddress(java.net.InetAddress.getLoopbackAddress())");
        al("java.net.CookiePolicy", "java.net.CookiePolicy.ACCEPT_ALL",
            "java.net.CookiePolicy.ACCEPT_NONE", "java.net.CookiePolicy.ACCEPT_ORIGINAL_SERVER");
        al("java.net.CookieStore", "new java.net.CookieManager().getCookieStore()");
        al("java.net.http.HttpClient", "java.net.http.HttpClient.newHttpClient()");
        al("java.net.http.HttpRequest$Builder", "java.net.http.HttpRequest.newBuilder()",
            "java.net.http.HttpRequest.newBuilder(java.net.URI.create(\"http://localhost/\"))");
        al("java.net.http.HttpRequest$BodyPublisher",
            "java.net.http.HttpRequest.BodyPublishers.noBody()",
            "java.net.http.HttpRequest.BodyPublishers.ofString(\"a\")");
        al("java.net.http.HttpResponse$PushPromiseHandler", "(a, b, c) -> {}");
        al("java.net.http.WebSocket$Builder",
            "java.net.http.HttpClient.newHttpClient().newWebSocketBuilder()");
        al("java.security.Permission", "new java.lang.RuntimePermission(\"a\")",
            "new java.lang.RuntimePermission(\"\")");
        al("java.security.Guard", "new java.lang.RuntimePermission(\"a\")");
        al("java.security.PrivilegedAction", "() -> null");
        al("java.security.PrivilegedExceptionAction", "() -> null");
        al("java.security.Principal", "() -> \"a\"");
        al("java.security.Provider", "java.security.Security.getProviders()[0]");
        al("java.security.spec.EncodedKeySpec", "new java.security.spec.PKCS8EncodedKeySpec(new byte[0])",
            "new java.security.spec.X509EncodedKeySpec(new byte[0])");
        al("java.security.spec.AlgorithmParameterSpec", "new java.security.spec.PSSParameterSpec(0)");
        al("java.text.CharacterIterator", "new java.text.StringCharacterIterator(\"a\")");
        al("java.text.AttributedCharacterIterator",
            "new java.text.AttributedString(\"a\").getIterator()");
        al("java.text.Format", "new java.text.SimpleDateFormat(\"a\")",
            "java.text.NumberFormat.getInstance()");
        al("java.text.DateFormat", "java.text.DateFormat.getInstance()");
        al("java.text.NumberFormat", "java.text.NumberFormat.getInstance()");
        al("java.text.Collator", "java.text.Collator.getInstance()");
        al("java.text.BreakIterator", "java.text.BreakIterator.getCharacterInstance()");
        al("java.time.temporal.TemporalAdjuster", "java.time.LocalDate.of(2000, 1, 2)");
        al("java.time.temporal.TemporalQuery", "java.time.temporal.TemporalQueries.localDate()",
            "java.time.temporal.TemporalQueries.zone()", "java.time.temporal.TemporalQueries.precision()");
        al("java.time.temporal.TemporalField", "java.time.temporal.ChronoField.NANO_OF_SECOND",
            "java.time.temporal.ChronoField.DAY_OF_MONTH", "java.time.temporal.ChronoField.YEAR");
        al("java.time.temporal.TemporalAccessor", "java.time.LocalDate.of(2000, 1, 2)",
            "java.time.LocalTime.of(1, 2)", "java.time.Year.of(2000)");
        al("java.time.chrono.Chronology", "java.time.chrono.IsoChronology.INSTANCE");
        al("java.time.chrono.ChronoLocalDate", "java.time.LocalDate.of(2000, 1, 2)");
        al("java.time.chrono.ChronoLocalDateTime", "java.time.LocalDateTime.of(2000, 1, 2, 3, 4)");
        al("java.time.chrono.ChronoZonedDateTime",
            "java.time.ZonedDateTime.of(2000, 1, 2, 3, 4, 5, 6, java.time.ZoneId.of(\"UTC\"))");
        al("java.time.chrono.ChronoPeriod", "java.time.Period.ofDays(1)");
        al("java.time.Clock", "java.time.Clock.systemUTC()");
        al("java.time.ZoneId", "java.time.ZoneId.of(\"UTC\")", "java.time.ZoneId.of(\"+01:00\")",
            "java.time.ZoneId.of(\"Europe/Paris\")");
        al("java.time.zone.ZoneRules", "java.time.zone.ZoneRules.of(java.time.ZoneOffset.UTC)");
        al("java.sql.SQLType", "java.sql.JDBCType.NULL", "java.sql.JDBCType.VARCHAR");
        al("java.lang.classfile.ClassHierarchyResolver",
            "java.lang.classfile.ClassHierarchyResolver.defaultResolver()");
        al("java.lang.foreign.SegmentAllocator", "java.lang.foreign.Arena.ofAuto()");
        al("java.lang.foreign.MemorySegment", "java.lang.foreign.MemorySegment.NULL",
            "java.lang.foreign.MemorySegment.ofAddress(0L)");
        al("java.lang.foreign.SymbolLookup", "java.lang.foreign.SymbolLookup.loaderLookup()");
        al("java.lang.foreign.Linker", "java.lang.foreign.Linker.nativeLinker()");
        al("java.lang.foreign.FunctionDescriptor", "java.lang.foreign.FunctionDescriptor.ofVoid()",
            "java.lang.foreign.FunctionDescriptor.of(java.lang.foreign.ValueLayout.JAVA_INT)",
            "java.lang.foreign.FunctionDescriptor.ofVoid(java.lang.foreign.ValueLayout.JAVA_INT)");
        al("java.lang.foreign.MemoryLayout", "java.lang.foreign.ValueLayout.JAVA_BYTE",
            "java.lang.foreign.ValueLayout.JAVA_INT", "java.lang.foreign.MemoryLayout.paddingLayout(1L)");
        al("java.lang.foreign.StructLayout",
            "java.lang.foreign.MemoryLayout.structLayout(java.lang.foreign.ValueLayout.JAVA_BYTE)");
        al("java.lang.foreign.UnionLayout",
            "java.lang.foreign.MemoryLayout.unionLayout(java.lang.foreign.ValueLayout.JAVA_BYTE)");
        al("java.lang.foreign.SequenceLayout",
            "java.lang.foreign.MemoryLayout.sequenceLayout(1L, java.lang.foreign.ValueLayout.JAVA_BYTE)");
        al("java.lang.foreign.PaddingLayout", "java.lang.foreign.MemoryLayout.paddingLayout(1L)");
        al("java.lang.foreign.GroupLayout",
            "java.lang.foreign.MemoryLayout.structLayout(java.lang.foreign.ValueLayout.JAVA_BYTE)");
        al("java.lang.foreign.AddressLayout", "java.lang.foreign.ValueLayout.ADDRESS");
        al("java.lang.foreign.Linker$Option", "java.lang.foreign.Linker.Option.firstVariadicArg(0)",
            "java.lang.foreign.Linker.Option.isTrivial()",
            "java.lang.foreign.Linker.Option.captureCallState(\"a\")");
        al("java.lang.constant.MethodHandleDesc",
            "java.lang.constant.MethodHandleDesc.ofMethod("
                + "java.lang.constant.DirectMethodHandleDesc.Kind.STATIC,"
                + " java.lang.String.class.describeConstable().get(), \"length\","
                + " java.lang.constant.MethodTypeDesc.ofDescriptor(\"()I\"))");
        al("java.lang.constant.DirectMethodHandleDesc",
            "java.lang.constant.MethodHandleDesc.ofMethod("
                + "java.lang.constant.DirectMethodHandleDesc.Kind.STATIC,"
                + " java.lang.String.class.describeConstable().get(), \"length\","
                + " java.lang.constant.MethodTypeDesc.ofDescriptor(\"()I\"))");
    }

    private static void al(String type, String... variants) {
        ALIASES.put(type, variants);
    }

    private static final class Entry implements Comparable<Entry> {
        final String owner;
        final String name;
        final String desc;
        final String kind;

        Entry(String owner, String name, String desc, String kind) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.kind = kind;
        }

        String key() {
            return owner + " " + name + " " + desc;
        }

        @Override
        public int compareTo(Entry o) {
            return key().compareTo(o.key());
        }
    }

    // value-ladder step names, surfaced in index.txt for debugging
    private static final String[] PROFILES = {"trivial", "meaningful", "boundary"};
    // id suffix per emitted site within one member — the first stays bare so
    // bare m#### ids match the member across regenerations
    private static final String[] SITE_SUFFIX = {"", "b", "c"};

    public static void main(String[] args) throws Exception {
        Path srcOut = Paths.get(args[0]);
        Path reportDir = Paths.get(args[1]);
        // compiled jvmdg.stubtests.manual classes dir — reflected for @StubTest
        Path manualClasses = args.length > 2 && !args[2].isEmpty() ? Paths.get(args[2]) : null;
        Path pkgDir = srcOut.resolve("jvmdg/stubtests");
        // clear stale generated sources (e.g. the pre-reorg StubParityMain)
        if (Files.isDirectory(pkgDir)) {
            List<Path> stale = Files.list(pkgDir).collect(Collectors.toList());
            for (Path p : stale) {
                Files.delete(p);
            }
        }
        Files.createDirectories(pkgDir);
        Files.createDirectories(reportDir);

        List<Entry> members = collect();
        List<ManualTest> manual = discoverManual(manualClasses);
        List<String> untestable = new ArrayList<>();
        List<String> index = new ArrayList<>();
        Map<String, List<String>> files = new TreeMap<>();

        int id = 0;
        int coveredMembers = 0;
        for (Entry e : members) {
            if (emit(e, String.format("m%04d", id++), files, index, untestable)) {
                coveredMembers++;
            }
        }

        for (Map.Entry<String, List<String>> f : files.entrySet()) {
            writeOwnerFile(pkgDir, f.getKey(), f.getValue());
        }
        writeIndex(pkgDir, files.keySet(), manual);

        // manual tests claim registered members they cover
        Map<String, String> manualByMember = new LinkedHashMap<>();
        for (ManualTest m : manual) {
            if (!m.member.isEmpty()) {
                manualByMember.putIfAbsent(m.member, m.id);
            }
            index.add(m.id + " " + m.className + "." + m.method + " [manual]"
                + (m.member.isEmpty() ? "" : " member=" + m.member));
        }
        int manualClaimed = 0;
        for (int i = 0; i < untestable.size(); i++) {
            String line = untestable.get(i);
            String claimed = null;
            for (Map.Entry<String, String> mm : manualByMember.entrySet()) {
                if (line.startsWith(mm.getKey() + " ")) {
                    claimed = mm.getValue();
                    break;
                }
            }
            if (claimed != null) {
                untestable.set(i, line + " [manual:" + claimed + "]");
                manualClaimed++;
            }
        }

        try (Writer w = Files.newBufferedWriter(reportDir.resolve("untestable.txt"), StandardCharsets.UTF_8)) {
            w.write("# stub members with no synthesizable call site\n");
            for (String line : untestable) {
                w.write(line);
                w.write('\n');
            }
        }
        try (Writer w = Files.newBufferedWriter(reportDir.resolve("index.txt"), StandardCharsets.UTF_8)) {
            for (String line : index) {
                w.write(line);
                w.write('\n');
            }
        }
        // stub-utilization summary: emitted call sites vs registered members,
        // grouped by untestable reason — CI artifact for the coverage ratchet
        Map<String, Integer> reasonCounts = new TreeMap<>();
        for (String line : untestable) {
            String reason = line.substring(line.lastIndexOf(' ') + 1).split(":")[0];
            reasonCounts.merge(reason, 1, Integer::sum);
        }
        try (Writer w = Files.newBufferedWriter(reportDir.resolve("coverage.txt"), StandardCharsets.UTF_8)) {
            w.write("# stub call-site coverage\n");
            int total = coveredMembers + untestable.size();
            w.write(String.format("covered %d / %d (%.1f%%) sites=%d%n",
                coveredMembers, total, total == 0 ? 0d : coveredMembers * 100.0 / total,
                index.size() - manual.size()));
            for (Map.Entry<String, Integer> e : reasonCounts.entrySet()) {
                w.write("untestable[" + e.getKey() + "] " + e.getValue() + "\n");
            }
            w.write("manual " + manual.size() + " tests, claiming " + manualClaimed
                + " untestable members\n");
        }
        System.out.println("stub test corpus: " + (index.size() - manual.size()) + " call sites for "
            + coveredMembers + " members in " + files.size() + " classes, " + untestable.size()
            + " untestable (" + manualClaimed + " manual-claimed), " + manual.size()
            + " manual -> " + pkgDir);
    }

    /** fake type internal name -> real JDK type internal name (via {@code classStubs} adapters) */
    private static final Map<String, String> fakeToReal = new HashMap<>();

    private static List<Entry> collect() throws IOException {
        Map<String, Entry> members = new TreeMap<>();
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            for (VersionProvider provider : ServiceLoader.load(VersionProvider.class)) {
                provider.ensureInit(d);
                for (Map.Entry<Type, Pair<Type, Pair<Class<?>, Adapter>>> e :
                        provider.classStubs.entrySet()) {
                    // key is the real JDK type; first is the fake stand-in class
                    fakeToReal.put(e.getValue().getFirst().getInternalName(), e.getKey().getInternalName());
                }
                for (Map.Entry<Type, ClassMapping> e :
                        new ArrayList<>(provider.stubMappings.entrySet())) {
                    ClassMapping mapping = e.getValue();
                    if (mapping == null) continue;
                    String owner = e.getKey().getInternalName();
                    for (MemberNameAndDesc m : mapping.getMethodStubMap().keySet()) {
                        add(members, owner, m, "method");
                    }
                    for (MemberNameAndDesc m : mapping.getFieldStubMap().keySet()) {
                        add(members, owner, m, "field");
                    }
                    for (MemberNameAndDesc m : mapping.getMethodModifyMap().keySet()) {
                        add(members, owner, m, "modify");
                    }
                    for (MemberNameAndDesc m : mapping.getAbstracts().keySet()) {
                        add(members, owner, m, "abstract");
                    }
                }
            }
        }
        return new ArrayList<>(members.values());
    }

    private static void add(Map<String, Entry> members, String owner, MemberNameAndDesc m, String kind) {
        if (m == null) return; // placeholder registrations
        Entry e = new Entry(owner, m.getName(), m.getDesc().getDescriptor(), kind);
        members.putIfAbsent(e.key(), e);
    }

    private static boolean denied(String owner, String name) {
        if (owner.startsWith("sun/") || owner.startsWith("jdk/") || owner.startsWith("com/sun/")) {
            return true;
        }
        if (DENIED_OWNERS.contains(owner) || DENIED_NAMES.contains(name)) {
            return true;
        }
        for (Map.Entry<String, Set<String>> d : DENIED_BY_OWNER.entrySet()) {
            if (owner.equals(d.getKey()) && (d.getValue().isEmpty() || d.getValue().contains(name))) {
                return true;
            }
        }
        if (owner.startsWith("java/io/") || owner.startsWith("java/nio/file/")) {
            if (name.startsWith("set") || name.startsWith("delete") || name.startsWith("create")
                || name.startsWith("mkdir") || name.startsWith("rename") || name.startsWith("write")
                || name.equals("move") || name.equals("copy") || name.equals("newOutputStream")
                || name.equals("newBufferedWriter") || name.equals("newByteChannel")
                || name.equals("newBufferedReader") || name.equals("newInputStream")) {
                return true;
            }
        }
        if (owner.startsWith("java/net/") || owner.startsWith("java/nio/channels/")) {
            if (name.equals("accept") || name.equals("connect") || name.equals("read")
                || name.equals("receive") || name.equals("bind") || name.equals("listen")
                || name.equals("open")) {
                return true;
            }
        }
        if (owner.startsWith("java/time/") && name.equals("now")) {
            return true;
        }
        return false;
    }

    /** a call-site expression plus whether it is a void statement invocation */
    private static final class Expr {
        final String text;
        final boolean voidCall;

        Expr(String text, boolean voidCall) {
            this.text = text;
            this.voidCall = voidCall;
        }
    }

    /**
     * Emits up to {@code PROFILES.length} call sites for one member — one per
     * value-ladder step, deduplicated on the emitted expression — appended to
     * the per-owner file map. Members with no surviving site land in
     * {@code untestable} with the last failure reason. Returns whether at
     * least one site was emitted.
     */
    private static boolean emit(Entry e, String baseId, Map<String, List<String>> files,
                                List<String> index, List<String> untestable) {
        String memberKey = e.owner + "." + e.name + e.desc + " [" + e.kind + "]";
        // provider stand-in classes (e.g. J_U_R_RandomGenerator) map back to
        // the real JDK type so call sites compile against real signatures.
        // Only jvmdg-internal fakes may remap: real java owner types like
        // java/util/Map also appear as erased stand-ins (Map for SequencedMap)
        // and must not have their genuine members redirected.
        String realOwner = e.owner.startsWith("xyz/wagyourtail/")
            ? fakeToReal.getOrDefault(e.owner, e.owner)
            : e.owner;
        String fail = null;
        // member-resolution candidates: the owner class first, then the
        // adapter "value" type when the owner is an erased stand-in target
        // (e.g. a SequencedMap member registered under java/util/Map).
        List<Class<?>> candidates = new ArrayList<>(2);
        List<String> candNames = new ArrayList<>(2);
        if (realOwner.startsWith("xyz/wagyourtail/")) {
            fail = "jvmdg-fake-type";
        } else if (denied(realOwner, e.name) || DENIED_MEMBERS.contains(e.key())) {
            fail = "denied";
        } else {
            String mapped = fakeToReal.get(realOwner);
            for (String cand : mapped == null || mapped.equals(realOwner)
                ? Collections.singletonList(realOwner) : Arrays.asList(realOwner, mapped)) {
                try {
                    Class<?> c = Class.forName(cand.replace('/', '.'), false,
                        StubTestGenerator.class.getClassLoader());
                    String cn = c.getCanonicalName();
                    if (cn == null || !Modifier.isPublic(c.getModifiers())) {
                        if (fail == null) fail = "owner-not-public";
                        continue;
                    }
                    candidates.add(c);
                    candNames.add(cn);
                } catch (Throwable t) {
                    if (fail == null) fail = "owner-not-loadable";
                }
            }
            if (candidates.isEmpty() && fail == null) fail = "owner-not-loadable";
        }
        if (fail != null && candidates.isEmpty()) {
            untestable.add(memberKey + " " + fail);
            return false;
        }

        StringBuilder reason = new StringBuilder();
        Set<String> seen = new HashSet<>();
        List<String> bodies = new ArrayList<>();
        for (int p = 0; p < PROFILES.length; p++) {
            Expr expr = "field".equals(e.kind)
                ? fieldExpr(e, candidates, candNames, p, reason)
                : methodExpr(e, candidates, candNames, p, reason);
            if (expr == null || !seen.add(expr.text)) {
                continue;
            }
            String sid = baseId + SITE_SUFFIX[bodies.size()];
            bodies.add(site(sid, expr));
            index.add(sid + " " + memberKey + " profile=" + PROFILES[p]);
        }
        if (bodies.isEmpty()) {
            untestable.add(memberKey + " " + (reason.length() == 0 ? "no-sites" : reason));
            return false;
        }
        files.computeIfAbsent(fileClass(realOwner), k -> new ArrayList<>()).addAll(bodies);
        return true;
    }

    private static void setReason(StringBuilder reason, String r) {
        reason.setLength(0);
        reason.append(r);
    }

    private static Expr fieldExpr(Entry e, List<Class<?>> candidates, List<String> candNames,
                                  int profile, StringBuilder reason) {
        for (int ci = 0; ci < candidates.size(); ci++) {
            Class<?> clazz = candidates.get(ci);
            Field f;
            try {
                f = clazz.getField(e.name); // public only; non-public is unreachable from source
            } catch (NoSuchFieldException ex) {
                continue;
            }
            String cName = candNames.get(ci);
            if (Modifier.isStatic(f.getModifiers())) {
                return new Expr(cName + "." + e.name, false);
            }
            String recv = synth(clazz, 0, profile);
            if (recv == null) {
                setReason(reason, "no-receiver");
                return null;
            }
            return new Expr("((" + cName + ") " + recv + ")." + e.name, false);
        }
        setReason(reason, "field-not-public");
        return null;
    }

    private static Expr methodExpr(Entry e, List<Class<?>> candidates, List<String> candNames,
                                   int profile, StringBuilder reason) {
        Class<?>[] params;
        try {
            params = toClasses(Type.getArgumentTypes(e.desc));
        } catch (Throwable t) {
            setReason(reason, "param-type-not-loadable");
            return null;
        }

        Method method = null;
        Constructor<?> ctor = null;
        Class<?> clazz = null;
        String cName = null;
        boolean receiverInArg0 = false;
        if ("<init>".equals(e.name)) {
            clazz = candidates.get(0);
            cName = candNames.get(0);
            if (clazz.isInterface() || clazz.isEnum() || clazz.isAnnotation()
                || Modifier.isAbstract(clazz.getModifiers())) {
                setReason(reason, "abstract-owner-ctor");
                return null;
            }
            try {
                ctor = clazz.getConstructor(params);
            } catch (NoSuchMethodException ex) {
                setReason(reason, "ctor-not-public");
                return null;
            }
        } else {
            for (int ci = 0; ci < candidates.size(); ci++) {
                Class<?> cand = candidates.get(ci);
                try {
                    method = cand.getMethod(e.name, params);
                    clazz = cand;
                    cName = candNames.get(ci);
                    break;
                } catch (NoSuchMethodException ex) {
                    continue;
                }
            }
            if (method == null && params.length > 0 && !"<init>".equals(e.name)) {
                // static-stub registrations carry the receiver as arg0 —
                // interpret the remainder as the real instance signature
                Class<?> recvType = params[0];
                try {
                    method = recvType.getMethod(e.name,
                        Arrays.copyOfRange(params, 1, params.length));
                    clazz = recvType;
                    cName = recvType.getCanonicalName();
                    receiverInArg0 = true;
                } catch (NoSuchMethodException ex) {
                    // fall through to failure
                }
            }
            if (method == null) {
                setReason(reason, "method-not-public-or-missing");
                return null;
            }
            // can't invoke a member declared on a non-public class from source
            if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())
                && method.getDeclaringClass() != clazz) {
                setReason(reason, "declared-on-non-public");
                return null;
            }
        }
        if (receiverInArg0) {
            params = Arrays.copyOfRange(params, 1, params.length);
        }

        String recv = null;
        if (ctor == null && !Modifier.isStatic(method.getModifiers())) {
            recv = synth(clazz, 0, profile);
            if (recv == null) {
                setReason(reason, "no-receiver");
                return null;
            }
        }

        StringBuilder argList = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) argList.append(", ");
            String arg = synth(params[i], 0, profile);
            if (arg == null) {
                setReason(reason, "unsynthesizable-arg:" + params[i].getName());
                return null;
            }
            argList.append('(').append(params[i].getCanonicalName()).append(") ").append(arg);
        }

        String text;
        if (ctor != null) {
            text = "new " + cName + "(" + argList + ")";
        } else if (recv == null) {
            text = cName + "." + e.name + "(" + argList + ")";
        } else {
            text = "((" + cName + ") " + recv + ")." + e.name + "(" + argList + ")";
        }
        return new Expr(text, ctor == null && method.getReturnType() == void.class);
    }

    private static String site(String id, Expr e) {
        StringBuilder sb = new StringBuilder("        try {\n");
        if (e.voidCall) {
            sb.append("            ").append(e.text).append(";\n")
                .append("            Support.print(\"").append(id).append("\", \"void\");\n");
        } else {
            sb.append("            Support.print(\"").append(id).append("\", ")
                .append(e.text).append(");\n");
        }
        return sb.append("        } catch (Throwable t) { Support.exc(\"")
            .append(id).append("\", t); }\n").toString();
    }

    /** {@code java/util/Map$Entry} → {@code JavaUtilMap_Entry} */
    private static String fileClass(String realOwner) {
        String flat = realOwner.replace('/', '_').replace("$", "__");
        StringBuilder sb = new StringBuilder();
        for (String s : flat.split("_", -1)) {
            if (s.isEmpty()) {
                sb.append('_');
            } else {
                sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
            }
        }
        return sb.toString();
    }

    private static void writeOwnerFile(Path pkgDir, String cls, List<String> bodies)
            throws IOException {
        Path f = pkgDir.resolve(cls + ".java");
        try (Writer w = Files.newBufferedWriter(f, StandardCharsets.UTF_8)) {
            w.write("package jvmdg.stubtests;\n\n");
            w.write("/** generated by StubTestGenerator — do not edit */\n");
            w.write("public final class " + cls + " {\n");
            w.write("    public static void run() {\n");
            for (int s = 0; s < (bodies.size() + CHUNK - 1) / CHUNK; s++) {
                w.write("        s" + s + "();\n");
            }
            w.write("    }\n\n");
            for (int s = 0, i = 0; i < bodies.size(); s++) {
                w.write("    static void s" + s + "() {\n");
                for (int end = Math.min(i + CHUNK, bodies.size()); i < end; i++) {
                    w.write(bodies.get(i));
                }
                w.write("    }\n\n");
            }
            w.write("}\n");
        }
    }

    private static void writeIndex(Path pkgDir, Set<String> ownerClasses,
                                   List<ManualTest> manual) throws IOException {
        Path f = pkgDir.resolve("Index.java");
        try (Writer w = Files.newBufferedWriter(f, StandardCharsets.UTF_8)) {
            w.write("package jvmdg.stubtests;\n\n");
            w.write("/** generated by StubTestGenerator — do not edit */\n");
            w.write("public final class Index {\n");
            w.write("    public static void main(String[] args) {\n        run();\n    }\n\n");
            w.write("    public static void run() {\n");
            for (String cls : ownerClasses) {
                w.write("        " + cls + ".run();\n");
            }
            for (ManualTest m : manual) {
                w.write("        try { " + m.className + "." + m.method + "(\"" + m.id
                    + "\"); } catch (Throwable t) { Support.exc(\"" + m.id + "\", t); }\n");
            }
            w.write("    }\n}\n");
        }
    }

    /** a reflected {@code @StubTest} method: {@code public static void m(String id)} */
    private static final class ManualTest implements Comparable<ManualTest> {
        final String id;
        final String className;
        final String method;
        final String member;

        ManualTest(String id, String className, String method, String member) {
            this.id = id;
            this.className = className;
            this.method = method;
            this.member = member;
        }

        @Override
        public int compareTo(ManualTest o) {
            int c = className.compareTo(o.className);
            return c != 0 ? c : method.compareTo(o.method);
        }
    }

    /**
     * Reflects compiled {@code jvmdg.stubtests.manual.*} classes for
     * {@code @StubTest} methods (looked up by name so the generator needs no
     * compile dependency on the corpus source set). Each must be
     * {@code public static void m(String id)}; ids are {@code x####} in
     * class/method-name order.
     */
    private static List<ManualTest> discoverManual(Path classesDir) throws IOException {
        List<ManualTest> found = new ArrayList<>();
        if (classesDir != null && Files.isDirectory(classesDir)) {
            List<Path> classFiles = new ArrayList<>();
            try (java.util.stream.Stream<Path> s = Files.walk(classesDir)) {
                s.filter(p -> p.toString().endsWith(".class")).forEach(classFiles::add);
            }
            for (Path p : classFiles) {
                String cn = classesDir.relativize(p).toString()
                    .replace(File.separatorChar, '.');
                cn = cn.substring(0, cn.length() - ".class".length());
                if (!cn.startsWith("jvmdg.stubtests.manual.") || cn.contains("$")) {
                    continue;
                }
                Class<?> c;
                try {
                    c = Class.forName(cn, false, StubTestGenerator.class.getClassLoader());
                } catch (Throwable t) {
                    throw new IllegalStateException("cannot load manual stub test " + cn, t);
                }
                for (Method m : c.getDeclaredMethods()) {
                    String member = stubTestMember(m);
                    if (member == null) {
                        continue;
                    }
                    if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())
                        || m.getReturnType() != void.class || m.getParameterCount() != 1
                        || m.getParameterTypes()[0] != String.class) {
                        throw new IllegalStateException("@StubTest " + cn + "." + m.getName()
                            + " must be public static void (String id)");
                    }
                    found.add(new ManualTest(null, cn, m.getName(), member));
                }
            }
        }
        Collections.sort(found);
        List<ManualTest> out = new ArrayList<>();
        for (int i = 0; i < found.size(); i++) {
            ManualTest m = found.get(i);
            out.add(new ManualTest(String.format("x%04d", i), m.className, m.method, m.member));
        }
        return out;
    }

    /**
     * Returns the canonical {@code <owner>.<name><desc>} key for
     * {@code @StubTest.member}, or null when not annotated. The annotation
     * carries a {@link Ref} triple; canonicalizing through
     * {@link FullyQualifiedMemberNameAndDesc} accepts both
     * {@code Ljava/lang/X;} and {@code java/lang/X} owner forms.
     */
    private static String stubTestMember(Method m) {
        for (java.lang.annotation.Annotation a : m.getDeclaredAnnotations()) {
            if (a.annotationType().getName().equals("jvmdg.stubtests.StubTest")) {
                try {
                    Ref ref = (Ref) a.annotationType().getMethod("member").invoke(a);
                    if (ref.value().isEmpty()) {
                        return "";
                    }
                    FullyQualifiedMemberNameAndDesc f = FullyQualifiedMemberNameAndDesc.of(ref);
                    if (f.getName() == null) {
                        return "";
                    }
                    return f.getOwner().getInternalName() + "." + f.getName() + f.getDesc().getDescriptor();
                } catch (Exception ex) {
                    throw new IllegalStateException(
                        "malformed @StubTest member on " + m.getDeclaringClass().getName() + "." + m.getName(), ex);
                }
            }
        }
        return null;
    }

    private static Class<?>[] toClasses(Type[] types) throws ClassNotFoundException {
        Class<?>[] out = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            out[i] = toClass(types[i]);
        }
        return out;
    }

    private static Class<?> toClass(Type t) throws ClassNotFoundException {
        switch (t.getSort()) {
            case Type.BOOLEAN: return boolean.class;
            case Type.CHAR: return char.class;
            case Type.BYTE: return byte.class;
            case Type.SHORT: return short.class;
            case Type.INT: return int.class;
            case Type.LONG: return long.class;
            case Type.FLOAT: return float.class;
            case Type.DOUBLE: return double.class;
            case Type.ARRAY: {
                // internalName is descriptor form: [Lpkg/Class;
                String internal = t.getInternalName();
                for (Map.Entry<String, String> f : fakeToReal.entrySet()) {
                    if (f.getKey().startsWith("xyz/wagyourtail/")) {
                        internal = internal.replace(f.getKey(), f.getValue());
                    }
                }
                return Class.forName(internal.replace('/', '.'));
            }
            case Type.OBJECT: {
                // only jvmdg fake types remap; real java/* stand-in targets
                // (e.g. Map for SequencedMap) denote themselves in descs
                String internal = t.getInternalName();
                String real = internal.startsWith("xyz/wagyourtail/")
                    ? fakeToReal.getOrDefault(internal, internal) : internal;
                return Class.forName(real.replace('/', '.'), false, StubTestGenerator.class.getClassLoader());
            }
            default: throw new ClassNotFoundException(t.toString());
        }
    }

    /** value-ladder step: profiles past the end reuse the last variant */
    private static String ladder(int profile, String... variants) {
        return variants[Math.min(profile, variants.length - 1)];
    }

    /**
     * Synthesizes a Java expression producing a value of {@code t}, or null if
     * none can be written. Depth-bounded: factories' own params recurse.
     * {@code profile} selects the probe-ladder step (0 trivial, 1 meaningful,
     * 2 boundary) and propagates into factory/ctor arguments.
     */
    private static String synth(Class<?> t, int depth, int profile) {
        if (depth > 2) return null;
        if (t.isPrimitive()) {
            if (t == boolean.class) return ladder(profile, "false", "true");
            if (t == char.class) return ladder(profile, "'\\0'", "'a'", "'\\uffff'");
            if (t == byte.class) return ladder(profile, "(byte) 0", "(byte) 1", "(byte) -1");
            if (t == short.class) return ladder(profile, "(short) 0", "(short) 1", "(short) -1");
            if (t == int.class) return ladder(profile, "0", "1", "-1");
            if (t == long.class) return ladder(profile, "0L", "1L", "-1L");
            if (t == float.class) return ladder(profile, "0.0f", "0.5f", "Float.NaN");
            if (t == double.class) return ladder(profile, "0.0d", "0.5d", "Double.NaN");
            return null;
        }
        String n = t.getCanonicalName();
        if (n == null) return null;
        if (t == String.class) return ladder(profile, "\"\"", "\"a\"", "\"\\u0000\"");
        if (t.isArray()) {
            String comp = t.getComponentType().getCanonicalName();
            if (comp == null) return null;
            if (profile == 0) return "new " + comp + "[0]";
            String elem = synth(t.getComponentType(), depth + 1, profile);
            return elem == null ? "new " + comp + "[0]"
                : "new " + comp + "[]{" + elem + "}";
        }
        if (t.isEnum() && t.getEnumConstants() != null && t.getEnumConstants().length > 0) {
            // name the constant rather than .values()[i]: values() is missing
            // on targets for types that only recently became enums (ByteOrder
            // turned enum in 26), while the constant field exists everywhere
            Object[] cs = t.getEnumConstants();
            // first / middle / early: the last constant is version-fragile on
            // growth enums (ClassFileFormatVersion.RELEASE_27 missing on 21)
            int idx = profile == 0 ? 0 : profile == 1 ? cs.length / 2 : Math.min(1, cs.length - 1);
            return n + "." + ((Enum<?>) cs[idx]).name();
        }
        // explicit aliases override ctor/factory discovery (they carry ladders)
        String[] alias = ALIASES.get(t.getName());
        if (alias != null) {
            return alias[Math.min(profile, alias.length - 1)];
        }
        if (!t.isInterface() && !Modifier.isAbstract(t.getModifiers())) {
            try {
                Constructor<?> c = t.getConstructor();
                if (Modifier.isPublic(c.getModifiers())) {
                    return "new " + n + "()";
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (depth < 2) {
            // public static constant of type T (MathContext.UNLIMITED, Level.INFO, ...)
            List<Field> constants = new ArrayList<>();
            for (Field f : t.getFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == t) {
                    constants.add(f);
                }
            }
            constants.sort(Comparator.comparing((Field f) -> {
                // prefer inert "empty" constants over extremes like Duration.MAX
                switch (f.getName()) {
                    case "ZERO": case "EMPTY": case "NONE": case "UNLIMITED":
                        return 0;
                    default:
                        return 1;
                }
            }).thenComparing(Field::getName));
            if (!constants.isEmpty()) {
                return n + "." + constants.get(Math.min(profile, constants.size() - 1)).getName();
            }
            // public ctors with all-synthesizable params (File(""), Scanner(""), ...):
            // collect every viable candidate and spread profiles across them so
            // e.g. Optional gets empty()/of()/ofNullable() on successive steps
            List<String> ctorExprs = new ArrayList<>();
            if (!t.isInterface() && !Modifier.isAbstract(t.getModifiers()) && !t.isEnum()) {
                List<Constructor<?>> ctors = new ArrayList<>();
                for (Constructor<?> c : t.getConstructors()) {
                    if (c.getParameterCount() > 0) ctors.add(c);
                }
                ctors.sort(Comparator.comparingInt((Constructor<?> c) -> c.getParameterCount())
                    .thenComparing(Constructor::toGenericString));
                for (Constructor<?> c : ctors) {
                    String args = synthArgs(c.getParameterTypes(), depth, profile);
                    if (args != null) {
                        ctorExprs.add("new " + n + "(" + args + ")");
                    }
                }
            }
            if (!ctorExprs.isEmpty()) {
                return ctorExprs.get(Math.min(profile, ctorExprs.size() - 1));
            }
            // public static factories on T returning T, all params synthesizable
            List<Method> factories = new ArrayList<>();
            for (Method m : t.getMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getReturnType() != t) continue;
                if (denied(t.getName().replace('.', '/'), m.getName())) continue;
                factories.add(m);
            }
            factories.sort(Comparator.comparingInt((Method m) -> m.getParameterCount())
                .thenComparing(Method::getName));
            List<String> factoryExprs = new ArrayList<>();
            for (Method m : factories) {
                String args = synthArgs(m.getParameterTypes(), depth, profile);
                if (args != null) {
                    factoryExprs.add(n + "." + m.getName() + "(" + args + ")");
                }
            }
            if (!factoryExprs.isEmpty()) {
                return factoryExprs.get(Math.min(profile, factoryExprs.size() - 1));
            }
        }
        return null;
    }

    private static String synthArgs(Class<?>[] ptypes, int depth, int profile) {
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < ptypes.length; i++) {
            String a = synth(ptypes[i], depth + 1, profile);
            if (a == null) return null;
            if (i > 0) args.append(", ");
            args.append('(').append(ptypes[i].getCanonicalName()).append(") ").append(a);
        }
        return args.toString();
    }
}
