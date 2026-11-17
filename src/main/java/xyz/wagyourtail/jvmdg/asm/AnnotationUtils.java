package xyz.wagyourtail.jvmdg.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.lang.annotation.Annotation;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationUtils {

    private AnnotationUtils() {
    }

    /**
     * Copies an annotation node to a new annotation node
     *
     * @param from original node
     * @param <E>  type of the annotation node
     * @return new node
     */
    @SuppressWarnings("unchecked")
    public static <E extends AnnotationNode> E copyAnnotationNode(E from) {
        if (from.getClass() == LocalVariableAnnotationNode.class) {
            LocalVariableAnnotationNode fromLv = (LocalVariableAnnotationNode) from;
            int[] indexes = new int[fromLv.index.size()];
            for (int i = 0; i < indexes.length; i++) {
                indexes[i] = fromLv.index.get(i);
            }
            LocalVariableAnnotationNode toLv = new LocalVariableAnnotationNode(fromLv.typeRef, fromLv.typePath, fromLv.start.toArray(new LabelNode[0]), fromLv.end.toArray(new LabelNode[0]), indexes, fromLv.desc);
            from.accept(toLv);
            return (E) toLv;
        }
        if (from.getClass() == TypeAnnotationNode.class) {
            TypeAnnotationNode fromT = (TypeAnnotationNode) from;
            TypeAnnotationNode toT = new TypeAnnotationNode(fromT.typeRef, fromT.typePath, fromT.desc);
            from.accept(toT);
            return (E) toT;
        }
        if (from.getClass() == AnnotationNode.class) {
            AnnotationNode toA = new AnnotationNode(from.desc);
            from.accept(toA);
            return (E) toA;
        }
        throw new RuntimeException("Unknown annotation type: " + from.getClass());
    }

    /**
     * Creates an annotation from an annotation node using the classloader of AnnotationUtils to find the annotation class (and the context classloader)
     *
     * @param annotationNode the node to construct the annotation from
     * @param <T>            the type of the annotation
     * @return the annotation
     * @throws ClassNotFoundException if the annotation class cannot be found
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T createAnnotation(AnnotationNode annotationNode) throws ClassNotFoundException {
        Class<?> annotationClass;
        try {
            annotationClass = ASMUtils.getClass(Type.getType(annotationNode.desc), AnnotationUtils.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            annotationClass = ASMUtils.getClass(Type.getType(annotationNode.desc), Thread.currentThread().getContextClassLoader());
        }
        return createAnnotation(annotationNode, (Class<T>) annotationClass);
    }

    /**
     * This is the recommended method in this class to create annotations
     *
     * @param annotationNode  the node to construct the annotation from
     * @param annotationClass the class of the annotation
     * @param <T>             the type of the annotation
     * @return the annotation
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T createAnnotation(AnnotationNode annotationNode, Class<T> annotationClass) {
        ClassLoader loader = annotationClass.getClassLoader();
        if (!ASMUtils.equals(Type.getType(annotationNode.desc), annotationClass)) {
            throw new IllegalArgumentException("AnnotationNode type (" + annotationNode.desc + ") does not match annotation class (" + annotationClass.getName() + ")");
        }
        return (T) Proxy.newProxyInstance(loader, new Class[]{annotationClass}, new Handler(annotationClass, annotationNode, loader));
    }

    /**
     * Creates an annotation from an AnnotationNode using the specified classloader to find the annotation class
     *
     * @param annotationNode the node to construct the annotation from
     * @param loader         the classloader to find the annotation class
     * @param <T>            the type of the annotation
     * @return the annotation
     * @throws ClassNotFoundException if the annotation class cannot be found
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T createAnnotation(AnnotationNode annotationNode, ClassLoader loader) throws ClassNotFoundException {
        Class<?> annotationClass = ASMUtils.getClass(Type.getType(annotationNode.desc), loader);
        return createAnnotation(annotationNode, (Class<T>) annotationClass);
    }


    static class Handler implements InvocationHandler {
        final Class<?> annotationClass;
        final Map<String, Object> values;
        final ClassLoader loader;

        Handler(Class<?> annotationClass, AnnotationNode node, ClassLoader loader) {
            this.annotationClass = annotationClass;
            values = new HashMap<>();
            if (node.values != null) {
                for (int i = 0; i < node.values.size(); i += 2) {
                    values.put((String) node.values.get(i), node.values.get(i + 1));
                }
            }
            this.loader = loader;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
                    case "toString": return toString();
                    case "hashCode": return annotationHashCode();
                    case "equals": return annotationEquals(proxy, args[0]);
                }
            } else if (method.getName().equals("annotationType")) {
                return annotationClass;
            }
            if (!values.containsKey(method.getName())) {
                Object annDefault = method.getDefaultValue();
                if (annDefault != null) {
                    return annDefault;
                }
                // matches JDK annotation-proxy behaviour for a missing
                // required member
                throw new IncompleteAnnotationException(
                    annotationClass.asSubclass(Annotation.class), method.getName());
            }
            //cache the converted value — array members materialize
            //once instead of being re-allocated on every call
            Object converted = convertType(values.get(method.getName()), method.getReturnType());
            values.put(method.getName(), converted);
            return converted;
        }

        private Object memberValue(String name, Method method) throws Throwable {
            if (values.containsKey(name)) {
                return convertType(values.get(name), method.getReturnType());
            }
            return method.getDefaultValue();
        }

        /** equals/hashCode/toString per java.lang.annotation.Annotation */
        boolean annotationEquals(Object proxy, Object other) throws Throwable {
            if (proxy == other) return true;
            if (!annotationClass.isInstance(other)) return false;
            for (Method m : annotationClass.getDeclaredMethods()) {
                if (!memberEquals(memberValue(m.getName(), m), m.invoke(other))) {
                    return false;
                }
            }
            return true;
        }

        int annotationHashCode() throws Throwable {
            int h = 0;
            for (Method m : annotationClass.getDeclaredMethods()) {
                h += (127 * m.getName().hashCode()) ^ memberHash(memberValue(m.getName(), m));
            }
            return h;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("@").append(annotationClass.getName()).append('(');
            boolean first = true;
            for (Map.Entry<String, Object> e : values.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(e.getKey()).append('=').append(memberString(e.getValue()));
            }
            return sb.append(')').toString();
        }

        static boolean memberEquals(Object a, Object b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            if (a.getClass().isArray() && b.getClass().isArray()) {
                int len = Array.getLength(a);
                if (len != Array.getLength(b)) return false;
                for (int i = 0; i < len; i++) {
                    if (!memberEquals(Array.get(a, i), Array.get(b, i))) return false;
                }
                return true;
            }
            return a.equals(b);
        }

        static int memberHash(Object v) {
            if (v == null) return 0;
            if (v instanceof Object[]) return Arrays.deepHashCode((Object[]) v);
            if (v.getClass().isArray()) {
                int h = 1;
                for (int i = 0; i < Array.getLength(v); i++) {
                    h = 31 * h + memberHash(Array.get(v, i));
                }
                return h;
            }
            return v.hashCode();
        }

        static String memberString(Object v) {
            if (v instanceof Object[]) return Arrays.deepToString((Object[]) v);
            if (v != null && v.getClass().isArray()) {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < Array.getLength(v); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(memberString(Array.get(v, i)));
                }
                return sb.append(']').toString();
            }
            return String.valueOf(v);
        }

        @SuppressWarnings("unchecked")
        private <T> T convertType(Object value, Class<T> type) throws ClassNotFoundException {
            if (value instanceof Type) {
                return (T) ASMUtils.getClass((Type) value, loader);
            }
            if (value instanceof AnnotationNode) {
                return AnnotationUtils.createAnnotation((AnnotationNode) value, loader);
            }
            if (value instanceof List<?>) {
                Class<?> componentType = type.getComponentType();
                Object arr = Array.newInstance(componentType, ((List<?>) value).size());
                for (int i = 0; i < ((List<?>) value).size(); i++) {
                    Array.set(arr, i, convertType(((List<?>) value).get(i), componentType));
                }
                return (T) arr;
            }
            if (value instanceof String[]) {
                String[] arr = (String[]) value;
                if (arr.length != 2) {
                    throw new IllegalArgumentException("Invalid array length for enum");
                }
                return (T) ASMUtils.enumValueOf(arr[0], arr[1], loader);
            }
            return (T) value;
        }

    }

}
