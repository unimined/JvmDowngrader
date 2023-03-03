package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class J_U_Scanner {

    @Stub(value = JavaVersion.VERSION_1_10, desc = "Ljava/util/Scanner;<init>")
    public static Scanner init(ReadableByteChannel source, Charset charset) {
        return new Scanner(source, charset.name());
    }

}
