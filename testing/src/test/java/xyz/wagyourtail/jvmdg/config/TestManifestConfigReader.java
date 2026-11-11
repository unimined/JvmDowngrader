package xyz.wagyourtail.jvmdg.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.wagyourtail.jvmdg.config.ManifestConfigReader.ConfigException;
import xyz.wagyourtail.jvmdg.config.ManifestConfigReader.ConfigFile;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

public class TestManifestConfigReader {

    private static ConfigFile parse(String text) throws Exception {
        return ManifestConfigReader.read(new StringReader(text), "test.mf");
    }

    @Test
    public void parsesMainAndNamedSectionsInOrder() throws Exception {
        ConfigFile f = parse(
            "Jvmdg-Config-Version: 1\n" +
            "Target: 8\n" +
            "\n" +
            "Name: com/example/**\n" +
            "Exclude-Jeps: 400\n" +
            "\n" +
            "Name: com/other/*\n" +
            "Exclude-Stubs: Ljava/lang/System;getProperty;(Ljava/lang/String;)Ljava/lang/String;\n");
        assertEquals("1", f.main.get("Jvmdg-Config-Version"));
        assertEquals("8", f.main.get("Target"));
        assertEquals(2, f.named.size());
        assertEquals("com/example/**", f.named.get(0).name);
        assertEquals("com/other/*", f.named.get(1).name);
        assertEquals("400", f.named.get(0).get("Exclude-Jeps"));
    }

    @Test
    public void continuationLinesAppendWithoutTheLeadingSpace() throws Exception {
        // manifest semantics: continuation content is appended directly —
        // reassembles a split token, matching java.util.jar.Manifest
        ConfigFile f = parse(
            "Jvmdg-Config-Version: 1\n" +
            "Exclude-Stubs: Ljava/lang/System;getProp\n" +
            " erty;(Ljava/lang/String;)Ljava/lang/Stri\n" +
            " ng;\n");
        assertEquals("Ljava/lang/System;getProperty;(Ljava/lang/String;)Ljava/lang/String;",
            f.main.get("Exclude-Stubs"));
        // a space-separated list: continuation lines lose exactly one leading
        // space, so a second space carries the separator into the value
        ConfigFile g = parse(
            "Jvmdg-Config-Version: 1\n" +
            "Exclude-Jeps: 400\n" +
            "  401\n" +
            "  402\n");
        assertEquals("400 401 402", g.main.get("Exclude-Jeps"));
    }

    @Test
    public void continuationWithoutAttributeIsAnError() {
        // a blank line resets the continuation anchor — ' stray' has nothing
        // to continue
        ConfigException e = assertThrows(ConfigException.class,
            () -> parse("Jvmdg-Config-Version: 1\n\n stray\n"));
        assertTrue(e.getMessage().contains("continuation"), e.getMessage());
        // same inside a fresh named section
        assertThrows(ConfigException.class,
            () -> parse("A: 1\n\nName: x/**\n cont\n"));
    }

    @Test
    public void missingFinalNewlineIsAccepted() throws Exception {
        ConfigFile f = parse("Jvmdg-Config-Version: 1\nTarget: 8");
        assertEquals("8", f.main.get("Target"));
    }

    @Test
    public void longLinesAreAccepted() throws Exception {
        StringBuilder value = new StringBuilder("X: ");
        for (int i = 0; i < 2000; i++) value.append('a');
        ConfigFile f = parse(value.toString());
        assertEquals(2000, f.main.get("X").length());
    }

    @Test
    public void duplicateKeyIsRejected() {
        ConfigException e = assertThrows(ConfigException.class,
            () -> parse("A: 1\nA: 2\n"));
        assertTrue(e.getMessage().contains("duplicate"), e.getMessage());
    }

    @Test
    public void duplicateSectionNameIsRejected() {
        ConfigException e = assertThrows(ConfigException.class,
            () -> parse("A: 1\n\nName: a/**\nB: 2\n\nName: a/**\nC: 3\n"));
        assertTrue(e.getMessage().contains("duplicate section"), e.getMessage());
    }

