package xyz.wagyourtail.jvmdg.test.unit;

import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jvmdg.stub.EnumExtender;

import java.util.EnumMap;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

public class EnumExtenderTest {

    enum InjectTarget {
        A, B
    }

    enum IdempotentTarget {
        A, B
    }

    enum ExistingTarget {
        A, B
    }

    @Test
    public void testExtendInjectsRealConstant() {
        assertEquals(2, InjectTarget.values().length);

        InjectTarget injected = EnumExtender.extend(InjectTarget.class, "C");
        assertNotNull(injected, "injection should succeed on the test enum");
        assertEquals("C", injected.name());
        assertEquals(2, injected.ordinal());
        assertSame(injected, InjectTarget.valueOf("C"));
        assertEquals(3, InjectTarget.values().length);
        assertSame(injected, InjectTarget.values()[2]);
        assertTrue(EnumSet.allOf(InjectTarget.class).contains(injected));
        assertEquals(3, EnumSet.allOf(InjectTarget.class).size());
        assertDoesNotThrow(() -> new EnumMap<InjectTarget, String>(InjectTarget.class));
    }

    @Test
    public void testExtendIsIdempotent() {
        int before = IdempotentTarget.values().length;
        IdempotentTarget first = EnumExtender.extend(IdempotentTarget.class, "C");
        IdempotentTarget second = EnumExtender.extend(IdempotentTarget.class, "C");
        assertNotNull(first);
        assertSame(first, second);
        assertEquals("C", first.name());
        assertEquals(before, first.ordinal());
        assertEquals(before + 1, IdempotentTarget.values().length);
    }

    @Test
    public void testExtendExistingConstantReturnsIt() {
        ExistingTarget existing = EnumExtender.extend(ExistingTarget.class, "A");
        assertSame(ExistingTarget.A, existing);
    }

}
