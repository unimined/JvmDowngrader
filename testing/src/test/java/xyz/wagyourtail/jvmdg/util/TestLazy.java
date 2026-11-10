package xyz.wagyourtail.jvmdg.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestLazy {

    @Test
    public void initializesOnceOnFirstGet() {
        AtomicInteger inits = new AtomicInteger();
        Lazy<String> lazy = new Lazy<String>() {
            @Override
            protected String init() {
                inits.incrementAndGet();
                return "value";
            }
        };
        assertEquals(0, inits.get(), "must not initialize eagerly");
        assertEquals("value", lazy.get());
        assertEquals("value", lazy.get());
        assertEquals(1, inits.get(), "init() must run exactly once");
    }

    @Test
    @Timeout(15)
    public void concurrentGetsInitializeExactlyOnce() throws Exception {
        final int threads = 16;
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch gate = new CountDownLatch(1);
        final AtomicInteger inits = new AtomicInteger();
        final Lazy<Object> lazy = new Lazy<Object>() {
            @Override
            protected Object init() {
                inits.incrementAndGet();
                return new Object();
            }
        };
        List<FutureTask<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            FutureTask<Object> task = new FutureTask<>(() -> {
                ready.countDown();
                gate.await(5, TimeUnit.SECONDS);
                return lazy.get();
            });
            tasks.add(task);
            new Thread(task).start();
        }
        ready.await(5, TimeUnit.SECONDS);
        gate.countDown();
        Object first = tasks.get(0).get(10, TimeUnit.SECONDS);
        for (FutureTask<Object> task : tasks) {
            assertSame(first, task.get(10, TimeUnit.SECONDS), "all threads must see the same instance");
        }
        assertEquals(1, inits.get(), "init() must run exactly once under contention");
    }
}
