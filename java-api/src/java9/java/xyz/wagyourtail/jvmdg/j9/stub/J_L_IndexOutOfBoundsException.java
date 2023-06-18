package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_IndexOutOfBoundsException {

    @Stub(opcVers = Opcodes.V9, ref = @Ref(value = "Ljava/lang/IndexOutOfBoundsException", member = "<init>"))
    public static IndexOutOfBoundsException init(int index) {
        return new IndexOutOfBoundsException("Index out of range: " + index);
    }

}
