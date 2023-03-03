package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.time.*;
import java.util.Objects;

public class J_T_LocalTime {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/time/LocalTime;")
    public static LocalTime ofInstant(Instant instant, ZoneId zone) {
        var zdt = instant.atZone(zone);
        return zdt.toLocalTime();
    }

    @Stub(JavaVersion.VERSION_1_9)
    public static long toEpochSecond(LocalTime time, LocalDate date, ZoneOffset offset) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(offset, "offset");
        long secs = date.toEpochDay() * 60 * 60 * 24 + time.toSecondOfDay();
        return secs - offset.getTotalSeconds();
    }
}
