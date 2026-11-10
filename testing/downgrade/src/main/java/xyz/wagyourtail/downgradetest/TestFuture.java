package xyz.wagyourtail.downgradetest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TestFuture {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // async callbacks run on pool threads whose stdout may be lost or
        // interleaved under batched runners — report completion on the main
        // thread instead of printing inside the callback
        java.util.concurrent.atomic.AtomicBoolean ran = new java.util.concurrent.atomic.AtomicBoolean();
        Object test = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("test");
        }).exceptionallyAsync(e -> {
            ran.set(true);
            return false;
        }).join();
        System.out.println(ran.get());
        System.out.println(test);
    }

}
