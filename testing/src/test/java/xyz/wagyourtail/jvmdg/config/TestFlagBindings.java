package xyz.wagyourtail.jvmdg.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.config.ManifestConfigReader.ConfigException;
import xyz.wagyourtail.jvmdg.config.ManifestConfigReader.ConfigFile;
import xyz.wagyourtail.jvmdg.logging.Logger;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Flags() reads system properties — serialize with the other flag tests so
// TestFlags' prop mutations can't race these
@Execution(ExecutionMode.SAME_THREAD)
public class TestFlagBindings {

    private static ConfigFile parse(String text) throws Exception {
        return ManifestConfigReader.read(new StringReader(text), "test.mf");
    }

    @Test
    public void everyPublicInstanceFieldIsBound() {
        // FlagBindings.<clinit> already throws for unbound fields; this asserts
        // the gate ran and every field is reachable through the index
        java.util.Set<String> bound = new java.util.HashSet<>();
        for (FlagBindings.Binding b : FlagBindings.ALL) {
            bound.add(b.field.getName());
        }
        int fields = 0;
        for (Field f : Flags.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            fields++;
            assertTrue(bound.contains(f.getName()), f.getName() + " missing @BoundFlag");
        }
        assertEquals(fields, FlagBindings.ALL.size());
    }

