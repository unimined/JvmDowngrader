package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;

public class J_T_C_Chronology {

    @Stub
    public static boolean isIsoBased(Chronology chrono) {
        return chrono.equals(IsoChronology.INSTANCE);
    }

}
