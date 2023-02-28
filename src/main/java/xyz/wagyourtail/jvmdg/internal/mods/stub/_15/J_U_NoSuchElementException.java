package xyz.wagyourtail.jvmdg.internal.mods.stub._15;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.lang.reflect.Method;
import java.util.NoSuchElementException;

public class J_U_NoSuchElementException {

    @Stub(value = JavaVersion.VERSION_15, desc = "Ljava/util/NoSuchElementException;<init>")
    public static NoSuchElementException create(String s, Throwable cause) throws NoSuchMethodException {
        var nse = new NoSuchElementException(s);
        // call setCause
        Method setCause = Throwable.class.getDeclaredMethod("setCause", Throwable.class);
        setCause.setAccessible(true);
        try {
            setCause.invoke(nse, cause);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nse;
    }

    @Stub(value = JavaVersion.VERSION_15, desc = "Ljava/util/NoSuchElementException;<init>")
    public static NoSuchElementException create(Throwable cause) throws NoSuchMethodException {
        var nse = new NoSuchElementException(cause==null ? null : cause.toString());
        // call setCause
        Method setCause = Throwable.class.getDeclaredMethod("setCause", Throwable.class);
        setCause.setAccessible(true);
        try {
            setCause.invoke(nse, cause);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nse;
    }
}
