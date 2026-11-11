package xyz.wagyourtail.jvmdg.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reader for {@code jvmdg.mf} — JAR-manifest syntax with config-file
 * semantics. Deliberately not {@link java.util.jar.Manifest}: that class loses
 * named-section order ({@code HashMap}), silently merges duplicate sections,
 * drops the last line of a file missing a trailing newline, rejects lines
 * over 512 bytes, and reports no error locations. This reader accepts the
 * same surface syntax — {@code Key: value}, blank line separates sections,
 * {@code Name:} opens a named section, a line starting with one space
 * continues the previous value — while preserving section order, rejecting
 * duplicate sections and keys, accepting a missing final newline, imposing
 * no line-length limit, and reporting {@code source:line} in errors.
 *
 * <p>Files that stay within manifest limits remain readable by
 * {@code java.util.jar.Manifest} as well.
 */
public final class ManifestConfigReader {

    /** Parse error carrying the source name and 1-based line number. */
    public static final class ConfigException extends IOException {
        public final String source;
        public final int line;

        public ConfigException(String source, int line, String message) {
            super(source + ":" + line + ": " + message);
            this.source = source;
            this.line = line;
        }
    }

    /** One section of a parsed file: ordered, case-insensitive-keyed attributes. */
    public static final class Section {
        /** The {@code Name:} value for named sections; null for the main section. */
        public final String name;
        /** 1-based line the section opened at (0 for the main section). */
        public final int line;
        /** Attributes in file order; keys canonicalized to lower-case for lookup. */
        public final LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        /** Original-case key spellings, for error/print fidelity. */
        private final Map<String, String> keySpelling = new LinkedHashMap<>();

        private Section(String name, int line) {
            this.name = name;
            this.line = line;
        }

        public String get(String key) {
            return attributes.get(canonical(key));
        }

        public String keySpelling(String key) {
            return keySpelling.get(canonical(key));
        }

        static String canonical(String key) {
            return key.toLowerCase(Locale.ROOT);
        }
    }

    /** A fully parsed config file: main section + named sections in file order. */
    public static final class ConfigFile {
        public final Section main;
        public final List<Section> named;
        public final String source;

        private ConfigFile(String source, Section main, List<Section> named) {
            this.source = source;
            this.main = main;
            this.named = Collections.unmodifiableList(named);
        }
    }

    public static ConfigFile read(Path file) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(r, file.toString());
        }
    }

    /**
     * Parse manifest-syntax content. {@code source} names the input in errors
     * (a path, or {@code "jar:entry"} for embedded rules).
     */
    public static ConfigFile read(Reader input, String source) throws IOException {
        BufferedReader r = input instanceof BufferedReader ? (BufferedReader) input : new BufferedReader(input);
        Section main = new Section(null, 0);
        List<Section> named = new ArrayList<>();
        Section current = main;
        String lastKey = null;
        int line = 0;
        String raw;
        while ((raw = r.readLine()) != null) {
            line++;
            if (raw.isEmpty()) {
                lastKey = null;
                continue;
            }
            if (raw.charAt(0) == ' ') {
                if (lastKey == null) {
                    throw new ConfigException(source, line, "continuation line without a preceding attribute");
                }
                String canon = Section.canonical(lastKey);
                current.attributes.put(canon, current.attributes.get(canon) + raw.substring(1));
                continue;
            }
            int colon = raw.indexOf(':');
            if (colon <= 0) {
                throw new ConfigException(source, line, "expected 'Key: value' or a continuation line, got: " + raw);
            }
            String key = raw.substring(0, colon).trim();
            validateKey(key, source, line);
            String value = raw.substring(colon + 1).trim();
            String canon = Section.canonical(key);
            if (current.attributes.containsKey(canon)) {
                throw new ConfigException(source, line, "duplicate key '" + key + "' in " +
                    (current.name == null ? "main section" : "section '" + current.name + "'"));
            }
            if ("name".equals(canon)) {
                if (value.isEmpty()) {
                    throw new ConfigException(source, line, "empty Name: section pattern");
                }
                current = new Section(value, line);
                named.add(current);
                lastKey = null;
                continue;
            }
            current.attributes.put(canon, value);
            current.keySpelling.put(canon, key);
            lastKey = key;
        }
        // duplicate section names are rejected — silently merging them would
        // hide a typo'd scope the user thought was separate
        Map<String, Integer> seen = new LinkedHashMap<>();
        for (Section s : named) {
            String canon = s.name.toLowerCase(Locale.ROOT);
            Integer prev = seen.put(canon, s.line);
            if (prev != null) {
                throw new ConfigException(source, s.line,
                    "duplicate section name '" + s.name + "' (first defined at line " + prev + ")");
            }
        }
        return new ConfigFile(source, main, named);
    }

    /** Manifest key rules: first char alphanumeric, then alnum/{@code -}/{@code _}. */
    private static void validateKey(String key, String source, int line) throws ConfigException {
        if (key.isEmpty() || !Character.isLetterOrDigit(key.charAt(0))) {
            throw new ConfigException(source, line, "invalid key '" + key + "'");
        }
        for (int i = 1; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_') {
                throw new ConfigException(source, line, "invalid character '" + c + "' in key '" + key + "'");
            }
        }
    }

    private ManifestConfigReader() {
    }
}
