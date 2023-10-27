package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/BiConsumer"))
public interface J_U_F_BiConsumer<T, U> {

    void accept(T t, U u);

    default J_U_F_BiConsumer<T, U> andThen(J_U_F_BiConsumer<? super T, ? super U> after) {
        Objects.requireNonNull(after);
        return (l, r) -> {
            accept(l, r);
            after.accept(l, r);
        };
    }

}
