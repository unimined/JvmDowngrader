package xyz.wagyourtail.jvmdg.cli;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.compile.PathDowngrader;
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class Main {
    protected final Flags flags = new Flags();
    protected final Deque<File> tempFiles = new ArrayDeque<>();

    public static void main(String[] args) throws Throwable {
        new Main().parseArgs(args);
    }

    protected Arguments buildArgumentList() {
        Arguments parser = new Arguments("JvmDowngrader", null, null, null);
        Arguments input = new Arguments("--target", "input to output\n  (required)\n  you can use - for a temp-file if you want to chain operations", new String[]{"-t"}, new String[]{"input jar|path", "output jar|path"});
        Arguments classpath = new Arguments("--classpath", "Classpath to use\n  (highly recommended)", new String[]{"-cp"}, new String[]{"classpath"});
        parser.addChildren(
            new Arguments("--help", "Prints this help", new String[]{"-h"}, null),
            new Arguments("--version", "Prints the version", new String[]{"-v"}, null),
            new Arguments("--logLevel", "Set the log level", new String[]{"-l"}, new String[]{"level"}),
            new Arguments("--quiet", "[Deprecated] Suppress all warnings", new String[]{"-q"}, null),
            new Arguments("--ignoreWarningsIn", "Ignore warnings of missing class/member stubs in package/class matching", new String[]{"-i"}, new String[]{"package or class identifier"}),
            new Arguments("--api", "Provide a java-api jar or jars", new String[]{"-a"}, new String[]{"jar"}),
            new Arguments("--classVersion", "Target class version (ex. \"52\" for java 8)", new String[]{"-c"}, new String[]{"version"}),
            new Arguments("--multiReleaseOriginal", "Use the original class file for a Multi-Release jar", new String[]{"-mro"}, null),
            new Arguments("--multiRelease", "Use semi-downgraded files for a Multi-Release jar, versions as class version (ex. \"55\" for java 11)", new String[]{"-mr"}, new String[]{"version"}),
            new Arguments("--noColor", "Disables ansi colors", new String[]{"-nc"}, null),
            new Arguments("--multiReleaseInputs", "Use the Multi-Release files as inputs for downgrading when available, instead of the normal ones", new String[]{"-mri"}, null),
            new Arguments("debug", "Set debug flags/call debug actions", null, null).addChildren(
                new Arguments("--print", "[Deprecated] Enable printing debug info", new String[]{"-p"}, null),
                new Arguments("--skipStub", "Skip a specific class/method, of form \"Lcom/example/ClassName;\" or \"Lcom/example/ClassName;methodName;()V\"", new String[] {"-ss"}, new String[] {"stub"}),
                new Arguments("--skipStubs", "Skip method/class stubs for these class versions", new String[]{"-s"}, new String[]{"versions"}),
                new Arguments("--skipJEP", "Skip JEP", new String[]{"-sj"}, new String[]{"jep"}),
                new Arguments("--dumpClasses", "Dump classes to the debug folder", new String[]{"-d"}, null),
                new Arguments("--noSynthetic", "Do not add `synthetic` modifier to methods", new String[]{"-ns"}, null),
                new Arguments("downgradeApi", "Retrieves and downgrades the java api jar", null, new String[]{"outputPath"})
            ),
            new Arguments("--disable-inlining", "Disables shade inlining api's that are only used in one class into that class", null, null),
            new Arguments("downgrade", "Downgrades a jar or folder", null, null).addChildren(
                input,
                classpath
            ),
            new Arguments("shade", "Shades necessary api's into targets", null, null).addChildren(
                new Arguments("--prefix", "Prefix to use for shaded classes\n  (required)", new String[]{"-p"}, new String[]{"prefix"}),
                new Arguments("--downgradedApi", "Pre-downgraded api jar", new String[]{"-d"}, new String[]{"jar"}),
                input
            ),
            new Arguments("bootstrap", "Bootstraps a downgrading environment, unparsed args will get passed", null, null).addChildren(
                new Arguments("--main", "Main class to run\n  (required)", new String[]{"-m"}, new String[]{"class"}),
                new Arguments("--seqmain", "Like --main but run sequentially on the driver thread first\n  (for mains observing JVM-global state like the child process list)", null, new String[]{"class"}),
                new Arguments("--batch", "Run every --main in one downgrading environment, framing each\n  (===JVMDG-BEGIN <name> / ===JVMDG-EXIT <code>)", null, null),
                classpath,
                new Arguments("", "Arguments for main run", new String[]{}, new String[]{"args..."})
            )
        );
        return parser;
    }

    public void parseArgs(String[] args) throws Throwable {
        Arguments parser = buildArgumentList();

        List<String> argList = new ArrayList<>(Arrays.asList(args));
        Map<String, List<String[]>> parsed = parser.read(argList, false);

        if (!parsed.containsKey("bootstrap") && !argList.isEmpty()) {
            throw new IllegalArgumentException("Unknown command: " + argList.get(0));
        }

        String version = Main.class.getPackage().getImplementationVersion();

        if (parsed.containsKey("--help") || parsed.isEmpty()) {
            System.out.println("JvmDowngrader " + version);
            System.out.println("Examples:");
            System.out.println();
            System.out.println("Chain downgrade/shade operations");
            System.out.println(">  jvmdg downgrade --target input.jar - shade --prefix uniqueName --target - output.jar");
            System.out.println();
            System.out.println("Downgrade a jar:");
            System.out.println(">  jvmdg downgrade --target input.jar output.jar");
            System.out.println();
            System.out.println("Shade jvmdg api into a downgraded jar");
            System.out.println(">  jvmdg shade --prefix uniqueName --target input.jar output.jar");
            System.out.println();
            System.out.println("Bootstrap run a jar");
            System.out.println(">  jvmdg bootstrap -cp input.jar --main com.example.Main");
            System.out.println();
            System.out.println();
            System.out.println("For more information see the help below, or the documentation at https://github.com/unimined/JvmDowngrader");
            System.out.println();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 40; i++) {
                sb.append("-");
            }
            System.out.println(sb);
            System.out.println();
            System.out.println(parser.help());
            return;
        }

        if (parsed.containsKey("--version")) {
            System.out.println("JvmDowngrader " + version);
            return;
        }

        for (Map.Entry<String, List<String[]>> entry : parsed.entrySet()) {
            switch (entry.getKey()) {
                case "--quiet":
                    flags.quiet = true;
                    break;
                case "--logLevel":
                    if (entry.getValue().size() > 1) {
                        throw new IllegalArgumentException("Multiple log levels specified");
                    }
                    flags.logLevel = Logger.Level.valueOf(entry.getValue().get(0)[0].toUpperCase());
                    break;
                case "--classVersion":
                    if (entry.getValue().size() > 1) {
                        throw new IllegalArgumentException("Multiple class versions specified");
                    }
                    flags.classVersion = Integer.parseInt(entry.getValue().get(0)[0]);
                    break;
                case "--ignoreWarningsIn":
                    for (String[] s : entry.getValue()) {
                        for (String string : s) {
                            flags.addIgnore(string);
                        }
                    }
                    break;
                case "--api":
                    List<File> api = new ArrayList<>();
                    for (String[] s : entry.getValue()) {
                        for (String string : s) {
                            String[] split = string.split(File.pathSeparator);
                            for (String s1 : split) {
                                api.add(new File(s1));
                            }
                        }
                    }
                    flags.api = api;
                    break;
                case "--multiReleaseOriginal":
                    flags.multiReleaseOriginal = true;
                    break;
                case "--multiRelease":
                    Set<Integer> versions = new HashSet<>();
                    for (String[] s : entry.getValue()) {
                        for (String string : s) {
                            versions.add(Integer.parseInt(string));
                        }
                    }
                    flags.multiReleaseVersions = versions;
                    break;
                case "--disable-inlining":
                    flags.shadeInlining = false;
                    break;
                case "--noColor":
                    flags.logAnsiColors = false;
                    break;
                case "--multiReleaseInputs":
                    flags.downgradeFromMultiReleases = true;
                    break;
                default:
            }
        }


        for (Map.Entry<String, List<String[]>> entry : parsed.entrySet()) {
            if (!entry.getKey().startsWith("--")) {
                Map<String, List<String[]>> subArgs = Arguments.subCommandArgs(entry.getKey(), parsed);
                switch (entry.getKey()) {
                    case "debug":
                        debug(subArgs);
                        break;
                    case "downgrade":
                        downgrade(subArgs);
                        break;
                    case "shade":
                        shade(subArgs);
                        break;
                    case "bootstrap":
                        bootstrap(subArgs);
                        break;
                    default:
                }
            }
        }

    }

    public void debug(Map<String, List<String[]>> args) throws IOException {
        for (Map.Entry<String, List<String[]>> entry : args.entrySet()) {
            switch (entry.getKey()) {
                case "--print":
                    flags.printDebug = true;
                    break;
                case "--dumpClasses":
                    flags.debugDumpClasses = true;
                    break;
                case "--skipStub":
                    for (String[] s : entry.getValue()) {
                        for (String string : s) {
                            flags.debugSkipStub.add(FullyQualifiedMemberNameAndDesc.of(string));
                        }
                    }
                    break;
                case "--skipStubs":
                    for (String[] s : entry.getValue()) {
                        for (String string : s) {
                            flags.debugSkipStubs.add(Integer.parseInt(string));
                        }
                    }
                    break;
                case "--skipJEP":
                    for (String[] s : entry.getValue()) {
                        for (String string : s) {
                            flags.debugSkipJeps.add(Integer.parseInt(string));
                        }
                    }
                    break;
                case "--noSynthetic":
                    flags.debugNoSynthetic = true;
                    break;
            }
        }

        if (args.containsKey("downgradeApi")) {
            if (args.get("downgradeApi").size() > 1) {
                throw new IllegalArgumentException("Multiple output paths specified");
            }
            if (flags.findJavaApi().isEmpty()) {
                throw new IllegalArgumentException("No api jar found");
            }
            if (flags.findJavaApi().size() > 1) {
                throw new IllegalArgumentException("Multiple api jars found");
            }
            try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(flags)) {
                ZipDowngrader.downgradeZip(downgrader, flags.findJavaApi().iterator().next().toPath(), new HashSet<URL>(), new File(args.get("downgradeApi").get(0)[0]).toPath());
            }
        }
    }

    public void getTargets(Map<String, List<String[]>> args, Map<Path, Path> targets, List<FileSystem> fileSystems) throws IOException {
        for (Map.Entry<String, List<String[]>> entry : args.entrySet()) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (entry.getKey()) {
                case "--target":
                    if (entry.getValue().size() > 1) {
                        throw new IllegalArgumentException("Multiple target paths specified");
                    }
                    String[] target = args.get("--target").get(0);
                    File input;
                    if (target[0].equals("-")) {
                        input = tempFiles.pollFirst();
                        if (input == null) {
                            throw new IllegalArgumentException("No input file found for " + Arrays.toString(target));
                        }
                    } else {
                        input = new File(target[0]);
                    }
                    if (!input.exists()) {
                        throw new IllegalArgumentException("input \"" + input + "\" does not exist");
                    }
                    Path inputPath;
                    if (input.isDirectory() || input.toString().endsWith(".class")) {
                        inputPath = input.toPath();
                    } else {
                        FileSystem fs = Utils.openZipFileSystem(input.toPath(), false);
                        fileSystems.add(fs);
                        inputPath = fs.getPath("/");
                    }
                    File output;
                    if (target[1].equals("-")) {
                        if (!Constants.DIR.exists() && !Constants.DIR.mkdirs()) {
                            throw new IOException("Failed to create directory: " + Constants.DIR);
                        }
                        String tempPrefix = input.getName();
                        if (tempPrefix.length() < 3) {
                            tempPrefix = tempPrefix + "___";
                        }
                        output = File.createTempFile(tempPrefix, ".jar", Constants.DIR).getAbsoluteFile();
                        output.deleteOnExit();
                        tempFiles.add(output);
                    } else {
                        output = new File(target[1]);
                        File parent = output.getParentFile();
                        if (parent != null && !parent.exists() && !parent.mkdirs()) {
                            throw new IOException("Failed to create directory: " + parent);
                        }
                    }
                    if (output.exists()) {
                        if (!output.delete()) {
                            throw new IOException("Failed to delete output file: " + output);
                        }
                    }
                    if (output.toString().endsWith(".jar") || output.toString().endsWith(".zip")) {
                        FileSystem fs = Utils.openZipFileSystem(output.toPath(), true);
                        fileSystems.add(fs);
                        targets.put(inputPath, fs.getPath("/"));
                    } else {
                        targets.put(inputPath, output.toPath());
                    }
                    break;
                default:
            }
        }
    }

    public Set<URL> getClasspath(Map<String, List<String[]>> args) throws MalformedURLException {
        Set<URL> classpath = new HashSet<>();
        if (args.containsKey("--classpath")) {
            for (String[] s : args.get("--classpath")) {
                for (String path : s) {
                    for (String string : path.split(File.pathSeparator)) {
                        classpath.add(new File(string).toURI().toURL());
                    }
                }
            }
        }
        return classpath;
    }

    public void downgrade(Map<String, List<String[]>> args) throws IOException {
        Map<Path, Path> targets = new HashMap<>();
        List<FileSystem> fileSystems = new ArrayList<>();
        try {
            getTargets(args, targets, fileSystems);

            List<Path> inputs = new ArrayList<>();
            List<Path> outputs = new ArrayList<>();
            for (Map.Entry<Path, Path> entry : targets.entrySet()) {
                inputs.add(entry.getKey());
                outputs.add(entry.getValue());
            }

            try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(flags)) {
                PathDowngrader.downgradePaths(downgrader, inputs, outputs, getClasspath(args));
            }
        } finally {
            for (FileSystem fileSystem : fileSystems) {
                fileSystem.close();
            }
        }
    }

    public void shade(Map<String, List<String[]>> args) throws IOException {
        if (!args.containsKey("--prefix")) {
            throw new IllegalArgumentException("No prefix specified");
        }
        if (args.get("--prefix").size() > 1) {
            throw new IllegalArgumentException("Multiple prefixes specified");
        }
        Set<File> downgradedApi = new HashSet<>();
        if (args.containsKey("--downgradedApi")) {
            for (String[] s : args.get("--downgradedApi")) {
                String[] split = s[0].split(File.pathSeparator);
                for (String s1 : split) {
                    downgradedApi.add(new File(s1));
                }
            }
        }
        if (downgradedApi.isEmpty()) {
            downgradedApi = null;
        }

        Map<Path, Path> targets = new HashMap<>();
        List<FileSystem> fileSystems = new ArrayList<>();
        try {
            getTargets(args, targets, fileSystems);

            List<Path> inputs = new ArrayList<>();
            List<Path> outputs = new ArrayList<>();
            for (Map.Entry<Path, Path> entry : targets.entrySet()) {
                inputs.add(entry.getKey());
                outputs.add(entry.getValue());
            }

            List<String> prefixes = new ArrayList<>();
            List<String[]> prefix = args.get("--prefix");
            for (String[] strings : prefix) {
                if (!strings[0].endsWith("/")) {
                    strings[0] += "/";
                }
                prefixes.add(strings[0]);
            }

            ApiShader.shadeApis(flags, prefixes, inputs, outputs, downgradedApi);
        } finally {
            for (FileSystem fileSystem : fileSystems) {
                fileSystem.close();
            }
        }
    }

    public void bootstrap(Map<String, List<String[]>> args) throws Throwable {
        if (!args.containsKey("--main") && !args.containsKey("--seqmain")) {
            throw new IllegalArgumentException("No main class specified");
        }
        boolean batch = args.containsKey("--batch");
        int mainCount = (args.containsKey("--main") ? args.get("--main").size() : 0) + (args.containsKey("--seqmain") ? args.get("--seqmain").size() : 0);
        if (mainCount > 1 && !batch) {
            throw new IllegalArgumentException("Multiple main classes specified without --batch");
        }
        final String[] bootstrapArgs;
        if (args.containsKey("")) {
            bootstrapArgs = args.get("").get(0);
        } else {
            bootstrapArgs = new String[0];
        }

        Set<URL> classpath = getClasspath(args);

        try (ClassDowngrader currentVersionDowngrader = ClassDowngrader.getCurrentVersionDowngrader(flags)) {
            currentVersionDowngrader.getClassLoader().addDelegate(classpath.toArray(new URL[0]));
            final PrintStream realOut = System.out;
            if (!batch) {
                if (args.containsKey("--main")) {
                    for (String[] m : args.get("--main")) {
                        invokeMain(Class.forName(m[0], false, currentVersionDowngrader.getClassLoader()), bootstrapArgs);
                    }
                }
                if (args.containsKey("--seqmain")) {
                    for (String[] m : args.get("--seqmain")) {
                        invokeMain(Class.forName(m[0], false, currentVersionDowngrader.getClassLoader()), bootstrapArgs);
                    }
                }
                return;
            }
            List<String[]> mains = args.containsKey("--main") ? args.get("--main") : Collections.<String[]>emptyList();
            // sequential phase: runs before any worker thread exists, so a
            // main enumerating children/threads sees only its own
            if (args.containsKey("--seqmain")) {
                PrintStream realErr = System.err;
                for (String[] m : args.get("--seqmain")) {
                    String name = m[0];
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    int exit = 0;
                    PrintStream cap = new PrintStream(buf, true, "UTF-8");
                    System.setOut(cap);
                    System.setErr(cap);
                    try {
                        invokeMain(Class.forName(name, false, currentVersionDowngrader.getClassLoader()), bootstrapArgs);
                    } catch (InvocationTargetException e) {
                        e.getCause().printStackTrace(cap);
                        exit = 1;
                    } catch (Throwable t) {
                        t.printStackTrace(cap);
                        exit = 1;
                    } finally {
                        cap.flush();
                        System.setOut(realOut);
                        System.setErr(realErr);
                    }
                    emit(realOut, name, buf, exit);
                }
            }
            int threads = Integer.getInteger("jvmdg.batch.threads",
                Math.max(1, Math.min(mains.size(), Runtime.getRuntime().availableProcessors() / 4)));
            final Router router = new Router(realOut);
            System.setOut(new PrintStream(router, true, "UTF-8"));
            System.setErr(new PrintStream(router, true, "UTF-8"));
            final Semaphore permits = new Semaphore(threads);
            final List<Thread> workers = new ArrayList<>();
            for (final String[] m : mains) {
                permits.acquire();
                // own ThreadGroup so threads the main spawns inherit its routing
                final ThreadGroup group = new ThreadGroup("jvmdg:" + m[0]);
                final ByteArrayOutputStream buf = router.register(group);
                final ClassDowngrader downgrader = currentVersionDowngrader;
                Thread worker = new Thread(group, new Runnable() {
                    @Override
                    public void run() {
                        int exit = 0;
                        String main = m[0];
                        try {
                            invokeMain(Class.forName(main, false, downgrader.getClassLoader()), bootstrapArgs);
                        } catch (InvocationTargetException e) {
                            e.getCause().printStackTrace();
                            exit = 1;
                        } catch (Throwable t) {
                            t.printStackTrace();
                            exit = 1;
                        } finally {
                            try {
                                emit(realOut, main, buf, exit);
                            } catch (IOException e) {
                                realOut.println("===JVMDG-BEGIN " + main);
                                realOut.println("===JVMDG-EXIT 1");
                            }
                            permits.release();
                        }
                    }
                });
                workers.add(worker);
                worker.start();
            }
            for (Thread worker : workers) {
                worker.join();
            }
        }
    }

    // emitted atomically so frames from parallel mains never interleave
    private static void emit(PrintStream realOut, String name, ByteArrayOutputStream buf, int exit) throws IOException {
        synchronized (realOut) {
            realOut.println("===JVMDG-BEGIN " + name);
            realOut.print(buf.toString("UTF-8"));
            realOut.println("===JVMDG-EXIT " + exit);
        }
    }

    /**
     * Routes writes to the capturing buffer of the current thread's group (or
     * any ancestor group — so threads a main spawns stay captured). Threads
     * outside any registered group fall through to the real stream, where
     * their output lands between frames and is ignored by the frame parser.
     */
    private static final class Router extends OutputStream {
        private final PrintStream real;
        private final Map<ThreadGroup, ByteArrayOutputStream> buffers = new ConcurrentHashMap<ThreadGroup, ByteArrayOutputStream>();

        Router(PrintStream real) {
            this.real = real;
        }

        ByteArrayOutputStream register(ThreadGroup group) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            buffers.put(group, buf);
            // intentionally never removed: threads still running after their
            // frame was emitted keep routing into the dead buffer rather than
            // leaking into another main's frame
            return buf;
        }

        private OutputStream route() {
            for (ThreadGroup g = Thread.currentThread().getThreadGroup(); g != null; g = g.getParent()) {
                ByteArrayOutputStream buf = buffers.get(g);
                if (buf != null) {
                    return buf;
                }
            }
            return real;
        }

        @Override
        public void write(int b) throws IOException {
            route().write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            route().write(b, off, len);
        }
    }

    // package-private: private would force a synthetic nestmate accessor on
    // downgraded jars, leaking an extra frame into stack walks
    void invokeMain(Class<?> clazz, String[] bootstrapArgs) throws Throwable {
//        0. A public static void main(String[] args) method
//        1. A static void main(String[] args) method of non-private access (i.e., public, protected or package) declared in the launched class,
//        2. A static void main() method of non-private access declared in the launched class,
//        3. A void main(String[] args) instance method of non-private access declared in the launched class or inherited from a superclass, or, finally,
//        4. A void main() instance method of non-private access declared in the launched class or inherited from a superclass.
        // 0
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodHandle traditional = null;
        try {
            traditional = publicLookup.findStatic(clazz, "main", MethodType.methodType(void.class, String[].class));
        } catch (NoSuchMethodException | IllegalAccessException ignored) {}
        if (traditional != null) {
            traditional.invoke((Object) bootstrapArgs);
            return;
        }

        if (Utils.getCurrentClassVersion() < Opcodes.V25) {
            throw new IllegalArgumentException("Main method not found");
        }

        Method mWithStr = null;
        try {
            mWithStr = clazz.getDeclaredMethod("main", String[].class);
            if (mWithStr.getReturnType() != void.class) {
                mWithStr = null;
            }
        } catch (NoSuchMethodException ignored) {}

        if (mWithStr != null && Modifier.isStatic(mWithStr.getModifiers()) && !Modifier.isPrivate(mWithStr.getModifiers())) {
            mWithStr.setAccessible(true);
            mWithStr.invoke(null, (Object) bootstrapArgs);
            return;
        }

        Method m = null;
        try {
            m = clazz.getDeclaredMethod("main");
            if (m.getReturnType() != void.class) {
                m = null;
            }
        } catch (NoSuchMethodException ignored) {}

        if (m != null && Modifier.isStatic(m.getModifiers()) && !Modifier.isPrivate(m.getModifiers())) {
            m.setAccessible(true);
            m.invoke(null);
            return;
        }

        Method instanceMain = findInstanceMainMethod(clazz);
        Object obj;
        try {
            Constructor<?> c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            obj = c.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        instanceMain.setAccessible(true);
        if (instanceMain.getParameterTypes().length > 0) {
            instanceMain.invoke(obj, (Object) bootstrapArgs);
        } else {
            instanceMain.invoke(obj);
        }
    }

    private static Method findInstanceMainMethod(Class<?> clazz) throws NoSuchMethodException {
        try {
            return clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException ignored) {}
        Class<?> current = clazz;
        Method noArgsCandidate = null;

        while (current != null && current != Object.class) {
            // Priority 1: Check for main(String[] args) in the current class layer
            try {
                Method m = current.getDeclaredMethod("main", String[].class);
                if (!Modifier.isPrivate(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && m.getReturnType() == void.class) {
                    return m;
                }
            } catch (NoSuchMethodException e) {
                // Fall through to check for a no-arg main variant
            }

            // Priority 2: Check for main() if no array version was found yet
            if (noArgsCandidate == null) {
                try {
                    noArgsCandidate = current.getDeclaredMethod("main");
                    if (Modifier.isPrivate(noArgsCandidate.getModifiers()) || Modifier.isStatic(noArgsCandidate.getModifiers()) || noArgsCandidate.getReturnType() != void.class) {
                        noArgsCandidate = null;
                    }
                } catch (NoSuchMethodException e) {
                    // Method doesn't exist at this specific layer
                }
            }

            // Move up to the parent superclass
            current = current.getSuperclass();
        }

        if (noArgsCandidate != null) {
            return noArgsCandidate;
        }

        try {
            return clazz.getMethod("main");
        } catch (NoSuchMethodException ignored) {}

        throw new NoSuchMethodException("no main method found!");
    }

}
