package xyz.wagyourtail.jvmdg.j21.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code newVirtualThreadPerTaskExecutor} emulation via a private cached
 * carrier pool whose workers are themselves {@link FakeVirtualThread}
 * instances.
 *
 * <p>Why a pool and not {@link ThreadPerTaskExecutor}: real virtual threads
 * amortize to near-zero per task; starting a real OS thread per task
 * (~50-100us churn each) is the worst-case emulation for the executor
 * pattern's whole point — cheap throwaway concurrency. Carriers are pooled
 * with 60s keep-alive, so short task bursts reuse workers instead of
 * respawning OS threads.</p>
 *
 * <p>Why {@code FakeVirtualThread} carriers and not {@code commonPool}:
 * {@code Thread.currentThread()} inside a task must remain a
 * {@code FakeVirtualThread} or {@code Thread.isVirtual} regresses to false —
 * a fidelity loss vs. the thread-per-task form. A private pool is also
 * unboundedly growable on demand (see {@link #execute}), which commonPool's
 * fixed parallelism can't do without starving on blocking tasks.</p>
 *
 * <p>Growth/liveness: every {@code execute} spawns a carrier when no carrier
 * is parked in {@code poll()} — so N concurrently-submitted tasks get up to
 * N carriers (unbounded like real vthreads; a <em>bounded</em> pool would
 * deadlock on task-blocks-on-task). Under sustained all-blocking load this
 * degenerates to thread-per-task — the honest floor, since true carrier
 * unmount on blocking ops is VM-level (Loom) and unemulable.</p>
 *
 * <p>Known semantic diffs vs. real vthreads (and vs. per-task real threads):
 * tasks on one carrier share its identity ({@code getName()}/{@code getId()}
 * show the carrier, not a fresh vthread) and its {@code ThreadLocal} map —
 * state leaks across tasks that real vthreads' fresh maps would isolate.
 * {@code join()}/{@code isAlive()} on a returned {@code Thread} are
 * unaffected because this path returns no {@code Thread} objects at all.</p>
 */
public class VirtualThreadPoolExecutor extends AbstractExecutorService {

    /** Cached-pool convention: idle carriers retire after a minute. */
    private static final long KEEPALIVE_NS = TimeUnit.SECONDS.toNanos(60);

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final Set<Thread> live = ConcurrentHashMap.newKeySet();
    private final Set<Thread> parked = ConcurrentHashMap.newKeySet();
    private final AtomicInteger carrierSeq = new AtomicInteger();
    private volatile boolean shutdown;

    @Override
    public void shutdown() {
        shutdown = true;
        // wake only parked carriers (same split as TPE's idle-worker interrupt):
        // running tasks must never see a shutdown() interrupt
        for (Thread t : parked) {
            t.interrupt();
        }
        synchronized (live) {
            live.notifyAll();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        List<Runnable> pending = new ArrayList<>();
        queue.drainTo(pending);
        for (Thread t : live) {
            t.interrupt();
        }
        synchronized (live) {
            live.notifyAll();
        }
        return pending;
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
            while (!isTerminated()) {
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
        queue.offer(command);
        // every parked carrier is either in poll() (wakes for this task) or
        // about to be — so only spawn when nobody is idle. No shutdown guard:
        // a carrier spawned after shutdown simply drains and retires, which
        // closes the offer/shutdown race. Mild overshoot on racing submits is
        // fine: surplus carriers retire on keep-alive.
        if (parked.isEmpty()) {
            spawnCarrier();
        }
    }

    private void spawnCarrier() {
        FakeVirtualThread carrier = new FakeVirtualThread(this::carrierLoop,
            "VirtualThread-worker-" + carrierSeq.getAndIncrement(), false);
        live.add(carrier);
        carrier.start();
    }

    private void carrierLoop() {
        try {
            for (;;) {
                if (shutdown && queue.isEmpty()) {
                    break;
                }
                Runnable task;
                Thread self = Thread.currentThread();
                parked.add(self);
                try {
                    task = queue.poll(KEEPALIVE_NS, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    // shutdown/shutdownNow interrupt while parked — recheck below
                    task = null;
                } finally {
                    parked.remove(self);
                }
                if (task == null) {
                    if (!queue.isEmpty()) {
                        continue; // timed out / raced — a task is waiting
                    }
                    break; // retire
                }
                try {
                    task.run();
                } catch (Throwable t) {
                    self.getUncaughtExceptionHandler().uncaughtException(self, t);
                }
            }
        } finally {
            live.remove(Thread.currentThread());
            synchronized (live) {
                live.notifyAll();
            }
        }
    }

}
