package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@InitRewrite(ref = @Ref(value = "Ljava/lang/ClassLoader;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/ClassLoader;)V"), to = "(Ljava/lang/ClassLoader;)V", keep = {1})
public class J_L_ClassLoader {
    private static final Map<ClassLoader, String> nameMap = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ClassLoader, J_L_Module> unnamedModuleMap = Collections.synchronizedMap(new WeakHashMap<>());
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle IS_REGISTERED;

    static {
        try {
            Class<?> parallelLoaders = Class.forName("java.lang.ClassLoader$ParallelLoaders");
            IS_REGISTERED = IMPL_LOOKUP.findStatic(parallelLoaders, "isRegistered", MethodType.methodType(boolean.class, Class.class));
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // ClassLoader(String,ClassLoader) -> ClassLoader(ClassLoader) then record the name
    @InitPost(target = "(Ljava/lang/String;Ljava/lang/ClassLoader;)V", from = {0})
    public static void setName(ClassLoader self, String name) {
        setClassloaderName(name, self);
    }

    public static void setClassloaderName(String name, ClassLoader classLoader) {
        if (name != null && name.isEmpty()) {
            throw new IllegalArgumentException("name must be non-empty or null");
        }
        nameMap.put(classLoader, name);
    }

    @Stub
    public static String getName(ClassLoader classLoader) {
        // TODO: check if subclass actually overrides this method
        String name = nameMap.get(classLoader);
        if (name != null) {
            return name;
        }
        // the builtin loaders have fixed names
        if (classLoader == null) {
            return "bootstrap";
        }
        ClassLoader sys = ClassLoader.getSystemClassLoader();
        if (classLoader == sys) {
            return "app";
        }
        if (classLoader == sys.getParent()) {
            return "platform";
        }
        return null;
    }

    @Stub
    public static J_L_Module getUnnamedModule(ClassLoader classLoader) {
        return unnamedModuleMap.computeIfAbsent(classLoader, J_L_Module::new);
    }

    @Stub(ref = @Ref("Ljava/lang/ClassLoader;"))
    public static ClassLoader getPlatformClassLoader() {
        // the system loader's parent is the closest analogue to the
        // platform loader (bootstrap's direct child)
        return ClassLoader.getSystemClassLoader().getParent();
    }

    @Stub
    public static boolean isRegisteredAsParallelCapable(ClassLoader loader) throws Throwable {
        return (boolean) IS_REGISTERED.invokeExact(loader.getClass());
    }

}
