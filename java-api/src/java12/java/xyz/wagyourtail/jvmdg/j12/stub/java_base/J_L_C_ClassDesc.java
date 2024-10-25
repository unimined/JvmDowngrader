package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.CoverageIgnore;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Adapter("java/lang/constant/ClassDesc")
public interface J_L_C_ClassDesc extends J_L_C_ConstantDesc, J_L_I_TypeDescriptor.OfField<J_L_C_ClassDesc> {

    private static void validateBinaryClassName(String name) {
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == ';' || ch == '[' || ch == '/') {
                throw new IllegalArgumentException("Invalid class name: " + name);
            }
        }
    }

    private static void validateMemberName(String name) {
        requireNonNull(name);
        if (name.isEmpty())
            throw new IllegalArgumentException("zero-length member name");
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '.' || ch == ';' || ch == '[' || ch == '/')
                throw new IllegalArgumentException("Invalid member name: " + name);
        }
    }


    static J_L_C_ClassDesc of(String name) {
        validateBinaryClassName(name);
        return J_L_C_ClassDesc.ofDescriptor("L" + name.replace('.', '/') + ";");
    }

    static J_L_C_ClassDesc of(String packageName, String className) {
        validateBinaryClassName(Objects.requireNonNull(packageName));
        if (packageName.isEmpty()) {
            return of(className);
        }
        return J_L_C_ClassDesc.ofDescriptor("L" + packageName.replace('.', '/') + '/' + className + ";");
    }

    static J_L_C_ClassDesc ofDescriptor(String descriptor) {
        // count the number of '['
        int arrayCount = 0;
        while (descriptor.charAt(arrayCount) == '[') {
            arrayCount++;
        }
        if (arrayCount > 255) {
            throw new IllegalArgumentException("Cannot create an array type descriptor with more than 255 dimensions");
        }
        return new ClassDescImpl(descriptor);
    }

    default J_L_C_ClassDesc arrayType() {
        int arrayCount = 0;
        String descriptor = descriptorString();
        while (descriptor.charAt(arrayCount) == '[') {
            arrayCount++;
        }
        if (arrayCount >= 255) {
            throw new IllegalArgumentException("Cannot create an array type descriptor with more than 255 dimensions");
        }
        return ofDescriptor("[" + descriptor);
    }

    default J_L_C_ClassDesc arrayType(int rank) {
        if (rank <= 0) {
            throw new IllegalArgumentException("rank " + rank + " is not a positive value");
        }
        int arrayCount = 0;
        String descriptor = descriptorString();
        while (descriptor.charAt(arrayCount) == '[') {
            arrayCount++;
        }
        if (arrayCount + rank > 255) {
            throw new IllegalArgumentException("Cannot create an array type descriptor with more than 255 dimensions");
        }
        return ofDescriptor("[".repeat(rank) + descriptorString());
    }

    default J_L_C_ClassDesc nested(String nestedName) {
        validateMemberName(nestedName);
        if (!isClassOrInterface()) {
            throw new IllegalStateException("Outer class is not a class or interface type");
        }
        String descriptor = descriptorString();
        return ofDescriptor(descriptor.substring(0, descriptor.length() - 1) + "$" + nestedName + ";");
    }

    default J_L_C_ClassDesc nested(String first, String... rest) {
        StringBuilder name = new StringBuilder(first);
        for (String s : rest) {
            name.append("$").append(s);
        }
        return nested(name.toString());
    }

    default boolean isArray() {
        return descriptorString().charAt(0) == '[';
    }

    default boolean isPrimitive() {
        return descriptorString().length() == 1;
    }

    default boolean isClassOrInterface() {
        return descriptorString().startsWith("L");
    }

    default J_L_C_ClassDesc componentType() {
        if (!isArray()) return null;
        return ofDescriptor(descriptorString().substring(1));
    }

    default String packageName() {
        if (!isClassOrInterface()) {
            return "";
        }
        String descriptor = descriptorString().substring(1);
        int slash = descriptor.lastIndexOf('/');
        if (slash == -1) {
            return "";
        }
        return descriptor.substring(0, slash).replace('/', '.');
    }

    default String displayName() {
        if (isPrimitive()) {
            switch (descriptorString()) {
                case "B":
                    return "byte";
                case "C":
                    return "char";
                case "D":
                    return "double";
                case "F":
                    return "float";
                case "I":
                    return "int";
                case "J":
                    return "long";
                case "S":
                    return "short";
                case "Z":
                    return "boolean";
                case "V":
                    return "void";
                default:
                    throw new InternalError("Unexpected primitive type descriptor: " + descriptorString());
            }
        }
        if (isArray()) {
            int arrayCount = 0;
            String descriptor = descriptorString();
            while (descriptor.charAt(arrayCount) == '[') {
                arrayCount++;
            }
            String component = descriptor.substring(arrayCount);
            return J_L_C_ClassDesc.ofDescriptor(component).displayName() + "[]".repeat(arrayCount);
        } else {
            String descriptor = descriptorString();
            int lastSlash = descriptor.lastIndexOf('/');
            if (lastSlash == -1) {
                return descriptor.substring(1, descriptor.length() - 1);
            } else {
                return descriptor.substring(lastSlash + 1, descriptor.length() - 1);
            }
        }
    }

    @Override
    @CoverageIgnore
        // for generics
    Class<?> resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException;

    boolean equals(Object obj);

    class ClassDescImpl implements J_L_C_ClassDesc {
        private final String descriptor;

        private ClassDescImpl(String descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public Class<?> resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
            if (isPrimitive()) {
                switch (descriptorString()) {
                    case "B":
                        return byte.class;
                    case "C":
                        return char.class;
                    case "D":
                        return double.class;
                    case "F":
                        return float.class;
                    case "I":
                        return int.class;
                    case "J":
                        return long.class;
                    case "S":
                        return short.class;
                    case "Z":
                        return boolean.class;
                    case "V":
                        return void.class;
                    default:
                        throw new InternalError("Unexpected primitive type descriptor: " + descriptorString());
                }
            }
            if (isArray()) {
                int arrayCount = 0;
                String descriptor = descriptorString();
                while (descriptor.charAt(arrayCount) == '[') {
                    arrayCount++;
                }
                String component = descriptor.substring(arrayCount);
                Class<?> componentClass = J_L_C_ClassDesc.ofDescriptor(component).resolveConstantDesc(lookup);
                return Class.forName(componentClass.getName() + "[]".repeat(arrayCount), false, lookup.lookupClass().getClassLoader());
            } else {
                String descriptor = descriptorString();
                String className = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                return Class.forName(className, false, lookup.lookupClass().getClassLoader());
            }
        }

        @Override
        public String descriptorString() {
            return descriptor;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof J_L_C_ClassDesc)) return false;
            return descriptor.equals(((J_L_C_ClassDesc) obj).descriptorString());
        }

        @Override
        public int hashCode() {
            return descriptor.hashCode();
        }

        @Override
        public String toString() {
            return "ClassDesc[" + displayName() + "]";
        }

    }

}
