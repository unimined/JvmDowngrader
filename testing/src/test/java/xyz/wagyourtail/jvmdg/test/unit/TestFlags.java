package xyz.wagyourtail.jvmdg.test.unit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

// system properties are jvm-global and the suite runs concurrently —
// serialize this class so flag-parsing tests don't race on shared props
@Execution(ExecutionMode.SAME_THREAD)
public class TestFlags {

    private static final List<String> PROPS = Arrays.asList(
        Constants.CLASS_VERSION, Constants.QUIET, Constants.LOG_ANSI_COLORS,
        Constants.LOG_LEVEL, Constants.ALLOW_MAVEN_LOOKUP, Constants.DEBUG,
        Constants.SHADE_INLINING, Constants.DEBUG_DUMP_CLASSES,
        Constants.DEBUG_NO_SYNTHETIC, Constants.MULTI_RELEASE_ORIGINAL,
        Constants.MULTI_RELEASE_VERSIONS, Constants.DEBUG_SKIP_STUBS,
        Constants.DEBUG_SKIP_JEPS, Constants.DEBUG_SKIP_STUB,
        Constants.IGNORE_WARNINGS, Constants.JAVA_API
    );

    @AfterEach
    public void clearProps() {
        for (String prop : PROPS) {
            System.clearProperty(prop);
        }
    }

