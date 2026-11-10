package xyz.wagyourtail.jvmdg.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class TestAsyncUtils {

    private static <T> Future<T> neverFuture() {
        return new CompletableFuture<>();
    }

    @Test
    public void collectsResultsInOrder() throws Exception {
        Future<List<Integer>> f = AsyncUtils.waitForFutures(
            CompletableFuture.completedFuture(1),
            CompletableFuture.completedFuture(2),
            CompletableFuture.completedFuture(3));
        assertEquals(Arrays.asList(1, 2, 3), f.get());
    }

    @Test
    public void repeatedGetReturnsSameResult() throws Exception {
        Future<List<Integer>> f = AsyncUtils.waitForFutures(
            CompletableFuture.completedFuture(7),
            CompletableFuture.completedFuture(8));
        List<Integer> first = f.get();
        assertEquals(Arrays.asList(7, 8), first);
        // previously the internal queue was drained by the first get(), so a second returned []
        assertEquals(Arrays.asList(7, 8), f.get(), "second get() must return the computed result");
        assertTrue(f.isDone());
    }

    @Test
    @Timeout(10)
    public void timeoutIsHonoredInMilliseconds() {
        Future<List<Object>> f = AsyncUtils.waitForFutures(neverFuture());
        assertThrows(TimeoutException.class, () -> f.get(50, TimeUnit.MILLISECONDS));
    }

    @Test
    @Timeout(10)
    public void timeoutIsHonoredInSeconds() {
        // regression: get(1, SECONDS) used to wait get(1000, SECONDS) because the
        // computed ms were passed back with the original unit
        Future<List<Object>> f = AsyncUtils.waitForFutures(neverFuture());
        long start = System.nanoTime();
        try {
            f.get(1, TimeUnit.SECONDS);
            fail("must time out on a never-completing future");
        } catch (TimeoutException expected) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertTrue(elapsedMs < 9000, "timeout of 1s took " + elapsedMs + "ms — unit conversion is wrong");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void emptyFuturesResolveToEmptyList() throws Exception {
        assertTrue(AsyncUtils.waitForFutures().get().isEmpty());
    }

    @Test
    public void exceptionInFuturePropagates() {
        CompletableFuture<Integer> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("boom"));
        Future<List<Integer>> f = AsyncUtils.waitForFutures(failed);
        ExecutionException e = assertThrows(ExecutionException.class, f::get);
        assertInstanceOf(IllegalStateException.class, e.getCause());
    }
}
