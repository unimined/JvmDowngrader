package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class J_U_C_TimeUnit {

    @Stub(JavaVersion.VERSION_1_9)
    public static ChronoUnit toChronoUnit(TimeUnit tu) {
        return switch (tu) {
            case NANOSECONDS -> ChronoUnit.NANOS;
            case MICROSECONDS -> ChronoUnit.MICROS;
            case MILLISECONDS -> ChronoUnit.MILLIS;
            case SECONDS -> ChronoUnit.SECONDS;
            case MINUTES -> ChronoUnit.MINUTES;
            case HOURS -> ChronoUnit.HOURS;
            case DAYS -> ChronoUnit.DAYS;
            default -> throw new AssertionError();
        };
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/concurrent/TimeUnit;")
    public static TimeUnit of(ChronoUnit unit) {
        return switch (unit) {
            case NANOS -> TimeUnit.NANOSECONDS;
            case MICROS -> TimeUnit.MICROSECONDS;
            case MILLIS -> TimeUnit.MILLISECONDS;
            case SECONDS -> TimeUnit.SECONDS;
            case MINUTES -> TimeUnit.MINUTES;
            case HOURS -> TimeUnit.HOURS;
            case DAYS -> TimeUnit.DAYS;
            default -> throw new IllegalArgumentException("No TimeUnit equivalent for " + unit);
        };
    }
}
