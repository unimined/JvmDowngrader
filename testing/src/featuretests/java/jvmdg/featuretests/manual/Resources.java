package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Classpath resources + java.util.logging: getResourceAsStream on jar entries
 * must work through the downgraded/shaded classloader, and LogManager config
 * from a resource drives System.Logger output (format pinned by
 * logging.properties: {@code LEVEL: message}).
 */
public class Resources {

    public static void main(String[] args) {
        Support.run(Resources.class);
    }

    @FeatureTest
    static void readResource() throws IOException {
        // getResourceAsStream on jar entries — must resolve through the
        // downgrading/shading classloader
        try (InputStream is = Resources.class.getResourceAsStream("/testResource.txt")) {
            System.out.println(new String(is.readAllBytes()));
        }
        try (InputStream is = Resources.class.getResourceAsStream("/test/testResource2.txt")) {
            System.out.println(new String(is.readAllBytes()));
        }
    }

    @FeatureTest
    static void logging() throws IOException {
        // JUL config loaded from a resource + System.Logger levels/params —
        // deterministic output pinned by logging.properties
        LogManager.getLogManager().readConfiguration(Resources.class.getResourceAsStream("/logging.properties"));

        System.Logger logger = System.getLogger("testLogger");
        logger.log(System.Logger.Level.INFO, "info");
        logger.log(System.Logger.Level.ERROR, "error", new Throwable());
        logger.log(System.Logger.Level.DEBUG, "debug");
        logger.log(System.Logger.Level.WARNING, "{0}, {1}!", "Hello", "World");
    }
}
