package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/DoubleConsumer"))
public interface J_U_F_DoubleConsumer {

    void accept(double value);

    J_U_F_DoubleConsumer andThen(J_U_F_DoubleConsumer after);

    class DoubleConsumerDefaults {

        @Stub(abstractDefault = true)
        public static J_U_F_DoubleConsumer andThen(final J_U_F_DoubleConsumer c1, final J_U_F_DoubleConsumer c2) {
            Objects.requireNonNull(c2);
            return new J_U_F_DoubleConsumer.DoubleConsumerAdapter() {
                @Override
                public void accept(double value) {
                    c1.accept(value);
                    c2.accept(value);
                }
            };
        }

    }

    abstract class DoubleConsumerAdapter implements J_U_F_DoubleConsumer {

        @Override
        public J_U_F_DoubleConsumer andThen(J_U_F_DoubleConsumer after) {
            return DoubleConsumerDefaults.andThen(this, after);
        }

    }

}
