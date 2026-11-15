package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Override-preserving dispatch: a stubbed instance member call is emitted as
 * an {@code invokedynamic} bootstrapped by {@code DispatchRuntime} — the
 * per-callsite PIC resolves the receiver's own member via {@code findVirtual}
 * before the impl, so a subclass that overrides the member must run its
 * override on every target, exactly as the unstubbed member would dispatch
 * natively.
 *
 * <p>Gained supertypes: a user class whose supertype gained an interface at
 * this boundary gets the real interface injected (e.g. {@code AutoCloseable}
 * onto {@link ThreadPoolExecutor} subclasses, whose {@code ExecutorService}
 * parent gained it at j19) plus impl bridges for members it doesn't serve —
 * {@code instanceof}/{@code checkcast}/{@code invokeinterface} on the gained
 * interface then behave exactly like the authored JDK.
 */
public class OvDispatch {

    public static void main(String[] args) {
        Support.run(OvDispatch.class);
    }

    /** overrides the j19 {@code ExecutorService.close} default the j8 target stubs */
    static class ClosingExec extends ThreadPoolExecutor {
        boolean custom;

        ClosingExec() {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        }

        @Override
        public void close() {
            custom = true;
            shutdown();
        }
    }

    @FeatureTest
    static void execOverrideRuns(String id) {
        ClosingExec e = new ClosingExec();
        ExecutorService svc = e;
        svc.close();
        // the flag only sets when the call reached this class's override
        Support.print(id, e.custom);
    }

    @FeatureTest
    static void execImplFallback(String id) {
        ExecutorService svc = Executors.newSingleThreadExecutor();
        svc.close();
        // unmarked receiver → impl → the default close semantics (shutdown)
        Support.print(id, svc.isShutdown());
    }

    /** invokespecial through the interface must stay direct — no Ov bounce */
    static class SuperExec implements ExecutorService {
        private final ExecutorService d = Executors.newSingleThreadExecutor();
        boolean after;

        @Override
        public void close() {
            ExecutorService.super.close();
            after = true;
        }

        @Override
        public void shutdown() {
            d.shutdown();
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            return d.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return d.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return d.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return d.awaitTermination(timeout, unit);
        }

        @Override
        public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) {
            return d.submit(task);
        }

        @Override
        public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) {
            return d.submit(task, result);
        }

        @Override
        public java.util.concurrent.Future<?> submit(Runnable task) {
            return d.submit(task);
        }

        @Override
        public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(
                java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) throws InterruptedException {
            return d.invokeAll(tasks);
        }

        @Override
        public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(
                java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks,
                long timeout, TimeUnit unit) throws InterruptedException {
            return d.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(
                java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks)
                throws InterruptedException, java.util.concurrent.ExecutionException {
            return d.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(
                java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks,
                long timeout, TimeUnit unit)
                throws InterruptedException, java.util.concurrent.ExecutionException,
                java.util.concurrent.TimeoutException {
            return d.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            d.execute(command);
        }
    }

    @FeatureTest
    static void execSuperCallsImpl(String id) {
        ExecutorService svc = new SuperExec();
        svc.close();
        // super.close() runs the default impl (shutdown) — if the
        // invokespecial were routed through the Ov probe the receiver's own
        // close() would re-enter and recurse
        Support.print(id, svc.isShutdown());
    }

    /** no {@code close()} — the gained-member bridge must serve it */
    static class PlainExec extends ThreadPoolExecutor {
        PlainExec() {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        }
    }

    @FeatureTest
    static void execGainedMembership(String id) {
        ExecutorService svc = new PlainExec();
        // j19: ExecutorService is AutoCloseable — the interface is injected
        // into user subclasses so instanceof verifies like the authored JDK
        Support.print(id, svc instanceof AutoCloseable);
    }

    @FeatureTest
    static void execIfaceCloseOverride(String id) throws Exception {
        ClosingExec e = new ClosingExec();
        // AutoCloseable.close sites stay native (real member on j8) — the
        // injected interface must dispatch to the declared override
        AutoCloseable a = (AutoCloseable) e;
        a.close();
        Support.print(id, e.custom);
    }

    @FeatureTest
    static void execIfaceCloseBridge(String id) throws Exception {
        PlainExec e = new PlainExec();
        // no declared close → synthesized bridge → impl → default (shutdown)
        AutoCloseable a = (AutoCloseable) e;
        a.close();
        Support.print(id, e.isShutdown());
    }

    /** overrides the j21 {@code List.getFirst} the j8 target stubs */
    static class FirstList<E> extends ArrayList<E> {
        @SuppressWarnings("unchecked")
        @Override
        public E getFirst() {
            return (E) "mine";
        }
    }

    @FeatureTest
    static void listOverrideRuns(String id) {
        FirstList<String> l = new FirstList<>();
        l.add("real");
        // invokeinterface on the stubbed List owner — the indy callsite's PIC
        // must resolve the declared getFirst override on FirstList
        List<String> view = l;
        Support.print(id, view.getFirst());
    }

    @FeatureTest
    static void listImplFallback(String id) {
        ArrayList<String> l = new ArrayList<>();
        l.add("real");
        Support.print(id, l.getFirst());
    }
}
