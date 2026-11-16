package xyz.wagyourtail.jvmdg.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestBootstrap {

    @Test
    public void missingArgsFailLoudly() {
        // previously main() swallowed every throwable and exited 0
        assertThrows(IllegalArgumentException.class, () -> Bootstrap.main(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> Bootstrap.main(new String[]{"."}));
    }

    @Test
    public void missingMainClassPropagates() {
        // failure to find the main class must propagate (nonzero exit in a real JVM),
        // not be swallowed as success
        assertThrows(ClassNotFoundException.class,
            () -> Bootstrap.main(new String[]{".", "com.nonexistent.MainClass"}));
    }
}
