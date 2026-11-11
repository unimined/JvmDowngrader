package xyz.wagyourtail.jvmdg.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a {@link xyz.wagyourtail.jvmdg.cli.Flags} field to its option surface:
 * manifest config key, CLI flag, system property, and Gradle DSL property.
 * {@link FlagBindings} reflects over the annotated fields and drives sysprop
 * initialization, config-file application, CLI parsing, and
 * copy/equals/hashCode/toString — adding a field without the annotation fails
 * the binding-completeness test.
 *
 * <p>Repeatable: the primary annotation is the value binding; additional
 * annotations may declare constant-setting CLI flags (e.g. {@code --noColor}
 * sets {@code logAnsiColors = false} via {@link #cliConst()}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(BoundFlag.List.class)
public @interface BoundFlag {

    /**
     * Manifest config-file key (e.g. {@code "Target"}). Case-insensitive per
     * manifest rules; letters/digits/{@code -}/{@code _} only.
     */
    String key();

    /**
     * Primary CLI flag name (e.g. {@code "--classVersion"}). Empty for fields
     * that have no value-taking CLI flag.
     */
    String cli() default "";

    /** Short-form aliases for {@link #cli()} (e.g. {@code "-c"}). */
    String[] aliases() default {};

    /**
     * Constant value assigned when {@link #cli()} is present without arguments
     * (e.g. {@code "false"} for {@code --noColor}). Empty means the flag takes
     * a value argument.
     */
    String cliConst() default "";

    /** Subcommand the CLI flag lives under (e.g. {@code "debug"}); empty = top level. */
    String command() default "";

    /** Argument names for help output (e.g. {@code {"version"}}). */
    String[] argNames() default {};

    /**
     * System property this field initializes from; empty = no sysprop binding.
     * A set sysprop always outranks config-file values.
     */
    String sysprop() default "";

    /** DowngradeFlags/ShadeFlags property name on the Gradle DSL; empty = not exposed. */
    String gradle() default "";

    /** May appear in a {@code Name:} scoped section of a config file. */
    boolean scoped() default false;

    /** Deprecated option — kept for compat; excluded from generated docs. */
    boolean deprecated() default false;

    /** One-line description for CLI help / generated config docs. */
    String doc() default "";

    /**
     * Value parser for config-file / Gradle text. {@link Parser#AUTO} infers
     * from the field type (scalars, enums, {@code TreeMap}); collection fields
     * must name it explicitly since the element type isn't inferable.
     */
    Parser form() default Parser.AUTO;

    /**
     * Value parser for system properties and CLI tokens. Differs from
     * {@link #form()} only where legacy surfaces take class versions ({@code 52})
     * while the config file takes Java versions ({@code 8}).
     */
    Parser legacyForm() default Parser.AUTO;

    /** Parser selection — resolves to a {@link FlagParser} singleton. */
    enum Parser {
        AUTO(null),
        BOOLEAN(FlagParser.BOOLEAN),
        INT(FlagParser.INT),
        JAVA_VERSION(FlagParser.JAVA_VERSION),
        STRING(FlagParser.STRING),
        LOG_LEVEL(FlagParser.LOG_LEVEL),
        CLASS_VERSION_SET(FlagParser.CLASS_VERSION_SET),
        JAVA_VERSION_SET(FlagParser.JAVA_VERSION_SET),
        JEP_SET(FlagParser.JEP_SET),
        FQMND_SET(FlagParser.FQMND_SET),
        FILE_LIST(FlagParser.FILE_LIST),
        WILDCARD_MAP(FlagParser.WILDCARD_MAP),
        INTERNAL(null);

        final FlagParser instance;

        Parser(FlagParser instance) {
            this.instance = instance;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface List {
        BoundFlag[] value();
    }
}
