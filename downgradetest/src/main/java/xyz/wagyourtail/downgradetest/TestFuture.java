package xyz.wagyourtail.downgradetest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

public class TestFuture {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Object test = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("test");
        }).exceptionallyAsync(e -> {
            System.out.println("exceptionallyAsync");
            return false;
        }).join();
        System.out.println(test);
    }

}
