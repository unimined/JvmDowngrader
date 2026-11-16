package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.file.attribute.FileTime;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.zip.ZipEntry;

public class J_U_Z_ZipEntry {

    @Stub
    public static void setTimeLocal(ZipEntry entry, LocalDateTime time) {
        ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(time);
        entry.setLastModifiedTime(FileTime.from(time.toInstant(offset)));
    }

    @Stub
    public static LocalDateTime getTimeLocal(ZipEntry entry) {
        FileTime ft = entry.getLastModifiedTime();
        if (ft == null) {
            // the default DOS-time fields decode to an invalid month
            throw new DateTimeException("Invalid value for MonthOfYear (valid values 1 - 12): 15");
        }
        return LocalDateTime.ofInstant(ft.toInstant(), ZoneId.systemDefault());
    }

}
