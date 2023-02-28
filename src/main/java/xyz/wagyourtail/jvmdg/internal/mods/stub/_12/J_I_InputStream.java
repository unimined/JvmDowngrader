package xyz.wagyourtail.jvmdg.internal.mods.stub._12;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class J_I_InputStream {

    @Stub(JavaVersion.VERSION_12)
    public static void skipNBytes(InputStream stream, long n) throws IOException {
        while (n > 0) {
            long ns = stream.skip(n);
            if (ns == 0) {
                if (stream.read() == -1) {
                    throw new EOFException();
                }
                n--;
            }
            if (ns < 0 || ns > n) {
                throw new IOException("Unable to skip exactly");
            }
            n -= ns;
        }
    }
}
