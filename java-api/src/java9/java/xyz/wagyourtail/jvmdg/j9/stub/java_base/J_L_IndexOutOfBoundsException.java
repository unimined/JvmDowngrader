package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

@InitRewrite(ref = @Ref(value = "Ljava/lang/IndexOutOfBoundsException;", member = "<init>", desc = "(I)V"), to = "(Ljava/lang/String;)V", keep = {-1})
public class J_L_IndexOutOfBoundsException {

    // IndexOutOfBoundsException(int) -> IndexOutOfBoundsException("Index out of range: " + index)
    @InitArg(target = "(I)V", slot = 0)
    public static String msg(int index) {
        return "Index out of range: " + index;
    }

}
