package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.TimeUnit;

public class J_L_Process {

    @Stub
    public static boolean waitFor(Process self, long timeout, TimeUnit unit) throws InterruptedException {
        long remainingNanos = unit.toNanos(timeout); // throw NPE before other conditions
        if (hasExited(self))
            return true;
        if (timeout <= 0)
            return false;

        long deadline = System.nanoTime() + remainingNanos;
        do {
            Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(remainingNanos) + 1, 100));
            if (hasExited(self))
                return true;
            remainingNanos = deadline - System.nanoTime();
        } while (remainingNanos > 0);

        return false;
    }

    private static boolean hasExited(Process self) {
        try {
            self.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            return false;
        }
    }

    @Stub
    public static Process destroyForcibly(Process self) {
        self.destroy();
        return self;
    }

    @Stub
    public static boolean isAlive(Process self) {
        try {
            self.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

}
