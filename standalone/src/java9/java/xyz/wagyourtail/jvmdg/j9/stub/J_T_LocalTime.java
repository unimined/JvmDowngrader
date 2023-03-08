package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.time.*;
import java.util.Objects;

public class J_T_LocalTime {

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/time/LocalTime;"))
    public static LocalTime ofInstant(Instant instant, ZoneId zone) {
        ZonedDateTime zdt = instant.atZone(zone);
        return zdt.toLocalTime();
    }

    @Stub(javaVersion = Opcodes.V9)
    public static long toEpochSecond(LocalTime time, LocalDate date, ZoneOffset offset) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(offset, "offset");
        long secs = date.toEpochDay() * 60 * 60 * 24 + time.toSecondOfDay();
        return secs - offset.getTotalSeconds();
    }

}