    private Flags withProp(String name, String value, Consumer<Flags> check) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
        Flags f = new Flags();
        check.accept(f);
        return f;
    }

    @Test
    public void testClassIn() {
        Flags f = new Flags();
        f.addIgnore("aaa/bbb/*");
        f.addIgnore("aaa/bbb/ccc/*");
        f.addIgnore("bbb/**");
        f.addIgnore("ccc/aa/dd*");

        assertTrue(f.checkInIgnoreWarnings("aaa/bbb/ccc/ddd"));
        assertTrue(f.checkInIgnoreWarnings("aaa/bbb/ccc"));
        assertFalse(f.checkInIgnoreWarnings("aaa/bbb/ddd/ccc"));
        assertTrue(f.checkInIgnoreWarnings("bbb/aaa/ccc"));
        assertTrue(f.checkInIgnoreWarnings("bbb/ccc"));
        assertTrue(f.checkInIgnoreWarnings("bbb/ccc/ddd"));
        assertTrue(f.checkInIgnoreWarnings("ccc/aa/ddd"));
        assertTrue(f.checkInIgnoreWarnings("ccc/aa/dddc"));
        assertFalse(f.checkInIgnoreWarnings("ccc/aa/dd/ee"));
        assertFalse(f.checkInIgnoreWarnings("ccc/aa/de"));
    }

    @Test
    public void noneWildcardIsExactOnly() {
        Flags f = new Flags();
        f.addIgnore("aaa/bbb/C");
        assertTrue(f.checkInIgnoreWarnings("aaa/bbb/C"));
        // an exact-name entry must not swallow siblings
        assertFalse(f.checkInIgnoreWarnings("aaa/bbb/C2"));
        assertFalse(f.checkInIgnoreWarnings("aaa/bbb/C/d"));
        assertFalse(f.checkInIgnoreWarnings("aaa/bbb"));
    }

    @Test
    public void singleWildcardIsDirectChildrenOnly() {
        Flags f = new Flags();
        f.addIgnore("a/b/*");
        assertTrue(f.checkInIgnoreWarnings("a/b/C"));
        assertTrue(f.checkInIgnoreWarnings("a/b/C2"));
        assertFalse(f.checkInIgnoreWarnings("a/b/c/D"), "* must not match sub-packages");
        assertFalse(f.checkInIgnoreWarnings("a/C"));
        assertFalse(f.checkInIgnoreWarnings("a/b"));
    }

    @Test
    public void singleWildcardPrefixForm() {
        Flags f = new Flags();
        f.addIgnore("a/b/dd*");
        assertTrue(f.checkInIgnoreWarnings("a/b/dddc"));
        assertTrue(f.checkInIgnoreWarnings("a/b/dd"));
        assertFalse(f.checkInIgnoreWarnings("a/b/dd/d"), "prefix-* must not match sub-packages");
        assertFalse(f.checkInIgnoreWarnings("a/b/dx"));
        assertFalse(f.checkInIgnoreWarnings("a/b/xdd"));
    }

    @Test
    public void doubleWildcardMatchesAnyDepth() {
        Flags f = new Flags();
        f.addIgnore("a/b/**");
        assertTrue(f.checkInIgnoreWarnings("a/b/c"));
        assertTrue(f.checkInIgnoreWarnings("a/b/c/d/e"));
        assertFalse(f.checkInIgnoreWarnings("a/bb/c"), "** must not match partial package names");
        assertFalse(f.checkInIgnoreWarnings("a/b"), "** strips to 'a/b/' so the package itself is not matched");
    }

    @Test
    public void nearerNonMatchDoesNotShadowBroaderWildcard() {
        Flags f = new Flags();
        f.addIgnore("a/b/**");
        f.addIgnore("a/b/c");
        // "a/b/c" is the floorEntry for "a/b/c/d" but doesn't match it;
        // the broader "a/b/**" must still win
        assertTrue(f.checkInIgnoreWarnings("a/b/c/d"));
        assertTrue(f.checkInIgnoreWarnings("a/b/c"));
        assertTrue(f.checkInIgnoreWarnings("a/b/e"));

        Flags g = new Flags();
        g.addIgnore("a/**");
        g.addIgnore("a/b");
        assertTrue(g.checkInIgnoreWarnings("a/b/c"));
        assertFalse(g.checkInIgnoreWarnings("c/d"));
    }

    @Test
    public void overlappingWildcardsMostSpecificMatchWins() {
        Flags f = new Flags();
        f.addIgnore("a/b/**");
        f.addIgnore("a/b/c/*");
        assertTrue(f.checkInIgnoreWarnings("a/b/c/D"));
        assertTrue(f.checkInIgnoreWarnings("a/b/c/d/E")); // falls through to **
        assertFalse(f.checkInIgnoreWarnings("x/y"));
    }

    @Test
    public void emptyIgnoreMapMatchesNothing() {
        Flags f = new Flags();
        assertFalse(f.checkInIgnoreWarnings("a/b/C"));
        assertFalse(f.checkInIgnoreWarnings(""));
    }

    /* ---- sysprop-driven flag parsing ---- */

    @Test
    public void logAnsiColorsHonorsProperty() {
        withProp(Constants.LOG_ANSI_COLORS, null, f -> assertTrue(f.logAnsiColors));
        withProp(Constants.LOG_ANSI_COLORS, "true", f -> assertTrue(f.logAnsiColors));
        withProp(Constants.LOG_ANSI_COLORS, "false", f -> assertFalse(f.logAnsiColors));
    }

    @Test
    public void shadeInliningHonorsProperty() {
        withProp(Constants.SHADE_INLINING, null, f -> assertTrue(f.shadeInlining));
        withProp(Constants.SHADE_INLINING, "true", f -> assertTrue(f.shadeInlining));
        withProp(Constants.SHADE_INLINING, "false", f -> assertFalse(f.shadeInlining));
    }

    @Test
    public void directGetBooleanFlags() {
        // these use Boolean.getBoolean(name) directly: default false, true only on "true"
        String[][] cases = {
            {Constants.QUIET, "quiet"},
            {Constants.ALLOW_MAVEN_LOOKUP, "allowMaven"},
            {Constants.DEBUG, "printDebug"},
            {Constants.DEBUG_DUMP_CLASSES, "debugDumpClasses"},
            {Constants.DEBUG_NO_SYNTHETIC, "debugNoSynthetic"},
            {Constants.MULTI_RELEASE_ORIGINAL, "multiReleaseOriginal"},
        };
        for (String[] c : cases) {
            String prop = c[0];
            String field = c[1];
            withProp(prop, null, f -> assertFalse(flagValue(f, field), field + " must default to false"));
            withProp(prop, "true", f -> assertTrue(flagValue(f, field), field + " must be true"));
            withProp(prop, "false", f -> assertFalse(flagValue(f, field), field + " must be false"));
        }
    }

    private static boolean flagValue(Flags f, String field) {
        switch (field) {
            case "quiet":
                return f.quiet;
            case "allowMaven":
                return f.allowMaven;
            case "printDebug":
                return f.printDebug;
            case "debugDumpClasses":
                return f.debugDumpClasses;
            case "debugNoSynthetic":
                return f.debugNoSynthetic;
            case "multiReleaseOriginal":
                return f.multiReleaseOriginal;
            default:
                throw new IllegalArgumentException(field);
        }
    }

    @Test
    public void logLevelHonorsProperty() {
        withProp(Constants.LOG_LEVEL, null, f -> assertEquals(Logger.Level.INFO, f.logLevel));
        withProp(Constants.LOG_LEVEL, "warn", f -> assertEquals(Logger.Level.WARN, f.logLevel));
        withProp(Constants.LOG_LEVEL, "FATAL", f -> assertEquals(Logger.Level.FATAL, f.logLevel));
    }

    @Test
    public void classVersionHonorsProperty() {
        withProp(Constants.CLASS_VERSION, null, f -> assertEquals(52, f.classVersion));
        withProp(Constants.CLASS_VERSION, "61", f -> assertEquals(61, f.classVersion));
    }

    @Test
    public void setFlagsParse() {
        withProp(Constants.MULTI_RELEASE_VERSIONS, "55,61", f ->
            assertEquals(new HashSet<>(Arrays.asList(55, 61)), f.multiReleaseVersions));
        withProp(Constants.DEBUG_SKIP_STUBS, "61,65", f ->
            assertEquals(new HashSet<>(Arrays.asList(61, 65)), f.debugSkipStubs));
        withProp(Constants.DEBUG_SKIP_JEPS, "400,416", f ->
            assertEquals(new HashSet<>(Arrays.asList(400, 416)), f.debugSkipJeps));
        withProp(Constants.DEBUG_SKIP_STUB, "Ljava/lang/String;length;()I", f ->
            assertTrue(f.debugSkipStub.contains(FullyQualifiedMemberNameAndDesc.of("Ljava/lang/String;length;()I"))));
    }

    @Test
    public void ignoreWarningsProperty() {
        withProp(Constants.IGNORE_WARNINGS, "aaa/**,bbb/c/*", f -> {
            assertTrue(f.checkInIgnoreWarnings("aaa/x/y"));
            assertTrue(f.checkInIgnoreWarnings("bbb/c/D"));
            assertFalse(f.checkInIgnoreWarnings("bbb/c/d/E"));
            assertFalse(f.checkInIgnoreWarnings("ccc"));
        });
    }

    /* ---- copy() ---- */

    @Test
    public void copyCarriesAllFields() {
        Flags f = new Flags();
        f.classVersion = 61;
        f.api = new ArrayList<>(Arrays.asList(new File("a.jar"), new File("b.jar")));
        f.quiet = true;
        f.logAnsiColors = false;
        f.logLevel = Logger.Level.WARN;
        f.allowMaven = true;
        f.shadeInlining = false;
        f.addIgnore("x/**");
        f.printDebug = true;
        f.debugSkipStub.add(FullyQualifiedMemberNameAndDesc.of("Ljava/lang/String;length;()I"));
        f.debugSkipStubs.add(61);
        f.debugSkipJeps.add(400);
        f.debugNoSynthetic = true;
        f.debugDumpClasses = true;
        f.multiReleaseOriginal = true;
        f.multiReleaseVersions.add(55);
        f.downgradeFromMultiReleases = true;

        Flags c = f.copy();
        assertEquals(f, c);
        assertEquals(f.hashCode(), c.hashCode());
    }

    @Test
    public void copyDoesNotAliasMutableFields() {
        Flags f = new Flags();
        f.api = new ArrayList<>();
        Flags c = f.copy();
        c.api.add(new File("leak.jar"));
        c.ignoreWarningsIn.put("leak/", Flags.WildcardType.DOUBLE);
        c.debugSkipStubs.add(99);
        c.multiReleaseVersions.add(99);

        assertTrue(f.api.isEmpty(), "copy().api must not alias the source list");
        assertFalse(f.ignoreWarningsIn.containsKey("leak/"));
        assertFalse(f.debugSkipStubs.contains(99));
        assertFalse(f.multiReleaseVersions.contains(99));
    }
}
