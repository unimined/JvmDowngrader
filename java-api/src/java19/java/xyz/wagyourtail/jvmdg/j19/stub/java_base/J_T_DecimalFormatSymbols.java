package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class J_T_DecimalFormatSymbols {
    // resolved lazily: the trusted lookup reaches the private field even on
    // modular runtimes where setAccessible is refused; plain reflection is the
    // fallback when no trusted lookup can be constructed
    private static final MethodHandle localeGetter = resolveLocaleGetter();

    private static MethodHandle resolveLocaleGetter() {
        try {
            return Utils.getImplLookup().findGetter(DecimalFormatSymbols.class, "locale", Locale.class);
        } catch (Throwable t) {
            return null;
        }
    }

    @Stub
    public static Locale getLocale(DecimalFormatSymbols dfs) throws Throwable {
        if (localeGetter != null) {
            return (Locale) localeGetter.invokeExact(dfs);
        }
        Field field = DecimalFormatSymbols.class.getDeclaredField("locale");
        field.setAccessible(true);
        return (Locale) field.get(dfs);
    }

}
