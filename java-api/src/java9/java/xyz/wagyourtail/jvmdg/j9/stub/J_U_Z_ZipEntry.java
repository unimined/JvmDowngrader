package xyz.wagyourtail.jvmdg.j9.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.file.attribute.FileTime;
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
        return LocalDateTime.ofInstant(entry.getLastModifiedTime().toInstant(), ZoneId.systemDefault());
    }

}
