package xyz.wagyourtail.downgradetest;

import java.io.IOException;
import java.util.logging.LogManager;

public class TestLogging {

    public static void main(String[] args) throws IOException {
        LogManager.getLogManager().readConfiguration(TestLogging.class.getResourceAsStream("/logging.properties"));

        System.Logger logger = System.getLogger("testLogger");
        logger.log(System.Logger.Level.INFO, "info");
        logger.log(System.Logger.Level.ERROR, "error", new Throwable());
        logger.log(System.Logger.Level.DEBUG, "debug");
        logger.log(System.Logger.Level.WARNING, "{0}, {1}!", "Hello", "World");
    }

}
