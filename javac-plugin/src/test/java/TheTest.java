import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TheTest {
    @Test
    public void test() {
        // if this class loads, the plugin is working

        // just make sure we're running on Java 8
        assertTrue(System.getProperty("java.version").startsWith("1.8"));

        "test %s".formatted("test2");

        String h = """
        Hello World!
        """;
        assertEquals("Hello World!\n", h);
    }
}
