package xyz.wagyourtail.jvmdg.j8.stub.function;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/LongConsumer"))
public interface J_U_F_LongConsumer {

    void accept(long value);

    J_U_F_LongConsumer andThen(J_U_F_LongConsumer after);

    class LongConsumerDefaults {

        @Stub(abstractDefault = true)
        public static J_U_F_LongConsumer andThen(final J_U_F_LongConsumer self, final J_U_F_LongConsumer after) {
            return new J_U_F_LongConsumer.LongConsumerAdapter() {
                @Override
                public void accept(long value) {
                    self.accept(value);
                    after.accept(value);
                }
            };
        }

    }

    abstract class LongConsumerAdapter implements J_U_F_LongConsumer {

        @Override
        public J_U_F_LongConsumer andThen(J_U_F_LongConsumer after) {
            return LongConsumerDefaults.andThen(this, after);
        }

    }
}
