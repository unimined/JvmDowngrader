package xyz.wagyourtail.jvmdg.j16.stub.java_base;


import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

@InitRewrite(ref = @Ref(value = "Ljava/lang/IndexOutOfBoundsException;", member = "<init>", desc = "(J)V"), to = "(Ljava/lang/String;)V", keep = {-1})
public class J_L_IndexOutOfBoundsException {

    // IndexOutOfBoundsException(long) -> IndexOutOfBoundsException("Index out of range: " + index)
    // explicit StringBuilder keeps the stub class itself indy-free
    @InitArg(target = "(J)V", slot = 0)
    public static String msg(long index) {
        return new StringBuilder().append("Index out of range: ").append(index).toString();
    }

}
