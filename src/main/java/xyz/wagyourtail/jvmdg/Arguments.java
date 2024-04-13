package xyz.wagyourtail.jvmdg;

import xyz.wagyourtail.jvmdg.util.Function;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Arguments {
    public final Set<Argument<?>> args = new HashSet<>();

    public Arguments() {
        add("help", null, Void.class, new Function<ArgSupplier, Void>() {
            @Override
            public Void apply(ArgSupplier argSupplier) {
                System.out.println("Usage: ");
                for (Argument<?> arg : args) {
                    String type;
                    if (arg.type == Boolean.class) {
                        type = "flag";
                    } else {
                        type = "<" + arg.type.getSimpleName() + ">";
                    }
                    System.out.println("--" + arg.name + " or -" + arg.shortName + " : " + type);
                }
                System.exit(0);
                return null;
            }
        });
    }

    public Argument<Boolean> addFlag(String name, String shortName) {
        return add(name, shortName, Boolean.class, new Function<ArgSupplier, Boolean>() {
            @Override
            public Boolean apply(ArgSupplier supplier) {
                return true;
            }
        });
    }

    public Argument<Boolean> addBoolean(String name, String shortName) {
        return add(name, shortName, Boolean.class, new Function<ArgSupplier, Boolean>() {
            @Override
            public Boolean apply(ArgSupplier supplier) {
                return Boolean.parseBoolean(supplier.get());
            }
        });
    }

    public Argument<String> addString(String name, String shortName) {
        return add(name, shortName, String.class, new Function<ArgSupplier, String>() {
            @Override
            public String apply(ArgSupplier supplier) {
                String value = supplier.get();
                while (value.startsWith("\"") && !value.endsWith("\"")) {
                    value += " " + supplier.get();
                }
                return value;
            }
        });
    }

    public Argument<Integer> addInt(String name, String shortName) {
        return add(name, shortName, Integer.class, new Function<ArgSupplier, Integer>() {
            @Override
            public Integer apply(ArgSupplier supplier) {
                return Integer.parseInt(supplier.get());
            }
        });
    }

    public Argument<Long> addLong(String name, String shortName) {
        return add(name, shortName, Long.class, new Function<ArgSupplier, Long>() {
            @Override
            public Long apply(ArgSupplier supplier) {
                return Long.parseLong(supplier.get());
            }
        });
    }

    public Argument<Double> addDouble(String name, String shortName) {
        return add(name, shortName, Double.class, new Function<ArgSupplier, Double>() {
            @Override
            public Double apply(ArgSupplier supplier) {
                return Double.parseDouble(supplier.get());
            }
        });
    }

    public Argument<Float> addFloat(String name, String shortName) {
        return add(name, shortName, Float.class, new Function<ArgSupplier, Float>() {
            @Override
            public Float apply(ArgSupplier supplier) {
                return Float.parseFloat(supplier.get());
            }
        });
    }

    public Argument<Byte> addByte(String name, String shortName) {
        return add(name, shortName, Byte.class, new Function<ArgSupplier, Byte>() {
            @Override
            public Byte apply(ArgSupplier supplier) {
                return Byte.parseByte(supplier.get());
            }
        });
    }

    public Argument<Short> addShort(String name, String shortName) {
        return add(name, shortName, Short.class, new Function<ArgSupplier, Short>() {
            @Override
            public Short apply(ArgSupplier supplier) {
                return Short.parseShort(supplier.get());
            }
        });
    }

    public Argument<File> addFile(String name, String shortName) {
        return add(name, shortName, File.class, new Function<ArgSupplier, File>() {
            @Override
            public File apply(ArgSupplier supplier) {
                return new File(supplier.get());
            }
        });
    }

    public <T> Argument<T> add(String name, String shortName, Class<T> type, Function<ArgSupplier, T> parser) {
        Argument<T> arg = new Argument<>(name, shortName, type, parser);
        args.add(arg);
        return arg;
    }

    public void parse(final String[] args) {
        int nullNamed = 0;
        for (final AtomicInteger i = new AtomicInteger(0); i.get() < args.length; i.incrementAndGet()) {
            String arg = args[i.get()];
            if (arg.startsWith("--")) {
                String name = arg.substring(2);
                for (Argument<?> argument : this.args) {
                    if (name.equals(argument.name)) {
                        argument.parse(i, args);
                    }
                }
            } else if (arg.startsWith("-")) {
                String name = arg.substring(1);
                for (Argument<?> argument : this.args) {
                    if (name.equals(argument.shortName)) {
                        argument.parse(i, args);
                    }
                }
            } else {
                // unnamed argument
                int currentNullCount = 0;
                for (Argument<?> argument : this.args) {
                    if (argument.name == null && argument.shortName == null) {
                        if (currentNullCount++ == nullNamed) {
                            argument.parse(i, args);
                            nullNamed++;
                            continue;
                        }
                        throw new IllegalArgumentException("Unknown argument: " + arg);
                    }
                }
            }
        }
    }

    public interface ArgSupplier {
        String get();
    }

    public static class Argument<T> {
        public final String name;
        public final String shortName;
        public final Class<T> type;
        public final Function<ArgSupplier, T> parser;
        private T value;


        public Argument(String name, String shortName, Class<T> type, Function<ArgSupplier, T> parser) {
            this.name = name;
            this.shortName = shortName;
            this.type = type;
            this.parser = parser;
        }

        public T get() {
            return value;
        }

        protected void parse(final AtomicInteger i, final String[] args) {
            value = parser.apply(new ArgSupplier() {
                @Override
                public String get() {
                    return args[i.getAndIncrement()];
                }
            });
        }
    }
}
