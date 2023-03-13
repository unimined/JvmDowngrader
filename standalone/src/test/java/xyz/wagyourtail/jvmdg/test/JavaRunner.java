package xyz.wagyourtail.jvmdg.test;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

public class JavaRunner {

    private static final String os = System.getProperty("os.name").toLowerCase();
    private static final String arch = System.getProperty("os.arch");

    private static String getOS() {
        if (os.contains("darwin")) {
            return "mac";
        } else if (os.contains("win")) {
            return "windows";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "linux";
        } else {
            return os;
        }
    }

    private static String getArch() {
        if (arch.contains("64")) {
            return "x64";
        } else if (arch.contains("86")) {
            return "x86";
        } else if (arch.contains("arm")) {
            return "arm";
        } else {
            return arch;
        }
    }

    private static Path getJavaHome(JavaVersion vers) throws IOException {
        Path jvmdir = Paths.get("./build/test/jvm/" + vers.getMajorVersion());
        try {
            if (!Files.exists(jvmdir)) {
                Files.createDirectories(jvmdir);
                //                         https://api.adoptium.net/v3/binary/latest/8                             /ga/linux          /x64              /jdk/hotspot/normal/eclipse
                URI download = URI.create("https://api.adoptium.net/v3/binary/latest/" + vers.getMajorVersion() + "/ga/" + getOS() + "/" + getArch() + "/jre/hotspot/normal/eclipse");
                // download to jvmdir
                try (TarArchiveInputStream archiver = new TarArchiveInputStream(new GZIPInputStream(download.toURL().openStream()))) {
                    var entry = archiver.getNextTarEntry();
                    while (entry != null) {
                        // remove first directory
                        String[] nameParts = entry.getName().split("/");
                        List<String> filteredNameParts = new ArrayList<>();
                        for (String namePart : nameParts) {
                            if (!namePart.isEmpty()) {
                                filteredNameParts.add(namePart);
                            }
                        }
                        if (filteredNameParts.get(0).startsWith("jdk")) {
                            filteredNameParts.remove(0);
                        }
                        String name = String.join("/", filteredNameParts);
                        if (entry.isDirectory()) {
                            Files.createDirectories(jvmdir.resolve(name));
                        } else {
                            Files.copy(archiver, jvmdir.resolve(name));
                        }
                        entry = archiver.getNextTarEntry();
                    }
                }
            }
        } catch (Throwable t) {
            try {
                Files.deleteIfExists(jvmdir);
            } catch (IOException e) {
                // Ignore exception
            }
            throw t;
        }
        return jvmdir;
    }

    public static Path getJava(int vers) throws IOException {
        JavaVersion javaVersion = JavaVersion.fromClassVers(vers);
        Path p = getJavaHome(javaVersion).resolve("bin").resolve("java" + (getOS().equals("windows") ? ".exe" : ""));
        if (!Files.exists(p)) {
            throw new IllegalStateException("java binary not found at " + p);
        }
        if (!getOS().equals("windows")) {
            Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
        return p;
    }

    public static Integer runJarInSubprocess(Path jar, String[] args, String mainClass, Set<Path> classPath,
        Path workingDir, Map<String, String> env, boolean wait, List<String> jvmArgs, int javaVersion,
        Consumer<String> output, Consumer<String> error) throws IOException, InterruptedException {

        Path javaBin = getJava(javaVersion);
        if (!javaBin.toFile().exists()) {
            throw new IllegalStateException("java binary not found at " + javaBin);
        }

        String[] processArgs;
        if (mainClass == null) {
            if (classPath.isEmpty()) {
                if (jar == null) throw new IllegalStateException("no jar or classpath");
                processArgs = new String[]{"-jar", jar.toString()};
            } else {
                Set<String> classpath = new HashSet<>();
                for (Path p : classPath) {
                    classpath.add(p.toString());
                }
                String cp = String.join(File.pathSeparator, classpath);
                processArgs = new String[]{"-cp", cp, "-jar", jar.toString()};
            }
        } else {
            Set<String> classpath = new HashSet<>();
            for (Path p : classPath) {
                classpath.add(p.toString());
            }
            String classPathString = classPath.isEmpty() ? "" : String.join(File.pathSeparator, classpath);
            if (jar != null) {
                classPathString += File.pathSeparator + jar;
            }
            processArgs = new String[]{"-cp", classPathString, mainClass};
        }

        List<String> command = new ArrayList<>();
        command.add(javaBin.toString());
        command.addAll(jvmArgs);
        command.addAll(Arrays.asList(processArgs));
        command.addAll(Arrays.asList(args));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDir.toFile());
        processBuilder.environment().putAll(env);

        System.out.println("Running: " + String.join(" ", processBuilder.command()));
        Process process = processBuilder.start();

        InputStream inputStream = process.getInputStream();
        InputStream errorStream = process.getErrorStream();

        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.accept(line);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.accept(line);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        outputThread.start();
        errorThread.start();

        if (wait) {
            int exitCode = process.waitFor();
            outputThread.join();
            errorThread.join();
            return exitCode;
        }

        return null;
    }

    public enum JavaVersion {
        V1_1,
        V1_2,
        V1_3,
        V1_4,
        V1_5,
        V1_6,
        V1_7,
        V1_8,
        V1_9,
        V10,
        V11,
        V12,
        V13,
        V14,
        V15,
        V16,
        V17,
        V18,
        V19,
        V20;

        public int getMajorVersion() {
            return this.ordinal() + 1;
        }

        public static JavaVersion fromClassVers(int vers) {
            return JavaVersion.values()[vers - 45];
        }
    }
    }