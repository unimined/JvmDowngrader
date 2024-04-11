package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_DoubleConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

@Adapter("java/util/DoubleSummaryStatistics")
public class J_U_DoubleSummaryStatistics implements J_U_F_DoubleConsumer {
    private long count;
    private double sum;
    private double sumCompensation; // Low order bits of sum
    private double simpleSum; // Used to compute right sum for non-finite inputs
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;
    @Override
    public void accept(double value) {
        ++count;
        simpleSum += value;
        sumWithCompensation(value);
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    public void combine(J_U_DoubleSummaryStatistics other) {
        count += other.count;
        simpleSum += other.simpleSum;
        sumWithCompensation(other.sum);
        sumWithCompensation(-other.sumCompensation);
        min = Math.min(min, other.min);
        max = Math.max(max, other.max);
    }

    private void sumWithCompensation(double value) {
        double tmp = value - sumCompensation;
        double velvel = sum + tmp; // Little wolf of rounding error
        sumCompensation = (velvel - sum) - tmp;
        sum = velvel;
    }

    public final long getCount() {
        return count;
    }

    public final double getSum() {
        double tmp = sum - sumCompensation;
        if (Double.isNaN(tmp) && Double.isInfinite(simpleSum)) {
            return simpleSum;
        }
        return tmp;
    }

    public final double getMin() {
        return min;
    }

    public final double getMax() {
        return max;
    }

    public final double getAverage() {
        return getCount() > 0 ? getSum() / getCount() : 0.0d;
    }

    @Override
    public String toString() {
        return "DoubleSummaryStatistics{" +
                "count=" + count +
                ", sum=" + getSum() +
                ", min=" + min +
                ", average=" + getAverage() +
                ", max=" + max +
                '}';
    }

    @Override
    public J_U_F_DoubleConsumer andThen(J_U_F_DoubleConsumer after) {
        return null;
    }
}
