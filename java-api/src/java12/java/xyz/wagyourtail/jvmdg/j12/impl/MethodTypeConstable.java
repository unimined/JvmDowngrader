package xyz.wagyourtail.jvmdg.j12.impl;

import xyz.wagyourtail.jvmdg.j12.stub.java_base.*;

import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MethodTypeConstable implements J_L_C_Constable, J_L_I_TypeDescriptor.OfMethod<ClassConstable, MethodTypeConstable> {
    private final MethodType methodType;

    public MethodTypeConstable(MethodType methodType) {
        this.methodType = methodType;
    }

    @Override
    public Optional<J_L_C_MethodTypeDesc> describeConstable() {
        J_L_C_ClassDesc[] parameterArray = Arrays.stream(parameterArray()).map(c -> c.describeConstable().orElseThrow()).toArray(J_L_C_ClassDesc[]::new);
        J_L_C_ClassDesc returnType = returnType().describeConstable().orElseThrow();
        return Optional.of(J_L_C_MethodTypeDesc.of(returnType, parameterArray));
    }

    @Override
    public int parameterCount() {
        return methodType.parameterCount();
    }

    @Override
    public ClassConstable parameterType(int index) {
        return new ClassConstable(methodType.parameterType(index));
    }

    @Override
    public ClassConstable returnType() {
        return new ClassConstable(methodType.returnType());
    }

    @Override
    public ClassConstable[] parameterArray() {
        return Arrays.stream(methodType.parameterArray()).map(ClassConstable::new).toArray(ClassConstable[]::new);
    }

    @Override
    public List<ClassConstable> parameterList() {
        return methodType.parameterList().stream().map(ClassConstable::new).collect(Collectors.toList());
    }

    @Override
    public MethodTypeConstable changeReturnType(ClassConstable newReturnType) {
        return new MethodTypeConstable(methodType.changeReturnType(newReturnType.clazz));
    }

    @Override
    public MethodTypeConstable changeParameterType(int index, ClassConstable newParameterType) {
        return new MethodTypeConstable(methodType.changeParameterType(index, newParameterType.clazz));
    }

    @Override
    public MethodTypeConstable dropParameterTypes(int fromIndex, int toIndex) {
        return new MethodTypeConstable(methodType.dropParameterTypes(fromIndex, toIndex));
    }

    @Override
    public MethodTypeConstable insertParameterTypes(int index, ClassConstable... newParameterTypes) {
        return new MethodTypeConstable(methodType.insertParameterTypes(index, Arrays.stream(newParameterTypes).map(c -> c.clazz).toArray(Class[]::new)));
    }

    @Override
    public String descriptorString() {
        return methodType.toMethodDescriptorString();
    }

}
