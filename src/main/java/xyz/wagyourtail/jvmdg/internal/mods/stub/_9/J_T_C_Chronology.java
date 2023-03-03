package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.time.ZoneOffset;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.temporal.ChronoField;
import java.util.Objects;

public class J_T_C_Chronology {

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
    public static long epochSecond(Chronology self, int prolepticYear, int month, int dayOfMonth, int hour, int minute, int second, ZoneOffset zoneOffset) {
        Objects.requireNonNull(zoneOffset, "zoneOffset");
        ChronoField.HOUR_OF_DAY.checkValidValue(hour);
        ChronoField.MINUTE_OF_HOUR.checkValidValue(minute);
        ChronoField.SECOND_OF_MINUTE.checkValidValue(second);
        long daysInSec = Math.multiplyExact(self.date(prolepticYear, month, dayOfMonth).toEpochDay(), 86400);
        long timeInSec = (hour * 60L + minute) * 60L + second;
        return Math.addExact(daysInSec, timeInSec - zoneOffset.getTotalSeconds());
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
    public static long epochSecond(Chronology self, Era era, int yearOfEra, int month, int dayOfMonth, int hour, int minute, int second, ZoneOffset zoneOffset) {
        Objects.requireNonNull(era, "era");
        Objects.requireNonNull(zoneOffset, "zoneOffset");
        ChronoField.HOUR_OF_DAY.checkValidValue(hour);
        ChronoField.MINUTE_OF_HOUR.checkValidValue(minute);
        ChronoField.SECOND_OF_MINUTE.checkValidValue(second);
        long daysInSec = Math.multiplyExact(self.date(era, yearOfEra, month, dayOfMonth).toEpochDay(), 86400);
        long timeInSec = (hour * 60L + minute) * 60L + second;
        return Math.addExact(daysInSec, timeInSec - zoneOffset.getTotalSeconds());
    }
}
