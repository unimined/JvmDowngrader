package xyz.wagyourtail.jvmdg.j12.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class J_I_InputStream {

    @Stub
    public static void skipNBytes(InputStream stream, long n) throws IOException {
        while (n > 0) {
            long ns = stream.skip(n);
            if (ns == 0) {
                if (stream.read() == -1) {
                    throw new EOFException();
                }
                n--;
            }
            if (ns > n) {
                throw new IOException("Unable to skip exactly");
            }
            n -= ns;
        }
    }

}
