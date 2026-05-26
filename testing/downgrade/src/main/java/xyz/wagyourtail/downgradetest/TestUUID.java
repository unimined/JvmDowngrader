package xyz.wagyourtail.downgradetest;

import java.util.UUID;

public class TestUUID {

    void main() {
        long time = 1779693843L;
        UUID uuid = UUID.ofEpochMillis(time);
        long time2 = uuid.getMostSignificantBits() >>> 16;
        System.out.println(time);
        System.out.println(time2);

        System.out.println(uuid.variant());
        System.out.println(uuid.version());
    }

}
