package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.time.DateTimeException;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.util.Objects;

import static java.time.temporal.ChronoField.*;

public class J_T_C_IsoChronology {

    @Stub(javaVersion = Opcodes.V9)
    public static long epochSecond(
        IsoChronology self, int prolepticYear, int month, int dayOfMonth,
        int hour, int minute, int second, ZoneOffset zoneOffset
    ) {
        YEAR.checkValidValue(prolepticYear);
        MONTH_OF_YEAR.checkValidValue(month);
        DAY_OF_MONTH.checkValidValue(dayOfMonth);
        HOUR_OF_DAY.checkValidValue(hour);
        MINUTE_OF_HOUR.checkValidValue(minute);
        SECOND_OF_MINUTE.checkValidValue(second);
        Objects.requireNonNull(zoneOffset, "zoneOffset");
        if (dayOfMonth > 28) {
            int dom;
            switch (month) {
                case 2:
                    dom = IsoChronology.INSTANCE.isLeapYear(prolepticYear) ? 29 : 28;
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    dom = 30;
                    break;
                default:
                    dom = 31;
                    break;
            }
            ;
            if (dayOfMonth > dom) {
                if (dayOfMonth == 29) {
                    throw new DateTimeException(
                        "Invalid date 'February 29' as '" + prolepticYear + "' is not a leap year");
                } else {
                    throw new DateTimeException("Invalid date '" + Month.of(month).name() + " " + dayOfMonth + "'");
                }
            }
        }

        long totalDays = 0;
        int timeinSec = 0;
        totalDays += 365L * prolepticYear;
        if (prolepticYear >= 0) {
            totalDays += (prolepticYear + 3L) / 4 - (prolepticYear + 99L) / 100 + (prolepticYear + 399L) / 400;
        } else {
            totalDays -= prolepticYear / -4 - prolepticYear / -100 + prolepticYear / -400;
        }
        totalDays += (367L * month - 362) / 12;
        totalDays += dayOfMonth - 1;
        if (month > 2) {
            totalDays--;
            if (!IsoChronology.INSTANCE.isLeapYear(prolepticYear)) {
                totalDays--;
            }
        }
        totalDays -= ((146097 * 5L) - (30L * 365L + 7L));
        timeinSec = (hour * 60 + minute) * 60 + second;
        return Math.addExact(Math.multiplyExact(totalDays, 86400L), timeinSec - zoneOffset.getTotalSeconds());
    }

}
