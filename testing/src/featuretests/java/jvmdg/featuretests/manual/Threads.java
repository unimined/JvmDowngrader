package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.util.concurrent.CountDownLatch;

/**
 * Thread forms: platform threads, virtual threads (j21). Output is collected
 * and flushed on the main thread after join — prints inside spawned threads
 * can route/interleave under batched runners.
 */
public class Threads {

    public static void main(String[] args) {
        Support.run(Threads.class);
    }

    @FeatureTest
    static void platform() throws InterruptedException {
        // platform Thread start/join — append via StringBuilder and print
        // after join so thread stdout can't interleave under batch routing
        StringBuilder sb = new StringBuilder();
        Thread t = new Thread(() -> sb.append("ran"));
        t.start();
        t.join();
        System.out.println(sb);
        System.out.println(Thread.currentThread().isVirtual());
    }

    @FeatureTest
    static void virtual() throws InterruptedException {
        // ofVirtual().unstarted + startVirtualThread — j21 virtual-thread
        // stub must actually RUN the task (regression: it once didn't)
        StringBuilder sb = new StringBuilder();
        Thread t = Thread.ofVirtual().unstarted(() -> sb.append("vran"));
        System.out.println(t.isVirtual());
        t.start();
        t.join();
        System.out.println(sb);

        Thread t2 = Thread.startVirtualThread(() -> sb.append("2"));
        t2.join();
        System.out.println(sb);
    }

    @FeatureTest
    static void executor() throws InterruptedException, java.util.concurrent.ExecutionException {
        // Executors.newVirtualThreadPerTaskExecutor — each task on a fresh
        // virtual thread; tasks report their own isVirtual so the probe
        // proves the fake-virtual-thread backing, not just "it ran"
        java.util.List<String> sb = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.concurrent.ExecutorService es =
            java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(2);
        es.submit(() -> {
            sb.add("a" + Thread.currentThread().isVirtual());
            latch.countDown();
        });
        es.submit(() -> {
            sb.add("b" + Thread.currentThread().isVirtual());
            latch.countDown();
        });
        latch.await();
        es.shutdown();
        es.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
        // order of a/b is racy — print sorted
        java.util.List<String> sorted = new java.util.ArrayList<>(sb);
        java.util.Collections.sort(sorted);
        System.out.println(String.join("", sorted));
    }
}
