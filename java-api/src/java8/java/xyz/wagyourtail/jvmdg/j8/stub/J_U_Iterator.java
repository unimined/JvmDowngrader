package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.Objects;

public class J_U_Iterator {

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/util/Iterator"), subtypes = true)
    public static void forEachRemaining(Iterator<?> iterator, J_U_F_Consumer<?> action) {
        Objects.requireNonNull(action);
        while (iterator.hasNext()) {
            action.accept(iterator.next());
        }
    }
}
