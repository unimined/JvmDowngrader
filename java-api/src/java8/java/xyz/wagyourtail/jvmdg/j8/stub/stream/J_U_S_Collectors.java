package xyz.wagyourtail.jvmdg.j8.stub.stream;

import xyz.wagyourtail.jvmdg.j8.stub.*;
import xyz.wagyourtail.jvmdg.j8.stub.function.*;
import xyz.wagyourtail.jvmdg.util.BiFunction;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Stub(ref = @Ref("java/util/stream/Collectors"))
public final class J_U_S_Collectors {

    private J_U_S_Collectors() {
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T, C extends Collection<T>> J_U_S_Collector<T, ?, C> toCollection(J_U_F_Supplier<C> collectionFactory) {
        return J_U_S_Collector.CollectorStatics.of(collectionFactory, new J_U_F_BiConsumer<C, T>() {

            @Override
            public void accept(C collection, T t) {
                collection.add(t);
            }

            @Override
            public J_U_F_BiConsumer<C, T> andThen(J_U_F_BiConsumer<? super C, ? super T> after) {
                return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
            }
        }, new J_U_F_BinaryOperator<C>() {

            @Override
            public C apply(C o, C o2) {
                o.addAll(o2);
                return o;
            }

            @Override
            public <V> J_U_F_BiFunction<C, C, V> andThen(J_U_F_Function<? super C, ? extends V> after) {
                return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
            }
        });
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T> J_U_S_Collector<T, ?, List<T>> toList() {
        return toCollection(new J_U_F_Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<>();
            }
        });
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T> J_U_S_Collector<T, ?, List<T>> toUnmodifiableList() {
        return J_U_S_Collector.CollectorStatics.of(new J_U_F_Supplier<List<T>>() {

            @Override
            public List<T> get() {
                return new ArrayList<>();
            }
        }, new J_U_F_BiConsumer<List<T>, T>() {

            @Override
            public void accept(List<T> collection, T t) {
                collection.add(t);
            }

            @Override
            public J_U_F_BiConsumer<List<T>, T> andThen(J_U_F_BiConsumer<? super List<T>, ? super T> after) {
                return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
            }
        }, new J_U_F_BinaryOperator<List<T>>() {

            @Override
            public List<T> apply(List<T> o, List<T> o2) {
                o.addAll(o2);
                return o;
            }

            @Override
            public <V> J_U_F_BiFunction<List<T>, List<T>, V> andThen(J_U_F_Function<? super List<T>, ? extends V> after) {
                return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
            }
        }, new J_U_F_Function<List<T>, List<T>>() {
            @Override
            public List<T> apply(List<T> ts) {
                return Collections.unmodifiableList(ts);
            }

            @Override
            public <V> J_U_F_Function<V, List<T>> compose(J_U_F_Function<? super V, ? extends List<T>> before) {
                return J_U_F_Function.FunctionDefaults.compose(this, before);
            }

            @Override
            public <V> J_U_F_Function<List<T>, V> andThen(J_U_F_Function<? super List<T>, ? extends V> after) {
                return J_U_F_Function.FunctionDefaults.andThen(this, after);
            }
        });
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T> J_U_S_Collector<T, ?, Set<T>> toSet() {
        return J_U_S_Collector.CollectorStatics.of(new J_U_F_Supplier<Set<T>>() {
            @Override
            public Set<T> get() {
                return new HashSet<>();
            }
        }, new J_U_F_BiConsumer<Set<T>, T>() {

            @Override
            public void accept(Set<T> collection, T t) {
                collection.add(t);
            }

            @Override
            public J_U_F_BiConsumer<Set<T>, T> andThen(J_U_F_BiConsumer<? super Set<T>, ? super T> after) {
                return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
            }
        }, new J_U_F_BinaryOperator<Set<T>>() {

            @Override
            public Set<T> apply(Set<T> o, Set<T> o2) {
                o.addAll(o2);
                return o;
            }

            @Override
            public <V> J_U_F_BiFunction<Set<T>, Set<T>, V> andThen(J_U_F_Function<? super Set<T>, ? extends V> after) {
                return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
            }
        },
        J_U_S_Collector.Characteristics.UNORDERED);
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T> J_U_S_Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return J_U_S_Collector.CollectorStatics.of(new J_U_F_Supplier<Set<T>>() {

               @Override
               public Set<T> get() {
                   return new HashSet<>();
               }
           }, new J_U_F_BiConsumer<Set<T>, T>() {

               @Override
               public void accept(Set<T> collection, T t) {
                   collection.add(t);
               }

               @Override
               public J_U_F_BiConsumer<Set<T>, T> andThen(J_U_F_BiConsumer<? super Set<T>, ? super T> after) {
                   return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
               }
           }, new J_U_F_BinaryOperator<Set<T>>() {

               @Override
               public Set<T> apply(Set<T> o, Set<T> o2) {
                   o.addAll(o2);
                   return o;
               }

               @Override
               public <V> J_U_F_BiFunction<Set<T>, Set<T>, V> andThen(J_U_F_Function<? super Set<T>, ? extends V> after) {
                   return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
               }
           }, new J_U_F_Function<Set<T>, Set<T>>() {
               @Override
               public Set<T> apply(Set<T> ts) {
                   return Collections.unmodifiableSet(ts);
               }

               @Override
               public <V> J_U_F_Function<V, Set<T>> compose(J_U_F_Function<? super V, ? extends Set<T>> before) {
                   return J_U_F_Function.FunctionDefaults.compose(this, before);
               }

               @Override
               public <V> J_U_F_Function<Set<T>, V> andThen(J_U_F_Function<? super Set<T>, ? extends V> after) {
                   return J_U_F_Function.FunctionDefaults.andThen(this, after);
               }
           },
            J_U_S_Collector.Characteristics.UNORDERED);
    }


    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static J_U_S_Collector<CharSequence, ?, String> joining() {
        return joining("", "", "");
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static J_U_S_Collector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return joining(delimiter, "", "");
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static J_U_S_Collector<CharSequence, ?, String> joining(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<J_U_StringJoiner>() {

                    @Override
                    public J_U_StringJoiner get() {
                        return new J_U_StringJoiner(delimiter, prefix, suffix);
                    }
                },
                new J_U_F_BiConsumer<J_U_StringJoiner, CharSequence>() {
                    @Override
                    public void accept(J_U_StringJoiner jUStringJoiner, CharSequence charSequence) {
                        jUStringJoiner.add(charSequence);
                    }

                    @Override
                    public J_U_F_BiConsumer<J_U_StringJoiner, CharSequence> andThen(J_U_F_BiConsumer<? super J_U_StringJoiner, ? super CharSequence> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<J_U_StringJoiner>() {
                    @Override
                    public J_U_StringJoiner apply(J_U_StringJoiner jUStringJoiner, J_U_StringJoiner jUStringJoiner2) {
                        jUStringJoiner.merge(jUStringJoiner2);
                        return jUStringJoiner;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<J_U_StringJoiner, J_U_StringJoiner, V> andThen(J_U_F_Function<? super J_U_StringJoiner, ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                },
                new J_U_F_Function<J_U_StringJoiner, String>() {
                    @Override
                    public String apply(J_U_StringJoiner jUStringJoiner) {
                        return jUStringJoiner.toString();
                    }

                    @Override
                    public <V> J_U_F_Function<V, String> compose(J_U_F_Function<? super V, ? extends J_U_StringJoiner> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<J_U_StringJoiner, V> andThen(J_U_F_Function<? super String, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T, U, A, R> J_U_S_Collector<T, ?, R> mapping(final J_U_F_Function<? super T, ? extends U> mapper, final J_U_S_Collector<? super U, A, R> downstream) {
        return new CollectorImpl<>(
                downstream.supplier(),
                new J_U_F_BiConsumer<A, T>() {
                    @Override
                    public void accept(A a, T t) {
                        downstream.accumulator().accept(a, mapper.apply(t));
                    }

                    @Override
                    public J_U_F_BiConsumer<A, T> andThen(J_U_F_BiConsumer<? super A, ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                downstream.combiner(),
                downstream.finisher(),
                downstream.characteristics()
        );
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T, A, R, RR> J_U_S_Collector<T, A, RR> collectingAndThen(J_U_S_Collector<T, A, R> downstream, J_U_F_Function<R, RR> andThen) {
        EnumSet<J_U_S_Collector.Characteristics> characteristics = EnumSet.copyOf(downstream.characteristics());
        characteristics.remove(J_U_S_Collector.Characteristics.IDENTITY_FINISH);
        return new CollectorImpl<>(
                downstream.supplier(),
                downstream.accumulator(),
                downstream.combiner(),
                downstream.finisher().andThen(andThen),
                Collections.unmodifiableSet(characteristics)
        );
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T> J_U_S_Collector<T, ?, Long> counting() {
        return summingLong(new J_U_F_ToLongFunction<T>() {
            @Override
            public long applyAsLong(T value) {
                return 1;
            }
        });
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T> J_U_S_Collector<T, ?, J_U_Optional<T>> minBy(final Comparator<? super T> comparator) {
        return reducing(new J_U_F_BinaryOperator<T>() {
            @Override
            public T apply(T t, T t2) {
                return BinaryOperatorStatics.minBy(comparator).apply(t, t2);
            }

            @Override
            public <V> J_U_F_BiFunction<T, T, V> andThen(J_U_F_Function<? super T, ? extends V> after) {
                return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
            }
        });
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T> J_U_S_Collector<T, ?, J_U_Optional<T>> maxBy(final Comparator<? super T> comparator) {
        return reducing(new J_U_F_BinaryOperator<T>() {
            @Override
            public T apply(T t, T t2) {
                return BinaryOperatorStatics.maxBy(comparator).apply(t, t2);
            }

            @Override
            public <V> J_U_F_BiFunction<T, T, V> andThen(J_U_F_Function<? super T, ? extends V> after) {
                return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
            }
        });
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T> J_U_S_Collector<T, ?, Integer> summingInt(final J_U_F_ToIntFunction<? super T> mapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<int[]>() {

                    @Override
                    public int[] get() {
                        return new int[1];
                    }
                },
                new J_U_F_BiConsumer<int[], T>() {

                    @Override
                    public void accept(int[] ints, T t) {
                        ints[0] += mapper.applyAsInt(t);
                    }

                    @Override
                    public J_U_F_BiConsumer<int[], T> andThen(J_U_F_BiConsumer<? super int[], ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<int[]>() {
                    @Override
                    public int[] apply(int[] ints, int[] ints2) {
                        ints[0] += ints2[0];
                        return ints;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<int[], int[], V> andThen(J_U_F_Function<? super int[], ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                },
                new J_U_F_Function<int[], Integer>() {
                    @Override
                    public Integer apply(int[] ints) {
                        return ints[0];
                    }

                    @Override
                    public <V> J_U_F_Function<V, Integer> compose(J_U_F_Function<? super V, ? extends int[]> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<int[], V> andThen(J_U_F_Function<? super Integer, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T> J_U_S_Collector<T, ?, Long> summingLong(final J_U_F_ToLongFunction<? super T> mapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<long[]>() {

                    @Override
                    public long[] get() {
                        return new long[1];
                    }
                },
                new J_U_F_BiConsumer<long[], T>() {

                    @Override
                    public void accept(long[] longs, T t) {
                        longs[0] += mapper.applyAsLong(t);
                    }

                    @Override
                    public J_U_F_BiConsumer<long[], T> andThen(J_U_F_BiConsumer<? super long[], ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<long[]>() {
                    @Override
                    public long[] apply(long[] longs, long[] longs2) {
                        longs[0] += longs2[0];
                        return longs;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<long[], long[], V> andThen(J_U_F_Function<? super long[], ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                },
                new J_U_F_Function<long[], Long>() {
                    @Override
                    public Long apply(long[] longs) {
                        return longs[0];
                    }

                    @Override
                    public <V> J_U_F_Function<V, Long> compose(J_U_F_Function<? super V, ? extends long[]> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<long[], V> andThen(J_U_F_Function<? super Long, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    @Stub(ref = @Ref("java/util/stream/Collectors"))
    public static <T> J_U_S_Collector<T, ?, Double> summingDouble(final J_U_F_ToDoubleFunction<? super T> mapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<double[]>() {

                    @Override
                    public double[] get() {
                        return new double[1];
                    }
                },
                new J_U_F_BiConsumer<double[], T>() {

                    @Override
                    public void accept(double[] doubles, T t) {
                        doubles[0] += mapper.applyAsDouble(t);
                    }

                    @Override
                    public J_U_F_BiConsumer<double[], T> andThen(J_U_F_BiConsumer<? super double[], ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<double[]>() {
                    @Override
                    public double[] apply(double[] doubles, double[] doubles2) {
                        doubles[0] += doubles2[0];
                        return doubles;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<double[], double[], V> andThen(J_U_F_Function<? super double[], ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                },
                new J_U_F_Function<double[], Double>() {
                    @Override
                    public Double apply(double[] longs) {
                        return longs[0];
                    }

                    @Override
                    public <V> J_U_F_Function<V, Double> compose(J_U_F_Function<? super V, ? extends double[]> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<double[], V> andThen(J_U_F_Function<? super Double, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    
    public static <T> J_U_S_Collector<T, ?, Double> averagingInt(final J_U_F_ToIntFunction<? super T> mapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<long[]>() {

                    @Override
                    public long[] get() {
                        return new long[2];
                    }
                },
                new J_U_F_BiConsumer<long[], T>() {
                    @Override
                    public void accept(long[] longs, T t) {
                        longs[0] += mapper.applyAsInt(t);
                        longs[1]++;
                    }

                    @Override
                    public J_U_F_BiConsumer<long[], T> andThen(J_U_F_BiConsumer<? super long[], ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<long[]>() {
                    @Override
                    public long[] apply(long[] longs, long[] longs2) {
                        longs[0] += longs2[0];
                        longs[1] += longs2[1];
                        return longs;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<long[], long[], V> andThen(J_U_F_Function<? super long[], ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                },
                new J_U_F_Function<long[], Double>() {
                    @Override
                    public Double apply(long[] longs) {
                        return longs[1] == 0 ? 0.0d : (double) longs[0] / longs[1];
                    }

                    @Override
                    public <V> J_U_F_Function<V, Double> compose(J_U_F_Function<? super V, ? extends long[]> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<long[], V> andThen(J_U_F_Function<? super Double, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    public static <T> J_U_S_Collector<T, ?, Double> averagingLong(final J_U_F_ToLongFunction<? super T> mapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<long[]>() {

                    @Override
                    public long[] get() {
                        return new long[2];
                    }
                },
                new J_U_F_BiConsumer<long[], T>() {
                    @Override
                    public void accept(long[] longs, T t) {
                        longs[0] += mapper.applyAsLong(t);
                        longs[1]++;
                    }

                    @Override
                    public J_U_F_BiConsumer<long[], T> andThen(J_U_F_BiConsumer<? super long[], ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<long[]>() {
                    @Override
                    public long[] apply(long[] longs, long[] longs2) {
                        longs[0] += longs2[0];
                        longs[1] += longs2[1];
                        return longs;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<long[], long[], V> andThen(J_U_F_Function<? super long[], ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                },
                new J_U_F_Function<long[], Double>() {
                    @Override
                    public Double apply(long[] longs) {
                        return longs[1] == 0 ? 0.0d : (double) longs[0] / longs[1];
                    }

                    @Override
                    public <V> J_U_F_Function<V, Double> compose(J_U_F_Function<? super V, ? extends long[]> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<long[], V> andThen(J_U_F_Function<? super Double, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    static double[] sumWithCompensation(double[] intermediateSum, double value) {
        double tmp = value - intermediateSum[1];
        double sum = intermediateSum[0];
        double velvel = sum + tmp; // Little wolf of rounding error
        intermediateSum[1] = (velvel - sum) - tmp;
        intermediateSum[0] = velvel;
        return intermediateSum;
    }

    static double computeFinalSum(double[] summands) {
        // Final sum with better error bounds subtract second summand as it is negated
        double tmp = summands[0] - summands[1];
        double simpleSum = summands[summands.length - 1];
        if (Double.isNaN(tmp) && Double.isInfinite(simpleSum))
            return simpleSum;
        else
            return tmp;
    }

    public static <T> J_U_S_Collector<T, ?, Double> averagingDouble(final J_U_F_ToDoubleFunction<? super T> mapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<double[]>() {

                    @Override
                    public double[] get() {
                        return new double[4];
                    }
                },
                new J_U_F_BiConsumer<double[], T>() {
                    @Override
                    public void accept(double[] doubles, T t) {
                        double val = mapper.applyAsDouble(t);
                        sumWithCompensation(doubles, val);
                        doubles[2]++;
                        doubles[3] += val;
                    }

                    @Override
                    public J_U_F_BiConsumer<double[], T> andThen(J_U_F_BiConsumer<? super double[], ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<double[]>() {
                    @Override
                    public double[] apply(double[] doubles, double[] doubles2) {
                        sumWithCompensation(doubles, doubles2[0]);
                        sumWithCompensation(doubles, -doubles2[1]);
                        doubles[2] += doubles2[2];
                        doubles[3] += doubles2[3];
                        return doubles;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<double[], double[], V> andThen(J_U_F_Function<? super double[], ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                },
                new J_U_F_Function<double[], Double>() {
                    @Override
                    public Double apply(double[] doubles) {
                        return doubles[2] == 0 ? 0.0d : (computeFinalSum(doubles) / doubles[2]);
                    }

                    @Override
                    public <V> J_U_F_Function<V, Double> compose(J_U_F_Function<? super V, ? extends double[]> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<double[], V> andThen(J_U_F_Function<? super Double, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    public static <T> J_U_S_Collector<T, ?, T> reducing(T identity, final J_U_F_BinaryOperator<T> op) {
        return J_U_S_Collector.CollectorStatics.of(
                boxSupplier(identity),
                new J_U_F_BiConsumer<T[], T>() {

                    @Override
                    public void accept(T[] ts, T t) {
                        ts[0] = op.apply(ts[0], t);
                    }

                    @Override
                    public J_U_F_BiConsumer<T[], T> andThen(J_U_F_BiConsumer<? super T[], ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<T[]>() {
                    @Override
                    public T[] apply(T[] ts, T[] ts2) {
                        ts[0] = op.apply(ts[0], ts2[0]);
                        return ts;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<T[], T[], V> andThen(J_U_F_Function<? super T[], ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                },
                new J_U_F_Function<T[], T>() {
                    @Override
                    public T apply(T[] ts) {
                        return ts[0];
                    }

                    @Override
                    public <V> J_U_F_Function<V, T> compose(J_U_F_Function<? super V, ? extends T[]> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<T[], V> andThen(J_U_F_Function<? super T, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    private static <T> J_U_F_Supplier<T[]> boxSupplier(final T in) {
        return new J_U_F_Supplier<T[]>() {
            @Override
            public T[] get() {
                return (T[]) new Object[] { in };
            }
        };
    }

    public static <T> J_U_S_Collector<T, ?, J_U_Optional<T>> reducing(final J_U_F_BinaryOperator<T> op) {
        class OptionalBox implements J_U_F_Consumer<T> {
            T value = null;
            boolean present = false;

            @Override
            public void accept(T t) {
                if (present) {
                    value = op.apply(value, t);
                } else {
                    value = t;
                    present = true;
                }
            }

            @Override
            public J_U_F_Consumer<T> andThen(J_U_F_Consumer<? super T> after) {
                return J_U_F_Consumer.ConsumerDefaults.andThen(this, after);
            }
        }
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<OptionalBox>() {
                    @Override
                    public OptionalBox get() {
                        return new OptionalBox();
                    }
                },
                new J_U_F_BiConsumer<OptionalBox, T>() {
                    @Override
                    public void accept(OptionalBox optionalBox, T t) {
                        optionalBox.accept(t);
                    }

                    @Override
                    public J_U_F_BiConsumer<OptionalBox, T> andThen(J_U_F_BiConsumer<? super OptionalBox, ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<OptionalBox>() {
                    @Override
                    public OptionalBox apply(OptionalBox optionalBox, OptionalBox optionalBox2) {
                        if (optionalBox2.present) {
                            optionalBox.accept(optionalBox2.value);
                        }
                        return optionalBox;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<OptionalBox, OptionalBox, V> andThen(J_U_F_Function<? super OptionalBox, ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                },
                new J_U_F_Function<OptionalBox, J_U_Optional<T>>() {
                    @Override
                    public J_U_Optional<T> apply(OptionalBox optionalBox) {
                        return J_U_Optional.ofNullable(optionalBox.value);
                    }

                    @Override
                    public <V> J_U_F_Function<V, J_U_Optional<T>> compose(J_U_F_Function<? super V, ? extends OptionalBox> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<OptionalBox, V> andThen(J_U_F_Function<? super J_U_Optional<T>, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    public static <T, U> J_U_S_Collector<T, ?, U> reducing(U identity, final J_U_F_Function<? super T, ? extends U> mapper, final J_U_F_BinaryOperator<U> op) {
        return J_U_S_Collector.CollectorStatics.of(
                boxSupplier(identity),
                new J_U_F_BiConsumer<U[], T>() {
                    @Override
                    public void accept(U[] us, T t) {
                        us[0] = op.apply(us[0], mapper.apply(t));
                    }

                    @Override
                    public J_U_F_BiConsumer<U[], T> andThen(J_U_F_BiConsumer<? super U[], ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<U[]>() {
                    @Override
                    public U[] apply(U[] us, U[] us2) {
                        us[0] = op.apply(us[0], us2[0]);
                        return us;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<U[], U[], V> andThen(J_U_F_Function<? super U[], ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                },
                new J_U_F_Function<U[], U>() {
                    @Override
                    public U apply(U[] us) {
                        return us[0];
                    }

                    @Override
                    public <V> J_U_F_Function<V, U> compose(J_U_F_Function<? super V, ? extends U[]> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<U[], V> andThen(J_U_F_Function<? super U, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    public static <T, K> J_U_S_Collector<T, ?, Map<K, List<T>>> groupingBy(J_U_F_Function<? super T, ? extends K> classifier) {
        return (J_U_S_Collector) groupingBy(classifier, toList());
    }

    public static <T, K, A, D> J_U_S_Collector<T, ?, Map<K, D>> groupingBy(J_U_F_Function<? super T, ? extends K> classifier, J_U_S_Collector<? super T, A, D> downstream) {
        return groupingBy(classifier, new J_U_F_Supplier<Map<K, D>>() {

            @Override
            public Map<K, D> get() {
                return new HashMap<>();
            }
        }, downstream);
    }

    private static <K, V, M extends Map<K,V>> J_U_F_BinaryOperator<M> mapMerger(final J_U_F_BinaryOperator<V> mergeFunction) {
        return new J_U_F_BinaryOperator<M>() {
            @Override
            public M apply(M m1, M m2) {
                for (Map.Entry<K,V> e : m2.entrySet())
                    J_U_Map.merge(m1, e.getKey(), e.getValue(), mergeFunction);
                return m1;
            }

            @Override
            public <V> J_U_F_BiFunction<M, M, V> andThen(J_U_F_Function<? super M, ? extends V> after) {
                return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
            }
        };
    }

    public static <T, K, D, A, M extends Map<K, D>> J_U_S_Collector<T, ?, M> groupingBy(final J_U_F_Function<? super T, ? extends K> classifier, J_U_F_Supplier<M> mapFactory, J_U_S_Collector<? super T, A, D> downstream) {
        final J_U_F_Supplier<A> downstreamSupplier = downstream.supplier();
        final J_U_F_BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        J_U_F_BiConsumer<Map<K, A>, T> accumulator = new J_U_F_BiConsumer<Map<K, A>, T>() {
            @Override
            public void accept(Map<K, A> m, T t) {
                K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
                if (!m.containsKey(key)) m.put(key, downstreamSupplier.get());
                downstreamAccumulator.accept(m.get(key), t);
            }

            @Override
            public J_U_F_BiConsumer<Map<K, A>, T> andThen(J_U_F_BiConsumer<? super Map<K, A>, ? super T> after) {
                return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
            }
        };
        J_U_F_BinaryOperator<Map<K, A>> merger = J_U_S_Collectors.<K, A, Map<K, A>>mapMerger(downstream.combiner());
        J_U_F_Supplier<Map<K, A>> mangledFactory = (J_U_F_Supplier<Map<K,A>>) mapFactory;
        if (downstream.characteristics().contains(J_U_S_Collector.Characteristics.IDENTITY_FINISH)) {
            return (J_U_S_Collector<T, ?, M>) J_U_S_Collector.CollectorStatics.of(
                    mangledFactory,
                    accumulator,
                    merger
            );
        } else {
            @SuppressWarnings("unchecked") final J_U_F_Function<A, A> downstreamFinisher = (J_U_F_Function<A, A>) downstream.finisher();
            J_U_F_Function<Map<K, A>, M> finisher = new J_U_F_Function<Map<K, A>, M>() {
                @Override
                public M apply(Map<K, A> intermediate) {
                    J_U_Map.replaceAll(intermediate, new J_U_F_BiFunction<K, A, A>() {

                        @Override
                        public A apply(K k, A a) {
                            return downstreamFinisher.apply(a);
                        }

                        @Override
                        public <V> J_U_F_BiFunction<K, A, V> andThen(J_U_F_Function<? super A, ? extends V> after) {
                            return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                        }
                    });
                    @SuppressWarnings("unchecked")
                    M castResult = (M) intermediate;
                    return castResult;
                }

                @Override
                public <V> J_U_F_Function<V, M> compose(J_U_F_Function<? super V, ? extends Map<K, A>> before) {
                    return J_U_F_Function.FunctionDefaults.compose(this, before);
                }

                @Override
                public <V> J_U_F_Function<Map<K, A>, V> andThen(J_U_F_Function<? super M, ? extends V> after) {
                    return J_U_F_Function.FunctionDefaults.andThen(this, after);
                }
            };
            return J_U_S_Collector.CollectorStatics.of(
                    mangledFactory,
                    accumulator,
                    merger,
                    finisher
            );
        }
    }

    public static <T, K> J_U_S_Collector<T, ?, ConcurrentMap<K, List<T>>> groupingByConcurrent(Function<? super T, ? extends K> classifier) {
        return (J_U_S_Collector) groupingByConcurrent(classifier, toList());
    }

    public static <T, K, A, D> J_U_S_Collector<T, ?, ConcurrentMap<K, D>> groupingByConcurrent(Function<? super T, ? extends K> classifier, J_U_S_Collector<? super T, A, D> downstream) {
        return groupingByConcurrent(classifier, new J_U_F_Supplier<ConcurrentMap<K, D>>() {

            @Override
            public ConcurrentMap<K, D> get() {
                return new ConcurrentHashMap<>();
            }
        }, downstream);
    }

    public static <T, K, D, A, M extends ConcurrentMap<K, D>> J_U_S_Collector<T, ?, M> groupingByConcurrent(final Function<? super T, ? extends K> classifier, J_U_F_Supplier<M> mapFactory, J_U_S_Collector<? super T, A, D> downstream) {
        final J_U_F_Supplier<A> downstreamSupplier = downstream.supplier();
        final J_U_F_BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        J_U_F_BinaryOperator<ConcurrentMap<K, A>> merger = J_U_S_Collectors.<K, A, ConcurrentMap<K, A>>mapMerger(downstream.combiner());
        @SuppressWarnings("unchecked")
        J_U_F_Supplier<ConcurrentMap<K, A>> mangledFactory = (J_U_F_Supplier<ConcurrentMap<K, A>>) mapFactory;
        J_U_F_BiConsumer<ConcurrentMap<K, A>, T> accumulator;
        if (downstream.characteristics().contains(J_U_S_Collector.Characteristics.CONCURRENT)) {
            accumulator = new J_U_F_BiConsumer<ConcurrentMap<K, A>, T>() {
                @Override
                public void accept(ConcurrentMap<K, A> kaConcurrentMap, T t) {
                    K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
                    A container = J_U_C_ConcurrentMap.computeIfAbsent(kaConcurrentMap, key, new J_U_F_Function<K, A>() {

                        @Override
                        public A apply(K k) {
                            return downstreamSupplier.get();
                        }

                        @Override
                        public <V> J_U_F_Function<V, A> compose(J_U_F_Function<? super V, ? extends K> before) {
                            return J_U_F_Function.FunctionDefaults.compose(this, before);
                        }

                        @Override
                        public <V> J_U_F_Function<K, V> andThen(J_U_F_Function<? super A, ? extends V> after) {
                            return J_U_F_Function.FunctionDefaults.andThen(this, after);
                        }
                    });
                    downstreamAccumulator.accept(container, t);
                }

                @Override
                public J_U_F_BiConsumer<ConcurrentMap<K, A>, T> andThen(J_U_F_BiConsumer<? super ConcurrentMap<K, A>, ? super T> after) {
                    return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                }
            };
        } else {
            accumulator = new J_U_F_BiConsumer<ConcurrentMap<K, A>, T>() {
                @Override
                public void accept(ConcurrentMap<K, A> kaConcurrentMap, T t) {
                    K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
                    A container = J_U_C_ConcurrentMap.computeIfAbsent(kaConcurrentMap, key, new J_U_F_Function<K, A>() {

                        @Override
                        public A apply(K k) {
                            return downstreamSupplier.get();
                        }

                        @Override
                        public <V> J_U_F_Function<V, A> compose(J_U_F_Function<? super V, ? extends K> before) {
                            return J_U_F_Function.FunctionDefaults.compose(this, before);
                        }

                        @Override
                        public <V> J_U_F_Function<K, V> andThen(J_U_F_Function<? super A, ? extends V> after) {
                            return J_U_F_Function.FunctionDefaults.andThen(this, after);
                        }
                    });
                    synchronized (container) {
                        downstreamAccumulator.accept(container, t);
                    }
                }

                @Override
                public J_U_F_BiConsumer<ConcurrentMap<K, A>, T> andThen(J_U_F_BiConsumer<? super ConcurrentMap<K, A>, ? super T> after) {
                    return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                }
            };
        }
        if (downstream.characteristics().contains(J_U_S_Collector.Characteristics.IDENTITY_FINISH)) {
            return (J_U_S_Collector<T, ?, M>) J_U_S_Collector.CollectorStatics.of(
                    mangledFactory,
                    accumulator,
                    merger,
                    J_U_S_Collector.Characteristics.CONCURRENT
            );
        } else {
            final J_U_F_Function<A, A> downstreamFinisher = (J_U_F_Function<A, A>) downstream.finisher();
            J_U_F_Function<ConcurrentMap<K, A>, M> finisher = new J_U_F_Function<ConcurrentMap<K, A>, M>() {

                @Override
                public M apply(ConcurrentMap<K, A> kaConcurrentMap) {
                    J_U_C_ConcurrentMap.replaceAll(kaConcurrentMap, new J_U_F_BiFunction<K, A, A>() {
                        @Override
                        public A apply(K k, A a) {
                            return downstreamFinisher.apply(a);
                        }

                        @Override
                        public <V> J_U_F_BiFunction<K, A, V> andThen(J_U_F_Function<? super A, ? extends V> after) {
                            return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                        }
                    });
                    M castResult = (M) kaConcurrentMap;
                    return castResult;
                }

                @Override
                public <V> J_U_F_Function<V, M> compose(J_U_F_Function<? super V, ? extends ConcurrentMap<K, A>> before) {
                    return J_U_F_Function.FunctionDefaults.compose(this, before);
                }

                @Override
                public <V> J_U_F_Function<ConcurrentMap<K, A>, V> andThen(J_U_F_Function<? super M, ? extends V> after) {
                    return J_U_F_Function.FunctionDefaults.andThen(this, after);
                }
            };
            return J_U_S_Collector.CollectorStatics.of(
                    mangledFactory,
                    accumulator,
                    merger,
                    finisher,
                    J_U_S_Collector.Characteristics.CONCURRENT
            );
        }
    }

    public static <T> J_U_S_Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(final J_U_F_Predicate<? super T> predicate) {
        return (J_U_S_Collector) partitioningBy(predicate, toList());
    }

    public static <T, D, A> J_U_S_Collector<T, ?, Map<Boolean, D>> partitioningBy(final J_U_F_Predicate<? super T> predicate, J_U_S_Collector<? super T, A, D> downstream) {
        return groupingBy(new J_U_F_Function<T, Boolean>() {
            @Override
            public Boolean apply(T t) {
                return predicate.test(t);
            }

            @Override
            public <V> J_U_F_Function<V, Boolean> compose(J_U_F_Function<? super V, ? extends T> before) {
                return J_U_F_Function.FunctionDefaults.compose(this, before);
            }

            @Override
            public <V> J_U_F_Function<T, V> andThen(J_U_F_Function<? super Boolean, ? extends V> after) {
                return J_U_F_Function.FunctionDefaults.andThen(this, after);
            }
        }, downstream);
    }

    private static <T, K, V> J_U_F_BiConsumer<Map<K, V>, T> uniqKeyMapAccumulator(final J_U_F_Function<? super T, ? extends K> keyMapper, final J_U_F_Function<? super T, ? extends V> valueMapper) {
        return new J_U_F_BiConsumer<Map<K, V>, T>() {
            @Override
            public void accept(Map<K, V> kvMap, T t) {
                K k = keyMapper.apply(t);
                V v = Objects.requireNonNull(valueMapper.apply(t));
                V old = J_U_Map.putIfAbsent(kvMap, k, v);
                if (old != null) {
                    throw new IllegalStateException(String.format("Duplicate key %s (attempted merging values %s and %s)", k, old, v));
                }
            }

            @Override
            public J_U_F_BiConsumer<Map<K, V>, T> andThen(J_U_F_BiConsumer<? super Map<K, V>, ? super T> after) {
                return null;
            }
        };
    }

    private static <K, V, M extends Map<K, V>> J_U_F_BinaryOperator<M> uniqKeysMapMerger() {
        return new J_U_F_BinaryOperator<M>() {
            @Override
            public M apply(M m, M m2) {
                for (Map.Entry<K, V> e : m2.entrySet()) {
                    K k = e.getKey();
                    V v = Objects.requireNonNull(e.getValue());
                    V old = J_U_Map.putIfAbsent(m, k, v);
                    if (old != null) {
                        throw new IllegalStateException(String.format("Duplicate key %s (attempted merging values %s and %s)", k, old, v));
                    }
                }
                return m;
            }

            @Override
            public <V> J_U_F_BiFunction<M, M, V> andThen(J_U_F_Function<? super M, ? extends V> after) {
                return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
            }
        };
    }

    public static <T, K, U> J_U_S_Collector<T, ?, Map<K, U>> toMap(J_U_F_Function<? super T, ? extends K> keyMapper, J_U_F_Function<? super T, ? extends U> valueMapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<Map<K, U>>() {
                    @Override
                    public Map<K, U> get() {
                        return new HashMap<>();
                    }
                },
                uniqKeyMapAccumulator(keyMapper, valueMapper),
                (J_U_F_BinaryOperator) uniqKeysMapMerger()
        );
    }

    public static <T, K, U> J_U_S_Collector<T, ?, Map<K, U>> toUnmodifiableMap(J_U_F_Function<? super T, ? extends K> keyMapper, J_U_F_Function<? super T, ? extends U> valueMapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<Map<K, U>>() {
                    @Override
                    public Map<K, U> get() {
                        return new HashMap<>();
                    }
                },
                uniqKeyMapAccumulator(keyMapper, valueMapper),
                (J_U_F_BinaryOperator) uniqKeysMapMerger(),
                new J_U_F_Function<Map<K, U>, Map<K, U>>() {

                    @Override
                    public Map<K, U> apply(Map<K, U> kuMap) {
                        return Collections.unmodifiableMap(kuMap);
                    }

                    @Override
                    public <V> J_U_F_Function<V, Map<K, U>> compose(J_U_F_Function<? super V, ? extends Map<K, U>> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<Map<K, U>, V> andThen(J_U_F_Function<? super Map<K, U>, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    public static <T, K, U> J_U_S_Collector<T, ?, Map<K, U>> toMap(J_U_F_Function<? super T, ? extends K> keyMapper, J_U_F_Function<? super T, ? extends U> valueMapper, J_U_F_BinaryOperator<U> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, new J_U_F_Supplier<Map<K, U>>() {
            @Override
            public Map<K, U> get() {
                return new HashMap<>();
            }
        });
    }

    public static <T, K, U> J_U_S_Collector<T, ?, Map<K, U>> toUnmodifiableMap(J_U_F_Function<? super T, ? extends K> keyMapper, J_U_F_Function<? super T, ? extends U> valueMapper, J_U_F_BinaryOperator<U> mergeFunction) {
        Objects.requireNonNull(keyMapper, "keyMapper");
        Objects.requireNonNull(valueMapper, "valueMapper");
        Objects.requireNonNull(mergeFunction, "mergeFunction");
        return collectingAndThen(
                toMap(keyMapper, valueMapper, mergeFunction),
                new J_U_F_Function<Map<K, U>, Map<K, U>>() {
                    @Override
                    public Map<K, U> apply(Map<K, U> kuMap) {
                        return Collections.unmodifiableMap(kuMap);
                    }

                    @Override
                    public <V> J_U_F_Function<V, Map<K, U>> compose(J_U_F_Function<? super V, ? extends Map<K, U>> before) {
                        return J_U_F_Function.FunctionDefaults.compose(this, before);
                    }

                    @Override
                    public <V> J_U_F_Function<Map<K, U>, V> andThen(J_U_F_Function<? super Map<K, U>, ? extends V> after) {
                        return J_U_F_Function.FunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    public static <T, K, U, M extends Map<K, U>> J_U_S_Collector<T, ?, M> toMap(J_U_F_Function<? super T, ? extends K> keyMapper, J_U_F_Function<? super T, ? extends U> valueMapper, J_U_F_BinaryOperator<U> mergeFunction, J_U_F_Supplier<M> mapSupplier) {
        return J_U_S_Collector.CollectorStatics.of(
                mapSupplier,
                (J_U_F_BiConsumer<M, T>) uniqKeyMapAccumulator(keyMapper, valueMapper),
                (J_U_F_BinaryOperator<M>) mapMerger(mergeFunction)
        );
    }

    public static <T, K, U> J_U_S_Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(J_U_F_Function<? super T, ? extends K> keyMapper, J_U_F_Function<? super T, ? extends U> valueMapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<Map<K, U>>() {
                    @Override
                    public Map<K, U> get() {
                        return new ConcurrentHashMap<>();
                    }
                },
                uniqKeyMapAccumulator(keyMapper, valueMapper),
                (J_U_F_BinaryOperator) uniqKeysMapMerger(),
                J_U_S_Collector.Characteristics.CONCURRENT
        );
    }

    public static <T, K, U> J_U_S_Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(J_U_F_Function<? super T, ? extends K> keyMapper, J_U_F_Function<? super T, ? extends U> valueMapper, J_U_F_BinaryOperator<U> mergeFunction) {
        return toConcurrentMap(keyMapper, valueMapper, mergeFunction, new J_U_F_Supplier<ConcurrentMap<K, U>>() {
            @Override
            public ConcurrentMap<K, U> get() {
                return new ConcurrentHashMap<>();
            }
        });
    }

    public static <T, K, U, M extends ConcurrentMap<K, U>> J_U_S_Collector<T, ?, M> toConcurrentMap(J_U_F_Function<? super T, ? extends K> keyMapper, J_U_F_Function<? super T, ? extends U> valueMapper, J_U_F_BinaryOperator<U> mergeFunction, J_U_F_Supplier<M> mapSupplier) {
        return J_U_S_Collector.CollectorStatics.of(
                mapSupplier,
                (J_U_F_BiConsumer) uniqKeyMapAccumulator(keyMapper, valueMapper),
                (J_U_F_BinaryOperator) mapMerger(mergeFunction),
                J_U_S_Collector.Characteristics.CONCURRENT
        );
    }

    public static <T> J_U_S_Collector<T, ?, J_U_IntSummaryStatistics> summarizingInt(final J_U_F_ToIntFunction<? super T> mapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<J_U_IntSummaryStatistics>() {
                    @Override
                    public J_U_IntSummaryStatistics get() {
                        return new J_U_IntSummaryStatistics();
                    }
                },
                new J_U_F_BiConsumer<J_U_IntSummaryStatistics, T>() {
                    @Override
                    public void accept(J_U_IntSummaryStatistics jUIntSummaryStatistics, T t) {
                        jUIntSummaryStatistics.accept(mapper.applyAsInt(t));
                    }

                    @Override
                    public J_U_F_BiConsumer<J_U_IntSummaryStatistics, T> andThen(J_U_F_BiConsumer<? super J_U_IntSummaryStatistics, ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<J_U_IntSummaryStatistics>() {
                    @Override
                    public J_U_IntSummaryStatistics apply(J_U_IntSummaryStatistics jUIntSummaryStatistics, J_U_IntSummaryStatistics jUIntSummaryStatistics2) {
                        jUIntSummaryStatistics.combine(jUIntSummaryStatistics2);
                        return jUIntSummaryStatistics;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<J_U_IntSummaryStatistics, J_U_IntSummaryStatistics, V> andThen(J_U_F_Function<? super J_U_IntSummaryStatistics, ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    public static <T> J_U_S_Collector<T, ?, J_U_LongSummaryStatistics> summarizingLong(final J_U_F_ToLongFunction<? super T> mapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<J_U_LongSummaryStatistics>() {
                    @Override
                    public J_U_LongSummaryStatistics get() {
                        return new J_U_LongSummaryStatistics();
                    }
                },
                new J_U_F_BiConsumer<J_U_LongSummaryStatistics, T>() {
                    @Override
                    public void accept(J_U_LongSummaryStatistics jUIntSummaryStatistics, T t) {
                        jUIntSummaryStatistics.accept(mapper.applyAsLong(t));
                    }

                    @Override
                    public J_U_F_BiConsumer<J_U_LongSummaryStatistics, T> andThen(J_U_F_BiConsumer<? super J_U_LongSummaryStatistics, ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<J_U_LongSummaryStatistics>() {
                    @Override
                    public J_U_LongSummaryStatistics apply(J_U_LongSummaryStatistics jUIntSummaryStatistics, J_U_LongSummaryStatistics jUIntSummaryStatistics2) {
                        jUIntSummaryStatistics.combine(jUIntSummaryStatistics2);
                        return jUIntSummaryStatistics;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<J_U_LongSummaryStatistics, J_U_LongSummaryStatistics, V> andThen(J_U_F_Function<? super J_U_LongSummaryStatistics, ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                }
        );
    }

    public static <T> J_U_S_Collector<T, ?, J_U_DoubleSummaryStatistics> summarizingDouble(final J_U_F_ToDoubleFunction<? super T> mapper) {
        return J_U_S_Collector.CollectorStatics.of(
                new J_U_F_Supplier<J_U_DoubleSummaryStatistics>() {
                    @Override
                    public J_U_DoubleSummaryStatistics get() {
                        return new J_U_DoubleSummaryStatistics();
                    }
                },
                new J_U_F_BiConsumer<J_U_DoubleSummaryStatistics, T>() {
                    @Override
                    public void accept(J_U_DoubleSummaryStatistics jUIntSummaryStatistics, T t) {
                        jUIntSummaryStatistics.accept(mapper.applyAsDouble(t));
                    }

                    @Override
                    public J_U_F_BiConsumer<J_U_DoubleSummaryStatistics, T> andThen(J_U_F_BiConsumer<? super J_U_DoubleSummaryStatistics, ? super T> after) {
                        return J_U_F_BiConsumer.BiConsumerDefaults.andThen(this, after);
                    }
                },
                new J_U_F_BinaryOperator<J_U_DoubleSummaryStatistics>() {
                    @Override
                    public J_U_DoubleSummaryStatistics apply(J_U_DoubleSummaryStatistics jUIntSummaryStatistics, J_U_DoubleSummaryStatistics jUIntSummaryStatistics2) {
                        jUIntSummaryStatistics.combine(jUIntSummaryStatistics2);
                        return jUIntSummaryStatistics;
                    }

                    @Override
                    public <V> J_U_F_BiFunction<J_U_DoubleSummaryStatistics, J_U_DoubleSummaryStatistics, V> andThen(J_U_F_Function<? super J_U_DoubleSummaryStatistics, ? extends V> after) {
                        return J_U_F_BiFunction.BiFunctionDefaults.andThen(this, after);
                    }
                }
        );
    }



    static class CollectorImpl<T, A, R> implements J_U_S_Collector<T, A, R> {

        private final J_U_F_Supplier<A> supplier;
        private final J_U_F_BiConsumer<A, T> accumulator;
        private final J_U_F_BinaryOperator<A> combiner;
        private final J_U_F_Function<A, R> finisher;
        private final Set<J_U_S_Collector.Characteristics> characteristics;

        public CollectorImpl(J_U_F_Supplier<A> supplier, J_U_F_BiConsumer<A, T> accumulator, J_U_F_BinaryOperator<A> combiner, Set<J_U_S_Collector.Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = new J_U_F_Function<A, R>() {

                @Override
                public R apply(A a) {
                    return (R) a;
                }

                @Override
                public <V> J_U_F_Function<V, R> compose(J_U_F_Function<? super V, ? extends A> before) {
                    return J_U_F_Function.FunctionDefaults.compose(this, before);
                }

                @Override
                public <V> J_U_F_Function<A, V> andThen(J_U_F_Function<? super R, ? extends V> after) {
                    return J_U_F_Function.FunctionDefaults.andThen(this, after);
                }

            };
            this.characteristics = characteristics;
        }

        public CollectorImpl(J_U_F_Supplier<A> supplier, J_U_F_BiConsumer<A, T> accumulator, J_U_F_BinaryOperator<A> combiner, J_U_F_Function<A, R> finisher, Set<J_U_S_Collector.Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }


        @Override
        public J_U_F_Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public J_U_F_BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        @Override
        public J_U_F_BinaryOperator<A> combiner() {
            return combiner;
        }

        @Override
        public J_U_F_Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }

}
