package xyz.wagyourtail.jvmdg.j26.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.Closeable;
import java.io.IOException;

public class J_L_Process {

    @Stub
    public static void close(Process self) throws IOException {
        synchronized (self) {
            IOException ioe = quietClose(self.outputWriter(), null);
            ioe = quietClose(self.inputReader(), ioe);
            ioe = quietClose(self.errorReader(), ioe);

            if (tryWait(self)) {
                self.destroyForcibly();
                while (tryWait(self)) {
                    Thread.onSpinWait();
                }

                Thread.currentThread().interrupt();
            }

            if (ioe != null) {
                throw ioe;
            }
        }
    }

    private static boolean tryWait(Process self) {
        try {
            self.waitFor();
            return false;
        } catch (InterruptedException e) {
            return true;
        }
    }

    private static IOException quietClose(Closeable c, IOException firstIOE) {
        try {
            c.close();
        } catch (IOException ioe) {
            if (firstIOE == null) return ioe;
            firstIOE.addSuppressed(ioe);
        }
        return firstIOE;
    }

}
