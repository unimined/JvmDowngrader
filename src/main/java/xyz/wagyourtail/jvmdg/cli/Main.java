package xyz.wagyourtail.jvmdg.cli;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.compile.PathDowngrader;
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader;
import xyz.wagyourtail.jvmdg.compile.shade.ReferenceIndex;
import xyz.wagyourtail.jvmdg.config.BoundFlag;
import xyz.wagyourtail.jvmdg.config.FlagBindings;
import xyz.wagyourtail.jvmdg.config.JvmdgConfig;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;

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
import java.nio.file.Files;
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
        Arguments debugCmd = new Arguments("debug", "Set debug flags/call debug actions", null, null)
            .addChildren(new Arguments("downgradeApi", "Retrieves and downgrades the java api jar", null, new String[]{"outputPath"}));
        List<Arguments> top = new ArrayList<>(Arrays.asList(
            new Arguments("--help", "Prints this help", new String[]{"-h"}, null),
            new Arguments("--version", "Prints the version", new String[]{"-v"}, null),
            new Arguments("--config", "Apply a jvmdg.mf config file\n  (repeatable, applied in order, below CLI flags)", new String[]{"-cfg"}, new String[]{"file"})
        ));
        // flag surface is generated from @BoundFlag on Flags — see FlagBindings
        for (FlagBindings.Binding b : FlagBindings.ALL) {
            for (BoundFlag m : b.metas) {
                if (m.cli().isEmpty()) continue;
                Arguments arg = new Arguments(m.cli(), m.doc(), m.aliases(),
                    m.cliConst().isEmpty() ? m.argNames() : null);
                if (m.command().isEmpty()) {
                    top.add(arg);
                } else if ("debug".equals(m.command())) {
                    debugCmd.addChildren(arg);
                }
            }
        }
        parser.addChildren(top.toArray(new Arguments[0]));
        parser.addChildren(
            debugCmd,
            new Arguments("config", "Inspect the effective configuration", null, null).addChildren(
                new Arguments("--print", "Print the merged effective config and the source of each value", null, null)
            ),
            new Arguments("downgrade", "Downgrades a jar or folder", null, null).addChildren(
                input,
                classpath
            ),
            new Arguments("shade", "Shades necessary api's into targets", null, null).addChildren(
                new Arguments("--prefix", "Prefix to use for shaded classes\n  (required unless Shade-Prefix is configured)", new String[]{"-p"}, new String[]{"prefix"}),
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
            System.out.println("Emit multi-release output (primary downgraded + per-version variants):");
            System.out.println(">  jvmdg downgrade --target input.jar output.jar --multiRelease 8,11,17");
            System.out.println("   add --multiReleaseInputs to also treat META-INF/versions/ entries as inputs (highest per name wins)");
            System.out.println();
            System.out.println("Bootstrap run a jar");
            System.out.println(">  jvmdg bootstrap -cp input.jar --main com.example.Main");
            System.out.println();
            System.out.println("Or attach the agent to run a too-new jar on an old JVM:");
            System.out.println(">  java -javaagent:JvmDowngrader-all.jar -jar myapp.jar");
            System.out.println();
            System.out.println("Smallest shaded output (language desugars only, no stub impls, no reflection table):");
            System.out.println(">  jvmdg shade --prefix uniqueName --target input.jar output.jar --stubPolicy mechanical --onMissingApi warn --shadeReflection none");
            System.out.println("   add -c 49 for Java 5 target — drops all stack maps, but lowers every lambda to an inner class and every indy to field+method");
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

        Map<String, FlagBindings.Source> sources = new LinkedHashMap<>();

        // config files apply below explicit CLI flags — read them first, then
        // the generic flag pass overwrites whatever they set
        if (parsed.containsKey("--config")) {
            List<Path> files = new ArrayList<>();
            for (String[] s : parsed.get("--config")) {
                files.add(new File(s[0]).toPath());
            }
            JvmdgConfig.applyFiles(files, flags, sources);
        }

        for (Map.Entry<String, List<String[]>> entry : parsed.entrySet()) {
            if (!entry.getKey().startsWith("--")) continue;
            switch (entry.getKey()) {
                case "--help":
                case "--version":
                case "--config":
                    break;
                default: {
                    FlagBindings.Binding b = FlagBindings.byCliFlag(entry.getKey());
                    if (b == null) {
                        throw new IllegalArgumentException("Unknown argument: " + entry.getKey());
                    }
                    FlagBindings.applyCli(b, b.metaForCli(entry.getKey()), flags, entry.getValue(), sources);
                }
            }
        }


        List<Map.Entry<String, List<String[]>>> ops = new ArrayList<>(parsed.entrySet());
        for (int i = 0; i < ops.size(); i++) {
            Map.Entry<String, List<String[]>> entry = ops.get(i);
            if (!entry.getKey().startsWith("--")) {
                Map<String, List<String[]>> subArgs = Arguments.subCommandArgs(entry.getKey(), parsed);
                switch (entry.getKey()) {
                    case "debug":
                        debug(subArgs);
                        break;
                    case "config":
                        config(subArgs, sources);
                        break;
                    case "downgrade": {
                        // fuse `downgrade - shade` when they're chained through
                        // a temp file — one jar pass instead of two
                        Map<String, List<String[]>> nextArgs = null;
                        if (i + 1 < ops.size() && "shade".equals(ops.get(i + 1).getKey()) &&
                            fuses(subArgs, nextArgs = Arguments.subCommandArgs(ops.get(i + 1).getKey(), parsed))) {
                            downgradeShade(subArgs, nextArgs);
                            i++;
                            break;
                        }
                        downgrade(subArgs);
                        break;
                    }
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

    /** The `downgrade - shade` chain is fusable only through the temp-file hop: downgrade writes `-`, shade reads `-`. */
    private static boolean fuses(Map<String, List<String[]>> downgradeArgs, Map<String, List<String[]>> shadeArgs) {
        List<String[]> dt = downgradeArgs.get("--target");
        List<String[]> st = shadeArgs.get("--target");
        return dt != null && st != null && dt.size() == 1 && st.size() == 1 &&
            "-".equals(dt.get(0)[1]) && "-".equals(st.get(0)[0]);
    }

    /** The {@code config} command — currently just {@code --print}. */
    public void config(Map<String, List<String[]>> args, Map<String, FlagBindings.Source> sources) {
        if (args.containsKey("--print")) {
            System.out.println(FlagBindings.printConfig(flags, sources));
        }
    }

    public void debug(Map<String, List<String[]>> args) throws IOException {
        for (Map.Entry<String, List<String[]>> entry : args.entrySet()) {
            if (!entry.getKey().startsWith("--")) continue;
            FlagBindings.Binding b = FlagBindings.byCliFlag(entry.getKey());
            if (b == null) {
                throw new IllegalArgumentException("Unknown argument: " + entry.getKey());
            }
            FlagBindings.applyCli(b, b.metaForCli(entry.getKey()), flags, entry.getValue(), null);
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
                    if (output.exists() && Files.isSameFile(input.toPath(), output.toPath())) {
                        throw new IOException("input and output are the same file: " + input);
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
        if (!args.containsKey("--prefix") && flags.shadePrefix == null) {
            throw new IllegalArgumentException("No prefix specified (use --prefix or Shade-Prefix in a config file)");
        }
        if (args.get("--prefix") != null && args.get("--prefix").size() > 1) {
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
            if (prefix == null && flags.shadePrefix != null) {
                prefix = Collections.singletonList(new String[]{flags.shadePrefix});
            }
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

    /**
     * Fused {@code downgrade - shade}: one {@link PathDowngrader} pass where
     * the class step runs the downgrade union chain then the shade stage list
     * — the temp jar the chain would hop through is never written. Input
     * comes from the downgrade op's {@code --target}, output from the shade
     * op's; {@code --prefix}/{@code --downgradedApi} are the shade op's.
     */
    public void downgradeShade(Map<String, List<String[]>> dArgs, Map<String, List<String[]>> sArgs) throws IOException {
        if (!sArgs.containsKey("--prefix") && flags.shadePrefix == null) {
            throw new IllegalArgumentException("No prefix specified (use --prefix or Shade-Prefix in a config file)");
        }
        String prefix = sArgs.containsKey("--prefix") ? sArgs.get("--prefix").get(0)[0] : flags.shadePrefix;
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        Set<File> apiJars = new HashSet<>();
        if (sArgs.containsKey("--downgradedApi")) {
            for (String[] s : sArgs.get("--downgradedApi")) {
                for (String s1 : s[0].split(File.pathSeparator)) {
                    apiJars.add(new File(s1));
                }
            }
        }
        if (apiJars.isEmpty()) {
            apiJars = null;
        }

        String[] dTarget = dArgs.get("--target").get(0);
        String[] sTarget = sArgs.get("--target").get(0);
        List<FileSystem> fileSystems = new ArrayList<>();
        List<FileSystem> apiFs = new ArrayList<>();
        try {
            File input = new File(dTarget[0]);
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

            File output = new File(sTarget[1]);
            File parent = output.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create directory: " + parent);
            }
            if (output.exists() && !output.delete()) {
                throw new IOException("Failed to delete output file: " + output);
            }
            Path outputPath;
            if (output.toString().endsWith(".jar") || output.toString().endsWith(".zip")) {
                FileSystem fs = Utils.openZipFileSystem(output.toPath(), true);
                fileSystems.add(fs);
                outputPath = fs.getPath("/");
            } else {
                outputPath = output.toPath();
            }

            Set<Path> downgradedApiPath = ApiShader.resolveDowngradedApi(flags, apiJars);
            List<Path> apiRoots = new ArrayList<>();
            for (Path file : downgradedApiPath) {
                FileSystem fs = Utils.openZipFileSystem(file, false);
                apiFs.add(fs);
                apiRoots.add(fs.getPath("/"));
            }
            Pair<ReferenceIndex, Set<Type>> api = ApiShader.scanApis(flags, apiRoots);

            Set<URL> classpath = new HashSet<>();
            classpath.addAll(getClasspath(dArgs));
            classpath.addAll(getClasspath(sArgs));
            try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(flags)) {
                PathDowngrader.downgradePaths(downgrader,
                    Collections.singletonList(inputPath), Collections.singletonList(outputPath), classpath,
                    prefix, api.getFirst(), api.getSecond(), apiRoots);
            }
        } finally {
            for (FileSystem fileSystem : fileSystems) {
                fileSystem.close();
            }
            for (FileSystem fileSystem : apiFs) {
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
                Thread worker = new Thread(group, () -> {
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
        // match `java -cp` semantics: the TCCL of the thread running main is
        // the app's classloader, so ServiceLoader.load / resource lookups see
        // the downgraded classpath (a plain launcher sets it to the app loader)
        Thread thread = Thread.currentThread();
        ClassLoader prevTccl = thread.getContextClassLoader();
        thread.setContextClassLoader(clazz.getClassLoader());
        try {
            invokeMainImpl(clazz, bootstrapArgs);
        } finally {
            thread.setContextClassLoader(prevTccl);
        }
    }

    void invokeMainImpl(Class<?> clazz, String[] bootstrapArgs) throws Throwable {
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
