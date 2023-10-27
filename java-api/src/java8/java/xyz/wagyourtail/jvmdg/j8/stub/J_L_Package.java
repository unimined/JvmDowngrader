package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class J_L_Package {

    @Stub
    public static <A extends Annotation> A[] getAnnotationsByType(Package self, Class<A> annotationClass) {
        List<A> result = new ArrayList<>();
        for (Annotation a : self.getAnnotations()) {
            if (annotationClass.isInstance(a)) {
                result.add(annotationClass.cast(a));
            }
        }
        return result.toArray((A[]) new Annotation[0]);
    }

    @Stub
    public static <A extends Annotation> A getDeclaredAnnotation(Package self, Class<A> annotationClass) {
        for (Annotation a : self.getDeclaredAnnotations()) {
            if (annotationClass.isInstance(a)) {
                return annotationClass.cast(a);
            }
        }
        return null;
    }

    @Stub
    public static <A extends Annotation> A[] getDeclaredAnnotationsByType(Package self, Class<A> annotationClass) {
        List<A> result = new ArrayList<>();
        for (Annotation a : self.getDeclaredAnnotations()) {
            if (annotationClass.isInstance(a)) {
                result.add(annotationClass.cast(a));
            }
        }
        return result.toArray((A[]) new Annotation[0]);
    }


}
