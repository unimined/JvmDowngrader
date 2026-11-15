package xyz.wagyourtail.jvmdg.stub.all.stub;

import xyz.wagyourtail.jvmdg.stub.StubMethodView;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * {@link Method} stubs presenting the source-API view for members the
 * reflection table hands out — thin delegates into the ASM-free
 * {@link StubMethodView} layer. Impl methods carry the receiver in arg0 and
 * answer through their registered view; ordinary {@code Method} objects
 * pass straight through on one identity-map probe.
 */
public class J_L_R_Method {

    @Stub
    public static Object invoke(Method self, Object obj, Object[] args) throws InvocationTargetException, IllegalAccessException {
        return StubMethodView.invoke(self, obj, args);
    }

    @Stub
    public static Class<?> getDeclaringClass(Method self) {
        return StubMethodView.getDeclaringClass(self);
    }

    @Stub
    public static Class<?>[] getParameterTypes(Method self) {
        return StubMethodView.getParameterTypes(self);
    }

    /**
     * {@code Method.getReturnType} — the source signature's return type;
     * an impl's declared return can be adapter-typed where the source was
     * a stand-in.
     */
    @Stub
    public static Class<?> getReturnType(Method self) {
        return StubMethodView.getReturnType(self);
    }

    @Stub
    public static int getParameterCount(Method self) {
        return StubMethodView.getParameterCount(self);
    }

    @Stub
    public static int getModifiers(Method self) {
        return StubMethodView.getModifiers(self);
    }

    @Stub
    public static String getName(Method self) {
        return StubMethodView.getName(self);
    }

}
