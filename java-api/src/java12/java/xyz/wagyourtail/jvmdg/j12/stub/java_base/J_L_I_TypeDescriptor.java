package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.j12.impl.ClassConstable;
import xyz.wagyourtail.jvmdg.j12.impl.MethodTypeConstable;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Coerce;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodType;
import java.util.List;

@Adapter("java/lang/invoke/TypeDescriptor")
public interface J_L_I_TypeDescriptor {

    static boolean jvmdg$instanceof(Object obj) {
        return OfField.jvmdg$instanceof(obj) || OfMethod.jvmdg$instanceof(obj);
    }

    static J_L_I_TypeDescriptor jvmdg$checkcast(Object obj) {
        if (OfField.jvmdg$instanceof(obj)) {
            return OfField.jvmdg$checkcast(obj);
        }
        return OfMethod.jvmdg$checkcast(obj);
    }

    String descriptorString();

    @Adapter("java/lang/invoke/TypeDescriptor$OfField")
    interface OfField<T extends J_L_I_TypeDescriptor.OfField<T>> extends J_L_I_TypeDescriptor {

        static boolean jvmdg$instanceof(Object obj) {
            return obj instanceof OfField ||
                obj instanceof Class<?>;
        }

        @SuppressWarnings("rawtypes")
        static OfField jvmdg$checkcast(Object obj) {
            if (obj instanceof OfField) {
                return (OfField) obj;
            }
            if (obj instanceof Class<?>) {
                return new ClassConstable((Class<?>) obj);
            }
            throw new IllegalArgumentException("Cannot cast " + obj.getClass() + " to " + OfField.class);
        }

        boolean isArray();

        boolean isPrimitive();

        T componentType();

        T arrayType();

        @Stub(noSpecial = true)
        static boolean isArray(@Coerce(OfField.class) Object self) {
            return jvmdg$checkcast(self).isArray();
        }

        @Stub(noSpecial = true)
        static boolean isPrimitive(@Coerce(OfField.class) Object self) {
            return jvmdg$checkcast(self).isPrimitive();
        }

        @Stub(noSpecial = true)
        static OfField componentType(@Coerce(OfField.class) Object self) {
            return jvmdg$checkcast(self).componentType();
        }

        @Stub(noSpecial = true)
        static OfField arrayType(@Coerce(OfField.class) Object self) {
            return jvmdg$checkcast(self).arrayType();
        }

    }

    @Adapter("java/lang/invoke/TypeDescriptor$OfMethod")
    interface OfMethod<F extends J_L_I_TypeDescriptor.OfField<F>, M extends J_L_I_TypeDescriptor.OfMethod<F, M>> extends J_L_I_TypeDescriptor {

        static boolean jvmdg$instanceof(Object obj) {
            return obj instanceof OfMethod;
        }

        @SuppressWarnings("rawtypes")
        static OfMethod jvmdg$checkcast(Object obj) {
            if (obj instanceof OfMethod) {
                return (OfMethod) obj;
            }
            if (obj instanceof MethodType) {
                return new MethodTypeConstable((MethodType) obj);
            }
            throw new IllegalArgumentException("Cannot cast " + obj.getClass() + " to " + OfMethod.class);
        }

        int parameterCount();

        F parameterType(int index);

        F returnType();

        F[] parameterArray();

        List<F> parameterList();

        M changeReturnType(F newReturnType);

        M changeParameterType(int index, F newParameterType);

        M dropParameterTypes(int fromIndex, int toIndex);

        M insertParameterTypes(int index, F... newParameterTypes);

        @Stub(noSpecial = true)
        static int parameterCount(@Coerce(OfMethod.class) Object self) {
            return jvmdg$checkcast(self).parameterCount();
        }

        @Stub(noSpecial = true)
        static OfField parameterType(@Coerce(OfMethod.class) Object self, int index) {
            return jvmdg$checkcast(self).parameterType(index);
        }

        @Stub(noSpecial = true)
        static OfField returnType(@Coerce(OfMethod.class) Object self) {
            return jvmdg$checkcast(self).returnType();
        }

        @Stub(noSpecial = true)
        static OfField[] parameterArray(@Coerce(OfMethod.class) Object self) {
            return jvmdg$checkcast(self).parameterArray();
        }

        @Stub(noSpecial = true)
        static List<OfField> parameterList(@Coerce(OfMethod.class) Object self) {
            return jvmdg$checkcast(self).parameterList();
        }

        @Stub(noSpecial = true)
        static OfMethod changeReturnType(@Coerce(OfMethod.class) Object self, OfField newReturnType) {
            return jvmdg$checkcast(self).changeReturnType(newReturnType);
        }

        @Stub(noSpecial = true)
        static OfMethod changeParameterType(@Coerce(OfMethod.class) Object self, int index, OfField newParameterType) {
            return jvmdg$checkcast(self).changeParameterType(index, newParameterType);
        }

        @Stub(noSpecial = true)
        static OfMethod dropParameterTypes(@Coerce(OfMethod.class) Object self, int fromIndex, int toIndex) {
            return jvmdg$checkcast(self).dropParameterTypes(fromIndex, toIndex);
        }

        @Stub(noSpecial = true)
        static OfMethod insertParameterTypes(@Coerce(OfMethod.class) Object self, int index, OfField... newParameterTypes) {
            return jvmdg$checkcast(self).insertParameterTypes(index, newParameterTypes);
        }

    }


}
