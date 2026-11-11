package xyz.wagyourtail.jvmdg.cli;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.config.BoundFlag;
import xyz.wagyourtail.jvmdg.config.FlagBindings;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Consumer;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    @BoundFlag(key = "Target", cli = "--classVersion", aliases = "-c", argNames = {"version"},
        sysprop = Constants.CLASS_VERSION, gradle = "downgradeTo",
        form = BoundFlag.Parser.JAVA_VERSION, legacyForm = BoundFlag.Parser.INT,
        doc = "Target version — java major in config, class version on CLI")
    public int classVersion = Opcodes.V1_8;
    /**
     * sets the api jar to use, if null will attempt to automatically find it
     */
    @BoundFlag(key = "Api", cli = "--api", aliases = "-a", argNames = {"jar"},
        sysprop = Constants.JAVA_API, gradle = "apiJar",
        form = BoundFlag.Parser.FILE_LIST,
        doc = "Provide a java-api jar or jars")
    public List<File> api = null;
    /**
     * sets the log level to {@link Logger.Level#FATAL}
     *
     * @deprecated use logLevel, if this is true, it will override {@link #logLevel} to {@link Logger.Level#FATAL}
     */
    @Deprecated
    @BoundFlag(key = "Quiet", cli = "--quiet", aliases = "-q", cliConst = "true",
        sysprop = Constants.QUIET, gradle = "quiet", deprecated = true,
        doc = "[Deprecated] Suppress all warnings — use Log-Level: FATAL")
    public boolean quiet = false;
    /**
     * sets if the logger should use ansi colors for the console to look pretty
     */
    @BoundFlag.List({
        @BoundFlag(key = "Log-Ansi-Colors", sysprop = Constants.LOG_ANSI_COLORS,
            gradle = "logAnsiColors", doc = "ANSI colors in console output"),
        @BoundFlag(key = "", cli = "--noColor", aliases = "-nc", cliConst = "false",
            doc = "Disables ansi colors")
    })
    public boolean logAnsiColors = true;
    /**
     * sets the log level
     *
     * @see Logger.Level
     * @since 0.9.0
     */
    @BoundFlag(key = "Log-Level", cli = "--logLevel", aliases = "-l", argNames = {"level"},
        sysprop = Constants.LOG_LEVEL, gradle = "logLevel",
        form = BoundFlag.Parser.LOG_LEVEL, doc = "Set the log level")
    public Logger.Level logLevel = Logger.Level.INFO;
    /**
     * sets if any classes should be set to ignore missing class/member warnings
     * this will prevent {@link xyz.wagyourtail.jvmdg.version.VersionProvider#printWarnings(Set, String)} for
     * any classes in this set
     * <p>
     * This set also allows for packages to be ignored, by ending with a {@code *} or {@code **} to ignore all sub-packages
     *
     * @since 0.9.0
     */
    @BoundFlag(key = "Ignore-Warnings-In", cli = "--ignoreWarningsIn", aliases = "-i",
        argNames = {"package or class identifier"}, sysprop = Constants.IGNORE_WARNINGS,
        gradle = "ignoreWarningsIn", form = BoundFlag.Parser.WILDCARD_MAP,
        doc = "Ignore missing class/member warnings in matching packages")
    public TreeMap<String, WildcardType> ignoreWarningsIn = new TreeMap<>();
    /**
     * sets if maven lookup is allowed for auto resolving {@link #api}
     */
    @BoundFlag(key = "Allow-Maven", sysprop = Constants.ALLOW_MAVEN_LOOKUP,
        doc = "Allow maven lookup for auto-resolving the api jar")
    public boolean allowMaven = false;
    /**
     * sets the log level to {@link Logger.Level#DEBUG}
     *
     * @deprecated use logLevel, if this is true, it will override {@link #logLevel} to {@link Logger.Level#DEBUG}
     */
    @Deprecated
    @BoundFlag(key = "Print-Debug", cli = "--print", aliases = "-p", cliConst = "true",
        command = "debug", sysprop = Constants.DEBUG, gradle = "debug", deprecated = true,
        doc = "[Deprecated] Enable printing debug info — use Log-Level: DEBUG")
    public boolean printDebug = false;
    /**
     * sets whether to enable "inlining" of methods during {@link ApiShader} tasks.
     *
     * @since 1.2.0
     */
    @BoundFlag.List({
        @BoundFlag(key = "Shade-Inlining", sysprop = Constants.SHADE_INLINING,
            gradle = "shadeInlining",
            doc = "Inline api members used by exactly one class during shade"),
        @BoundFlag(key = "", cli = "--disable-inlining", cliConst = "false",
            doc = "Disables shade inlining")
    })
    public boolean shadeInlining = true;
    /**
     * this skips applying stubs for the specified input class version, this will still apply the
     * {@link xyz.wagyourtail.jvmdg.version.VersionProvider#otherTransforms(ClassNode, Set, Function, Set)}
     * such as {@code INVOKE_INTERFACE} -> {@code INVOKE_SPECIAL} for private interface methods in java 9 -> 8
     */
    @BoundFlag(key = "Exclude-Stub-Versions", cli = "--skipStubs", aliases = "-s",
        argNames = {"versions"}, command = "debug", sysprop = Constants.DEBUG_SKIP_STUBS,
        gradle = "debugSkipStubs", form = BoundFlag.Parser.JAVA_VERSION_SET,
        legacyForm = BoundFlag.Parser.CLASS_VERSION_SET,
        doc = "Skip stubs for the specified input versions")
    public Set<Integer> debugSkipStubs = new HashSet<>();

    /**
     * skip the specified JEPs stubs. this is only guarenteed to be present for methods that exist in both the source and target class versions,
     * ie. if the behavior of the method changes, such as JEP 400 where native.encoding isn't set.
     *
     * @since 1.4.0
     */
    @BoundFlag(key = "Exclude-Jeps", cli = "--skipJEP", aliases = "-sj",
        argNames = {"jep"}, command = "debug", sysprop = Constants.DEBUG_SKIP_JEPS,
        gradle = "debugSkipJeps", form = BoundFlag.Parser.JEP_SET,
        doc = "Skip stubs for the specified JEPs")
    public Set<Integer> debugSkipJeps = new HashSet<>();

    /**
     * sets if classes should be dumped to the {@link Constants#DEBUG_DIR} directory
     *
     * @since 0.9.0
     */
    @BoundFlag(key = "Debug-Dump-Classes", cli = "--dumpClasses", aliases = "-d",
        cliConst = "true", command = "debug", sysprop = Constants.DEBUG_DUMP_CLASSES,
        gradle = "debugDumpClasses", doc = "Dump classes to the debug folder")
    public boolean debugDumpClasses = false;

    /**
     * this skips applying specific stub classes/methods, for example to disable the JEP 400, you would put
     * {@link xyz.wagyourtail.jvmdg.j18.stub.java_base.J_L_System} in.
     *
     * @since 1.3.0
     */
    @BoundFlag(key = "Exclude-Stubs", cli = "--skipStub", aliases = "-ss",
        argNames = {"stub"}, command = "debug", sysprop = Constants.DEBUG_SKIP_STUB,
        gradle = "debugSkipStub", form = BoundFlag.Parser.FQMND_SET,
        doc = "Skip specific stub classes/methods")
    public Set<FullyQualifiedMemberNameAndDesc> debugSkipStub = new HashSet<>();

    /**
     * set generated methods to not be synthetic
     *
     * @since 1.2.3
     */
    @BoundFlag(key = "Debug-No-Synthetic", cli = "--noSynthetic", aliases = "-ns",
        cliConst = "true", command = "debug", sysprop = Constants.DEBUG_NO_SYNTHETIC,
        gradle = "debugNoSynthetic", doc = "Generated methods are not marked synthetic")
    public boolean debugNoSynthetic = false;

    /**
     * disables {@link xyz.wagyourtail.jvmdg.pipeline.FusibleTransform} fusion —
     * adjacent fusible transforms run sequentially. The sequential path is
     * the correctness oracle; set this to bisect a fused-output difference.
     *
     * @since 3.0.0
     */
    @BoundFlag(key = "Debug-No-Fuse", sysprop = Constants.DEBUG_NO_FUSE,
        doc = "Disable fused transforms (sequential oracle path)")
    public boolean debugNoFuse = false;

    /**
     * internal debugging flag, disable loading the {@code META-INF/jvmdg/stubs.idx}
     * index — forces the reflective stub scan even when the api jar ships an index
     */
    @BoundFlag(key = "Debug-No-Stub-Idx", sysprop = Constants.DEBUG_NO_STUB_IDX,
        doc = "Force reflective stub scan even when the api jar ships an index")
    public boolean debugNoStubIdx = false;
    /**
     * if should move the original class file to the multi-release directory
     *
     * @since 1.0.0
     */
    @BoundFlag(key = "Multi-Release-Original", cli = "--multiReleaseOriginal",
        aliases = "-mro", cliConst = "true", sysprop = Constants.MULTI_RELEASE_ORIGINAL,
        gradle = "multiReleaseOriginal",
        doc = "Move the original class file to the multi-release directory")
    public boolean multiReleaseOriginal = false;
    /**
     * if should provide extra multi-release versions. As class versions, not java versions.
     *
     * @since 1.0.0
     */
    @BoundFlag(key = "Multi-Release-Versions", cli = "--multiRelease", aliases = "-mr",
        argNames = {"version"}, sysprop = Constants.MULTI_RELEASE_VERSIONS,
        gradle = "multiReleaseVersions", form = BoundFlag.Parser.JAVA_VERSION_SET,
        legacyForm = BoundFlag.Parser.CLASS_VERSION_SET,
        doc = "Extra multi-release versions (java major in config, class version on CLI)")
    public Set<Integer> multiReleaseVersions = new HashSet<>();

    /**
     * if should downgrade classes from the multi-release folder, instead of selecting the normal one
     *
     * @since 1.2.1
     */
    @BoundFlag(key = "Multi-Release-Inputs", cli = "--multiReleaseInputs", aliases = "-mri",
        cliConst = "true", gradle = "downgradeFromMultiReleases",
        doc = "Downgrade classes from the multi-release folder instead of the normal ones")
    public boolean downgradeFromMultiReleases = false;

    /**
     * prefix used when shading api classes into the output — the {@code shade}
     * command's {@code --prefix} argument overrides it
     *
     * @since 3.0.0
     */
    @BoundFlag(key = "Shade-Prefix", gradle = "shadePrefix", form = BoundFlag.Parser.STRING,
        doc = "Prefix for shaded api classes")
    public String shadePrefix = null;

    /**
     * disables reading {@code META-INF/jvmdg/*.mf} rules embedded in input jars
     *
     * @since 3.0.0
     */
    @BoundFlag(key = "Ignore-Embedded-Rules", cli = "--ignoreEmbeddedRules", cliConst = "true",
        doc = "Do not apply META-INF/jvmdg/*.mf rules embedded in input jars")
    public boolean ignoreEmbeddedRules = false;

    /**
     * keeps {@code META-INF/jvmdg/*.mf} rule files in the output jar instead of
     * stripping them (lets a downgraded library re-export its rules)
     *
     * @since 3.0.0
     */
    @BoundFlag(key = "Keep-Embedded-Rules", cli = "--keepEmbeddedRules", cliConst = "true",
        doc = "Copy META-INF/jvmdg/*.mf rules into the output instead of stripping them")
    public boolean keepEmbeddedRules = false;

    /**
     * {@code Name:}-scoped config sections, in file order — resolved per class
     * (most specific matching pattern wins) at use sites. Internal state, not
     * an option surface.
     *
     * @since 3.0.0
     */
    @BoundFlag(key = "")
    public List<xyz.wagyourtail.jvmdg.config.ScopedRule> scopedRules = new ArrayList<>();

    public Flags() {
        FlagBindings.applySysprops(this);
    }

    public Flags copy() {
        Flags flags = new Flags();
        FlagBindings.copyInto(this, flags);
        return flags;
    }

    public Flags copy(Consumer<Flags> modifier) {
        Flags flags = copy();
        modifier.accept(flags);
        return flags;
    }

    @Override
    public String toString() {
        return FlagBindings.describe(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return FlagBindings.flagsEqual(this, (Flags) o);
    }

    @Override
    public int hashCode() {
        return FlagBindings.flagsHash(this);
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
        if (className.isEmpty()) {
            return false;
        }
        // find entries that are <= the className; any matching candidate is a
        // prefix of className. a nearer non-matching entry must not shadow a
        // broader one (eg. "a/b/**" should still match "a/b/c/d" even when
        // "a/b/c" is also registered), so walk the floor chain until the first
        // character can't match anymore.
        Map.Entry<String, Flags.WildcardType> entry = ignoreWarningsIn.floorEntry(className);
        while (entry != null && entry.getKey().charAt(0) == className.charAt(0)) {
            if (className.startsWith(entry.getKey()) && wildcardMatches(entry.getValue(), entry.getKey(), className)) {
                return true;
            }
            entry = ignoreWarningsIn.lowerEntry(entry.getKey());
        }
        if (!scopedRules.isEmpty()) {
            // a matching Name:-scoped section with Ignore-Warnings: true
            // suppresses warnings for the class — an equal-specificity
            // conflict surfaces here since it can't be checked eagerly
            try {
                xyz.wagyourtail.jvmdg.config.ScopedRule rule =
                    xyz.wagyourtail.jvmdg.config.ScopedRule.resolve(scopedRules, className);
                return rule != null &&
                    Boolean.parseBoolean(rule.attributes.get("ignore-warnings"));
            } catch (xyz.wagyourtail.jvmdg.config.ManifestConfigReader.ConfigException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return false;
    }

    private static boolean wildcardMatches(WildcardType type, String key, String className) {
        switch (type) {
            case NONE:
                return className.equals(key);
            case SINGLE:
                int lastSlash = className.lastIndexOf('/');
                if (lastSlash == -1) {
                    return true;
                }
                String parent = className.substring(0, lastSlash + 1);
                String value = key.endsWith("/") ? key : key.substring(0, key.lastIndexOf('/') + 1);
                return parent.equals(value);
            case DOUBLE:
                return true;
            default:
                return false;
        }
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
        if (version == null) {
            // running from raw class dirs (dev/tests) — no artifact
            // coordinates to resolve, same as any other not-found path
            return null;
        }
        if (version.contains("SNAPSHOT")) {
            // retrieve maven metadata
            URL url = URI.create("https://maven.wagyourtail.xyz/snapshots/xyz/wagyourtail/jvmdowngrader/jvmdowngrader-java-api/" + version + "/maven-metadata.xml").toURL();
            // get actual latest — parse as plain XML; XMLDecoder is a
            // deserialization codec and unsafe on remote input
            try (InputStream in = url.openStream()) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
                try {
                    dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                } catch (ParserConfigurationException ignored) {
                }
                dbf.setExpandEntityReferences(false);
                Document doc = dbf.newDocumentBuilder().parse(in);
                NodeList timestamp = doc.getElementsByTagName("timestamp");
                NodeList buildNumber = doc.getElementsByTagName("buildNumber");
                if (timestamp.getLength() == 0 || buildNumber.getLength() == 0) {
                    throw new IOException("maven-metadata.xml missing snapshot timestamp/buildNumber");
                }
                String snapshotVersion = timestamp.item(0).getTextContent().trim() + "-" + buildNumber.item(0).getTextContent().trim();
                return URI.create("https://maven.wagyourtail.xyz/snapshots/xyz/wagyourtail/jvmdowngrader/jvmdowngrader-java-api/" + version + "/jvmdowngrader-java-api-" + version + "-" + snapshotVersion + ".jar").toURL();
            } catch (ParserConfigurationException | SAXException e) {
                throw new IOException("failed to parse maven-metadata.xml", e);
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
            // fetch the published sha1 for integrity verification (optional —
            // older artifacts may not publish one)
            String expectedSha1 = null;
            try (InputStream shaIn = URI.create(url.toString() + ".sha1").toURL().openStream()) {
                expectedSha1 = readFirstToken(shaIn);
            } catch (IOException ignored) {
            }
            // download
            try (InputStream in = url.openStream()) {
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            if (expectedSha1 != null) {
                try (InputStream verify = Files.newInputStream(file.toPath())) {
                    if (!expectedSha1.equalsIgnoreCase(hash(verify))) {
                        file.delete();
                        throw new IOException("downloaded java-api jar failed sha1 verification: " + file);
                    }
                }
            }
            return file.toURI().toURL();
        }
    }

    /** Reads the first whitespace-delimited token (maven {@code .sha1} files are "{@code <hex> <name>}"). */
    private static String readFirstToken(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1 && !Character.isWhitespace(c)) {
            sb.append((char) c);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String hash(InputStream is) {
        try {
            return Utils.getSha1(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash", e);
        }
    }

    public enum WildcardType {
        NONE,
        SINGLE,
        DOUBLE
    }

}
