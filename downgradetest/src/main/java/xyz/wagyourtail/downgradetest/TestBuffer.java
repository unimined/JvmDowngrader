package xyz.wagyourtail.downgradetest;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class TestBuffer {

    public static void main(String[] args) {
        var a = ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        a.position(4);
        var b = new byte[a.remaining()];
        a.get(b);
        System.out.println(Arrays.toString(b));
    }

}
