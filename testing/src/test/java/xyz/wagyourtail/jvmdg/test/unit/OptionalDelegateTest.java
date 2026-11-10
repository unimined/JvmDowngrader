package xyz.wagyourtail.jvmdg.test.unit;

import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jvmdg.stub.OptionalDelegate;
import xyz.wagyourtail.jvmdg.stub.OptionalDependency;

import static org.junit.jupiter.api.Assertions.*;

public class OptionalDelegateTest {

    @OptionalDependency(value = {"java.lang.String"}, name = "present-probe", maven = "example:present")
    public static class PresentDelegate {
    }

    @OptionalDependency(value = {"this.class.does.not.Exist"}, name = "absent-probe", maven = "example:absent")
    public static class AbsentDelegate {
    }

    @OptionalDependency(value = {"java.lang.String"}, name = "broken-probe")
    public static class BrokenDelegate {
        public BrokenDelegate() {
            throw new IllegalStateException("simulated native-load failure");
        }
    }

    @Test
    public void testPresentDependencyInstantiates() {
        PresentDelegate delegate = OptionalDelegate.get(PresentDelegate.class);
        assertNotNull(delegate);
        assertSame(delegate, OptionalDelegate.get(PresentDelegate.class), "delegate should be a cached singleton");
        assertTrue(OptionalDelegate.available(PresentDelegate.class));
    }

    @Test
    public void testAbsentDependencyReturnsNull() {
        assertNull(OptionalDelegate.get(AbsentDelegate.class));
        assertFalse(OptionalDelegate.available(AbsentDelegate.class));
        // cached absent — still null on second probe
        assertNull(OptionalDelegate.get(AbsentDelegate.class));
    }

    @Test
    public void testBrokenDependencyCountsAsAbsent() {
        assertNull(OptionalDelegate.get(BrokenDelegate.class));
    }

    @Test
    public void testUnsupportedMessageCarriesCoordinate() {
        UnsupportedOperationException e = OptionalDelegate.unsupported(AbsentDelegate.class, "Test capability");
        assertTrue(e.getMessage().contains("Test capability"));
        assertTrue(e.getMessage().contains("absent-probe"));
        assertTrue(e.getMessage().contains("example:absent"));
    }

    @Test
    public void testUnannotatedClassIsAbsent() {
        assertNull(OptionalDelegate.get(String.class));
    }

}
