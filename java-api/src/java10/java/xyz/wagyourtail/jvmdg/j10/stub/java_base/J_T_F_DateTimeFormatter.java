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
    private static final MethodHandle convertLDMLShortID;

    static {
        MethodHandles.Lookup lookup = Utils.getImplLookup();
        try {
            convertLDMLShortID = lookup.findStatic(Class.forName("sun.util.locale.provider.TimeZoneNameUtility"), "convertLDMLShortID", MethodType.methodType(Optional.class, String.class));
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
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
        ZoneId z = tzType != null ? ((Optional<String>) convertLDMLShortID.invokeExact(tzType)).map(ZoneId::of).orElse(self.getZone()) : self.getZone();

        return self.withLocale(locale).withChronology(c).withDecimalStyle(d).withZone(z);
    }

}
