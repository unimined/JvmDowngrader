package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

public class J_L_Class {

    @Stub
    public static String toGenericString(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return clazz.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            Class<?> component = clazz;
            int arrayDepth = 0;

            if (clazz.isArray()) {
                do {
                    arrayDepth++;
                    component = component.getComponentType();
                } while (component.isArray());
                sb.append(component.getName());
            } else {
                int modifiers = clazz.getModifiers() & Modifier.classModifiers();
                if (modifiers != 0) {
                    sb.append(Modifier.toString(modifiers));
                    sb.append(' ');
                }
                if (clazz.isAnnotation()) {
                    sb.append('@');
                }
                if (clazz.isInterface()) {
                    sb.append("interface");
                } else {
                    if (clazz.isEnum())
                        sb.append("enum");
                        //TODO: record case in java 16
                    else
                        sb.append("class");
                }
                sb.append(' ');
                sb.append(clazz.getName());
            }

            TypeVariable<?>[] typeparms = component.getTypeParameters();
            if (typeparms.length > 0) {
                sb.append('<');
                for (int i = 0; i < typeparms.length; i++) {
                    sb.append(typeVarBounds(typeparms[i]));
                    if (i < typeparms.length - 1)
                        sb.append(',');
                }
                sb.append('>');
            }

            if (arrayDepth > 0) {
                for (int i = 0; i < arrayDepth; i++) {
                    sb.append("[]");
                }
            }

            return sb.toString();
        }
    }

    // internal in java.lang.Class
    private static String typeVarBounds(TypeVariable<?> typeVar) {
        Type[] bounds = typeVar.getBounds();
        if (bounds.length == 1 && bounds[0].equals(Object.class)) {
            return typeVar.getName();
        } else {
            StringBuilder sb = new StringBuilder(typeVar.getName()).append(" extends ");
            for (int i = 0; i < bounds.length; i++) {
                sb.append((bounds[i] instanceof Class) ? getTypeName((Class<?>) bounds[i]) : J_L_R_Type.getTypeName(bounds[i]));
                if (i < bounds.length - 1)
                    sb.append(" & ");
            }
            return sb.toString();
        }
    }

    @Stub
    public static String getTypeName(Class clazz) {
        if (clazz.isArray()) {
            try {
                Class<?> cl = clazz;
                int dimensions = 0;
                do {
                    dimensions++;
                    cl = cl.getComponentType();
                } while (cl.isArray());
                StringBuilder sb = new StringBuilder(cl.getName());
                for (int i = 0; i < dimensions; i++) {
                    sb.append("[]");
                }
                return sb.toString();
            } catch (Throwable ignored) {
            }
        }
        return clazz.getName();
    }

    @Stub
    public static <A extends Annotation> A[] getAnnotationsByType(Class<?> self, Class<A> clazz) {
        List<A> annotations = new ArrayList<>();
        for (Annotation a : self.getAnnotations()) {
            if (clazz.isInstance(a)) {
                annotations.add(clazz.cast(a));
            }
        }
        return annotations.toArray((A[]) new Annotation[0]);
    }

    @Stub
    public static <A extends Annotation> A getDeclaredAnnotation(Class<?> self, Class<A> clazz) {
        for (Annotation a : self.getDeclaredAnnotations()) {
            if (clazz.isInstance(a)) {
                return clazz.cast(a);
            }
        }
        return null;
    }

    @Stub
    public static <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<?> self, Class<A> clazz) {
        List<A> annotations = new ArrayList<>();
        for (Annotation a : self.getDeclaredAnnotations()) {
            if (clazz.isInstance(a)) {
                annotations.add(clazz.cast(a));
            }
        }
        return annotations.toArray((A[]) new Annotation[0]);
    }

//    @Stub(javaVersion = Opcodes.V1_8)
//    public static J_L_R_AnnotationType getAnnotatedSuperclass(Class<?> clazz) {
//        //TODO
//        throw new UnsupportedOperationException("TODO");
//    }
//
//    @Stub(javaVersion = Opcodes.V1_8)
//    public static J_L_R_AnnotationType[] getAnnotatedInterfaces(Class<?> clazz) {
//        //TODO
//        throw new UnsupportedOperationException("TODO");
//    }
}
