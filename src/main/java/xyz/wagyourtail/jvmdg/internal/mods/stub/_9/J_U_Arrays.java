package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Comparator;
import java.util.Objects;

public class J_U_Arrays {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static boolean equals(
        long[] a, int aFromIndex, int aToIndex,
        long[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static boolean equals(
        int[] a, int aFromIndex, int aToIndex,
        int[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static boolean equals(
        short[] a, int aFromIndex, int aToIndex,
        short[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static boolean equals(
        char[] a, int aFromIndex, int aToIndex,
        char[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static boolean equals(
        byte[] a, int aFromIndex, int aToIndex,
        byte[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static boolean equals(
        boolean[] a, int aFromIndex, int aToIndex,
        boolean[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static boolean equals(
        double[] a, int aFromIndex, int aToIndex,
        double[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static boolean equals(
        float[] a, int aFromIndex, int aToIndex,
        float[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static boolean equals(
        Object[] a, int aFromIndex, int aToIndex,
        Object[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (!Objects.equals(a[aFromIndex + i], b[bFromIndex + i])) {
                return false;
            }
        }
        return true;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static <T> boolean equals(
        T[] a, T[] b, Comparator<? super T> comparator
    ) {
        int aLength = a.length;
        int bLength = b.length;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (comparator.compare(a[i], b[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static <T> boolean equals(
        T[] a, int aFromIndex, int aToIndex,
        T[] b, int bFromIndex, int bToIndex,
        Comparator<? super T> comparator
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (comparator.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return false;
            }
        }
        return true;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(boolean[] a, boolean[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Boolean.compare(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(
        boolean[] a, int aFromIndex, int aToIndex,
        boolean[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Boolean.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(byte[] a, byte[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Byte.compare(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(
        byte[] a, int aFromIndex, int aToIndex,
        byte[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Byte.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compareUnsigned(byte[] a, byte[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Byte.compareUnsigned(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compareUnsigned(
        byte[] a, int aFromIndex, int aToIndex,
        byte[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Byte.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(short[] a, short[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Short.compare(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(
        short[] a, int aFromIndex, int aToIndex,
        short[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Short.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compareUnsigned(short[] a, short[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Short.compareUnsigned(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compareUnsigned(
        short[] a, int aFromIndex, int aToIndex,
        short[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Short.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(char[] a, char[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Character.compare(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(
        char[] a, int aFromIndex, int aToIndex,
        char[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Character.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(int[] a, int[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Integer.compare(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(
        int[] a, int aFromIndex, int aToIndex,
        int[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Integer.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compareUnsigned(int[] a, int[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Integer.compareUnsigned(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compareUnsigned(
        int[] a, int aFromIndex, int aToIndex,
        int[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Integer.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(long[] a, long[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Long.compare(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(
        long[] a, int aFromIndex, int aToIndex,
        long[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Long.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compareUnsigned(long[] a, long[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Long.compareUnsigned(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compareUnsigned(
        long[] a, int aFromIndex, int aToIndex,
        long[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Long.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(float[] a, float[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Float.compare(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(
        float[] a, int aFromIndex, int aToIndex,
        float[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Float.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(double[] a, double[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Double.compare(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int compare(
        double[] a, int aFromIndex, int aToIndex,
        double[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = Double.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static <T extends Comparable<? super T>> int compare(T[] a, T[] b) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = a[i].compareTo(b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static <T extends Comparable<? super T>> int compare(
        T[] a, int aFromIndex, int aToIndex,
        T[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = a[aFromIndex + i].compareTo(b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static <T> int compare(T[] a, T[] b, Comparator<? super T> c) {
        if (a == b) return 0;
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        int aLength = a.length;
        int bLength = b.length;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = c.compare(a[i], b[i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static <T> int compare(
        T[] a, int aFromIndex, int aToIndex,
        T[] b, int bFromIndex, int bToIndex,
        Comparator<? super T> c
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            int result = c.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (result != 0) {
                return result;
            }
        }
        return aLength - bLength;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int mismatch(boolean[] a, boolean[] b) {
        int length = Math.min(a.length, b.length);
        if (a == b) return -1;
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        if (a.length != b.length) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;", include = ArraySupport.class)
    public static int mismatch(
        boolean[] a, int aFromIndex, int aToIndex,
        boolean[] b, int bFromIndex, int bToIndex
    ) {
        ArraySupport.rangeCheck(a.length, aFromIndex, aToIndex);
        ArraySupport.rangeCheck(b.length, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return i;
            }
        }
        if (aLength != bLength) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int mismatch(byte[] a, byte[] b) {
        int length = Math.min(a.length, b.length);
        if (a == b) return -1;
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        if (a.length != b.length) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;", include = ArraySupport.class)
    public static int mismatch(
        byte[] a, int aFromIndex, int aToIndex,
        byte[] b, int bFromIndex, int bToIndex
    ) {
        ArraySupport.rangeCheck(a.length, aFromIndex, aToIndex);
        ArraySupport.rangeCheck(b.length, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return i;
            }
        }
        if (aLength != bLength) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int mismatch(char[] a, char[] b) {
        int length = Math.min(a.length, b.length);
        if (a == b) return -1;
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        if (a.length != b.length) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;", include = ArraySupport.class)
    public static int mismatch(
        char[] a, int aFromIndex, int aToIndex,
        char[] b, int bFromIndex, int bToIndex
    ) {
        ArraySupport.rangeCheck(a.length, aFromIndex, aToIndex);
        ArraySupport.rangeCheck(b.length, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return i;
            }
        }
        if (aLength != bLength) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int mismatch(short[] a, short[] b) {
        int length = Math.min(a.length, b.length);
        if (a == b) return -1;
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        if (a.length != b.length) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;", include = ArraySupport.class)
    public static int mismatch(
        short[] a, int aFromIndex, int aToIndex,
        short[] b, int bFromIndex, int bToIndex
    ) {
        ArraySupport.rangeCheck(a.length, aFromIndex, aToIndex);
        ArraySupport.rangeCheck(b.length, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return i;
            }
        }
        if (aLength != bLength) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int mismatch(int[] a, int[] b) {
        int length = Math.min(a.length, b.length);
        if (a == b) return -1;
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        if (a.length != b.length) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;", include = ArraySupport.class)
    public static int mismatch(
        int[] a, int aFromIndex, int aToIndex,
        int[] b, int bFromIndex, int bToIndex
    ) {
        ArraySupport.rangeCheck(a.length, aFromIndex, aToIndex);
        ArraySupport.rangeCheck(b.length, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return i;
            }
        }
        if (aLength != bLength) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int mismatch(long[] a, long[] b) {
        int length = Math.min(a.length, b.length);
        if (a == b) return -1;
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        if (a.length != b.length) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;", include = ArraySupport.class)
public static int mismatch(
        long[] a, int aFromIndex, int aToIndex,
        long[] b, int bFromIndex, int bToIndex
    ) {
        ArraySupport.rangeCheck(a.length, aFromIndex, aToIndex);
        ArraySupport.rangeCheck(b.length, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return i;
            }
        }
        if (aLength != bLength) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int mismatch(float[] a, float[] b) {
        int length = Math.min(a.length, b.length);
        if (a == b) return -1;
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        if (a.length != b.length) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;", include = ArraySupport.class)
    public static int mismatch(
        float[] a, int aFromIndex, int aToIndex,
        float[] b, int bFromIndex, int bToIndex
    ) {
        ArraySupport.rangeCheck(a.length, aFromIndex, aToIndex);
        ArraySupport.rangeCheck(b.length, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return i;
            }
        }
        if (aLength != bLength) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int mismatch(double[] a, double[] b) {
        int length = Math.min(a.length, b.length);
        if (a == b) return -1;
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        if (a.length != b.length) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;", include = ArraySupport.class)
    public static int mismatch(
        double[] a, int aFromIndex, int aToIndex,
        double[] b, int bFromIndex, int bToIndex
    ) {
        ArraySupport.rangeCheck(a.length, aFromIndex, aToIndex);
        ArraySupport.rangeCheck(b.length, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return i;
            }
        }
        if (aLength != bLength) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static int mismatch(Object[] a, Object[] b) {
        int length = Math.min(a.length, b.length);
        if (a == b) return -1;
        for (int i = 0; i < length; i++) {
            if (!Objects.equals(a[i], b[i])) {
                return i;
            }
        }
        if (a.length != b.length) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;", include = ArraySupport.class)
    public static int mismatch(
        Object[] a, int aFromIndex, int aToIndex,
        Object[] b, int bFromIndex, int bToIndex
    ) {
        ArraySupport.rangeCheck(a.length, aFromIndex, aToIndex);
        ArraySupport.rangeCheck(b.length, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            if (!Objects.equals(a[aFromIndex + i], b[bFromIndex + i])) {
                return i;
            }
        }
        if (aLength != bLength) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;")
    public static <T> int mismatch(T[] a, T[] b, Comparator<? super T> comparator) {
        int length = Math.min(a.length, b.length);
        if (a == b) return -1;
        for (int i = 0; i < length; i++) {
            if (comparator.compare(a[i], b[i]) != 0) {
                return i;
            }
        }
        if (a.length != b.length) {
            return length;
        }
        return -1;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Arrays;", include = ArraySupport.class)
    public static <T> int mismatch(
        T[] a, int aFromIndex, int aToIndex,
        T[] b, int bFromIndex, int bToIndex,
        Comparator<? super T> comparator
    ) {
        ArraySupport.rangeCheck(a.length, aFromIndex, aToIndex);
        ArraySupport.rangeCheck(b.length, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            if (comparator.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return i;
            }
        }
        if (aLength != bLength) {
            return length;
        }
        return -1;
    }

    public static class ArraySupport {
        public static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
            if (fromIndex > toIndex) {
                throw new IllegalArgumentException(
                    "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
            }
            if (fromIndex < 0) {
                throw new ArrayIndexOutOfBoundsException(fromIndex);
            }
            if (toIndex > arrayLength) {
                throw new ArrayIndexOutOfBoundsException(toIndex);
            }
        }
    }

}
