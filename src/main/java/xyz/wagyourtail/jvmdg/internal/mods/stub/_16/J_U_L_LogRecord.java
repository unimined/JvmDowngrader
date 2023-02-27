package xyz.wagyourtail.jvmdg.internal.mods.stub._16;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.logging.LogRecord;

public class J_U_L_LogRecord {

    @Stub(value = JavaVersion.VERSION_16)
    public static long getLongThreadID(LogRecord record) {
        return record.getThreadID();
    }

    @Stub(value = JavaVersion.VERSION_16)
    public static void setLongThreadID(LogRecord record, long threadID) {
        if (threadID >= 0 && threadID <= Integer.MAX_VALUE) {
            record.setThreadID((int) threadID);
        } else {
            int hash = Long.hashCode(threadID);
            record.setThreadID(hash < 0 ? hash : (-1 - hash));
        }
    }
}
