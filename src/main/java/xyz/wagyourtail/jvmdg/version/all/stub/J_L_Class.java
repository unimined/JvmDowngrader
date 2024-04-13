package xyz.wagyourtail.jvmdg.version.all.stub;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class J_L_Class {

    static final IOFunction<Type, Set<MemberNameAndDesc>> getMethods = new IOFunction<Type, Set<MemberNameAndDesc>>() {
        @Override
        public Set<MemberNameAndDesc> apply(Type type) throws IOException {
            try {
                Class<?> clazz = Class.forName(type.getClassName());
                Set<MemberNameAndDesc> methods = new HashSet<>();
                for (Method declaredMethod : clazz.getDeclaredMethods()) {
                    if (Modifier.isAbstract(declaredMethod.getModifiers()) || Modifier.isPrivate(declaredMethod.getModifiers()))
                        continue;
                    MemberNameAndDesc member = new MemberNameAndDesc(declaredMethod.getName(), Type.getType(declaredMethod));
                    methods.add(member);
                }
                return methods;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    };
    static final IOFunction<Type, List<Type>> getSuperTypes = new IOFunction<Type, List<Type>>() {
        @Override
        public List<Type> apply(Type type) throws IOException {
            try {
                Class<?> clazz = Class.forName(type.getClassName());
                List<Type> parents = new ArrayList<>();
                parents.add(Type.getType(clazz.getSuperclass()));
                for (Class<?> i : clazz.getInterfaces()) {
                    parents.add(Type.getType(i));
                }
                return parents;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    };

    private static boolean isReflectionFrame(String className) {
        return className.equals(Method.class.getName()) ||
            className.equals(Constructor.class.getName()) ||
            className.startsWith("sun.reflect.") ||
            className.startsWith("jdk.internal.reflect.") ||
            className.startsWith("java.lang.invoke.LambdaForm");
    }

    private static Class<?> getCaller() throws ClassNotFoundException {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stack.length; i++) {
            String className = stack[i].getClassName();
            if (!isReflectionFrame(className)) {
                return Class.forName(className);
            }
        }
        throw new ClassNotFoundException("Could not find caller class???");
    }

    //TODO: FIELD STUBS

    @Stub(ref = @Ref("java/lang/Class"), downgradeVersion = true, requiresRuntime = true)
    public static Class<?> forName(String className, int origVersion) throws ClassNotFoundException {
        List<VersionProvider> versionProviders = ClassDowngrader.currentVersionDowngrader.versionProviders(origVersion);
        Type classType = Type.getObjectType(className.replace('.', '/'));
        Class<?> caller = getCaller();
        for (VersionProvider vp : versionProviders) {
            if (vp.classStubs.containsKey(classType)) {
                return Class.forName(vp.classStubs.get(classType).getFirst().getInternalName().replace('/', '.'));
            }
        }
        return Class.forName(className);
    }

    @Stub(ref = @Ref("java/lang/Class"), downgradeVersion = true, requiresRuntime = true)
    public static Class<?> forName(String className, boolean initialize, ClassLoader loader, int origVersion) throws ClassNotFoundException {
        List<VersionProvider> versionProviders = ClassDowngrader.currentVersionDowngrader.versionProviders(origVersion);
        Type classType = Type.getObjectType(className.replace('.', '/'));
        for (VersionProvider vp : versionProviders) {
            if (vp.classStubs.containsKey(classType)) {
                return Class.forName(vp.classStubs.get(classType).getFirst().getInternalName().replace('/', '.'), initialize, loader);
            }
        }
        return Class.forName(className, initialize, loader);
    }

    @Stub(requiresRuntime = true, downgradeVersion = true)
    public static Method[] getMethods(Class<?> clazz, int origVersion) {
        Type target;
        if (clazz.isAnnotationPresent(Stub.class)) {
            target = Type.getObjectType(clazz.getAnnotation(Stub.class).ref().value());
        } else {
            target = Type.getType(clazz);
        }
        List<VersionProvider> versionProviders = ClassDowngrader.currentVersionDowngrader.versionProviders(origVersion);
        List<Method> methods = new ArrayList<>(Arrays.asList(clazz.getMethods()));
        for (VersionProvider vp : versionProviders) {
            if (vp.classStubs.containsKey(target)) {
                try {
                    List<Pair<Method, Stub>> targets = vp.getStubMapper(target, getMethods, getSuperTypes).getStubTargets();
                    for (Pair<Method, Stub> t : targets) {
                        if (!methods.contains(t.getFirst())) {
                            methods.add(t.getFirst());
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return methods.toArray(new Method[0]);
    }


}
