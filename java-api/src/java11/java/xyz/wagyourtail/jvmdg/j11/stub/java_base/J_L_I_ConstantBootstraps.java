package xyz.wagyourtail.jvmdg.j11.stub.java_base;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Adapter("java/lang/invoke/ConstantBootstraps")
public class J_L_I_ConstantBootstraps {

    public static Object nullConstant(MethodHandles.Lookup lookup, String name, Class<?> type) {
        if (Objects.requireNonNull(type).isPrimitive()) {
            throw new IllegalArgumentException("Primitive type cannot be null");
        }
        return null;
    }

    public static Class<?> primitiveClass(MethodHandles.Lookup lookup, String name, Class<?> type) {
        if (!Objects.requireNonNull(type).equals(Class.class)) {
            throw new IllegalArgumentException("Type must be Class");
        }
        try {
            return Utils.getClassForDesc(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Primitive not found", e);
        }
    }

    public static <E extends Enum<E>> E enumConstant(MethodHandles.Lookup lookup, String name, Class<E> type) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        checkClass(lookup, type);
        return Enum.valueOf(type, name);
    }

    public static Object getStaticFinal(MethodHandles.Lookup lookup, String name, Class<?> type, Class<?> declaringClass) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        Objects.requireNonNull(declaringClass);

        MethodHandle h;
        try {
            Field f = declaringClass.getDeclaredField(name);
            if (!Modifier.isFinal(f.getModifiers())) {
                throw new IncompatibleClassChangeError("not a final field: " + name);
            }
            h = lookup.unreflectGetter(f);
        } catch (NoSuchFieldException e) {
            throw new NoSuchFieldError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
        try {
            return h.invoke();
        } catch (Throwable t) {
            Utils.sneakyThrow(t);
        }
        throw new AssertionError("unreachable");
    }

    public static Object getStaticFinal(MethodHandles.Lookup lookup, String name, Class<?> type) {
        Objects.requireNonNull(type);
        Class<?> declaring = type.isPrimitive() ? Utils.getBoxFor(type) : type;
        return getStaticFinal(lookup, name, type, declaring);
    }

    public static Object invoke(MethodHandles.Lookup lookup, String name, Class<?> type, MethodHandle handle, Object... args) throws Throwable {
        Objects.requireNonNull(type);
        Objects.requireNonNull(handle);
        Objects.requireNonNull(args);
        if (type != handle.type().returnType()) {
            handle = handle.asType(handle.type().changeReturnType(type)).withVarargs(handle.isVarargsCollector());
        }
        return handle.invokeWithArguments(args);
    }

    /**
     * callsite for ldc condy to become an invokeDynamic
     */
    public static CallSite ldcCondyToIndy(MethodHandles.Lookup lookup, String invocationName, MethodType invokeType, MethodHandle condyBSM, Object... condyArgs) throws Throwable {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(invocationName);
        Objects.requireNonNull(condyBSM);
        Objects.requireNonNull(condyArgs);
        if (invokeType.parameterCount() > 0) {
            throw new IllegalArgumentException("Unexpected arguments");
        }
        Object[] args = new Object[condyArgs.length + 3];
        args[0] = lookup;
        args[1] = invocationName;
        args[2] = invokeType.returnType();
        System.arraycopy(condyArgs, 0, args, 3, condyArgs.length);
        Object value = condyBSM.invokeWithArguments(args);
        return new ConstantCallSite(MethodHandles.constant(invokeType.returnType(), value));
    }

    /**
     * callsite for condy's within invokeDynamics
     *
     * flattened args, inner condy args:
     * MethodHandle condyBSM, String name, Class desc, Int argCount, String condyArgs, // use condy bsm to determine arg count to eat
     *
     * @param condyArgs chars who's int value are the args to parse as condys, in order.
     */
    public static CallSite nestedCondyInIndy(MethodHandles.Lookup lookup, String invocationName, MethodType invokeType, MethodHandle indyBsm, String condyArgs, Object... args) throws Throwable {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(invocationName);
        Objects.requireNonNull(indyBsm);
        Objects.requireNonNull(condyArgs);
        char[] condyArgLst = (condyArgs).toCharArray();
        List<Object> indyArgs = new ArrayList<>();
        indyArgs.add(lookup);
        indyArgs.add(invocationName);
        indyArgs.add(invokeType);
        for (int i = 0, j = 0; i < args.length; i++) {
            if (j < condyArgLst.length && i == condyArgLst[j]) {
                j++;
                int[] iValue = new int[] {i};
                indyArgs.add(getCondyValue(lookup, args, iValue));
                i = iValue[0];
            } else {
                indyArgs.add(args[i]);
            }
        }
        return (CallSite) indyBsm.invokeWithArguments(indyArgs);
    }

    private static Object getCondyValue(MethodHandles.Lookup lookup, Object[] args, int[] iValue) throws Throwable {
        int i = iValue[0];
        MethodHandle bsm = (MethodHandle) args[i++];
        String name = (String) args[i++];
        Class<?> desc = (Class<?>) args[i++];
        int argCount = (int) args[i++] + 3;
        char[] condyArgLst = ((String) args[i++]).toCharArray();
        List<Object> condyArgs = new ArrayList<>();
        condyArgs.add(lookup);
        condyArgs.add(name);
        condyArgs.add(desc);
        for (int j = 0, k = 3; k < argCount; i++, k++) {
            if (j < condyArgLst.length && i == condyArgLst[j]) {
                j++;
                iValue[0] = i;
                condyArgs.add(getCondyValue(lookup, args, iValue));
                i = iValue[0];
            } else {
                condyArgs.add(args[i]);
            }
        }
        iValue[0] = --i;
        return bsm.invokeWithArguments(condyArgs);
    }

    // var handle stuff

    private static void checkClass(MethodHandles.Lookup lookup, Class<?> type) {
        try {
            lookup.accessClass(type);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }
}
