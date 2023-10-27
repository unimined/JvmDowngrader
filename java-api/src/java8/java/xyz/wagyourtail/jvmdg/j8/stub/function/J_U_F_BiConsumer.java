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

    J_U_F_BiConsumer<T, U> andThen(J_U_F_BiConsumer<? super T, ? super U> after);

    class BiConsumerDefaults {

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static <T, U> J_U_F_BiConsumer<T, U> andThen(final J_U_F_BiConsumer<T, U> c1, final J_U_F_BiConsumer<? super T, ? super U> c2) {
            return new J_U_F_BiConsumer<T, U>() {
                @Override
                public void accept(T t, U u) {
                    c1.accept(t, u);
                    c2.accept(t, u);
                }

                @Override
                public J_U_F_BiConsumer<T, U> andThen(J_U_F_BiConsumer<? super T, ? super U> after) {
                    Objects.requireNonNull(after);
                    return BiConsumerDefaults.andThen(this, after);
                }
            };
        }
    }

}
