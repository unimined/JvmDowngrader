package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

@Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Base64"))
public class J_U_Base64 {

    private J_U_Base64() {}

    public static Encoder getEncoder() {
        return Encoder.RFC4648;
    }

    public static Encoder getUrlEncoder() {
        return Encoder.RFC4648_URLSAFE;
    }

    public static Encoder getMimeEncoder() {
        return Encoder.RFC2045;
    }

    public static Encoder getMimeEncoder(int lineLength, byte[] lineSeparator) {
        Objects.requireNonNull(lineSeparator);
        int[] base64 = Decoder.fromBase64;
        for (byte b : lineSeparator) {
            if (base64[b & 0xff] != -1)
                throw new IllegalArgumentException(
                    "Illegal base64 line separator character 0x" + Integer.toString(b, 16));
        }
        // round down to nearest multiple of 4
        lineLength &= ~0b11;
        if (lineLength <= 0) {
            return Encoder.RFC4648;
        }
        return new Encoder(false, lineSeparator, lineLength, true);
    }

    public static Decoder getDecoder() {
        return Decoder.RFC4648;
    }

    public static Decoder getUrlDecoder() {
        return Decoder.RFC4648_URLSAFE;
    }

    public static Decoder getMimeDecoder() {
        return Decoder.RFC2045;
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Base64$Encoder"))
    public static class Encoder {
        private final byte[] newline;
        private final int linemax;
        private final boolean isURL;
        private final boolean doPadding;

        private Encoder(boolean isURL, byte[] newline, int linemax, boolean doPadding) {
            this.isURL = isURL;
            this.newline = newline;
            this.linemax = linemax;
            this.doPadding = doPadding;
        }

        private static final char[] toBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

        private static final char[] toBase64URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();

        private static final int MIMELINEMAX = 76;

        private static final byte[] CRLF = new byte[] {'\r', '\n'};

        static final Encoder RFC4648 = new Encoder(false, null, -1, true);
        static final Encoder RFC4648_URLSAFE = new Encoder(true, null, -1, true);
        static final Encoder RFC2045 = new Encoder(false, CRLF, MIMELINEMAX, true);

        private final int encodedOutLength(int srclen, boolean throwOOME) {
            int len = 0;
            try {
                if (doPadding) {
                    len = J_L_Math.multiplyExact(4, (J_L_Math.addExact(srclen, 2) / 3));
                } else {
                    int n = srclen % 3;
                    len = J_L_Math.addExact(J_L_Math.multiplyExact(4, (srclen / 3)), (n == 0 ? 0 : n + 1));
                }
                if (linemax > 0) {
                    len = J_L_Math.addExact(len, (len - 1) / linemax * newline.length);
                }
            } catch (ArithmeticException ex) {
                if (throwOOME) {
                    throw new OutOfMemoryError("Encoded size is too large");
                } else {
                    len = -1;
                }
            }
            return len;
        }

        public byte[] encode(byte[] src) {
            int len = encodedOutLength(src.length, true);
            byte[] dst = new byte[len];
            int ret = encode0(src, 0, src.length, dst);
            if (ret != dst.length)
                return Arrays.copyOf(dst, ret);
            return dst;
        }

        public int encode(byte[] src, byte[] dst) {
            int len = encodedOutLength(src.length, false);
            if (len > dst.length)
                throw new IllegalArgumentException("Output byte array is too small for encoding all input bytes");
            return encode0(src, 0, src.length, dst);
        }

        public String encodeToString(byte[] src) {
            byte[] encoded = encode(src);
            return new String(encoded, 0, 0, encoded.length);
        }

        public ByteBuffer encode(ByteBuffer buffer) {
            int len = encodedOutLength(buffer.remaining(), false);
            byte[] dst = new byte[len];
            int ret = 0;
            if (buffer.hasArray()) {
                ret = encode0(buffer.array(),
                              buffer.arrayOffset() + buffer.position(),
                              buffer.arrayOffset() + buffer.limit(),
                              dst);
                buffer.position(buffer.limit());
            } else {
                byte[] src = new byte[buffer.remaining()];
                buffer.get(src);
                ret = encode0(src, 0, src.length, dst);
            }
            if (ret != dst.length)
                dst = Arrays.copyOf(dst, ret);
            return ByteBuffer.wrap(dst);
        }

        public OutputStream wrap(OutputStream os) {
            Objects.requireNonNull(os);
            return new EncOutputStream(os, isURL ? toBase64URL : toBase64, newline, linemax, doPadding);
        }

        public Encoder withoutPadding() {
            if (!doPadding) {
                return this;
            }
            return new Encoder(isURL, newline, linemax, false);
        }

        private void encodeBlock(byte[] src, int sp, int sl, byte[] dst, int dp, boolean isURL) {
            char[] base64 = isURL ? toBase64URL : toBase64;
            for (int sp0 = sp, dp0 = dp ; sp0 < sl; ) {
                int bits = (src[sp0++] & 0xff) << 16 | (src[sp0++] & 0xff) <<  8 | (src[sp0++] & 0xff);
                dst[dp0++] = (byte)base64[(bits >>> 18) & 0x3f];
                dst[dp0++] = (byte)base64[(bits >>> 12) & 0x3f];
                dst[dp0++] = (byte)base64[(bits >>> 6)  & 0x3f];
                dst[dp0++] = (byte)base64[bits & 0x3f];
            }
        }

        private int encode0(byte[] src, int off, int end, byte[] dst) {
            char[] base64 = isURL ? toBase64URL : toBase64;
            int sp = off;
            int slen = (end - off) / 3 * 3;
            int sl = off + slen;
            if (linemax > 0 && slen  > linemax / 4 * 3)
                slen = linemax / 4 * 3;
            int dp = 0;
            while (sp < sl) {
                int sl0 = Math.min(sp + slen, sl);
                encodeBlock(src, sp, sl0, dst, dp, isURL);
                int dlen = (sl0 - sp) / 3 * 4;
                dp += dlen;
                sp = sl0;
                if (dlen == linemax && sp < end) {
                    for (byte b : newline){
                        dst[dp++] = b;
                    }
                }
            }
            if (sp < end) {               // 1 or 2 leftover bytes
                int b0 = src[sp++] & 0xff;
                dst[dp++] = (byte)base64[b0 >> 2];
                if (sp == end) {
                    dst[dp++] = (byte)base64[(b0 << 4) & 0x3f];
                    if (doPadding) {
                        dst[dp++] = '=';
                        dst[dp++] = '=';
                    }
                } else {
                    int b1 = src[sp++] & 0xff;
                    dst[dp++] = (byte)base64[(b0 << 4) & 0x3f | (b1 >> 4)];
                    dst[dp++] = (byte)base64[(b1 << 2) & 0x3f];
                    if (doPadding) {
                        dst[dp++] = '=';
                    }
                }
            }
            return dp;
        }

    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Base64$Decoder"))
    public static class Decoder {
        private final boolean isURL;
        private final boolean isMIME;

        private Decoder(boolean isURL, boolean isMIME) {
            this.isURL = isURL;
            this.isMIME = isMIME;
        }

        private static final int[] fromBase64 = new int[256];
        static {
            Arrays.fill(fromBase64, -1);
            for (int i = 0; i < Encoder.toBase64.length; i++)
                fromBase64[Encoder.toBase64[i]] = i;
            fromBase64['='] = -2;
        }

        private static final int[] fromBase64URL = new int[256];

        static {
            Arrays.fill(fromBase64URL, -1);
            for (int i = 0; i < Encoder.toBase64URL.length; i++)
                fromBase64URL[Encoder.toBase64URL[i]] = i;
            fromBase64URL['='] = -2;
        }

        static final Decoder RFC4648 = new Decoder(false, false);
        static final Decoder RFC4648_URLSAFE = new Decoder(true, false);
        static final Decoder RFC2045 = new Decoder(false, true);

        public byte[] decode(byte[] src) {
            int len = decodedOutLength(src, 0, src.length);
            byte[] dst = new byte[len];
            int ret = decode0(src, 0, src.length, dst);
            if (ret != dst.length)
                return Arrays.copyOf(dst, ret);
            return dst;
        }

        public byte[] decode(String src) {
            return decode(src.getBytes(StandardCharsets.ISO_8859_1));
        }

        public int decode(byte[] src, byte[] dst) {
            int len = decodedOutLength(src, 0, src.length);
            if (dst.length < len)
                throw new IllegalArgumentException("Output byte array is too small for decoding all input bytes");
            return decode0(src, 0, src.length, dst);
        }

        public ByteBuffer decode(ByteBuffer buffer) {
            int pos0 = buffer.position();
            try {
                byte[] src;
                int sp, sl;
                if (buffer.hasArray()) {
                    src = buffer.array();
                    sp = buffer.arrayOffset() + buffer.position();
                    sl = buffer.arrayOffset() + buffer.limit();
                    buffer.position(buffer.limit());
                } else {
                    src = new byte[buffer.remaining()];
                    buffer.get(src);
                    sp = 0;
                    sl = src.length;
                }
                byte[] dst = new byte[decodedOutLength(src, sp, sl)];
                return ByteBuffer.wrap(dst, 0, decode0(src, sp, sl, dst));
            } catch (IllegalArgumentException iae) {
                buffer.position(pos0);
                throw iae;
            }
        }

        public InputStream wrap(InputStream is) {
            Objects.requireNonNull(is);
            return new DecInputStream(is, isURL ? fromBase64URL : fromBase64, isMIME);
        }

        private int decodedOutLength(byte[] src, int sp, int sl) {
            int[] base64 = isURL ? fromBase64URL : fromBase64;
            int paddings = 0;
            int len = sl - sp;
            if (len == 0)
                return 0;
            if (len < 2) {
                if (isMIME && base64[0] == -1)
                    return 0;
                throw new IllegalArgumentException(
                    "Input byte[] should at least have 2 bytes for base64 bytes");
            }
            if (isMIME) {
                int n = 0;
                while (sp < sl) {
                    int b = src[sp++] & 0xff;
                    if (b == '=') {
                        len -= (sl - sp + 1);
                        break;
                    }
                    if ((b = base64[b]) == -1)
                        n++;
                }
                len -= n;
            } else {
                if (src[sl - 1] == '=') {
                    paddings++;
                    if (src[sl - 2] == '=')
                        paddings++;
                }
            }
            if (paddings == 0 && (len & 0x3) !=  0)
                paddings = 4 - (len & 0x3);
            return 3 * (int) ((len + 3L) / 4) - paddings;
        }


        private int decodeBlock(byte[] src, int sp, int sl, byte[] dst, int dp, boolean isURL, boolean isMIME) {
            int[] base64 = isURL ? fromBase64URL : fromBase64;
            int sl0 = sp + ((sl - sp) & ~0b11);
            int new_dp = dp;
            while (sp < sl0) {
                int b1 = base64[src[sp++] & 0xff];
                int b2 = base64[src[sp++] & 0xff];
                int b3 = base64[src[sp++] & 0xff];
                int b4 = base64[src[sp++] & 0xff];
                if ((b1 | b2 | b3 | b4) < 0) {    // non base64 byte
                    return new_dp - dp;
                }
                int bits0 = b1 << 18 | b2 << 12 | b3 << 6 | b4;
                dst[new_dp++] = (byte)(bits0 >> 16);
                dst[new_dp++] = (byte)(bits0 >>  8);
                dst[new_dp++] = (byte)(bits0);
            }
            return new_dp - dp;
        }

        private int decode0(byte[] src, int sp, int sl, byte[] dst) {
            int[] base64 = isURL ? fromBase64URL : fromBase64;
            int dp = 0;
            int bits = 0;
            int shiftto = 18;
            while (sp < sl) {
                if (shiftto == 18 && sp < sl - 4) {
                    int dl = decodeBlock(src, sp, sl, dst, dp, isURL, isMIME);
                    int chars_decoded = ((dl + 2) / 3) * 4;

                    sp += chars_decoded;
                    dp += dl;
                }
                if (sp >= sl) {
                    // we're done
                    break;
                }
                int b = src[sp++] & 0xff;
                if ((b = base64[b]) < 0) {
                    if (b == -2) {
                        if (shiftto == 6 && (sp == sl || src[sp++] != '=') ||
                            shiftto == 18) {
                            throw new IllegalArgumentException(
                                "Input byte array has wrong 4-byte ending unit");
                        }
                        break;
                    }
                    if (isMIME)
                        continue;
                    else
                        throw new IllegalArgumentException(
                            "Illegal base64 character " +
                                Integer.toString(src[sp - 1], 16));
                }
                bits |= (b << shiftto);
                shiftto -= 6;
                if (shiftto < 0) {
                    dst[dp++] = (byte)(bits >> 16);
                    dst[dp++] = (byte)(bits >>  8);
                    dst[dp++] = (byte)(bits);
                    shiftto = 18;
                    bits = 0;
                }
            }
            if (shiftto == 6) {
                dst[dp++] = (byte)(bits >> 16);
            } else if (shiftto == 0) {
                dst[dp++] = (byte)(bits >> 16);
                dst[dp++] = (byte)(bits >>  8);
            } else if (shiftto == 12) {
                throw new IllegalArgumentException(
                    "Last unit does not have enough valid bits");
            }
            while (sp < sl) {
                if (isMIME && base64[src[sp++] & 0xff] < 0)
                    continue;
                throw new IllegalArgumentException(
                    "Input byte array has incorrect ending byte at " + sp);
            }
            return dp;
        }
    }

    private static class EncOutputStream extends FilterOutputStream {

        private int leftover = 0;
        private int b0, b1, b2;
        private boolean closed = false;

        private final char[] base64;    // byte->base64 mapping
        private final byte[] newline;   // line separator, if needed
        private final int linemax;
        private final boolean doPadding;// whether or not to pad
        private int linepos = 0;
        private byte[] buf;

        EncOutputStream(OutputStream os, char[] base64,
                        byte[] newline, int linemax, boolean doPadding) {
            super(os);
            this.base64 = base64;
            this.newline = newline;
            this.linemax = linemax;
            this.doPadding = doPadding;
            this.buf = new byte[linemax <= 0 ? 8124 : linemax];
        }

        @Override
        public void write(int b) throws IOException {
            byte[] buf = new byte[1];
            buf[0] = (byte)(b & 0xff);
            write(buf, 0, 1);
        }

        private void checkNewline() throws IOException {
            if (linepos == linemax) {
                out.write(newline);
                linepos = 0;
            }
        }

        private void writeb4(char b1, char b2, char b3, char b4) throws IOException {
            buf[0] = (byte)b1;
            buf[1] = (byte)b2;
            buf[2] = (byte)b3;
            buf[3] = (byte)b4;
            out.write(buf, 0, 4);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (closed)
                throw new IOException("Stream is closed");
            if (len == 0)
                return;
            if (leftover != 0) {
                if (leftover == 1) {
                    b1 = b[off++] & 0xff;
                    len--;
                    if (len == 0) {
                        leftover++;
                        return;
                    }
                }
                b2 = b[off++] & 0xff;
                len--;
                checkNewline();
                writeb4(base64[b0 >> 2],
                    base64[(b0 << 4) & 0x3f | (b1 >> 4)],
                    base64[(b1 << 2) & 0x3f | (b2 >> 6)],
                    base64[b2 & 0x3f]);
                linepos += 4;
            }
            int nBits24 = len / 3;
            leftover = len - (nBits24 * 3);

            while (nBits24 > 0) {
                checkNewline();
                int dl = linemax <= 0 ? buf.length : buf.length - linepos;
                int sl = off + Math.min(nBits24, dl / 4) * 3;
                int dp = 0;
                for (int sp = off; sp < sl; ) {
                    int bits = (b[sp++] & 0xff) << 16 |
                        (b[sp++] & 0xff) <<  8 |
                        (b[sp++] & 0xff);
                    buf[dp++] = (byte)base64[(bits >>> 18) & 0x3f];
                    buf[dp++] = (byte)base64[(bits >>> 12) & 0x3f];
                    buf[dp++] = (byte)base64[(bits >>> 6)  & 0x3f];
                    buf[dp++] = (byte)base64[bits & 0x3f];
                }
                out.write(buf, 0, dp);
                off = sl;
                linepos += dp;
                nBits24 -= dp / 4;
            }
            if (leftover == 1) {
                b0 = b[off++] & 0xff;
            } else if (leftover == 2) {
                b0 = b[off++] & 0xff;
                b1 = b[off++] & 0xff;
            }
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                if (leftover == 1) {
                    checkNewline();
                    out.write(base64[b0 >> 2]);
                    out.write(base64[(b0 << 4) & 0x3f]);
                    if (doPadding) {
                        out.write('=');
                        out.write('=');
                    }
                } else if (leftover == 2) {
                    checkNewline();
                    out.write(base64[b0 >> 2]);
                    out.write(base64[(b0 << 4) & 0x3f | (b1 >> 4)]);
                    out.write(base64[(b1 << 2) & 0x3f]);
                    if (doPadding) {
                        out.write('=');
                    }
                }
                leftover = 0;
                out.close();
            }
        }
    }

    /*
     * An input stream for decoding Base64 bytes
     */
    private static class DecInputStream extends InputStream {

        private final InputStream is;
        private final boolean isMIME;
        private final int[] base64;
        private int bits = 0;
        private int wpos = 0;
        private int rpos = 0;

        private boolean eof = false;
        private boolean closed = false;

        DecInputStream(InputStream is, int[] base64, boolean isMIME) {
            this.is = is;
            this.base64 = base64;
            this.isMIME = isMIME;
        }

        private byte[] sbBuf = new byte[1];

        @Override
        public int read() throws IOException {
            return read(sbBuf, 0, 1) == -1 ? -1 : sbBuf[0] & 0xff;
        }

        private int leftovers(byte[] b, int off, int pos, int limit) {
            eof = true;
            while (rpos - 8 >= wpos && pos != limit) {
                rpos -= 8;
                b[pos++] = (byte) (bits >> rpos);
            }
            return pos - off != 0 || rpos - 8 >= wpos ? pos - off : -1;
        }

        private int eof(byte[] b, int off, int pos, int limit) throws IOException {
            if (wpos == 18) {
                throw new IOException("Base64 stream has one un-decoded dangling byte.");
            }
            rpos = 24;
            return leftovers(b, off, pos, limit);
        }

        private int padding(byte[] b, int off, int pos, int limit) throws IOException {
            if (wpos >= 18 || wpos == 12 && is.read() != '=') {
                throw new IOException("Illegal base64 ending sequence:" + wpos);
            }
            rpos = 24;
            return leftovers(b, off, pos, limit);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (closed) {
                throw new IOException("Stream is closed");
            }
            Objects.checkFromIndexSize(off, len, b.length);
            if (len == 0) {
                return 0;
            }
            int pos = off;
            final int limit = off + len;
            if (eof) {
                return leftovers(b, off, pos, limit);
            }
            if (rpos == 16) {
                b[pos++] = (byte) (bits >> 8);
                rpos = 8;
                if (pos == limit) {
                    return len;
                }
            }
            if (rpos == 8) {
                b[pos++] = (byte) bits;
                rpos = 0;
                if (pos == limit) {
                    return len;
                }
            }

            bits = 0;
            wpos = 24;
            while (true) {
                final int i = is.read();
                if (i < 0) {
                    return eof(b, off, pos, limit);
                }
                final int v = base64[i];
                if (v < 0) {
                    if (v == -1) {
                        if (isMIME) {
                            continue;
                        }
                        throw new IOException("Illegal base64 character 0x" +
                            Integer.toHexString(i));
                    }
                    return padding(b, off, pos, limit);
                }
                wpos -= 6;
                bits |= v << wpos;
                if (wpos != 0) {
                    continue;
                }
                if (limit - pos >= 3) {
                    b[pos++] = (byte) (bits >> 16);
                    b[pos++] = (byte) (bits >> 8);
                    b[pos++] = (byte) bits;
                    bits = 0;
                    wpos = 24;
                    if (pos == limit) {
                        return len;
                    }
                    continue;
                }
                b[pos++] = (byte) (bits >> 16);
                if (pos == limit) {
                    rpos = 16;
                    return len;
                }
                b[pos++] = (byte) (bits >> 8);
                rpos = 8;
                return len;
            }
        }

        @Override
        public int available() throws IOException {
            if (closed)
                throw new IOException("Stream is closed");
            return is.available();   // TBD:
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                is.close();
            }
        }
    }

}
