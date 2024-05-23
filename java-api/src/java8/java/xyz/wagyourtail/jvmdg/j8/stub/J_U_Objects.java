package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Supplier;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_U_Objects {

    @Stub(ref = @Ref("java/util/Objects"))
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    @Stub(ref = @Ref("java/util/Objects"))
    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    @Stub(ref = @Ref("java/util/Objects"))
    public static <T> T requireNonNull(T obj, J_U_F_Supplier<String> messageSupplier) {
        if (obj == null) {
            throw new NullPointerException(messageSupplier == null ? null : messageSupplier.get());
        }
        return obj;
    }

}
