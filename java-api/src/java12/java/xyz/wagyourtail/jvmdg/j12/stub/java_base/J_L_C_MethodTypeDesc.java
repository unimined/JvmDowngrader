package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.j11.impl.CharReader;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Adapter("java/lang/constant/MethodTypeDesc")
public interface J_L_C_MethodTypeDesc extends J_L_C_ConstantDesc, J_L_I_TypeDescriptor.OfMethod<J_L_C_ClassDesc, J_L_C_MethodTypeDesc> {

    static J_L_C_MethodTypeDesc ofDescriptor(String descriptor) {
        return MethodTypeDescImpl.ofDescriptor(descriptor);
    }

    static J_L_C_MethodTypeDesc of(J_L_C_ClassDesc returnDesc, J_L_C_ClassDesc... args) {
        return new MethodTypeDescImpl(returnDesc, args);
    }

    @Override
    J_L_C_ClassDesc returnType();

    @Override
    int parameterCount();

    @Override
    J_L_C_ClassDesc parameterType(int index);

    @Override
    List<J_L_C_ClassDesc> parameterList();

    @Override
    J_L_C_ClassDesc[] parameterArray();

    @Override
    J_L_C_MethodTypeDesc changeReturnType(J_L_C_ClassDesc returnType);

    @Override
    J_L_C_MethodTypeDesc changeParameterType(int index, J_L_C_ClassDesc type);

    @Override
    J_L_C_MethodTypeDesc dropParameterTypes(int index, int end);

    @Override
    J_L_C_MethodTypeDesc insertParameterTypes(int index, J_L_C_ClassDesc... newParameterTypes);

    default String descriptorString() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < parameterCount(); i++) {
            sb.append(parameterType(i).descriptorString());
        }
        sb.append(")");
        sb.append(returnType().descriptorString());
        return sb.toString();
    }

    default String displayDescriptor() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < parameterCount(); i++) {
            sb.append(parameterType(i).displayName());
            if (i < parameterCount() - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        sb.append(returnType().displayName());
        return sb.toString();
    }

    @Override
    boolean equals(Object o);

    class MethodTypeDescImpl implements J_L_C_MethodTypeDesc {
        J_L_C_ClassDesc returnType;
        J_L_C_ClassDesc[] parameterType;

        MethodTypeDescImpl(J_L_C_ClassDesc returnType, J_L_C_ClassDesc[] parameterType) {
            this.returnType = returnType;
            this.parameterType = parameterType;

            for (J_L_C_ClassDesc c : parameterType) {
                if (c.isPrimitive() && c.descriptorString().equals("V")) {
                    throw new IllegalArgumentException("void types cannot be used as a parameter.");
                }
            }
        }

        static MethodTypeDescImpl ofDescriptor(String descriptor) {
            CharReader cr = new CharReader(descriptor, 0);
            List<J_L_C_ClassDesc> arg = new ArrayList<>();
            cr.expect('(');
            while (cr.peek() != ')') {
                if (cr.peek() == 'L') {
                    String c = cr.takeUntil(';');
                    cr.expect(';');
                    arg.add(J_L_C_ClassDesc.ofDescriptor(c + ";"));
                } else {
                    arg.add(J_L_C_ClassDesc.ofDescriptor(Character.toString(cr.take())));
                }
            }
            cr.expect(')');
            if (cr.peek() == 'L') {
                String c = cr.takeUntil(';');
                cr.expect(';');
                if (!cr.exhausted()) throw new IllegalArgumentException("invalid descriptor");
                return new MethodTypeDescImpl(J_L_C_ClassDesc.ofDescriptor(c + ";"), arg.toArray(new J_L_C_ClassDesc[0]));
            } else {
                String c = Character.toString(cr.take());
                if (!cr.exhausted()) throw new IllegalArgumentException("invalid descriptor");
                return new MethodTypeDescImpl(J_L_C_ClassDesc.ofDescriptor(c), arg.toArray(new J_L_C_ClassDesc[0]));
            }
        }

        @Override
        public J_L_C_ClassDesc returnType() {
            return returnType;
        }

        @Override
        public int parameterCount() {
            return parameterType.length;
        }

        @Override
        public J_L_C_ClassDesc parameterType(int index) {
            return parameterType[index];
        }

        @Override
        public List<J_L_C_ClassDesc> parameterList() {
            return List.of(parameterType);
        }

        @Override
        public J_L_C_ClassDesc[] parameterArray() {
            return parameterType.clone();
        }

        @Override
        public J_L_C_MethodTypeDesc changeReturnType(J_L_C_ClassDesc returnType) {
            return new MethodTypeDescImpl(returnType, parameterType);
        }

        @Override
        public J_L_C_MethodTypeDesc changeParameterType(int index, J_L_C_ClassDesc type) {
            J_L_C_ClassDesc[] params = parameterArray();
            params[index] = type;
            return new MethodTypeDescImpl(returnType, params);
        }

        @Override
        public J_L_C_MethodTypeDesc dropParameterTypes(int index, int end) {
            Objects.checkIndex(index, parameterType.length);
            Objects.checkFromToIndex(index, end, parameterType.length);

            J_L_C_ClassDesc[] newArgs = new J_L_C_ClassDesc[parameterType.length - (end - index)];
            System.arraycopy(parameterType, 0, newArgs, 0, index);
            System.arraycopy(parameterType, end, newArgs, index, parameterType.length - end);
            return new MethodTypeDescImpl(returnType, newArgs);
        }

        @Override
        public J_L_C_MethodTypeDesc insertParameterTypes(int index, J_L_C_ClassDesc... newParameterTypes) {
            Objects.checkIndex(index, parameterType.length + 1);
            J_L_C_ClassDesc[] newArgs = new J_L_C_ClassDesc[parameterType.length + newParameterTypes.length];
            System.arraycopy(parameterType, 0, newArgs, 0, index);
            System.arraycopy(newParameterTypes, 0, newArgs, index, newParameterTypes.length);
            System.arraycopy(parameterType, index, newArgs, index + newParameterTypes.length, parameterType.length - index);
            return new MethodTypeDescImpl(returnType, newArgs);
        }

        @Override
        public MethodType resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
            List<Class<?>> args = new ArrayList<>();
            for (int i = 0; i < parameterCount(); i++) {
                args.add(parameterType(i).resolveConstantDesc(lookup));
            }
            return MethodType.methodType(returnType.resolveConstantDesc(lookup), args.toArray(new Class<?>[0]));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodTypeDescImpl that = (MethodTypeDescImpl) o;
            return Objects.equals(returnType, that.returnType) && Objects.deepEquals(parameterType, that.parameterType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(returnType, Arrays.hashCode(parameterType));
        }

        @Override
        public String toString() {
            return "MethodTypeDesc[" + displayDescriptor() + "]";
        }

    }

}
