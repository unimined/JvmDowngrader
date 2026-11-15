package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.j12.impl.ClassConstable;
import xyz.wagyourtail.jvmdg.j12.impl.MethodTypeConstable;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.List;

@Adapter("java/lang/invoke/TypeDescriptor")
public interface J_L_I_TypeDescriptor {

    String descriptorString();

    @Adapter(value = "java/lang/invoke/TypeDescriptor$OfField", impls = {ClassConstable.class})
    interface OfField<T extends J_L_I_TypeDescriptor.OfField<T>> extends J_L_I_TypeDescriptor {

        @SuppressWarnings("rawtypes")
        boolean isArray();

        boolean isPrimitive();

        T componentType();

        T arrayType();

    }

    @Adapter(value = "java/lang/invoke/TypeDescriptor$OfMethod", impls = {MethodTypeConstable.class})
    interface OfMethod<F extends J_L_I_TypeDescriptor.OfField<F>, M extends J_L_I_TypeDescriptor.OfMethod<F, M>> extends J_L_I_TypeDescriptor {

        @SuppressWarnings("rawtypes")
        int parameterCount();

        F parameterType(int index);

        F returnType();

        F[] parameterArray();

        List<F> parameterList();

        M changeReturnType(F newReturnType);

        M changeParameterType(int index, F newParameterType);

        M dropParameterTypes(int fromIndex, int toIndex);

        M insertParameterTypes(int index, F... newParameterTypes);

    }


}
