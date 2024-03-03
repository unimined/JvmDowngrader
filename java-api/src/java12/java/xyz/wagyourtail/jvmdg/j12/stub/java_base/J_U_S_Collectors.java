package xyz.wagyourtail.jvmdg.j12.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collector;

public class J_U_S_Collectors {

    @Stub(ref = @Ref("Ljava/util/stream/Collectors;"))
    public static <T, R1, R2, R> Collector<? super T, ?, R> teeing(Collector<? super T, ?, R1> downstream1, Collector<? super T, ?, R2> downstream2, BiFunction<? super R1, ? super R2, R> merger) {
        return new Teeing<>(downstream1, downstream2, merger).collector();
    }

    public static class Teeing<T, A1, A2, R1, R2, R> {

        private final BiFunction<? super R1, ? super R2, R> merger;

        private final Supplier<A1> supplier1;
        private final Supplier<A2> supplier2;

        private final BiConsumer<A1, ? super T> accumulator1;
        private final BiConsumer<A2, ? super T> accumulator2;

        private final BinaryOperator<A1> combiner1;
        private final BinaryOperator<A2> combiner2;

        private final Function<A1, R1> finisher1;
        private final Function<A2, R2> finisher2;

        private final Set<Collector.Characteristics> characteristics;

        public Teeing(Collector<? super T, A1, R1> downstream1, Collector<? super T, A2, R2> downstream2, BiFunction<? super R1, ? super R2, R> merger) {
            Objects.requireNonNull(downstream1);
            Objects.requireNonNull(downstream2);
            Objects.requireNonNull(merger);

            this.merger = merger;

            this.supplier1 = Objects.requireNonNull(downstream1.supplier());
            this.supplier2 = Objects.requireNonNull(downstream2.supplier());

            this.accumulator1 = Objects.requireNonNull(downstream1.accumulator());
            this.accumulator2 = Objects.requireNonNull(downstream2.accumulator());

            this.combiner1 = Objects.requireNonNull(downstream1.combiner());
            this.combiner2 = Objects.requireNonNull(downstream2.combiner());

            this.finisher1 = Objects.requireNonNull(downstream1.finisher());
            this.finisher2 = Objects.requireNonNull(downstream2.finisher());

            Set<Collector.Characteristics> chars1 = downstream1.characteristics();
            Set<Collector.Characteristics> chars2 = downstream2.characteristics();
            Set<Collector.Characteristics> CH_ID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
            if (CH_ID.containsAll(chars1) || CH_ID.containsAll(chars2)) {
                this.characteristics = Collections.emptySet();
            } else {
                EnumSet<Collector.Characteristics> c = EnumSet.noneOf(Collector.Characteristics.class);
                c.addAll(chars1);
                c.retainAll(chars2);
                c.remove(Collector.Characteristics.IDENTITY_FINISH);
                this.characteristics = Collections.unmodifiableSet(c);
            }
        }

        public Collector<T, PairBox, R> collector() {
            return Collector.of(
                PairBox::new,
                PairBox::add,
                PairBox::combine,
                PairBox::get,
                characteristics.toArray(new Collector.Characteristics[0])
            );
        }

        class PairBox {
            A1 left = supplier1.get();
            A2 right = supplier2.get();

            void add(T t) {
                accumulator1.accept(left, t);
                accumulator2.accept(right, t);
            }

            PairBox combine(PairBox other) {
                left = combiner1.apply(left, other.left);
                right = combiner2.apply(right, other.right);
                return this;
            }

            R get() {
                R1 r1 = finisher1.apply(left);
                R2 r2 = finisher2.apply(right);
                return merger.apply(r1, r2);
            }

        }

    }

}
