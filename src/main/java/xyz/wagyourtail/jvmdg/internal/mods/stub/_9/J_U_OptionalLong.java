
package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class J_U_OptionalLong {

    @Stub(JavaVersion.VERSION_1_9)
    public static void ifPresentOrElse(OptionalLong optional, LongConsumer action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.getAsLong());
        } else {
            emptyAction.run();
        }
    }

    @Stub(JavaVersion.VERSION_1_9)
    public static LongStream stream(OptionalLong optional) {
        return optional.isPresent() ? LongStream.of(optional.getAsLong()) : LongStream.empty();
    }
}
