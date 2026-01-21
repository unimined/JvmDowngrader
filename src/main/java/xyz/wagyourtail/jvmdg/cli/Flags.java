package xyz.wagyourtail.jvmdg.cli;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Consumer;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

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
import java.util.*;

public class Flags {
    public static final String jvmdgVersion = Flags.class.getPackage().getImplementationVersion();
    /**
     * internal method to resolve the java api jar
     */
    private static List<File> foundApi = null;
    /**
     * sets the target class version, default is {@link Opcodes#V1_8}
     */
    public int classVersion = Integer.parseInt(System.getProperty(Constants.CLASS_VERSION, "52"));
    /**
     * sets the api jar to use, if null will attempt to automatically find it
     */
    public List<File> api = null;
    /**
     * sets the log level to {@link Logger.Level#FATAL}
     *
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
     *
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
     *
     * @since 0.9.0
     */
    public TreeMap<String, WildcardType> ignoreWarningsIn = new TreeMap<>();
    /**
     * sets if maven lookup is allowed for auto resolving {@link #api}
     */
    public boolean allowMaven = Boolean.getBoolean(Constants.ALLOW_MAVEN_LOOKUP);
    /**
     * sets the log level to {@link Logger.Level#DEBUG}
     *
     * @deprecated use logLevel, if this is true, it will override {@link #logLevel} to {@link Logger.Level#DEBUG}
     */
    @Deprecated
    public boolean printDebug = Boolean.getBoolean(Constants.DEBUG);
    /**
     * sets whether to enable "inlining" of methods during {@link ApiShader} tasks.
     *
     * @since 1.2.0
     */
    public boolean shadeInlining = Boolean.getBoolean(System.getProperty(Constants.SHADE_INLINING, "true"));
    /**
     * this skips applying stubs for the specified input class version, this will still apply the
     * {@link xyz.wagyourtail.jvmdg.version.VersionProvider#otherTransforms(ClassNode, Set, Function, Set)}
     * such as {@code INVOKE_INTERFACE} -> {@code INVOKE_SPECIAL} for private interface methods in java 9 -> 8
     */
    public Set<Integer> debugSkipStubs = new HashSet<>(getDebugSkipStubs());
    /**
     * sets if classes should be dumped to the {@link Constants#DEBUG_DIR} directory
     *
     * @since 0.9.0
     */
    public boolean debugDumpClasses = Boolean.getBoolean(Constants.DEBUG_DUMP_CLASSES);

    /**
     * this skips applying specific stub classes/methods, for example to disable the JEP 400, you would put
     * {@link xyz.wagyourtail.jvmdg.j18.stub.java_base.J_L_System} in.
     *
     * @since 1.3.0
     */
    public Set<FullyQualifiedMemberNameAndDesc> debugSkipStub = new HashSet<>(getDebugSkipStub());

    /**
     * set generated methods to not be synthetic
     *
     * @since 1.2.3
     */
    public boolean debugNoSynthetic = Boolean.getBoolean(Constants.DEBUG_NO_SYNTHETIC);
    /**
     * if should move the original class file to the multi-release directory
     *
     * @since 1.0.0
     */
    public boolean multiReleaseOriginal = Boolean.getBoolean(Constants.MULTI_RELEASE_ORIGINAL);
    /**
     * if should provide extra multi-release versions. As class versions, not java versions.
     *
     * @since 1.0.0
     */
    public Set<Integer> multiReleaseVersions = new HashSet<>(getMultiReleaseVersions());

