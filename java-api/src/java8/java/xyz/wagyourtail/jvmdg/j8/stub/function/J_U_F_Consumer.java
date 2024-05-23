package xyz.wagyourtail.jvmdg.j8.stub.function;

import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Adapter("Ljava/util/function/Consumer;")
public interface J_U_F_Consumer<T> {

    void accept(T t);

    J_U_F_Consumer<T> andThen(J_U_F_Consumer<? super T> after);

    class ConsumerDefaults {

        @Stub(abstractDefault = true)
        public static <T> J_U_F_Consumer<T> andThen(final J_U_F_Consumer<T> c1, final J_U_F_Consumer<? super T> c2) {

            Objects.requireNonNull(c2);
            return new J_U_F_Consumer.ConsumerAdapter<T>() {
                @Override
                public void accept(T t) {
                    c1.accept(t);
                    c2.accept(t);
                }
            };
        }

    }

    abstract class ConsumerAdapter<T> implements J_U_F_Consumer<T> {

        @Override
        public J_U_F_Consumer<T> andThen(J_U_F_Consumer<? super T> after) {
            return ConsumerDefaults.andThen(this, after);
        }

    }

}
