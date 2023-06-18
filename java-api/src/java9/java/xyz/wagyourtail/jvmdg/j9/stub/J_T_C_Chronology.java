package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.time.ZoneOffset;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.temporal.ChronoField;
import java.util.Objects;

public class J_T_C_Chronology {

    @Stub(opcVers = Opcodes.V9, subtypes = true)
    public static long epochSecond(Chronology self, int prolepticYear, int month, int dayOfMonth, int hour, int minute, int second, ZoneOffset zoneOffset) {
        Objects.requireNonNull(zoneOffset, "zoneOffset");
        ChronoField.HOUR_OF_DAY.checkValidValue(hour);
        ChronoField.MINUTE_OF_HOUR.checkValidValue(minute);
        ChronoField.SECOND_OF_MINUTE.checkValidValue(second);
        long daysInSec = J_L_Math.multiplyExact(self.date(prolepticYear, month, dayOfMonth).toEpochDay(), 86400);
        long timeInSec = (hour * 60L + minute) * 60L + second;
        return Math.addExact(daysInSec, timeInSec - zoneOffset.getTotalSeconds());
    }

    @Stub(opcVers = Opcodes.V9, subtypes = true)
    public static long epochSecond(Chronology self, Era era, int yearOfEra, int month, int dayOfMonth, int hour, int minute, int second, ZoneOffset zoneOffset) {
        Objects.requireNonNull(era, "era");
        Objects.requireNonNull(zoneOffset, "zoneOffset");
        ChronoField.HOUR_OF_DAY.checkValidValue(hour);
        ChronoField.MINUTE_OF_HOUR.checkValidValue(minute);
        ChronoField.SECOND_OF_MINUTE.checkValidValue(second);
        long daysInSec = J_L_Math.multiplyExact(self.date(era, yearOfEra, month, dayOfMonth).toEpochDay(), 86400);
        long timeInSec = (hour * 60L + minute) * 60L + second;
        return Math.addExact(daysInSec, timeInSec - zoneOffset.getTotalSeconds());
    }

}
