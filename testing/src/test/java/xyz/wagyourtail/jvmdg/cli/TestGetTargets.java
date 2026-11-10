package xyz.wagyourtail.jvmdg.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestGetTargets {

    private static final byte[] EMPTY_ZIP = {
        0x50, 0x4B, 0x05, 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    @Test
    public void shortInputNameStillCreatesTempOutput(@TempDir Path dir) throws IOException {
        // File.createTempFile requires a >=3-char prefix; a 2-char input name used to throw IAE
        Path input = dir.resolve("ab");
        Files.write(input, EMPTY_ZIP);

        Map<String, List<String[]>> args = Collections.singletonMap(
            "--target", Collections.singletonList(new String[]{input.toString(), "-"}));
        Map<Path, Path> targets = new HashMap<>();
        List<FileSystem> fileSystems = new ArrayList<>();
        try {
            assertDoesNotThrow(() -> new Main().getTargets(args, targets, fileSystems));
            assertEquals(1, targets.size());
        } finally {
            for (FileSystem fs : fileSystems) {
                try {
                    fs.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Test
    public void missingInputIsAnError(@TempDir Path dir) {
        Map<String, List<String[]>> args = Collections.singletonMap(
            "--target", Collections.singletonList(new String[]{dir.resolve("nope.jar").toString(), "out.jar"}));
        assertThrows(IllegalArgumentException.class,
            () -> new Main().getTargets(args, new HashMap<>(), new ArrayList<>()));
    }
}
