package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.ZoneId;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.Locale;
import java.util.Optional;

public class J_T_F_DateTimeFormatter {
    // sun.util.locale.provider.TimeZoneNameUtility.convertLDMLShortID is
    // JDK-internal and absent on older runtimes — resolve lazily and fall
    // back to the formatter's own zone when it cannot be reached
    private static final MethodHandle convertLDMLShortID = resolveLdmlShortId();

    private static MethodHandle resolveLdmlShortId() {
        try {
            MethodHandles.Lookup lookup = Utils.getImplLookup();
            return lookup.findStatic(Class.forName("sun.util.locale.provider.TimeZoneNameUtility"), "convertLDMLShortID", MethodType.methodType(Optional.class, String.class));
        } catch (Throwable t) {
            return null;
        }
    }

    @Stub
    public static DateTimeFormatter localizedBy(DateTimeFormatter self, Locale locale) throws Throwable {
        if (self.getLocale().equals(locale)) {
            return self;
        }

        Chronology c = locale.getUnicodeLocaleType("ca") != null ? Chronology.ofLocale(locale) : self.getChronology();
        DecimalStyle d = locale.getUnicodeLocaleType("nu") != null ? DecimalStyle.of(locale) : self.getDecimalStyle();
        String tzType = locale.getUnicodeLocaleType("tz");
        ZoneId z = tzType != null && convertLDMLShortID != null
            ? ((Optional<String>) convertLDMLShortID.invokeExact(tzType)).map(ZoneId::of).orElse(self.getZone())
            : self.getZone();

        return self.withLocale(locale).withChronology(c).withDecimalStyle(d).withZone(z);
    }

}
