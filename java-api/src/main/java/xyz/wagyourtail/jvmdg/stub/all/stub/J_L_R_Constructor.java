package xyz.wagyourtail.jvmdg.stub.all.stub;

import xyz.wagyourtail.jvmdg.stub.reflection.StubCtorView;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * {@link Constructor} stubs presenting the source-API view for stubbed
 * {@code <init>} members the reflection table hands out — thin delegates
 * into the ASM-free {@link StubCtorView} layer. The handed-out object is the
 * retarget ctor's own {@code Constructor}; for registered instances the
 * view answers with the original (pre-retarget) signature — parameter
 * types/count, declaration flags, and {@code newInstance} evaluated through
 * the synthesized rewrite handle. Ordinary {@code Constructor} objects pass
 * straight through on one map probe.
 */
public class J_L_R_Constructor {

    @Stub
    public static Object newInstance(Constructor<?> self, Object[] initargs)
        throws InstantiationException, IllegalAccessException,
        IllegalArgumentException, InvocationTargetException {
        return StubCtorView.newInstance(self, initargs);
    }

    @Stub
    public static Class<?>[] getParameterTypes(Constructor<?> self) {
        return StubCtorView.getParameterTypes(self);
    }

    @Stub
    public static int getParameterCount(Constructor<?> self) {
        return StubCtorView.getParameterCount(self);
    }

    @Stub
    public static int getModifiers(Constructor<?> self) {
        return StubCtorView.getModifiers(self);
    }

}
