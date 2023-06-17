package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.List;

public class J_L_R_AccessibleObject {

    @Stub(javaVersion = Opcodes.V1_8, subtypes = true)
    public static <T extends Annotation> T[] getAnnotationsByType(AccessibleObject self, Class<T> annotationClass) {
        List<T> result = new ArrayList<>();
        for (Annotation annotation : self.getAnnotations()) {
            if (annotation.annotationType().equals(annotationClass)) {
                result.add((T) annotation);
            }
        }
        return (T[]) result.toArray(new Annotation[0]);
    }

    @Stub(javaVersion = Opcodes.V1_8, subtypes = true)
    public static <T extends Annotation> T getDeclaredAnnotation(AccessibleObject self, Class<T> annotationClass) {
        for (Annotation annotation : self.getDeclaredAnnotations()) {
            if (annotation.annotationType().equals(annotationClass)) {
                return (T) annotation;
            }
        }
        return null;
    }

    @Stub(javaVersion = Opcodes.V1_8, subtypes = true)
    public static <T extends Annotation> T[] getDeclaredAnnotationsByType(AccessibleObject self, Class<T> annotationClass) {
        List<T> result = new ArrayList<>();
        for (Annotation annotation : self.getDeclaredAnnotations()) {
            if (annotation.annotationType().equals(annotationClass)) {
                result.add((T) annotation);
            }
        }
        return (T[]) result.toArray(new Annotation[0]);
    }
}
