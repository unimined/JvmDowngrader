package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Locale;

public class J_U_Locale {

    @Stub(ref = @Ref("java/util/Locale"))
    public static Locale of(String language) {
        return new Locale(language);
    }

    @Stub(ref = @Ref("java/util/Locale"))
    public static Locale of(String language, String country) {
        return new Locale(language, country);
    }

    @Stub(ref = @Ref("java/util/Locale"))
    public static Locale of(String language, String country, String variant) {
        return new Locale(language, country, variant);
    }

}
