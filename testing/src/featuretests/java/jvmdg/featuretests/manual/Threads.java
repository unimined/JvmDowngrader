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
    static void executor() throws InterruptedException {
        // Executors.newVirtualThreadPerTaskExecutor isn't stubbed; the
        // supported executor-ish surface is ofVirtual().factory()
        java.util.List<String> sb = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.concurrent.ThreadFactory f = Thread.ofVirtual().factory();
        CountDownLatch latch = new CountDownLatch(2);
        Thread a = f.newThread(() -> {
            sb.add("a");
            latch.countDown();
        });
        Thread b = f.newThread(() -> {
            sb.add("b");
            latch.countDown();
        });
        a.start();
        b.start();
        latch.await();
        a.join();
        b.join();
        // order of a/b is racy — print sorted
        java.util.List<String> sorted = new java.util.ArrayList<>(sb);
        java.util.Collections.sort(sorted);
        System.out.println(String.join("", sorted));
    }
}
