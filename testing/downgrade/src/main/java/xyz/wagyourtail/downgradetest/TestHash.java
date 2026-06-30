package xyz.wagyourtail.downgradetest;

import java.util.zip.CRC32C;

public class TestHash {

    static void main() {
        CRC32C crc32c = new CRC32C();
        crc32c.update("Hello, World!".getBytes());
        System.out.println(crc32c.getValue());
        crc32c.reset();
        System.out.println(crc32c.getValue());
    }

}
