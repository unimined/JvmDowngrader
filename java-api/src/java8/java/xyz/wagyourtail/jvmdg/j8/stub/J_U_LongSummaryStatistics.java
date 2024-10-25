package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;
import xyz.wagyourtail.jvmdg.version.Adapter;

@Adapter("java/util/LongSummaryStatistics")
public class J_U_LongSummaryStatistics implements J_U_F_IntConsumer, J_U_F_LongConsumer {

    private long count;
    private long sum;
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;


    @Override
    public void accept(int value) {
        accept((long) value);
    }

    public void combine(J_U_LongSummaryStatistics other) {
        count += other.count;
        sum += other.sum;
        min = Math.min(min, other.min);
        max = Math.max(max, other.max);
    }

    public final long getCount() {
        return count;
    }

    public final long getSum() {
        return sum;
    }

    public final long getMin() {
        return min;
    }

    public final long getMax() {
        return max;
    }

    public final double getAverage() {
        return getCount() > 0 ? (double) getSum() / getCount() : 0.0d;
    }

    @Override
    public String toString() {
        return "IntSummaryStatistics{" +
            "count=" + count +
            ", sum=" + sum +
            ", min=" + min +
            ", max=" + max +
            '}';
    }

    @Override
    public J_U_F_IntConsumer andThen(J_U_F_IntConsumer after) {
        return IntConsumerDefaults.andThen(this, after);
    }

    @Override
    public void accept(long value) {
        ++count;
        sum += value;
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    @Override
    public J_U_F_LongConsumer andThen(J_U_F_LongConsumer after) {
        return LongConsumerDefaults.andThen(this, after);
    }

}
