package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.*;
import java.util.Objects;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class J_T_LocalDate {

    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/time/LocalDate;"))
    public static LocalDate ofInstant(Instant instant, ZoneId zone) {
        ZonedDateTime zdt = instant.atZone(zone);
        return zdt.toLocalDate();
    }

    @Stub(opcVers = Opcodes.V9)
    public static Stream<LocalDate> datesUntil(LocalDate start, LocalDate endExclusive) {
        long startEpochDay = start.toEpochDay();
        long endEpochDay = endExclusive.toEpochDay();
        if (endEpochDay < startEpochDay) {
            throw new IllegalArgumentException(endExclusive + " < " + start);
        }
        return LongStream.range(startEpochDay, endEpochDay).mapToObj(LocalDate::ofEpochDay);
    }

    @Stub(opcVers = Opcodes.V9, include = DatesUntil.class)
    public static Stream<LocalDate> datesUntil(LocalDate self, LocalDate endExclusive, Period step) {
        return new DatesUntil(self, endExclusive, step).dateStream();
    }

    @Stub(opcVers = Opcodes.V9)
    public static long toEpochSecond(LocalDate date, LocalTime time, ZoneOffset off) {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(off, "off");
        long secs = date.toEpochDay() * 60 * 60 * 24 + time.toSecondOfDay();
        return secs - off.getTotalSeconds();
    }

    public static class DatesUntil {
        private final long months;
        private final long days;

        private final LocalDate from;
        private final LocalDate to;

        private final long start;
        private final long end;

        public DatesUntil(LocalDate from, LocalDate to, Period step) {
            if (step.isZero()) {
                throw new IllegalArgumentException("step is zero");
            }
            long months = step.toTotalMonths();
            long days = step.getDays();
            if ((months < 0 && days > 0) || (months > 0 && days < 0)) {
                throw new IllegalArgumentException("period months and days are of opposite sign");
            }
            this.months = months;
            this.days = days;
            this.from = from;
            this.to = to;
            end = to.toEpochDay();
            start = from.toEpochDay();
        }

        public Stream<LocalDate> dateStream() {
            long until = end - start;
            if (until == 0) {
                return Stream.empty();
            }
            int sign = months > 0 || days > 0 ? 1 : -1;
            if (sign < 0 ^ until < 0) {
                throw new IllegalArgumentException(to + (sign < 0 ? " > " : " < ") + from);
            }
            if (months == 0) {
                long steps = (until - sign) / days; // non-negative
                return LongStream.rangeClosed(0, steps).mapToObj(this::mapWithinMonth);
            }
            // 48699/1600 = 365.2425/12, no overflow, non-negative result
            long steps = until * 1600 / (months * 48699 + days * 1600) + 1;
            long addMonths = months * steps;
            long addDays = days * steps;
            long maxAddMonths = months > 0 ? getProlepticMonth(LocalDate.MAX) - getProlepticMonth(from)
                : getProlepticMonth(from) - getProlepticMonth(LocalDate.MIN);
            // adjust steps estimation
            if (addMonths * sign > maxAddMonths
                || (from.plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign) {
                steps--;
                addMonths -= months;
                addDays -= days;
                if (addMonths * sign > maxAddMonths
                    || (from.plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign) {
                    steps--;
                }
            }
            return LongStream.rangeClosed(0, steps).mapToObj(this::map);
        }

        public static long getProlepticMonth(LocalDate date) {
            return (date.getYear() * 12L + date.getMonthValue() - 1);
        }

        public LocalDate mapWithinMonth(long n) {
            return LocalDate.ofEpochDay(start + n * days);
        }

        public LocalDate map(long n) {
            return from.plusMonths(months * n).plusDays(days * n);
        }


    }

}
