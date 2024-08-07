package xyz.wagyourtail.jvmdg.test.integration;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader;
import xyz.wagyourtail.jvmdg.test.JavaRunner;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseIntegrationTests {
    private static final String JVMDG_VERSION_KEY = "jvmdg.test.version";
    private static final String ORIGINAL_VERSION_KEY = "jvmdg.test.originalVersion";
    private static final String JAVA_VERSION_KEY = "jvmdg.test.javaVersion";
    private static final String LAUNCHER_KEY = "jvmdg.test.launcher";
    private static final String DOWNGRADE_CLASSPATH = "jvmdg.test.downgradeClasspath";

    private static final String JAVA_API_PATH = "jvmdg.test.javaApiPath";

    public static final Path javaApi = Path.of(System.getProperty(JAVA_API_PATH));
    public static final List<Path> downgradeClasspath = Arrays.stream(System.getProperty(DOWNGRADE_CLASSPATH).split(File.pathSeparator)).map(Path::of).toList();

    public static final Flags flags = new Flags();

    public static final JavaRunner.JavaVersion originalVersion = JavaRunner.JavaVersion.fromMajor(Integer.parseInt(System.getProperty(ORIGINAL_VERSION_KEY)));

    private static final List<JavaRunner.JavaVersion> javaVersions = Arrays.stream(System.getProperty(JAVA_VERSION_KEY).split(File.pathSeparator)).map(e -> JavaRunner.JavaVersion.fromMajor(Integer.parseInt(e))).toList();
    private static final List<Path> launchers = Arrays.stream(System.getProperty(LAUNCHER_KEY).split(File.pathSeparator)).map(Path::of).toList();

    public static final Map<JavaRunner.JavaVersion, Path> launchersByVersion = zip(javaVersions.stream(), launchers.stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    static {
        flags.api = List.of(javaApi.toFile());
    }


    private static <T, U> Stream<Map.Entry<T, U>> zip(Stream<T> a, Stream<U> b) {
        Iterator<T> aIt = a.iterator();
        Iterator<U> bIt = b.iterator();
        List<Map.Entry<T, U>> out = new ArrayList<>();
        while (aIt.hasNext() && bIt.hasNext()) {
            out.add(Map.entry(aIt.next(), bIt.next()));
        }
        return out.stream();
    }

    private static final Map<Flags, Path> apiPaths = new ConcurrentHashMap<>();

    public static Path getApiPath(FlagsAndRunner flags) {
        String fName = javaApi.getFileName().toString();
        String withoutExt = fName.substring(0, fName.lastIndexOf('.'));
        String ext = fName.substring(fName.lastIndexOf('.'));
        return Path.of("./build/tmp/test/" + withoutExt + "-downgrade-" + flags.readableSlug() + ext);
    }

    public static synchronized Path getApiJar(FlagsAndRunner flags) {
        return apiPaths.computeIfAbsent(flags.flags(), e -> {
            try {
                Path target = getApiPath(flags);
                ZipDowngrader.downgradeZip(
                    ClassDowngrader.downgradeTo(flags.flags()),
                    javaApi,
                    Set.of(),
                    target
                );
                return target;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

}
