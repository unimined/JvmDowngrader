package xyz.wagyourtail.jvmdg.j25.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Arrays;
import java.util.TimeZone;
import java.util.stream.Stream;

public class J_U_TimeZone {

    @Stub(ref = @Ref("java/util/TimeZone"))
    public static Stream<String> availableIDs() {
        return Arrays.stream(TimeZone.getAvailableIDs());
    }

    @Stub(ref = @Ref("java/util/TimeZone"))
    public static Stream<String> availableIDs(int rawOffset) {
        return Arrays.stream(TimeZone.getAvailableIDs(rawOffset));
    }

}
