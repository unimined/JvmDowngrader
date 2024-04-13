package xyz.wagyourtail.jvmdg.j16.stub;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.logging.LogRecord;

public class J_U_L_LogRecord {

    @Stub
    public static long getLongThreadID(LogRecord record) {
        return record.getThreadID();
    }

    @Stub
    public static void setLongThreadID(LogRecord record, long threadID) {
        if (threadID >= 0 && threadID <= Integer.MAX_VALUE) {
            record.setThreadID((int) threadID);
        } else {
            int hash = Long.hashCode(threadID);
            record.setThreadID(hash < 0 ? hash : (-1 - hash));
        }
    }

}
