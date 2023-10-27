package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class J_L_R_Field {

    @Stub
    public static <T extends Annotation> T[] getAnnotationsByType(Field self, Class<T> annotationClass) {
        List<T> result = new ArrayList<>();
        for (Annotation annotation : self.getAnnotations()) {
            if (annotation.annotationType().equals(annotationClass)) {
                result.add((T) annotation);
            }
        }
        return (T[]) result.toArray(new Annotation[0]);
    }

    // getAnnotatedType

}