    @Test
    public void boundFieldsCoverCopyEqualsHash() {
        Flags a = new Flags();
        a.classVersion = 61;
        a.api = java.util.Collections.singletonList(new java.io.File("x.jar"));
        a.debugSkipJeps.add(400);
        a.debugSkipStub.add(xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc.of("Ljava/lang/System;"));
        a.shadePrefix = "pre/fix/";
        a.debugNoFuse = true;
        Flags b = a.copy();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        // deep-copied: mutating the copy's collections leaves the original
        b.debugSkipJeps.add(401);
        assertNotEquals(a, b);
        assertFalse(a.debugSkipJeps.contains(401));
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    public void syspropInitComesFromBindings() {
        String prop = Constants.DEBUG_SKIP_JEPS;
        String prev = System.getProperty(prop);
        try {
            System.setProperty(prop, "400,410");
            Flags f = new Flags();
            assertTrue(f.debugSkipJeps.contains(400));
            assertTrue(f.debugSkipJeps.contains(410));
        } finally {
            if (prev == null) System.clearProperty(prop);
            else System.setProperty(prop, prev);
        }
    }

    @Test
    public void javaVersionFormTranslatesToClassVersion() throws Exception {
        Flags f = new Flags();
        ConfigFile c = parse("Jvmdg-Config-Version: 1\nTarget: 11\n");
        JvmdgConfig.apply(c, f, null);
        assertEquals(55, f.classVersion);
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    public void syspropOutranksConfig() throws Exception {
        String prop = Constants.CLASS_VERSION;
        String prev = System.getProperty(prop);
        try {
            System.setProperty(prop, "52");
            Flags f = new Flags();
            JvmdgConfig.apply(parse("Jvmdg-Config-Version: 1\nTarget: 11\n"), f, null);
            assertEquals(52, f.classVersion); // config could not override the sysprop
        } finally {
            if (prev == null) System.clearProperty(prop);
            else System.setProperty(prop, prev);
        }
    }

    @Test
    public void laterConfigFileWins() throws Exception {
        Flags f = new Flags();
        ConfigFile a = parse("Jvmdg-Config-Version: 1\nTarget: 8\nLog-Level: warn\n");
        ConfigFile b = parse("Jvmdg-Config-Version: 1\nTarget: 11\n");
        JvmdgConfig.apply(a, f, null);
        JvmdgConfig.apply(b, f, null);
        assertEquals(55, f.classVersion);
        assertEquals(Logger.Level.WARN, f.logLevel);
    }

    @Test
    public void unknownKeyIsAnError() {
        ConfigException e = assertThrows(ConfigException.class,
            () -> JvmdgConfig.apply(parse("Jvmdg-Config-Version: 1\nTargte: 8\n"), new Flags(), null));
        assertTrue(e.getMessage().contains("unknown config key"), e.getMessage());
    }

    @Test
    public void missingVersionHeaderIsAnError() {
        assertThrows(ConfigException.class,
            () -> JvmdgConfig.apply(parse("Target: 8\n"), new Flags(), null));
    }

    @Test
    public void unsupportedVersionIsAnError() {
        assertThrows(ConfigException.class,
            () -> JvmdgConfig.apply(parse("Jvmdg-Config-Version: 2\nTarget: 8\n"), new Flags(), null));
    }

    @Test
    public void globalKeyInNamedSectionIsAnError() {
        ConfigException e = assertThrows(ConfigException.class,
            () -> JvmdgConfig.apply(parse(
                "Jvmdg-Config-Version: 1\n\nName: com/example/**\nTarget: 8\n"), new Flags(), null));
        assertTrue(e.getMessage().contains("global-only"), e.getMessage());
    }

    @Test
    public void namedSectionsBecomeScopedRules() throws Exception {
        Flags f = new Flags();
        JvmdgConfig.apply(parse(
            "Jvmdg-Config-Version: 1\n" +
            "\n" +
            "Name: com/example/**\n" +
            "Ignore-Warnings: true\n" +
            "\n" +
            "Name: com/other/*\n" +
            "Ignore-Warnings: false\n"), f, null);
        assertEquals(2, f.scopedRules.size());
        ScopedRule first = f.scopedRules.get(0);
        assertEquals("com/example/", first.pattern);
        assertEquals(Flags.WildcardType.DOUBLE, first.wildcard);
        assertEquals("true", first.attributes.get("ignore-warnings"));
        assertTrue(first.matches("com/example/sub/Thing"));
        assertFalse(first.matches("com/other/Thing"));
    }

    @Test
    public void mostSpecificScopeWins() throws Exception {
        Flags f = new Flags();
        JvmdgConfig.apply(parse(
            "Jvmdg-Config-Version: 1\n" +
            "\n" +
            "Name: com/**\n" +
            "Ignore-Warnings: false\n" +
            "\n" +
            "Name: com/example/**\n" +
            "Ignore-Warnings: true\n"), f, null);
        // declared com/** first — resolution is order-independent
        ScopedRule winner = ScopedRule.resolve(f.scopedRules, "com/example/sub/Thing");
        assertNotNull(winner);
        assertEquals("true", winner.attributes.get("ignore-warnings"));
        assertTrue(f.checkInIgnoreWarnings("com/example/sub/Thing"));
    }

    @Test
    public void narrowerWildcardBeatsWiderOnSamePrefix() throws Exception {
        Flags f = new Flags();
        JvmdgConfig.apply(parse(
            "Jvmdg-Config-Version: 1\n" +
            "\n" +
            "Name: com/example/*\n" +
            "Ignore-Warnings: false\n" +
            "\n" +
            "Name: com/example/Thing\n" +
            "Ignore-Warnings: true\n"), f, null);
        // 'com/example/Thing' (NONE) beats 'com/example/*' (SINGLE)
        ScopedRule r = ScopedRule.resolve(f.scopedRules, "com/example/Thing");
        assertNotNull(r);
        assertEquals("true", r.attributes.get("ignore-warnings"));
    }

    @Test
    public void equallySpecificMatchingRulesConflict() {
        // same stripped prefix + same wildcard = identical specificity; when
        // both match a class the resolver must error rather than pick one
        ScopedRule a = new ScopedRule("com/a/", Flags.WildcardType.DOUBLE, "x.mf", null, 1, java.util.Collections.emptyMap());
        ScopedRule b = new ScopedRule("com/a/", Flags.WildcardType.DOUBLE, "y.mf", null, 2, java.util.Collections.emptyMap());
        assertThrows(ConfigException.class,
            () -> ScopedRule.resolve(java.util.Arrays.asList(a, b), "com/a/Thing"));
    }

    @Test
    public void scopedIgnoreWarningsSuppresses() throws Exception {
        Flags f = new Flags();
        JvmdgConfig.apply(parse(
            "Jvmdg-Config-Version: 1\n" +
            "\n" +
            "Name: com/example/**\n" +
            "Ignore-Warnings: true\n" +
            "\n" +
            "Name: com/other/**\n" +
            "Ignore-Warnings: false\n"), f, null);
        assertTrue(f.checkInIgnoreWarnings("com/example/sub/Thing"));
        assertFalse(f.checkInIgnoreWarnings("com/other/Thing"));
        assertFalse(f.checkInIgnoreWarnings("com/third/Thing"));
        // global Ignore-Warnings-In still works alongside scoped rules
        f.addIgnore("com/third/**");
        assertTrue(f.checkInIgnoreWarnings("com/third/Thing"));
    }

    @Test
    public void ignoreWarningsInIsGlobalOnly() {
        // the pattern-map key makes no sense inside a scope — the boolean
        // Ignore-Warnings is the scoped form
        ConfigException e = assertThrows(ConfigException.class,
            () -> JvmdgConfig.apply(parse(
                "Jvmdg-Config-Version: 1\n\nName: com/example/**\nIgnore-Warnings-In: com/x/**\n"),
                new Flags(), null));
        assertTrue(e.getMessage().contains("global-only"), e.getMessage());
    }

    @Test
    public void printConfigShowsSources(@TempDir Path dir) throws Exception {
        Path cfg = dir.resolve("jvmdg.mf");
        Files.write(cfg, "Jvmdg-Config-Version: 1\nLog-Level: error\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Flags f = new Flags();
        Map<String, FlagBindings.Source> sources = new LinkedHashMap<>();
        JvmdgConfig.applyFiles(java.util.Collections.singletonList(cfg), f, sources);
        String out = FlagBindings.printConfig(f, sources);
        assertTrue(out.contains("Log-Level"), out);
        assertTrue(out.contains("(config)"), out);
        assertTrue(out.contains("Target"), out);
        assertTrue(out.contains("(default)"), out);
    }

    @Test
    public void everyBoundKeyAndCliFlagIsDocumented() throws Exception {
        // the key table in docs/Configuration.md is generated knowledge —
        // this is the drift ratchet: adding a flag without documenting it fails
        java.nio.file.Path doc = java.nio.file.Paths.get("docs/Configuration.md");
        if (!Files.exists(doc)) doc = java.nio.file.Paths.get("../docs/Configuration.md");
        String text = new String(Files.readAllBytes(doc), java.nio.charset.StandardCharsets.UTF_8);
        for (FlagBindings.Binding b : FlagBindings.ALL) {
            for (BoundFlag m : b.metas) {
                if (!m.key().isEmpty()) {
                    assertTrue(text.contains("`" + m.key() + "`"),
                        "docs/Configuration.md missing key " + m.key());
                }
                if (!m.cli().isEmpty()) {
                    assertTrue(text.contains(m.cli()),
                        "docs/Configuration.md missing CLI flag " + m.cli());
                }
            }
        }
    }

    @Test
    public void cliFlagsResolveFromAnnotations() {
        assertNotNull(FlagBindings.byCliFlag("--classVersion"));
        assertNotNull(FlagBindings.byCliFlag("-c"));
        assertNotNull(FlagBindings.byCliFlag("--noColor"));
        assertNotNull(FlagBindings.byCliFlag("--quiet"));
        assertNull(FlagBindings.byCliFlag("--made-up-flag"));
        // metaForCli picks the right BoundFlag for a shared field
        FlagBindings.Binding colors = FlagBindings.byCliFlag("--noColor");
        assertEquals("false", colors.metaForCli("--noColor").cliConst());
    }

    @Test
    public void scopedRuleAccessorsParseTypedAttrs() throws Exception {
        ScopedRule rule = new ScopedRule("com/foo/", Flags.WildcardType.DOUBLE, "test", null, 1,
            new LinkedHashMap<String, String>() {{
                put("ignore-warnings", "true");
                put("exclude-stubs", "Ljava/lang/String;strip;()Ljava/lang/String;");
                put("exclude-jeps", "400 418");
                put("include-stubs", "Ljava/lang/System;getProperty;(Ljava/lang/String;)Ljava/lang/String;");
            }});
        assertTrue(rule.ignoreWarnings());
        assertTrue(rule.excludeStubs().contains(
            xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc.of(
                "Ljava/lang/String;strip;()Ljava/lang/String;")));
        assertEquals(1, rule.excludeStubs().size());
        assertTrue(rule.excludeJeps().contains(400));
        assertTrue(rule.excludeJeps().contains(418));
        assertTrue(rule.includeStubs().contains(
            xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc.of(
                "Ljava/lang/System;getProperty;(Ljava/lang/String;)Ljava/lang/String;")));
        // absent attrs are empty, not null
        ScopedRule empty = new ScopedRule("", Flags.WildcardType.DOUBLE, "test", null, 1,
            new LinkedHashMap<>());
        assertTrue(empty.excludeStubs().isEmpty());
        assertTrue(empty.excludeJeps().isEmpty());
        assertTrue(empty.includeStubs().isEmpty());
        assertFalse(empty.ignoreWarnings());
    }

    @Test
    public void scopedRuleBadAttrReportsSource() {
        ScopedRule bad = new ScopedRule("a/", Flags.WildcardType.DOUBLE, "cfg.mf", null, 7,
            new LinkedHashMap<String, String>() {{ put("exclude-jeps", "not-a-number"); }});
        ConfigException e = assertThrows(ConfigException.class, bad::excludeJeps);
        assertTrue(e.getMessage().contains("cfg.mf"), e.getMessage());
    }

    @Test
    public void excludeKeysAcceptedInNamedSections() throws Exception {
        // scoped Exclude-Stubs/Exclude-Jeps are consumed at emission —
        // named-section use must validate, not fail as unknown
        Flags f = new Flags();
        JvmdgConfig.apply(ManifestConfigReader.read(new StringReader(
            "Jvmdg-Config-Version: 1\n" +
            "\n" +
            "Name: com/example/**\n" +
            "Exclude-Stubs: Ljava/lang/String;strip;()Ljava/lang/String;\n" +
            "Include-Stubs: Ljava/lang/System;getProperty;(Ljava/lang/String;)Ljava/lang/String;\n" +
            "Exclude-Jeps: 400\n"), "test"), f, null);
        assertEquals(1, f.scopedRules.size());
        ScopedRule rule = f.scopedRules.get(0);
        assertFalse(rule.excludeStubs().isEmpty());
        assertTrue(rule.excludeJeps().contains(400));
    }
}
