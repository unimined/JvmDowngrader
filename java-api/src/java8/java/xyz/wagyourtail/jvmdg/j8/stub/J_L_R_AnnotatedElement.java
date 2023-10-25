package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

public class J_L_R_AnnotatedElement {

    @Stub(opcVers = Opcodes.V1_8)
    public static <T extends Annotation> T[] getAnnotationsByType(AnnotatedElement self, Class<T> annotationClass) {
        List<T> result = new ArrayList<>();
        for (Annotation annotation : self.getAnnotations()) {
            if (annotation.annotationType().equals(annotationClass)) {
                result.add((T) annotation);
            }
        }
        return (T[]) result.toArray(new Annotation[0]);
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static <T extends Annotation> T getDeclaredAnnotation(AnnotatedElement self, Class<T> annotationClass) {
        for (Annotation annotation : self.getDeclaredAnnotations()) {
            if (annotation.annotationType().equals(annotationClass)) {
                return (T) annotation;
            }
        }
        return null;
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static <T extends Annotation> T[] getDeclaredAnnotationsByType(AnnotatedElement self, Class<T> annotationClass) {
        List<T> result = new ArrayList<>();
        for (Annotation annotation : self.getDeclaredAnnotations()) {
            if (annotation.annotationType().equals(annotationClass)) {
                result.add((T) annotation);
            }
        }
        return (T[]) result.toArray(new Annotation[0]);
    }
}
