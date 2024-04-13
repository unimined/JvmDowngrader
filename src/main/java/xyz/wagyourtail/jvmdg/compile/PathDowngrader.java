package xyz.wagyourtail.jvmdg.compile;

import xyz.wagyourtail.jvmdg.ClassDowngrader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class PathDowngrader {

    public static void main(String[] args) throws MalformedURLException {
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
        downgradePaths(target, input, output, classpath);
    }

    public static void downgradePaths(int opcVersion, Set<Path> input, Set<Path> output, Set<File> classpath) throws MalformedURLException {
        Set<URL> classpathPaths = new HashSet<>();
        for (File file : classpath) {
            classpathPaths.add(file.toURI().toURL());
        }
        downgradePaths(ClassDowngrader.downgradeTo(opcVersion), input, output, classpathPaths);
    }

    public static void downgradePaths(final ClassDowngrader downgrader, Set<Path> input, Set<Path> output, Set<URL> classpath) {
        final URLClassLoader extraClasspath = new URLClassLoader(classpath.toArray(new URL[0]), PathDowngrader.class.getClassLoader());

    }

}
