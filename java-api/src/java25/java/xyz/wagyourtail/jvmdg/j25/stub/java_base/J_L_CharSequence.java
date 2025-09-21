package xyz.wagyourtail.jvmdg.j25.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

public class J_L_CharSequence {

    @Stub(excludeChild = "java/lang/String")
    public static void getChars(CharSequence self, int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (self instanceof String str) {
            str.getChars(srcBegin, srcEnd, dst, dstBegin);
            return;
        }
        Objects.checkFromToIndex(srcBegin, srcEnd, self.length());
        Objects.checkIndex(dstBegin, dst.length - (srcEnd - srcBegin) + 1);
        self.subSequence(srcBegin, srcEnd).toString().getChars(0, srcEnd - srcBegin, dst, dstBegin);
    }

}
