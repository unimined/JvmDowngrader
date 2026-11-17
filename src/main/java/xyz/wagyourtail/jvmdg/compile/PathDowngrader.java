package xyz.wagyourtail.jvmdg.compile;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.compile.jar.ClassDowngradeStep;
import xyz.wagyourtail.jvmdg.compile.jar.EmbeddedRulesStep;
import xyz.wagyourtail.jvmdg.compile.jar.FusedJarRun;
import xyz.wagyourtail.jvmdg.compile.jar.JarStep;
import xyz.wagyourtail.jvmdg.compile.jar.ManifestStep;
import xyz.wagyourtail.jvmdg.compile.jar.MultiReleaseRouteStep;
import xyz.wagyourtail.jvmdg.compile.jar.PipelineJarStep;
import xyz.wagyourtail.jvmdg.compile.jar.SignatureStripStep;
import xyz.wagyourtail.jvmdg.compile.jar.StepContext;
import xyz.wagyourtail.jvmdg.compile.shade.ReferenceIndex;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.classloader.ResourceClassLoader;
import xyz.wagyourtail.jvmdg.util.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PathDowngrader {

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        int target = Integer.parseInt(args[0]);
        Set<Path> input = new HashSet<>();
        Set<Path> output = new HashSet<>();
        for (String s : args[1].split(File.pathSeparator)) {
            input.add(new File(s).toPath());
        }
        for (String s : args[2].split(File.pathSeparator)) {
            output.add(new File(s).toPath());
        }
        if (input.size() != output.size()) {
            throw new IllegalArgumentException("Input and output paths must be the same size");
        }
        Set<File> classpath = new HashSet<>();
        if (args.length > 3) {
            for (String s : args[3].split(File.pathSeparator)) {
                classpath.add(new File(s));
            }
        }
        downgradePaths(target, new ArrayList<>(input), new ArrayList<>(output), classpath);
        new Logger(PathDowngrader.class, Logger.Level.INFO, true, System.out)
            .info("Downgraded in " + (System.currentTimeMillis() - start) + "ms");
    }

    public static void downgradePaths(int opcVersion, List<Path> inputRoots, List<Path> outputRoots, Set<File> classpath) throws IOException {
        Set<URL> classpathPaths = new HashSet<>();
        for (File file : classpath) {
            classpathPaths.add(file.toURI().toURL());
        }
        try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(opcVersion)) {
            downgradePaths(downgrader, inputRoots, outputRoots, classpathPaths);
        }
    }

    public static void downgradePaths(final ClassDowngrader downgrader, final List<Path> inputRoots, List<Path> outputRoots, Set<URL> classpath) throws IOException {
        downgradePaths(downgrader, inputRoots, outputRoots, classpath, null, null, null, null);
    }

    /**
     * Fused downgrade + shade: when {@code shadePrefix} is non-null the class
     * step runs the whole-set pipeline — the downgrade union chain for the
     * observed input versions followed by the shade stage list — over every
     * class in one pass, so shading sees the complete post-transform set
     * without an intermediate jar write.
     */
    public static void downgradePaths(final ClassDowngrader downgrader, final List<Path> inputRoots, List<Path> outputRoots, Set<URL> classpath,
                                      final String shadePrefix, final ReferenceIndex apiIndex, final Set<Type> apiClasses, final List<Path> apiRoots) throws IOException {
        try (final ResourceClassLoader extraClasspath = new ResourceClassLoader(classpath, PathDowngrader.class.getClassLoader()) {

            @Override
            protected int maxClassVersionSupported() {
                return downgrader.maxVersion();
            }

            @Override
            protected List<String> multiVersionPrefixes() {
                if (downgrader.flags.downgradeFromMultiReleases) {
                    return super.multiVersionPrefixes();
                } else {
                    return Collections.singletonList("");
                }
            }

        }) {
            final List<String> multiReleasePaths = new ArrayList<>();
            for (int j = downgrader.maxVersion(); j >= 52; j--) {
                multiReleasePaths.add("META-INF/versions/" + Utils.classVersionToMajorVersion(j));
            }
            final Function<String, byte[]> extraRead = s -> {
                try {
                    // multi-version
                    if (downgrader.flags.downgradeFromMultiReleases) {
                        for (String pth : multiReleasePaths) {
                            for (Path in : inputRoots) {
                                Path p = in.resolve(pth).resolve(s + ".class");
                                if (Files.exists(p)) {
                                    return Files.readAllBytes(p);
                                }
                            }
                        }
                    }

                    // main version
                    for (Path in : inputRoots) {
                        Path p = in.resolve(s + ".class");
                        if (Files.exists(p)) {
                            return Files.readAllBytes(p);
                        }
                    }

                    URL url = extraClasspath.getResource(s + ".class");
                    if (url == null) return null;
                    try (InputStream is = Utils.openUncached(url)) {
                        return Utils.readAllBytes(is);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };

            // zip input and output
            for (int i = 0; i < inputRoots.size(); i++) {
                final Path in = inputRoots.get(i);
                final Path out = outputRoots.get(i);

                List<JarStep> steps = new ArrayList<>();
                // embedded META-INF/jvmdg/*.mf rules must land in
                // flags.scopedRules before any entry is transformed
                steps.add(new EmbeddedRulesStep());
                steps.add(new SignatureStripStep());
                steps.add(new ManifestStep());
                steps.add(new MultiReleaseRouteStep(shadePrefix != null));
                if (shadePrefix != null) {
                    steps.add(new PipelineJarStep(shadePrefix, apiIndex, apiClasses, apiRoots));
                } else {
                    steps.add(new ClassDowngradeStep());
                }
                StepContext ctx = new StepContext(downgrader.flags, downgrader.logger, downgrader, inputRoots, extraRead);
                FusedJarRun.run(in, steps, ctx, out);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
