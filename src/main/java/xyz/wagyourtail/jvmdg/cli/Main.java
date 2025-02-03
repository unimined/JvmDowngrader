package xyz.wagyourtail.jvmdg.cli;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.compile.PathDowngrader;
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.*;

public class Main {
    protected final Flags flags = new Flags();
    protected final Deque<File> tempFiles = new ArrayDeque<>();

    public static void main(String[] args) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
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
                new Arguments("--dumpClasses", "Dump classes to the debug folder", new String[]{"-d"}, null),
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
                classpath,
                new Arguments("", "Arguments for main run", new String[]{}, new String[]{"args..."})
            )
        );
        return parser;
    }

    public void parseArgs(String[] args) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
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
                        output = File.createTempFile(input.getName(), ".jar", Constants.DIR).getAbsoluteFile();
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

    public void bootstrap(Map<String, List<String[]>> args) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (!args.containsKey("--main")) {
            throw new IllegalArgumentException("No main class specified");
        }
        if (args.get("--main").size() > 1) {
            throw new IllegalArgumentException("Multiple main classes specified");
        }
        String main = args.get("--main").get(0)[0];
        String[] bootstrapArgs;
        if (args.containsKey("")) {
            bootstrapArgs = args.get("").get(0);
        } else {
            bootstrapArgs = new String[0];
        }

        Set<URL> classpath = getClasspath(args);

        try (ClassDowngrader currentVersionDowngrader = ClassDowngrader.getCurrentVersionDowngrader(flags)) {
            currentVersionDowngrader.getClassLoader().addDelegate(classpath.toArray(new URL[0]));
            Class.forName(main, false, currentVersionDowngrader.getClassLoader()).getMethod("main", String[].class).invoke(
                null,
                (Object) bootstrapArgs
            );
        }
    }

}
