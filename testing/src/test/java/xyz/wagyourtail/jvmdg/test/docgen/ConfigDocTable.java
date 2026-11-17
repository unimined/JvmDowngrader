package xyz.wagyourtail.jvmdg.test.docgen;

import xyz.wagyourtail.jvmdg.config.BoundFlag;
import xyz.wagyourtail.jvmdg.config.FlagBindings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the flag-keys table in docs/Configuration.md from
 * {@link FlagBindings#ALL} — the doc mirrors the {@link BoundFlag} surface
 * exactly, so it is emitted rather than hand-maintained. The table lives
 * between {@code <!-- flag-table:begin -->} / {@code <!-- flag-table:end -->}
 * markers; {@code genConfigDocs} rewrites that section in place and
 * {@link xyz.wagyourtail.jvmdg.config.TestFlagBindings} asserts it is current.
 */
public final class ConfigDocTable {

    public static final String BEGIN = "<!-- flag-table:begin -->";
    public static final String END = "<!-- flag-table:end -->";

    /** The generated table — header row + one row per non-internal binding. */
    public static String table() {
        StringBuilder sb = new StringBuilder();
        sb.append("| Key | CLI flag | System property | Gradle DSL | Form |\n");
        sb.append("|---|---|---|---|---|\n");
        for (FlagBindings.Binding b : FlagBindings.ALL) {
            if (b.internal()) continue;
            BoundFlag primary = b.primary();
            sb.append("| `").append(primary.key().isEmpty() ? "—" : primary.key()).append('`');
            if (primary.deprecated()) sb.append(" *(deprecated)*");
            sb.append(" | ").append(cliColumn(b));
            sb.append(" | ").append(primary.sysprop().isEmpty() ? "—" : "`" + primary.sysprop() + "`");
            String gradle = primary.gradle().isEmpty() ? "—" : "`" + primary.gradle()
                + (primary.gradleType().isEmpty() ? "" : ": " + primary.gradleType()) + "`";
            sb.append(" | ").append(gradle);
            sb.append(" | ").append(formLabel(b));
            sb.append(" |\n");
        }
        return sb.toString();
    }

    private static String cliColumn(FlagBindings.Binding b) {
        List<String> flags = new ArrayList<>();
        for (BoundFlag m : b.metas) {
            if (m.cli().isEmpty()) continue;
            StringBuilder s = new StringBuilder("`");
            if (!m.command().isEmpty()) s.append(m.command()).append(' ');
            s.append(m.cli()).append('`');
            for (String alias : m.aliases()) s.append(" / `").append(alias).append('`');
            if (!m.cliConst().isEmpty()) s.append(" (const-").append(m.cliConst()).append(')');
            flags.add(s.toString());
        }
        return flags.isEmpty() ? "—" : String.join(", ", flags);
    }

    /** `{config form}` or `{config form} / {legacy form}` when they differ. */
    private static String formLabel(FlagBindings.Binding b) {
        BoundFlag m = b.primary();
        String config = parserLabel(m.form() == BoundFlag.Parser.AUTO ? autoForm(b.field.getType()) : m.form(), b);
        BoundFlag.Parser legacy = m.legacyForm();
        if (legacy != BoundFlag.Parser.AUTO && legacy != m.form()) {
            String legacyLabel = parserLabel(legacy, b);
            if (!legacyLabel.equals(config)) return config + " / " + legacyLabel;
        }
        return config;
    }

    private static BoundFlag.Parser autoForm(Class<?> type) {
        if (type == boolean.class) return BoundFlag.Parser.BOOLEAN;
        if (type == int.class) return BoundFlag.Parser.INT;
        if (type == String.class) return BoundFlag.Parser.STRING;
        if (type.isEnum()) return BoundFlag.Parser.LOG_LEVEL;
        if (java.util.Map.class.isAssignableFrom(type)) return BoundFlag.Parser.WILDCARD_MAP;
        throw new IllegalStateException("no default form for " + type);
    }

    private static String parserLabel(BoundFlag.Parser p, FlagBindings.Binding b) {
        switch (p) {
            case BOOLEAN: return "bool";
            case INT: return "int";
            case JAVA_VERSION: return "java-version";
            case STRING: return "string";
            case FILE_LIST: return "path-list";
            case WILDCARD_MAP: return "wildcard-map";
            case JAVA_VERSION_SET:
            case CLASS_VERSION_SET: return "version-set";
            case JEP_SET: return "int-set";
            case FQMND_SET: return "member-set";
            case INTERNAL: return "—";
            default: break;
        }
        // enum-valued parsers (LOG_LEVEL, STUB_POLICY, ON_MISSING_API,
        // SHADE_REFLECTION): short enums spell their values, long ones say `enum`
        Class<?> type = b.field.getType();
        if (!type.isEnum()) return "enum";
        Object[] constants = type.getEnumConstants();
        if (constants.length > 5) return "enum";
        StringBuilder sb = new StringBuilder();
        for (Object c : constants) {
            if (sb.length() > 0) sb.append('/');
            sb.append('`').append(lowerCamel(((Enum<?>) c).name())).append('`');
        }
        return sb.toString();
    }

    /** {@code NO_BEHAVIOR} → {@code noBehavior} — the config-file spelling. */
    private static String lowerCamel(String enumName) {
        StringBuilder sb = new StringBuilder();
        String[] parts = enumName.toLowerCase(java.util.Locale.ROOT).split("_");
        sb.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    /** The table text currently inside the markers in {@code docFile}. */
    public static String currentSection(Path docFile) throws IOException {
        String text = new String(Files.readAllBytes(docFile), StandardCharsets.UTF_8);
        int i = text.indexOf(BEGIN);
        int j = text.indexOf(END);
        if (i < 0 || j < 0 || j < i) throw new IllegalStateException("flag-table markers not found in " + docFile);
        return text.substring(i + BEGIN.length(), j).trim();
    }

    /** Rewrites the marked section of docs/Configuration.md in place. */
    public static void main(String[] args) throws IOException {
        Path doc = Paths.get(args[0]);
        String text = new String(Files.readAllBytes(doc), StandardCharsets.UTF_8);
        int i = text.indexOf(BEGIN);
        int j = text.indexOf(END);
        if (i < 0 || j < 0 || j < i) throw new IllegalStateException("flag-table markers not found in " + doc);
        String out = text.substring(0, i + BEGIN.length()) + "\n" + table() + "\n" + text.substring(j);
        Files.write(doc, out.getBytes(StandardCharsets.UTF_8));
        System.out.println("regenerated flag table in " + doc);
    }

    private ConfigDocTable() {
    }
}
