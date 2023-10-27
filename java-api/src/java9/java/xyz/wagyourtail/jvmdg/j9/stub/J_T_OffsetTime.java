package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.LocalDate;
import java.time.OffsetTime;
import java.util.Objects;

public class J_T_OffsetTime {

    @Stub
    public static long toEpochSecond(OffsetTime time, LocalDate date) {
        Objects.requireNonNull(date, "date");
        long epochDay = date.toEpochDay();
        long secs = epochDay * 86400 + time.toLocalTime().toSecondOfDay();
        return secs - time.getOffset().getTotalSeconds();
    }

}
