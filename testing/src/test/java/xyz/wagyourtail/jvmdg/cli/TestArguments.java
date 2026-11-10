package xyz.wagyourtail.jvmdg.cli;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestArguments {

    private static Map<String, List<String[]>> parse(String... args) {
        return new Main().buildArgumentList().read(new ArrayList<>(Arrays.asList(args)), false);
    }

    @Test
    public void bootstrapArgsKeepTheirCase() {
        Map<String, List<String[]>> parsed = parse(
            "bootstrap", "--main", "com.example.Main", "-Dflag=Value", "-Xmx1G", "CamelArg"
        );
        Map<String, List<String[]>> sub = Arguments.subCommandArgs("bootstrap", parsed);
        String[] bootArgs = sub.get("").get(0);
        assertArrayEquals(new String[]{"-Dflag=Value", "-Xmx1G", "CamelArg"}, bootArgs,
            "bootstrap args must reach the user verbatim, not lowercased");
        assertArrayEquals(new String[]{"com.example.Main"}, sub.get("--main").get(0));
    }

    @Test
    public void flagNamesAreCaseInsensitive() {
        Map<String, List<String[]>> parsed = parse("bootstrap", "--MAIN", "com.example.Main", "arg");
        Map<String, List<String[]>> sub = Arguments.subCommandArgs("bootstrap", parsed);
        assertNotNull(sub.get("--main"));
        assertArrayEquals(new String[]{"com.example.Main"}, sub.get("--main").get(0));
    }

    @Test
    public void missingValueIsAnError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> parse("downgrade", "--target", "input.jar"));
        assertTrue(e.getMessage().contains("--target"), "error should name the flag: " + e.getMessage());
    }

    @Test
    public void unknownArgumentIsAnError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> parse("--bogusFlag"));
        assertTrue(e.getMessage().contains("--bogusFlag"), "error should name the arg: " + e.getMessage());
    }

    @Test
    public void altNamesResolveToCanonicalKeys() {
        Map<String, List<String[]>> parsed = parse(
            "bootstrap", "-m", "com.example.Main", "-cp", "a.jar:b.jar", "arg"
        );
        Map<String, List<String[]>> sub = Arguments.subCommandArgs("bootstrap", parsed);
        assertNotNull(sub.get("--main"), "-m must map to --main");
        assertNotNull(sub.get("--classpath"), "-cp must map to --classpath");
        assertArrayEquals(new String[]{"a.jar:b.jar"}, sub.get("--classpath").get(0));
    }

    @Test
    public void repeatedFlagsAccumulate() {
        Map<String, List<String[]>> parsed = parse("-i", "a/**", "-i", "b/**", "--ignoreWarningsIn", "c/*");
        List<String[]> values = parsed.get("--ignoreWarningsIn");
        assertEquals(3, values.size());
        assertArrayEquals(new String[]{"a/**"}, values.get(0));
        assertArrayEquals(new String[]{"b/**"}, values.get(1));
        assertArrayEquals(new String[]{"c/*"}, values.get(2));
    }

    @Test
    public void chainedSubcommandsScopeKeys() {
        Map<String, List<String[]>> parsed = parse(
            "downgrade", "-t", "in.jar", "-",
            "shade", "-p", "prefix", "-t", "-", "out.jar"
        );
        assertTrue(parsed.containsKey("downgrade"));
        assertTrue(parsed.containsKey("downgrade:--target"));
        assertTrue(parsed.containsKey("shade"));
        assertTrue(parsed.containsKey("shade:--prefix"));
        assertTrue(parsed.containsKey("shade:--target"));
        assertArrayEquals(new String[]{"in.jar", "-"}, parsed.get("downgrade:--target").get(0));
        assertArrayEquals(new String[]{"-", "out.jar"}, parsed.get("shade:--target").get(0));
        assertArrayEquals(new String[]{"prefix"}, parsed.get("shade:--prefix").get(0));
    }

    @Test
    public void valuesAfterCatchAllStayVerbatim() {
        // args that look like other flags must still pass through to bootstrap
        Map<String, List<String[]>> parsed = parse(
            "bootstrap", "--main", "M", "--help", "-t", "notATarget"
        );
        Map<String, List<String[]>> sub = Arguments.subCommandArgs("bootstrap", parsed);
        assertArrayEquals(new String[]{"--help", "-t", "notATarget"}, sub.get("").get(0));
    }
}
