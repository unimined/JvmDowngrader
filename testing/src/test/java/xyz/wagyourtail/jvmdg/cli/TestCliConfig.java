package xyz.wagyourtail.jvmdg.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import xyz.wagyourtail.jvmdg.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Flags() reads system properties — serialize with the other flag tests
@Execution(ExecutionMode.SAME_THREAD)
public class TestCliConfig {

    private static Path writeConfig(Path dir, String text) throws Exception {
        Path cfg = dir.resolve("jvmdg.mf");
        Files.write(cfg, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return cfg;
    }

    @Test
    public void configFileAppliesThroughParseArgs(@TempDir Path dir) throws Throwable {
        Path cfg = writeConfig(dir,
            "Jvmdg-Config-Version: 1\n" +
            "Target: 11\n" +
            "Log-Level: error\n");
        Main m = new Main();
        m.parseArgs(new String[]{"--config", cfg.toString()});
        assertEquals(55, m.flags.classVersion);
        assertEquals(Logger.Level.ERROR, m.flags.logLevel);
    }

    @Test
    public void cliFlagBeatsConfigFile(@TempDir Path dir) throws Throwable {
        Path cfg = writeConfig(dir,
            "Jvmdg-Config-Version: 1\n" +
            "Target: 11\n");
        Main m = new Main();
        // CLI takes the legacy form (raw class-version opcode), config takes
        // the Java-version form — 52 beats config's Target: 11
        m.parseArgs(new String[]{"--config", cfg.toString(), "-c", "52"});
        assertEquals(52, m.flags.classVersion);
    }

    @Test
    public void laterConfigFileWinsThroughCli(@TempDir Path dir) throws Throwable {
        Path a = writeConfig(dir, "Jvmdg-Config-Version: 1\nTarget: 8\n");
        Path b = dir.resolve("second.mf");
        Files.write(b, "Jvmdg-Config-Version: 1\nTarget: 11\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Main m = new Main();
        m.parseArgs(new String[]{"--config", a.toString(), "--config", b.toString()});
        assertEquals(55, m.flags.classVersion);
    }

    @Test
    public void badConfigFileFailsFast(@TempDir Path dir) {
        assertThrows(Exception.class, () -> {
            Path cfg = writeConfig(dir, "Target: 8\n");
            new Main().parseArgs(new String[]{"--config", cfg.toString()});
        });
        assertThrows(Exception.class, () -> {
            Path cfg = writeConfig(dir, "Jvmdg-Config-Version: 1\nBogus-Key: 1\n");
            new Main().parseArgs(new String[]{"--config", cfg.toString()});
        });
    }

    @Test
    public void shadePrefixFromConfigFeedsShade(@TempDir Path dir) throws Throwable {
        Path cfg = writeConfig(dir,
            "Jvmdg-Config-Version: 1\n" +
            "Shade-Prefix: com/acme/shaded/\n");
        Main m = new Main();
        m.parseArgs(new String[]{"--config", cfg.toString()});
        assertEquals("com/acme/shaded/", m.flags.shadePrefix);
    }
}
