package xyz.wagyourtail.jvmdg;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.util.Function;

import java.lang.instrument.IllegalClassFormatException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TestClassDowngrader {

    private static final Function<String, byte[]> NO_EXTRA = new Function<String, byte[]>() {
        @Override
        public byte[] apply(String s) {
            return null;
        }
    };

    @Test
    public void emptyBytesThrowIllegalClassFormatNotAioobe() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            assertThrows(IllegalClassFormatException.class,
                () -> d.downgrade(new AtomicReference<>("test/Empty"), new byte[0], false, NO_EXTRA));
        }
    }

    @Test
    public void truncatedBytesThrowIllegalClassFormatNotAioobe() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            // fewer than 8 bytes: can't even hold magic + version
            assertThrows(IllegalClassFormatException.class,
                () -> d.downgrade(new AtomicReference<>("test/Short"), new byte[]{(byte) 0xCA, (byte) 0xFE}, false, NO_EXTRA));
            // exactly magic, missing version
            assertThrows(IllegalClassFormatException.class,
                () -> d.downgrade(new AtomicReference<>("test/Magic"),
                    new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}, false, NO_EXTRA));
        }
    }

    @Test
    public void wrongMagicThrowsIllegalClassFormat() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            assertThrows(IllegalClassFormatException.class,
                () -> d.downgrade(new AtomicReference<>("test/Bad"),
                    new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, false, NO_EXTRA));
        }
    }

    @Test
    public void sharedDowngraderIsOneInstanceAcrossThreads() throws Exception {
        // the no-arg singleton must be race-free: concurrent first callers all
        // get the same instance (previously unsynchronized check-then-set)
        int threads = 8;
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(threads);
        java.util.concurrent.CountDownLatch go = new java.util.concurrent.CountDownLatch(1);
        java.util.List<ClassDowngrader> results =
            java.util.Collections.synchronizedList(new java.util.ArrayList<ClassDowngrader>());
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    ready.countDown();
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    results.add(ClassDowngrader.getCurrentVersionDowngrader());
                }
            });
            t.start();
        }
        ready.await();
        go.countDown();
        long deadline = System.currentTimeMillis() + 30000;
        while (results.size() < threads && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(threads, results.size());
        for (ClassDowngrader d : results) {
            assertSame(results.get(0), d, "concurrent callers got different singleton instances");
        }
    }

    @Test
    public void classAtTargetVersionPassesThroughUnchanged() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            // header only: CAFEBABE + minor 0 + major 52 — version <= target short-circuits before parsing
            byte[] header = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 52};
            assertNull(d.downgrade(new AtomicReference<>("test/Current"), header, false, NO_EXTRA),
                "classes at/below target must pass through without parsing");
        }
    }
}
