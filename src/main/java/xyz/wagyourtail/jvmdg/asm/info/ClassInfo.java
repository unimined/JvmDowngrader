package xyz.wagyourtail.jvmdg.asm.info;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ClassInfo {
    private final String name;
    private final String superName;
    private final boolean isInterface;
    private final int version;
    private final List<String> interfaces;
    private final Lazy<Set<MethodInfo>> methods;
    private final Lazy<Set<FieldInfo>> fields;

    public ClassInfo(boolean isInterface, String name, String superName, int version, List<String> interfaces, Lazy<Set<MethodInfo>> methods, Lazy<Set<FieldInfo>> fields) {
        this.isInterface = isInterface;
        this.name = name;
        this.superName = superName;
        this.version = version;
        this.interfaces = new ArrayList<>(interfaces);
        this.methods = methods;
        this.fields = fields;
    }

    public static ClassInfo of(final Class<?> clazz) {
        Class<?> superClass = clazz.getSuperclass();
        // should only occur for java/lang/Object and it's handled specially
        if (superClass == null) {
            superClass = clazz;
        }
        List<String> interfaces = new ArrayList<>();
        for (Class<?> i : clazz.getInterfaces()) {
            interfaces.add(Type.getInternalName(i));
        }

        return new ClassInfo(
            clazz.isInterface(),
            Type.getInternalName(clazz),
            Type.getInternalName(superClass),
            Utils.getCurrentClassVersion(),
            interfaces,
            new Lazy<Set<MethodInfo>>() {
                @Override
                protected Set<MethodInfo> init() {
                    Set<MethodInfo> methods = new LinkedHashSet<>();

                    for (Method m : clazz.getDeclaredMethods()) {
                        methods.add(MethodInfo.of(m));
                    }
                    for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                        methods.add(MethodInfo.of(c));
                    }

                    if (Utils.hasStaticInitializer(clazz)) {
                        methods.add(MethodInfo.createClinit());
                    }

                    return methods;
                }
            },
            new Lazy<Set<FieldInfo>>() {
                @Override
                protected Set<FieldInfo> init() {
                    Set<FieldInfo> fields = new LinkedHashSet<>();
                    for (Field f : clazz.getDeclaredFields()) {
                        fields.add(FieldInfo.of(f));
                    }
                    return fields;
                }
            }
        );
    }

    public static ClassInfo of(final ClassNode cn) {
        return new ClassInfo(
            (cn.access & Opcodes.ACC_INTERFACE) != 0,
            cn.name,
            cn.superName,
            cn.version,
            cn.interfaces,
            new Lazy<Set<MethodInfo>>() {
                @Override
                protected Set<MethodInfo> init() {
                    Set<MethodInfo> methods = new LinkedHashSet<>();
                    for (MethodNode mn : cn.methods) {
                        methods.add(MethodInfo.of(mn));
                    }
                    return methods;
                }
            },
            new Lazy<Set<FieldInfo>>() {
                @Override
                protected Set<FieldInfo> init() {
                    Set<FieldInfo> fields = new LinkedHashSet<>();
                    for (FieldNode fd : cn.fields) {
                        fields.add(FieldInfo.of(fd));
                    }
                    return fields;
                }
            }
        );
    }

    public static ClassInfo of(final ClassReader reader) {
        final Lazy<ClassNode> cn = new Lazy<ClassNode>() {
            @Override
            protected ClassNode init() {
                ClassNode cn = new ClassNode();
                // header-only facts are read above; the node only materializes
                // when members are actually queried
                reader.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                return cn;
            }
        };
        return new ClassInfo(
            (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0,
            reader.getClassName(),
            reader.getSuperName(),
            reader.readShort(6),
            Arrays.asList(reader.getInterfaces()),
            new Lazy<Set<MethodInfo>>() {
                @Override
                protected Set<MethodInfo> init() {
                    Set<MethodInfo> methods = new LinkedHashSet<>();
                    for (MethodNode mn : cn.get().methods) {
                        methods.add(MethodInfo.of(mn));
                    }
                    return methods;
                }
            },
            new Lazy<Set<FieldInfo>>() {
                @Override
                protected Set<FieldInfo> init() {
                    Set<FieldInfo> fields = new LinkedHashSet<>();
                    for (FieldNode fd : cn.get().fields) {
                        fields.add(FieldInfo.of(fd));
                    }
                    return fields;
                }
            }
        );
    }

    public static IOFunction<String, ClassInfo> withFallback(final IOFunction<String, ClassInfo> infoRetriever, final IOFunction<String, ClassInfo> fallback) {
        return new IOFunction<String, ClassInfo>() {
            @Override
            public ClassInfo apply(String name) throws IOException {
                ClassInfo info = null;
                IOException ioe = null;
                try {
                    info = infoRetriever.apply(name);
                } catch (IOException e) {
                    ioe = e;
                }
                if (info == null) {
                    try {
                        return fallback.apply(name);
                    } catch (IOException e) {
                        if (ioe != null) e.addSuppressed(ioe);
                        throw e;
                    }
                }
                return info;
            }
        };
    }

    @SafeVarargs
    public static IOFunction<String, ClassInfo> withFallbacks(final IOFunction<String, ClassInfo>... infoRetrievers) {
        return new IOFunction<String, ClassInfo>() {
            @Override
            public ClassInfo apply(String name) throws IOException {
                IOException ioe = null;
                for (IOFunction<String, ClassInfo> infoRetriever : infoRetrievers) {
                    try {
                        ClassInfo info = infoRetriever.apply(name);
                        if (info != null) {
                            return info;
                        }
                    } catch (IOException e) {
                        if (ioe != null) e.addSuppressed(ioe);
                        ioe = e;
                    }
                }
                if (ioe != null) throw ioe;
                return null;
            }
        };
    }

    public static IOFunction<String, ClassInfo> fromByteSupplier(final IOFunction<String, byte[]> infoRetriever) {
        return new IOFunction<String, ClassInfo>() {
            @Override
            public ClassInfo apply(String s) throws IOException {
                byte[] bytes = infoRetriever.apply(s);
                if (bytes != null) {
                    ClassReader reader = new ClassReader(bytes);
                    return of(reader);
                }
                return null;
            }
        };
    }

    public static IOFunction<String, ClassInfo> fromClassloader(final ClassLoader loader) {
        return new IOFunction<String, ClassInfo>() {
            @Override
            public ClassInfo apply(String name) {
                try {
                    Type t = Type.getObjectType(name);
                    Class<?> c = Class.forName(t.getClassName(), false, loader);
                    return of(c);
                } catch (ClassNotFoundException e) {
                    return null;
                }
            }
        };
    }

    public static IOFunction<String, ClassInfo> fromClassloaderWithoutLoading(final ClassLoader loader) {
        return new IOFunction<String, ClassInfo>() {
            @Override
            public ClassInfo apply(String name) throws IOException {
                try (InputStream is = loader.getResourceAsStream(name + ".class")) {
                    if (is == null) return null;
                    ClassReader cr = new ClassReader(is);
                    return of(cr);
                }
            }
        };
    }

    public static IOFunction<String, ClassInfo> fromCurrent() {
        return fromClassloader(ClassInfo.class.getClassLoader());
    }

    public static IOFunction<String, ClassInfo> caching(final IOFunction<String, ClassInfo> impl, final Map<String, ClassInfo> cache) {
        return new IOFunction<String, ClassInfo>() {
            @Override
            public ClassInfo apply(String s) throws IOException {
                if (!cache.containsKey(s)) {
                    cache.put(s, impl.apply(s));
                }
                return cache.get(s);
            }
        };
    }

    public String getName() {
        return name;
    }

    public String getSuperName() {
        return superName;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public int getVersion() {
        return version;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public Set<MethodInfo> getMethods() {
        return methods.get();
    }

    public Set<FieldInfo> getFields() {
        return fields.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(name, ((ClassInfo) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

}