    @Test
    public void keysAreCaseInsensitive() throws Exception {
        ConfigFile f = parse("Jvmdg-Config-Version: 1\ntarget: 8\nTARGET-EXTRA: x\n");
        assertEquals("8", f.main.get("TARGET"));
        assertEquals("x", f.main.get("target-extra"));
    }

    @Test
    public void invalidKeysAreRejected() {
        assertThrows(ConfigException.class, () -> parse("my.key: 1\n"));
        assertThrows(ConfigException.class, () -> parse("-Key: 1\n"));
        assertThrows(ConfigException.class, () -> parse("no colon here\n"));
    }

    @Test
    public void errorsCarrySourceAndLine() {
        ConfigException e = assertThrows(ConfigException.class,
            () -> parse("A: 1\nB: 2\nbad line\n"));
        assertEquals(3, e.line);
        assertTrue(e.getMessage().startsWith("test.mf:3:"), e.getMessage());
    }

    @Test
    public void withinLimitsFileParsesIdenticallyViaJdkManifest(@TempDir Path dir) throws Exception {
        // a file that obeys the 72-byte manifest line convention must read the
        // same through java.util.jar.Manifest
        String text =
            "Jvmdg-Config-Version: 1\r\n" +
            "Target: 8\r\n" +
            "Exclude-Jeps: 400\r\n" +
            "\r\n" +
            "Name: com/example/**\r\n" +
            "Exclude-Stubs: Ljava/lang/System;\r\n";
        Path file = dir.resolve("jvmdg.mf");
        Files.write(file, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ConfigFile ours = ManifestConfigReader.read(file);
        Manifest jdk = new Manifest(Files.newInputStream(file));
        assertEquals(jdk.getMainAttributes().getValue("Target"), ours.main.get("Target"));
        Attributes named = jdk.getEntries().get("com/example/**");
        assertNotNull(named);
        assertEquals(named.getValue("Exclude-Stubs"), ours.named.get(0).get("Exclude-Stubs"));
    }

    @Test
    public void scopedRuleMatchesLikeIgnoreWarnings() {
        ScopedRule exact = new ScopedRule("com/example/Foo", xyz.wagyourtail.jvmdg.cli.Flags.WildcardType.NONE, "t", null, 1, java.util.Collections.emptyMap());
        ScopedRule single = new ScopedRule("com/example/", xyz.wagyourtail.jvmdg.cli.Flags.WildcardType.SINGLE, "t", null, 1, java.util.Collections.emptyMap());
        ScopedRule dbl = new ScopedRule("com/example/", xyz.wagyourtail.jvmdg.cli.Flags.WildcardType.DOUBLE, "t", null, 1, java.util.Collections.emptyMap());

        assertTrue(exact.matches("com/example/Foo"));
        assertFalse(exact.matches("com/example/Bar"));

        assertTrue(single.matches("com/example/Foo"));
        assertFalse(single.matches("com/example/sub/Bar"));
        assertFalse(single.matches("com/other/Foo"));

        assertTrue(dbl.matches("com/example/Foo"));
        assertTrue(dbl.matches("com/example/sub/Bar"));
        assertFalse(dbl.matches("com/exampleX/Foo"));
        assertFalse(dbl.matches("com/other/Foo"));
    }

    @Test
    public void scopedRuleSpecificityOrdersPrefixLength() {
        ScopedRule short_ = new ScopedRule("com/", xyz.wagyourtail.jvmdg.cli.Flags.WildcardType.DOUBLE, "t", null, 1, java.util.Collections.emptyMap());
        ScopedRule long_ = new ScopedRule("com/example/sub/", xyz.wagyourtail.jvmdg.cli.Flags.WildcardType.DOUBLE, "t", null, 1, java.util.Collections.emptyMap());
        assertTrue(long_.specificity() > short_.specificity());
    }
}
