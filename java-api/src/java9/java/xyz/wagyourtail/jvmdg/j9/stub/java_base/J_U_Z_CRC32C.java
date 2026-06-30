package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.zip.Checksum;

@Adapter("java/util/zip/CRC32C")
public class J_U_Z_CRC32C implements Checksum {
    private static final int[] CRC32C_TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) == 1) {
                    crc = (crc >>> 1) ^ 0x82F63B78;
                } else {
                    crc >>>= 1;
                }
            }
            CRC32C_TABLE[i] = crc;
        }
    }

    int crc = ~0;

    @Override
    public void update(int b) {
        int index = (crc ^ b) & 0xFF;
        crc = (crc >>> 8) ^ CRC32C_TABLE[index];
    }

    @Override
    public void update(byte[] b, int off, int len) {
        for (byte bi : b) {
            int index = (crc ^ bi) & 0xFF;
            crc = (crc >>> 8) ^ CRC32C_TABLE[index];
        }
    }

    @Override
    public long getValue() {
        return ~crc;
    }

    @Override
    public void reset() {
        crc = ~0;
    }

}
