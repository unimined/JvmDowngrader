package xyz.wagyourtail.jvmdg.j16.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_IndexOutOfBoundsException {
    @Stub(ref = @Ref(value = "java/lang/IndexOutOfBoundsException", member = "<init>"))
    public static IndexOutOfBoundsException init(long i) {
        return new IndexOutOfBoundsException("Index out of range: " + i);
    }

}
  