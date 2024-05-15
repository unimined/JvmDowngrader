package xyz.wagyourtail.jvmdg.cli;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.compile.PathDowngrader;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Arguments parser = new Arguments("JvmDowngrader", null, null, null);
        Arguments input = new Arguments("--target", "input to output\n  (required)", new String[]{"-t"}, new String[]{"input jar|path", "output jar|path"});
        Arguments classpath = new Arguments("--classpath", "Classpath to use\n  (highly recommended)", new String[]{"-cp"}, new String[]{"classpath"});
        parser.addChildren(
            new Arguments("--help", "Prints this help", new String[]{"-h"}, null),
            new Arguments("--version", "Prints the version", new String[]{"-v"}, null),
            new Arguments("--quiet", "Suppress all warnings", new String[]{"-q"}, null),
            new Arguments("--api", "Provide a java-api jar", new String[]{"-a"}, new String[]{"jar"}),
            new Arguments("--classVersion", "Target class version (ex. \"52\" for java 8)", new String[]{"-c"}, new String[]{"version"}),
            new Arguments("debug", "Set debug flags/call debug actions", null, null).addChildren(
                new Arguments("--print", "Enable printing debug info", new String[]{"-p"}, null),
                new Arguments("--skipStubs", "Skip method/class stubs for these class versions", new String[]{"-s"}, new String[]{"versions"}),
                new Arguments("--downgradeApi", "Retrieves and downgrades the java api jar", new String[]{"-d"}, new String[]{"outputPath"})
            ),
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
                classpath
            )
        );

        List<String> argList = new ArrayList<>(Arrays.asList(args));
        Map<String, List<String[]>> parsed = parser.read(argList, true);

        if (!parsed.containsKey("bootstrap") && !argList.isEmpty()) {
            throw new IllegalArgumentException("Unknown command: " + argList.get(0));
        }

        String version = Main.class.getPackage().getImplementationVersion();

        if (parsed.containsKey("--help") || parsed.isEmpty()) {
            System.out.println("JvmDowngrader " + version);
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
                    Flags.quiet = true;
                    break;
                case "--classVersion":
                    if (entry.getValue().size() > 1) {
                        throw new IllegalArgumentException("Multiple class versions specified");
                    }
                    Flags.classVersion = Integer.parseInt(entry.getValue().get(0)[0]);
                    break;
                case "--api":
                    if (entry.getValue().size() > 1) {
                        throw new IllegalArgumentException("Multiple api paths specified");
                    }
                    File api = new File(entry.getValue().get(0)[0]);
                    if (!api.exists()) {
                        throw new IllegalArgumentException("Api jar does not exist");
                    }
                    Flags.api = api;
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
                        bootstrap(subArgs, argList);
                        break;
                    default:
                }
            }
        }

    }

    public static void debug(Map<String, List<String[]>> args) throws IOException {
        for (Map.Entry<String, List<String[]>> entry : args.entrySet()) {
            switch (entry.getKey()) {
                case "--print":
                    Flags.printDebug = true;
                    break;
                case "--skipStubs":
                    for (String[] s : entry.getValue()) {
                        for (String string : s) {
                            Flags.debugSkipStubs.add(Integer.parseInt(string));
                        }
                    }
                    break;
            }
        }

        if (args.containsKey("downgradeApi")) {
            if (args.get("downgradeApi").size() > 1) {
                throw new IllegalArgumentException("Multiple output paths specified");
            }
            ApiShader.downgradeApi(Flags.classVersion, new File(args.get("downgradeApi").get(0)[0]).toPath());
        }
    }

    public static void getTargets(Map<String, List<String[]>> args, Map<Path, Path> targets, List<FileSystem> fileSystems) throws IOException {
        for (Map.Entry<String, List<String[]>> entry : args.entrySet()) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (entry.getKey()) {
                case "--target":
                    if (entry.getValue().size() > 1) {
                        throw new IllegalArgumentException("Multiple target paths specified");
                    }
                    String[] target = args.get("--target").get(0);
                    File input = new File(target[0]);
                    if (!input.exists()) {
                        throw new IllegalArgumentException("input \"" + input + "\" does not exist");
                    }
                    Path inputPath;
                    if (input.isDirectory()) {
                        inputPath = input.toPath();
                    } else {
                        FileSystem fs = Utils.openZipFileSystem(input.toPath(), Collections.<String, Object>emptyMap());
                        fileSystems.add(fs);
                        inputPath = fs.getPath("/");
                    }
                    File output = new File(target[1]);
                    if (output.toString().endsWith(".jar") || output.toString().endsWith(".zip")) {
                        FileSystem fs = Utils.openZipFileSystem(output.toPath(), Collections.<String, Object>singletonMap("create", "true"));
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

    public static Set<File> getClasspath(Map<String, List<String[]>> args) {
        Set<File> classpath = new HashSet<>();
        if (args.containsKey("--classpath")) {
            for (String[] s : args.get("--classpath")) {
                for (String path : s) {
                    for (String string : path.split(File.pathSeparator)) {
                        classpath.add(new File(string));
                    }
                }
            }
        }
        return classpath;
    }

    public static void downgrade(Map<String, List<String[]>> args) throws IOException {
        Map<Path, Path> targets = new HashMap<>();
        List<FileSystem> fileSystems = new ArrayList<>();
        getTargets(args, targets, fileSystems);

        List<Path> inputs = new ArrayList<>();
        List<Path> outputs = new ArrayList<>();
        for (Map.Entry<Path, Path> entry : targets.entrySet()) {
            inputs.add(entry.getKey());
            outputs.add(entry.getValue());
        }

        PathDowngrader.downgradePaths(Flags.classVersion, inputs, outputs, getClasspath(args));
    }

    public static void shade(Map<String, List<String[]>> args) throws IOException {
        if (!args.containsKey("--prefix")) {
            throw new IllegalArgumentException("No prefix specified");
        }
        if (args.get("--prefix").size() > 1) {
            throw new IllegalArgumentException("Multiple prefixes specified");
        }
        File downgradedApi = null;
        if (args.containsKey("--downgradedApi")) {
            if (args.get("--downgradedApi").size() > 1) {
                throw new IllegalArgumentException("Multiple downgraded api paths specified");
            }
            downgradedApi = new File(args.get("--downgradedApi").get(0)[0]);
            if (!downgradedApi.exists()) {
                throw new IllegalArgumentException("Downgraded api jar does not exist");
            }
        }
        String prefix = args.get("--prefix").get(0)[0];

        Map<Path, Path> targets = new HashMap<>();
        List<FileSystem> fileSystems = new ArrayList<>();
        getTargets(args, targets, fileSystems);

        List<Path> inputs = new ArrayList<>();
        List<Path> outputs = new ArrayList<>();
        for (Map.Entry<Path, Path> entry : targets.entrySet()) {
            inputs.add(entry.getKey());
            outputs.add(entry.getValue());
        }

        ApiShader.shadeApis(Flags.classVersion, prefix, inputs, outputs, downgradedApi);
    }

    public static void bootstrap(Map<String, List<String[]>> args, List<String> unparsed) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (!args.containsKey("--main")) {
            throw new IllegalArgumentException("No main class specified");
        }
        if (args.get("--main").size() > 1) {
            throw new IllegalArgumentException("Multiple main classes specified");
        }
        String main = args.get("--main").get(0)[0];

        Set<File> classpath = getClasspath(args);
        URL[] cp = new URL[classpath.size()];
        int i = 0;
        for (File file : classpath) {
            cp[i++] = file.toURI().toURL();
        }

        ClassDowngrader.classLoader.addDelegate(cp);
        Class.forName(main, false, ClassDowngrader.classLoader).getMethod("main", String[].class).invoke(
            null,
            (Object) unparsed.toArray(new String[0])
        );
    }

}
