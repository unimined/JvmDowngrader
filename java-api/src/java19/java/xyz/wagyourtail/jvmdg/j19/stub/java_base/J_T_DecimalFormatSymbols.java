package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Field;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class J_T_DecimalFormatSymbols {

    @Stub
    public static Locale getLocale(DecimalFormatSymbols dfs) throws NoSuchFieldException, IllegalAccessException {
        Field field = DecimalFormatSymbols.class.getDeclaredField("locale");
        field.setAccessible(true);
        return (Locale) field.get(dfs);
    }

}
