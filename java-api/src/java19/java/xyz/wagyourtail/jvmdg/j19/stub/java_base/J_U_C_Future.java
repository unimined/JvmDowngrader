package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class J_U_C_Future {

    @Stub
    public static <V> V resultNow(Future<V> future) {
        if (!future.isDone()) {
            throw new IllegalStateException("Future is not done");
        }
        if (future.isCancelled()) {
            throw new IllegalStateException("Task was cancelled");
        }
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (ExecutionException e) {
                    throw new IllegalStateException("Task completed with exception");
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Stub
    public static Throwable exceptionNow(Future<?> future) {
        if (!future.isDone()) {
            throw new IllegalStateException("Future is not done");
        }
        if (future.isCancelled()) {
            throw new IllegalStateException("Task was cancelled");
        }
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    future.get();
                    return null;
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (ExecutionException e) {
                    return e.getCause();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }


    @Adapter("java/util/concurrent/Future$State")
    public enum State {
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    @Stub
    public static State state(Future<?> future) {
        if (!future.isDone()) {
            return State.RUNNING;
        }
        if (future.isCancelled()) {
            return State.CANCELLED;
        }
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    future.get();
                    return State.SUCCESS;
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (ExecutionException e) {
                    return State.FAILED;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
     }

}
