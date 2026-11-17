package xyz.wagyourtail.jvmdg.j21.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Minimal {@code ThreadPerTaskExecutor} analogue for the
 * {@code newVirtualThreadPerTaskExecutor}/{@code newThreadPerTaskExecutor}
 * stubs: every task runs on a fresh thread from the factory (the fake
 * virtual-thread factory for the virtual variant). Tracks live threads so
 * {@code shutdown}/{@code awaitTermination}/{@code shutdownNow} observe the
 * real ExecutorService contract — real vthreads interrupt on shutdownNow;
 * platform threads get {@link Thread#interrupt} which is the same observable
 * contract for cooperative tasks.
 */
public class ThreadPerTaskExecutor extends AbstractExecutorService {

    private final ThreadFactory factory;
    private final Set<Thread> live = ConcurrentHashMap.newKeySet();
    private volatile boolean shutdown;

    public ThreadPerTaskExecutor(ThreadFactory factory) {
        this.factory = factory;
    }

    @Override
    public void shutdown() {
        shutdown = true;
        synchronized (live) {
            live.notifyAll();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        for (Thread t : live) {
            t.interrupt();
        }
        synchronized (live) {
            live.notifyAll();
        }
        // submitted-but-not-started tasks can't exist here: execute() starts
        // synchronously, so the only pending work is the running set itself
        return new ArrayList<>();
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown && live.isEmpty();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        synchronized (live) {
            while (!(shutdown && live.isEmpty())) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                live.wait(remaining / 1_000_000, (int) (remaining % 1_000_000));
            }
            return true;
        }
    }

    @Override
    public void execute(Runnable command) {
        if (shutdown) {
            throw new RejectedExecutionException("executor is shut down");
        }
        Thread thread = factory.newThread(() -> {
            try {
                command.run();
            } finally {
                Thread self = Thread.currentThread();
                live.remove(self);
                synchronized (live) {
                    live.notifyAll();
                }
            }
        });
        live.add(thread);
        thread.start();
    }

}
