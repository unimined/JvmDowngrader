package xyz.wagyourtail.jvmdg.j27.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class J_L_String {

    @Stub
    public static int encodedLength(String self, Charset charset) {
        int length = self.length();
        if (charset.equals(StandardCharsets.UTF_8)) {
            int size = 0;
            for (int i = 0; i < length; i++) {
                char c = self.charAt(i);
                if (c < 0x80) {
                    size++;
                } else if (c < 0x800) {
                    size += 2;
                } else if (Character.isHighSurrogate(c)) {
                    if (i + 1 < length && Character.isLowSurrogate(self.charAt(i + 1))) {
                        size += 4;
                        i++;
                    } else {
                        // unpaired surrogate is replaced with '?'
                        size++;
                    }
                } else if (Character.isLowSurrogate(c)) {
                    // unpaired surrogate is replaced with '?'
                    size++;
                } else {
                    size += 3;
                }
            }
            return size;
        }

        if (charset.equals(StandardCharsets.US_ASCII) || charset.equals(StandardCharsets.ISO_8859_1)) {
            // every code point encodes to exactly one byte, so a surrogate pair counts once
            int size = length;
            for (int i = 0; i < length; i++) {
                if (Character.isHighSurrogate(self.charAt(i)) &&
                        i + 1 < length && Character.isLowSurrogate(self.charAt(i + 1))) {
                    size--;
                    i++;
                }
            }
            return size;
        }

        if (charset.equals(StandardCharsets.UTF_16)) {
            // 2 byte bom, only emitted for non-empty input
            return length == 0 ? 0 : 2 + length * 2;
        }

        if (charset.equals(StandardCharsets.UTF_16BE) || charset.equals(StandardCharsets.UTF_16LE)) {
            return length * 2;
        }

        return self.getBytes(charset).length;
    }

}
