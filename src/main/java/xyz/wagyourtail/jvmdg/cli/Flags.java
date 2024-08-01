package xyz.wagyourtail.jvmdg.cli;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Consumer;
import xyz.wagyourtail.jvmdg.util.Function;

import java.beans.XMLDecoder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Flags {
    public static final String jvmdgVersion = Flags.class.getPackage().getImplementationVersion();

    /**
     * sets the target class version, default is {@link Opcodes#V1_8}
     */
    public int classVersion = Opcodes.V1_8;

    /**
     * sets the api jar to use, if null will attempt to automatically find it
     */
    public List<File> api = null;

    /**
     * sets the log level to {@link Logger.Level#FATAL}
     * @deprecated use logLevel, if this is true, it will override {@link #logLevel} to {@link Logger.Level#FATAL}
     */
    @Deprecated
    public boolean quiet = Boolean.getBoolean(Constants.QUIET);

    /**
     * sets if the logger should use ansi colors for the console to look pretty
     */
    public boolean logAnsiColors = Boolean.getBoolean(System.getProperty(Constants.LOG_ANSI_COLORS, "true"));
    /**
     * sets the log level
     * @see Logger.Level
     * @since 0.9.0
     */
    public Logger.Level logLevel = Logger.Level.valueOf(System.getProperty(Constants.LOG_LEVEL, "INFO").toUpperCase());

    /**
     * sets if any classes should be set to ignore missing class/member warnings
     * this will prevent {@link xyz.wagyourtail.jvmdg.version.VersionProvider#printWarnings(Set, String)} for
     * any classes in this set
     * <p>
     * This set also allows for packages to be ignored, by ending with a {@code *} or {@code **} to ignore all sub-packages
     * @since 0.9.0
     */
    public TreeMap<String, WildcardType> ignoreWarningsIn = new TreeMap<>();

    /**
     * sets if maven lookup is allowed for auto resolving {@link #api}
     */
    public boolean allowMaven = Boolean.getBoolean(Constants.ALLOW_MAVEN_LOOKUP);

    // debug
    /**
     * sets the log level to {@link Logger.Level#DEBUG}
     * @deprecated use logLevel, if this is true, it will override {@link #logLevel} to {@link Logger.Level#DEBUG}
     */
    @Deprecated
    public boolean printDebug = Boolean.getBoolean(Constants.DEBUG);

    /**
     * this skips applying stubs for the specified input class version, this will still apply the
     * {@link xyz.wagyourtail.jvmdg.version.VersionProvider#otherTransforms(ClassNode, Set, Function, Set)}
     * such as {@code INVOKE_INTERFACE} -> {@code INVOKE_SPECIAL} for private interface methods in java 9 -> 8
     */
    public Set<Integer> debugSkipStubs = new HashSet<>(getDebugSkip());

    /**
     * sets if classes should be dumped to the {@link Constants#DEBUG_DIR} directory
     * @since 0.9.0
     */
    public boolean debugDumpClasses = Boolean.getBoolean(Constants.DEBUG_DUMP_CLASSES);

    /**
     * if should move the original class file to the multi-release directory
     * @since 1.0.0
     */
    public boolean multiReleaseOriginal = Boolean.getBoolean(Constants.MULTI_RELEASE_ORIGINAL);

    /**
     * if should provide extra multi-release versions. As class versions, not java versions.
     * @since 1.0.0
     */
    public Set<Integer> multiReleaseVersions = new HashSet<>(getMultiReleaseVersions());

    public Flags() {
        getIgnoreWarnings();
    }

    public Flags copy() {
        Flags flags = new Flags();
        flags.classVersion = classVersion;
        flags.api = api;
        flags.quiet = quiet;
        flags.logAnsiColors = logAnsiColors;
        flags.logLevel = logLevel;
        flags.allowMaven = allowMaven;
        flags.ignoreWarningsIn = new TreeMap<>(ignoreWarningsIn);
        flags.printDebug = printDebug;
        flags.debugSkipStubs = new HashSet<>(debugSkipStubs);
        flags.debugDumpClasses = debugDumpClasses;
        flags.multiReleaseOriginal = multiReleaseOriginal;
        flags.multiReleaseVersions = new HashSet<>(multiReleaseVersions);
        return flags;
    }

    public Flags copy(Consumer<Flags> modifier) {
        Flags flags = copy();
        modifier.accept(flags);
        return flags;
    }

    @Override
    public String toString() {
        return "Flags{" +
                "classVersion=" + classVersion +
                ", api=" + api +
                ", quiet=" + quiet +
                ", logAnsiColors=" + logAnsiColors +
                ", logLevel=" + logLevel +
                ", allowMaven=" + allowMaven +
                ", printDebug=" + printDebug +
                ", debugSkipStubs=" + debugSkipStubs +
                ", ignoreWarningsIn=" + ignoreWarningsIn +
                '}';
    }

    public void addIgnore(String s) {
        if (s.endsWith("**")) {
            ignoreWarningsIn.put(s.substring(0, s.length() - 2), WildcardType.DOUBLE);
        } else if (s.endsWith("*")) {
            ignoreWarningsIn.put(s.substring(0, s.length() - 1), WildcardType.SINGLE);
        } else {
            ignoreWarningsIn.put(s, WildcardType.NONE);
        }
    }

    /* getters */

    /**
     * internal method for retrieving the actual log level
     */
    @ApiStatus.Internal
    public Logger.Level getLogLevel() {
        if (quiet) {
            return Logger.Level.FATAL;
        }
        if (printDebug) {
            return Logger.Level.DEBUG;
        }
        return logLevel;
    }

    /**
     * internal method to resolve the java api jar
     */
    private static List<File> foundApi = null;

    public List<File> findJavaApi() {
        try {
            if (api != null) {
                return api;
            }
            if (foundApi != null) {
                api = foundApi;
                return foundApi;
            }
            synchronized (Flags.class) {
                Constants.DIR.mkdirs();
                Path tmp = Constants.DIR.toPath().resolve("jvmdg-api-" + jvmdgVersion + ".jar");
                List<File> prop = getJavaApiFromSystemProperty();
                if (prop != null) {
                    foundApi = prop;
                    return prop;
                }
                URL url = getJavaApiFromShade();
                if (Files.exists(tmp)) {
                    if (url == null) {
                        foundApi = Collections.singletonList(tmp.toFile());
                        return foundApi;
                    } else {
                        try (InputStream in = url.openStream()) {
                            try (InputStream in2 = Files.newInputStream(tmp)) {
                                if (hash(in).equals(hash(in2))) {
                                    foundApi = Collections.singletonList(tmp.toFile());
                                    return foundApi;
                                }
                            }
                        }

                    }
                }
                if (url == null && allowMaven) {
                    url = getJavaApiFromMaven();
                }
                if (url != null) {
                    try (InputStream in = url.openStream()) {
                        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                        foundApi = Collections.singletonList(tmp.toFile());
                        return foundApi;
                    }
                }
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to find java api", e);
        }
    }

    /**
     * internal method to check if a class should be ignored for missing class/member warnings
     */
    public boolean checkInIgnoreWarnings(String className) {
        // find the entry that is <= the className, because a treeSet is sorted, this is either className,
        // a prefix of classname, a random other string that is less than className, ie. "aaa" < "bbb", or null if there
        // is no entries that are <= className in the map
        Map.Entry<String, Flags.WildcardType> entry = ignoreWarningsIn.floorEntry(className);
        if (entry != null && className.startsWith(entry.getKey())) {
            switch (entry.getValue()) {
                case NONE:
                    if (className.equals(entry.getKey())) {
                        return true;
                    }
                    return true;
                case SINGLE:
                    int lastSlash = className.lastIndexOf('/');
                    if (lastSlash == -1) {
                        return true;
                    }
                    String parent = className.substring(0, lastSlash + 1);
                    String value = entry.getKey();
                    if (!value.endsWith("/")) {
                        value = value.substring(0, value.lastIndexOf('/') + 1);
                    }
                    if (parent.equals(value)) {
                        return true;
                    }
                    break;
                case DOUBLE:
                    return true;
            }
        }
        return false;
    }

    /* initialization methods */
    private Set<Integer> getDebugSkip() {
        Set<Integer> skip = new HashSet<>();
        String skipStubs = System.getProperty(Constants.DEBUG_SKIP_STUBS);
        if (skipStubs == null) return skip;
        for (String s : skipStubs.split(",")) {
            skip.add(Integer.parseInt(s));
        }
        return skip;
    }

    private Set<Integer> getMultiReleaseVersions() {
        Set<Integer> versions = new HashSet<>();
        String multiReleaseVersions = System.getProperty(Constants.MULTI_RELEASE_VERSIONS);
        if (multiReleaseVersions == null) return versions;
        for (String s : multiReleaseVersions.split(",")) {
            versions.add(Integer.parseInt(s));
        }
        return versions;
    }

    private TreeMap<String, WildcardType> getIgnoreWarnings() {
        String ignoreWarnings = System.getProperty(Constants.IGNORE_WARNINGS);
        if (ignoreWarnings == null) return new TreeMap<>();
        TreeMap<String, WildcardType> map = new TreeMap<>();
        for (String s : ignoreWarnings.split(",")) {
            addIgnore(s);
        }
        return map;
    }

    private URL getJavaApiFromShade() throws IOException {
        return ClassDowngrader.class.getResource("/META-INF/lib/java-api.jar");
    }

    private List<File> getJavaApiFromSystemProperty() throws IOException {
        String api = System.getProperty(Constants.JAVA_API);
        if (api == null) {
            return null;
        }
        List<File> files = new ArrayList<>();
        for (String s : api.split(File.pathSeparator)) {
            files.add(new File(s));
        }
        return files;
    }

    private URL getJavaApiFromMaven() throws IOException {
        Package pkg = ClassDowngrader.class.getPackage();
        String version = pkg.getImplementationVersion();
        if (version.contains("SNAPSHOT")) {
            // retrieve maven metadata
            URL url = URI.create("https://maven.wagyourtail.xyz/snapshots/xyz/wagyourtail/jvmdowngrader/jvmdowngrader-java-api/" + version + "/maven-metadata.xml").toURL();
            // get actual latest
            try (InputStream in = url.openStream()) {
                XMLDecoder decoder = new XMLDecoder(in);
                Map<String, Object> metadata = (Map<String, Object>) decoder.readObject();
                String snapshotVersion = ((Map<String, Object>) ((Map<String, Object>) metadata.get("versioning")).get("snapshot")).get("timestamp") + "-" + ((Map<String, Object>) ((Map<String, Object>) metadata.get("versioning")).get("snapshot")).get("buildNumber");
                return URI.create("https://maven.wagyourtail.xyz/snapshots/xyz/wagyourtail/jvmdowngrader/jvmdowngrader-java-api/" + version + "/jvmdowngrader-java-api-" + version + "-" + snapshotVersion + ".jar").toURL();
            }
        } else {
            File file = Constants.DIR;
            file.mkdirs();
            file = new File(file, "java-api-" + version + ".jar");
            // if already exists, return that
            if (file.exists()) {
                return file.toURI().toURL();
            }

            URL url = URI.create("https://maven.wagyourtail.xyz/releases/xyz/wagyourtail/jvmdowngrader/jvmdowngrader-java-api/" + version + "/jvmdowngrader-java-api-" + version + ".jar").toURL();
            // download
            try (InputStream in = url.openStream()) {
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return file.toURI().toURL();
        }
    }

    public enum WildcardType {
        NONE,
        SINGLE,
        DOUBLE
    }

    private String hash(InputStream is) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash", e);
        }
    }

}
