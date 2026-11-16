package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CompletableFuture async composition: callbacks run on pool threads whose
 * stdout may be lost/interleaved under batched runners — completion is
 * reported via flags on the main thread instead of printing inside callbacks.
 */
public class Futures {

    public static void main(String[] args) {
        Support.run(Futures.class);
    }

    @FeatureTest
    static void exceptionallyAsync() {
        // exceptionallyAsync runs the callback on a pool thread — reported
        // via a flag rather than printing inside the callback (racy stdout)
        AtomicBoolean ran = new AtomicBoolean();
        Object test = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("test");
        }).exceptionallyAsync(e -> {
            ran.set(true);
            return false;
        }).join();
        System.out.println(ran.get());
        System.out.println(test);
    }

    @FeatureTest
    static void chain() {
        // supplyAsync + thenApply chain — multiple lambda indy sites inside
        // an async pipeline, joined on the main thread
        String result = CompletableFuture.supplyAsync(() -> 1)
            .thenApply(i -> i + 1)
            .thenApply(i -> "v" + i)
            .join();
        System.out.println(result);
    }

    @FeatureTest
    static void combine() {
        // thenCombine + allOf — multi-future composition on completed futures
        CompletableFuture<String> a = CompletableFuture.completedFuture("a");
        CompletableFuture<String> b = CompletableFuture.completedFuture("b");
        System.out.println(a.thenCombine(b, (x, y) -> x + y).join());
        System.out.println(CompletableFuture.allOf(a, b).isDone());
    }
}
