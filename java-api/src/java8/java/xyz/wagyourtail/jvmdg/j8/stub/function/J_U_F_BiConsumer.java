package xyz.wagyourtail.jvmdg.j8.stub.function;

import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Adapter("Ljava/util/function/BiConsumer;")
public interface J_U_F_BiConsumer<T, U> {

    void accept(T t, U u);

    J_U_F_BiConsumer<T, U> andThen(J_U_F_BiConsumer<? super T, ? super U> after);

    class BiConsumerDefaults {

        @Stub(abstractDefault = true)
        public static <T, U> J_U_F_BiConsumer<T, U> andThen(final J_U_F_BiConsumer<T, U> c1, final J_U_F_BiConsumer<? super T, ? super U> c2) {
            Objects.requireNonNull(c2);
            return new J_U_F_BiConsumer.BiConsumerAdapter<T, U>() {
                @Override
                public void accept(T t, U u) {
                    c1.accept(t, u);
                    c2.accept(t, u);
                }
            };
        }
    }

    abstract class BiConsumerAdapter<T, U> implements J_U_F_BiConsumer<T, U> {

        @Override
        public J_U_F_BiConsumer<T, U> andThen(J_U_F_BiConsumer<? super T, ? super U> after) {
            return BiConsumerDefaults.andThen(this, after);
        }
    }

}
