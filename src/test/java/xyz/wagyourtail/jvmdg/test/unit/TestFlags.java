package xyz.wagyourtail.jvmdg.test.unit;

import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jvmdg.cli.Flags;

import static org.junit.jupiter.api.Assertions.*;

public class TestFlags {


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

}
