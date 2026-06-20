package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

@JEP(277)
public class J_L_R_AnnotatedElement {


    private static final J_L_Deprecated DEFAULT_DEP = new J_L_Deprecated() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return J_L_Deprecated.class;
        }

        @Override
        public boolean forRemoval() {
            return false;
        }

        @Override
        public String since() {
            return "";
        }
    };


    @Stub
    public static Annotation getAnnotation(AnnotatedElement self, Class<? extends Annotation> annotationClass) {
        if (annotationClass == J_L_Deprecated.class) {
            J_L_Deprecated deprecated = self.getAnnotation(J_L_Deprecated.class);
            if (deprecated != null) {
                return deprecated;
            }
            Deprecated old_dep = self.getAnnotation(Deprecated.class);
            if (old_dep != null) {
                return DEFAULT_DEP;
            }
        }
        return self.getAnnotation(annotationClass);
    }

    @Stub
    public static Annotation getDeclaredAnnotation(AnnotatedElement self, Class<? extends Annotation> annotationClass) {
        if (annotationClass == J_L_Deprecated.class) {
            J_L_Deprecated deprecated = self.getDeclaredAnnotation(J_L_Deprecated.class);
            if (deprecated != null) {
                return deprecated;
            }
            Deprecated old_dep = self.getDeclaredAnnotation(Deprecated.class);
            if (old_dep != null) {
                return DEFAULT_DEP;
            }
        }
        return self.getDeclaredAnnotation(annotationClass);
    }

    @Stub
    public static boolean isAnnotationPresent(AnnotatedElement self, Class<? extends Annotation> annotationClass) {
        return self.isAnnotationPresent(annotationClass) || (annotationClass == J_L_Deprecated.class && self.isAnnotationPresent(Deprecated.class));
    }

    @Stub
    public static Annotation[] getAnnotationsByType(AnnotatedElement self, Class<? extends Annotation> annotationClass) {
        if (annotationClass == J_L_Deprecated.class) {
            Annotation[] a = self.getAnnotationsByType(J_L_Deprecated.class);
            if (a.length > 0) {
                return a;
            }
            Annotation[] b = self.getAnnotationsByType(Deprecated.class);
            if (b.length > 0) {
                return new Annotation[]{DEFAULT_DEP};
            }
        }
        return self.getAnnotationsByType(annotationClass);
    }

    @Stub
    public static Annotation[] getDeclaredAnnotationsByType(AnnotatedElement self, Class<? extends Annotation> annotationClass) {
        if (annotationClass == J_L_Deprecated.class) {
            Annotation[] a = self.getDeclaredAnnotationsByType(J_L_Deprecated.class);
            if (a.length > 0) {
                return a;
            }
            Annotation[] b = self.getDeclaredAnnotationsByType(Deprecated.class);
            if (b.length > 0) {
                return new Annotation[]{DEFAULT_DEP};
            }
        }
        return self.getDeclaredAnnotationsByType(annotationClass);
    }

    @Stub
    public static Annotation[] getAnnotations(AnnotatedElement self) {
        List<Annotation> annotations = new ArrayList<>();
        int dep_idx = -1;
        for (Annotation a : self.getAnnotations()) {
            if (a.annotationType() == Deprecated.class && dep_idx == -1) {
                dep_idx = annotations.size();
                annotations.add(DEFAULT_DEP);
            }
            if (a.annotationType() == J_L_Deprecated.class && dep_idx != -1) {
                annotations.remove(dep_idx);
            }
            annotations.add(a);
        }
        return annotations.toArray(new Annotation[0]);
    }

    @Stub
    public static Annotation[] getDeclaredAnnotations(AnnotatedElement self) {
        List<Annotation> annotations = new ArrayList<>();
        int dep_idx = -1;
        for (Annotation a : self.getDeclaredAnnotations()) {
            if (a.annotationType() == Deprecated.class && dep_idx == -1) {
                dep_idx = annotations.size();
                annotations.add(DEFAULT_DEP);
            }
            if (a.annotationType() == J_L_Deprecated.class && dep_idx != -1) {
                annotations.remove(dep_idx);
            }
            annotations.add(a);
        }
        return annotations.toArray(new Annotation[0]);
    }

}
