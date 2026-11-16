package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.*;
import java.util.Objects;

public class J_T_LocalTime {

    @Stub(ref = @Ref("Ljava/time/LocalTime;"))
    public static LocalTime ofInstant(Instant instant, ZoneId zone) {
        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(zone, "zone");
        // JDK computes the nanos-of-day directly; atZone() overflows on
        // Instant.MIN/MAX
        ZoneOffset offset = zone.getRules().getOffset(instant);
        long localSecond = instant.getEpochSecond() + offset.getTotalSeconds();
        long secsOfDay = Math.floorMod(localSecond, 86400);
        return LocalTime.ofNanoOfDay(secsOfDay * 1000000000L + instant.getNano());
    }

    @Stub
    public static long toEpochSecond(LocalTime time, LocalDate date, ZoneOffset offset) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(offset, "offset");
        long secs = date.toEpochDay() * 60 * 60 * 24 + time.toSecondOfDay();
        return secs - offset.getTotalSeconds();
    }

}
