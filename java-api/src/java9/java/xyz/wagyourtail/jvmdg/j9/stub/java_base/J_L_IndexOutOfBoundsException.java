package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_IndexOutOfBoundsException {

    @Stub(ref = @Ref(value = "Ljava/lang/IndexOutOfBoundsException;", member = "<init>"))
    public static IndexOutOfBoundsException init(int index) {
        return new IndexOutOfBoundsException("Index out of range: " + index);
    }

}