    /**
     * if should downgrade classes from the multi-release folder, instead of selecting the normal one
     *
     * @since 1.2.1
     */
    public boolean downgradeFromMultiReleases = false;

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
        flags.debugSkipStub = new HashSet<>(debugSkipStub);
        flags.debugSkipStubs = new HashSet<>(debugSkipStubs);
        flags.debugNoSynthetic = debugNoSynthetic;
        flags.debugDumpClasses = debugDumpClasses;
        flags.multiReleaseOriginal = multiReleaseOriginal;
        flags.multiReleaseVersions = new HashSet<>(multiReleaseVersions);
        flags.downgradeFromMultiReleases = downgradeFromMultiReleases;
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
            ", ignoreWarningsIn=" + ignoreWarningsIn +
            ", allowMaven=" + allowMaven +
            ", printDebug=" + printDebug +
            ", shadeInlining=" + shadeInlining +
            ", debugSkipStubs=" + debugSkipStubs +
            ", debugDumpClasses=" + debugDumpClasses +
            ", multiReleaseOriginal=" + multiReleaseOriginal +
            ", multiReleaseVersions=" + multiReleaseVersions +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flags)) return false;
        Flags flags = (Flags) o;
        return classVersion == flags.classVersion && quiet == flags.quiet && logAnsiColors == flags.logAnsiColors && allowMaven == flags.allowMaven && printDebug == flags.printDebug && shadeInlining == flags.shadeInlining && debugDumpClasses == flags.debugDumpClasses && multiReleaseOriginal == flags.multiReleaseOriginal && Objects.equals(api, flags.api) && logLevel == flags.logLevel && Objects.equals(ignoreWarningsIn, flags.ignoreWarningsIn) && Objects.equals(debugSkipStubs, flags.debugSkipStubs) && Objects.equals(multiReleaseVersions, flags.multiReleaseVersions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classVersion, api, quiet, logAnsiColors, logLevel, ignoreWarningsIn, allowMaven, printDebug, shadeInlining, debugSkipStubs, debugDumpClasses, multiReleaseOriginal, multiReleaseVersions);
    }

    /* getters */

    public void addIgnore(String s) {
        if (s.endsWith("**")) {
            ignoreWarningsIn.put(s.substring(0, s.length() - 2), WildcardType.DOUBLE);
        } else if (s.endsWith("*")) {
            ignoreWarningsIn.put(s.substring(0, s.length() - 1), WildcardType.SINGLE);
        } else {
            ignoreWarningsIn.put(s, WildcardType.NONE);
        }
    }

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
    private Set<FullyQualifiedMemberNameAndDesc> getDebugSkipStub() {
        Set<FullyQualifiedMemberNameAndDesc> skip = new HashSet<>();
        String skipStubs = System.getProperty(Constants.DEBUG_SKIP_STUB);
        if (skipStubs == null) return skip;
        for (String s : skipStubs.split("\\|")) {
            skip.add(FullyQualifiedMemberNameAndDesc.of(s));
        }
        return skip;
    }

    private Set<Integer> getDebugSkipStubs() {
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

    public String serialize() {
        List<String> args = new ArrayList<>();
        if (classVersion != Opcodes.V1_8) {
            args.add("--classVersion");
            args.add(String.valueOf(classVersion));
        }
        if (quiet) args.add("--quiet");
        if (!logAnsiColors) args.add("--noColor");
        if (logLevel != Logger.Level.INFO) {
            args.add("--logLevel");
            args.add(logLevel.name());
        }
        for (Map.Entry<String, WildcardType> entry : ignoreWarningsIn.entrySet()) {
            args.add("--ignoreWarningsIn");
            String key = entry.getKey();
            switch (entry.getValue()) {
                case DOUBLE:
                    key += "*";
                case SINGLE:
                    key += "*";
                case NONE:
            }
            args.add(key);
        }
        if (multiReleaseOriginal) {
            args.add("--multiReleaseOriginal");
        }
        for (Integer v : multiReleaseVersions) {
            args.add("--multiRelease");
            args.add(String.valueOf(v));
        }
        if (downgradeFromMultiReleases) {
            args.add("--multiReleaseInputs");
        }
        if (!shadeInlining) {
            args.add("--noInlining");
        }
        for (File a : api) {
            args.add("--api");
            // todo: escape spaces, but, how does that work with javac plugin args?
            args.add(a.getAbsolutePath());
        }
        if (printDebug || !debugSkipStub.isEmpty() || !debugSkipStubs.isEmpty() || debugDumpClasses) {
            args.add("debug");
            if (printDebug) {
                args.add("--print");
            }
            if (!debugSkipStub.isEmpty()) {
                for (FullyQualifiedMemberNameAndDesc f : debugSkipStub) {
                    args.add("--skipStub");
                    args.add(f.toString());
                }
            }
            if (!debugSkipStubs.isEmpty()) {
                for (int i : debugSkipStubs) {
                    args.add("--skipStubs");
                    args.add(String.valueOf(i));
                }
            }
            if (debugDumpClasses) {
                args.add("--dumpClasses");
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg).append(" ");
        }
        if (!args.isEmpty()) sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public enum WildcardType {
        NONE,
        SINGLE,
        DOUBLE
    }

}
