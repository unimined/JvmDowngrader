package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.time.LocalDate;
import java.time.OffsetTime;
import java.util.Objects;

public class J_T_OffsetTime {

    @Stub(JavaVersion.VERSION_1_9)
    public static long toEpochSecond(OffsetTime time, LocalDate date) {
        Objects.requireNonNull(date, "date");
        long epochDay = date.toEpochDay();
        long secs = epochDay * 86400 + time.toLocalTime().toSecondOfDay();
        return secs - time.getOffset().getTotalSeconds();
    }
}
