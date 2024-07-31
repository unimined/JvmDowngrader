package xyz.wagyourtail.jvmdg.test;

import com.google.gson.JsonParser;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

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
//    private static final String os = System.getProperty("os.name").toLowerCase();
//    private static final String arch = System.getProperty("os.arch");
//
//    private static String getOS() {
//        if (os.contains("darwin")) {
//            return "mac";
//        } else if (os.contains("win")) {
//            return "windows";
//        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
//            return "linux";
//        } else {
//            return os;
//        }
//    }
//
//    private static String getArch() {
//        if (arch.contains("64")) {
//            return "x64";
//        } else if (arch.contains("86")) {
//            return "x86";
//        } else if (arch.contains("arm")) {
//            return "arm";
//        } else {
//            return arch;
//        }
//    }
//
//    private static InputStream downloadJavaFromAdopt(JavaVersion vers) throws IOException {
//        //                         https://api.adoptium.net/v3/binary/latest/8                             /ga/linux          /x64              /jdk/hotspot/normal/eclipse
//        URI download = URI.create("https://api.adoptium.net/v3/binary/latest/" + vers.getMajorVersion() + "/ga/" + getOS() + "/" + getArch() + "/jre/hotspot/normal/eclipse");
//        System.out.println("Attempting to download adoptium from " + download);
//        return download.toURL().openStream();
//    }
//
//    private static InputStream downloadJavaFromAzul(JavaVersion vers) throws IOException {
//        //                         https://api.azul.com/metadata/v1/zulu/packages/?java_version=7&os=windows&arch=x64&archive_type=zip&latest=true&distro_version=7&release_status=ga&availability_types=CA&certifications=tck&page=1&page_size=100
//        URI download = URI.create("https://api.azul.com/metadata/v1/zulu/packages/?java_version=" + vers.getMajorVersion() + "&os=" + getOS() + "&arch=" + getArch() + "&archive_type=" + (getOS().equals("windows") ? "zip" : "tar.gz") + "&latest=true&distro_version=" + vers.getMajorVersion() + "&release_status=ga&availability_types=CA&certifications=tck&page=1&page_size=100");
//        // parse json for url
//        System.out.println("getting download list from " + download);
//        try (InputStream stream = download.toURL().openStream()) {
//            String url = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonArray().get(0).getAsJsonObject().get("download_url").getAsString();
//            URI uri = URI.create(url);
//            System.out.println("Downloading from " + uri);
//            return uri.toURL().openStream();
//        }
//    }
//
//    private static void extractToJVMDir(InputStream stream, Path jvmdir) throws IOException {
//        try (ArchiveInputStream archiver = getOS().equals("windows") ? new ZipArchiveInputStream(stream) : new TarArchiveInputStream(new GZIPInputStream(stream))) {
//            var entry = archiver.getNextEntry();
//            while (entry != null) {
//                // remove first directory
//                String[] nameParts = entry.getName().split("/");
//                List<String> filteredNameParts = new ArrayList<>();
//                for (String namePart : nameParts) {
//                    if (!namePart.isEmpty()) {
//                        filteredNameParts.add(namePart);
//                    }
//                }
//                if (filteredNameParts.getFirst().startsWith("jdk")) {
//                    filteredNameParts.removeFirst();
//                } else if (filteredNameParts.getFirst().startsWith("zulu")) {
//                    filteredNameParts.removeFirst();
//                }
//                String name = String.join("/", filteredNameParts);
//                if (entry.isDirectory()) {
//                    Files.createDirectories(jvmdir.resolve(name));
//                } else {
//                    Files.copy(archiver, jvmdir.resolve(name));
//                }
//                entry = archiver.getNextEntry();
//            }
//        }
//    }
//
//    private static Path getJavaHome(JavaVersion vers) throws IOException {
//        Path jvmdir = Paths.get("./build/test/jvm/" + vers.getMajorVersion());
//        try {
//            if (!Files.exists(jvmdir)) {
//                Files.createDirectories(jvmdir);
//                InputStream stream;
//                try {
//                    stream = downloadJavaFromAdopt(vers);
//                    extractToJVMDir(stream, jvmdir);
//                } catch (IOException e) {
//                    stream = downloadJavaFromAzul(vers);
//                    extractToJVMDir(stream, jvmdir);
//                }
//            }
//        } catch (Throwable t) {
//            try {
//                Files.deleteIfExists(jvmdir);
//            } catch (IOException e) {
//                // Ignore exception
//            }
//            throw t;
//        }
//        return jvmdir;
//    }
//
//    public static Path getJava(int vers) throws IOException {
//        JavaVersion javaVersion = JavaVersion.fromClassVers(vers);
//        Path p = getJavaHome(javaVersion).resolve("bin").resolve("java" + (getOS().equals("windows") ? ".exe" : ""));
//        if (!Files.exists(p)) {
//            throw new IllegalStateException("java binary not found at " + p);
//        }
//        if (!getOS().equals("windows")) {
//            Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rwxr-xr-x"));
//        }
//        return p;
//    }

    public static Integer runJarInSubprocess(Path jar, String[] args, String mainClass, Set<Path> classPath,
                                             Path workingDir, Map<String, String> env, boolean wait, List<String> jvmArgs, Path javaBin,
                                             Consumer<String> output, Consumer<String> error) throws IOException, InterruptedException {

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
        V20,
        V21,
        V22,
        V23;

        public static JavaVersion fromClassVers(int vers) {
            return JavaVersion.values()[vers - 45];
        }

        public static JavaVersion fromMajor(int vers) {
            return JavaVersion.values()[vers - 1];
        }

        public int getMajorVersion() {
            return this.ordinal() + 1;
        }

        public int toOpcode() {
            return this.ordinal() + 45;
        }

        public static JavaVersion fromOpcode(int opcode) {
            return JavaVersion.values()[opcode - 45];
        }
    }
}