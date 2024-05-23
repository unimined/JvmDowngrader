package xyz.wagyourtail.jvmdg.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationUtils {

    // create annotation from AnnotationNode
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T createAnnotation(AnnotationNode classNode) throws ClassNotFoundException {
        return (T) Proxy.newProxyInstance(
                AnnotationUtils.class.getClassLoader(),
                new Class[]{Class.forName(Type.getType(classNode.desc).getClassName())},
                new Handler(classNode)
        );
    }

    private static Object enumValueOf(String desc, String value) throws ClassNotFoundException {
        return Enum.valueOf((Class<Enum>) AnnotationUtils.getClass(Type.getType(desc)), value);
    }

    private static Class<?> getClass(Type type) throws ClassNotFoundException {
        switch (type.getSort()) {
            case Type.VOID:
                return void.class;
            case Type.BOOLEAN:
                return boolean.class;
            case Type.CHAR:
                return char.class;
            case Type.BYTE:
                return byte.class;
            case Type.SHORT:
                return short.class;
            case Type.INT:
                return int.class;
            case Type.FLOAT:
                return float.class;
            case Type.LONG:
                return long.class;
            case Type.DOUBLE:
                return double.class;
            default:
                return Class.forName(type.getClassName());
        }

    }

    static class Handler implements InvocationHandler {
        final Map<String, Object> values;

        Handler(AnnotationNode node) {
            values = new HashMap<>();
            for (int i = 0; i < node.values.size(); i += 2) {
                values.put((String) node.values.get(i), node.values.get(i + 1));
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws ClassNotFoundException {
            return convertType(values.get(method.getName()), method.getReturnType());
        }

        @SuppressWarnings("unchecked")
        private <T> T convertType(Object value, Class<T> type) throws ClassNotFoundException {
            if (value instanceof Type) {
                return (T) AnnotationUtils.getClass((Type) value);
            }
            if (value instanceof AnnotationNode) {
                return AnnotationUtils.createAnnotation((AnnotationNode) value);
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
                return (T) AnnotationUtils.enumValueOf(arr[0], arr[1]);
            }
            return (T) value;
        }
    }

}
