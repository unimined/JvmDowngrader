package xyz.wagyourtail.jvmdg.j25.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Currency;
import java.util.stream.Stream;

public class J_U_Currency {

    @Stub(ref = @Ref("java/util/Currency"))
    public static Stream<Currency> availableCurrencies() {
        return Currency.getAvailableCurrencies().stream();
    }

}
