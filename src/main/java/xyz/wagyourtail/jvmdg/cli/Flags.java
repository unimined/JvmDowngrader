package xyz.wagyourtail.jvmdg.cli;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.Constants;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Flags {
    public static final String jvmdgVersion = Flags.class.getPackage().getImplementationVersion();

    public int classVersion = Opcodes.V1_8;
    public File api = null;
    public boolean quiet = Boolean.getBoolean(Constants.QUIET);
    public boolean allowMaven = Boolean.getBoolean(Constants.ALLOW_MAVEN_LOOKUP);

    // debug
    public boolean printDebug = Boolean.getBoolean(Constants.DEBUG);
    public Set<Integer> debugSkipStubs = new HashSet<>(getDebugSkip());

    public Flags copy() {
        Flags flags = new Flags();
        flags.classVersion = classVersion;
        flags.api = api;
        flags.quiet = quiet;
        flags.allowMaven = allowMaven;
        flags.printDebug = printDebug;
        flags.debugSkipStubs = new HashSet<>(debugSkipStubs);
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
            ", allowMaven=" + allowMaven +
            ", printDebug=" + printDebug +
            ", debugSkipStubs=" + debugSkipStubs +
            '}';
    }

    private Set<Integer> getDebugSkip() {
        Set<Integer> skip = new HashSet<>();
        String skipStubs = System.getProperty(Constants.DEBUG_SKIP_STUBS);
        if (skipStubs == null) return skip;
        for (String s : skipStubs.split(",")) {
            skip.add(Integer.parseInt(s));
        }
        return skip;
    }

    private URL getJavaApiFromShade() throws IOException {
        return ClassDowngrader.class.getResource("/META-INF/lib/java-api.jar");
    }

    private File getJavaApiFromSystemProperty() throws IOException {
        String api = System.getProperty(Constants.JAVA_API);
        if (api == null) {
            return null;
        }
        return new File(api);
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

    public File findJavaApi() {
        try {
            if (api != null) {
                return api;
            }
            Constants.DIR.mkdirs();
            Path tmp = Constants.DIR.toPath().resolve("jvmdg-api.jar");
            File prop = getJavaApiFromSystemProperty();
            if (prop != null) {
                return prop;
            }
            URL url = getJavaApiFromShade();
            if (url == null && allowMaven) {
                url = getJavaApiFromMaven();
            }
            if (url != null) {
                if (Files.exists(tmp)) {
                    try (InputStream in = url.openStream()) {
                        try (InputStream in2 = Files.newInputStream(tmp)) {
                            if (hash(in).equals(hash(in2))) {
                                return tmp.toFile();
                            }
                        }
                    }
                }
                try (InputStream in = url.openStream()) {
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                return tmp.toFile();
            } else {
                // failed to find java api
                if (!quiet) {
                    System.err.println("[JvmDowngrader] Failed to find java api jar!");
                }
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to find java api", e);
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
}
