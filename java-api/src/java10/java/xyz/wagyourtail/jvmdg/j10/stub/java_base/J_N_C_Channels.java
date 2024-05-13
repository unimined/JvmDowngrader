package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Objects;

public class J_N_C_Channels {

    @Stub(ref = @Ref("java/nio/channels/Channels"))
    public static Reader newReader(ReadableByteChannel channel, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        return Channels.newReader(channel, charset.newDecoder(), -1);
    }

    @Stub(ref = @Ref("java/nio/channels/Channels"))
    public static Writer newWriter(WritableByteChannel channel, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        return Channels.newWriter(channel, charset.newEncoder(), -1);
    }

}
