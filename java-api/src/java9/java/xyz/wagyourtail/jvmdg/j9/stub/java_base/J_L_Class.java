package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.j9.intl.module.ModuleConstantHelper;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class J_L_Class {
    @Stub(ref = @Ref("Ljava/lang/Class;"))
    public static Class<?> forName(J_L_Module module, String name) throws ClassNotFoundException {
        return Class.forName(name, true, module.getClassLoader());
    }

    @Stub
    public static J_L_Module getModule(Class<?> clazz) {
        J_L_Module module = ModuleConstantHelper.bootModuleFromClass(clazz);
        return module != null ? module :
            J_L_ClassLoader.getUnnamedModule(clazz.getClassLoader());
    }

    @Stub
    public static String getPackageName(Class<?> clazz) {
        String name = clazz.getName();
        int lastDot = name.lastIndexOf('.');
        if (clazz.isPrimitive() || clazz == void.class) {
            return "java.lang";
        }
        if (clazz.isArray()) {
            return getPackageName(clazz.getComponentType());
        }
        if (lastDot == -1) {
            return "";
        }
        return name.substring(0, lastDot);
    }

    J_L_Deprecated DEFAULT_DEP = new J_L_Deprecated() {
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
    @JEP(277)
    public Annotation getAnnotation(Class<?> self, Class<? extends Annotation> annotationClass) {
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
    @JEP(277)
    public Annotation getDeclaredAnnotation(Class<?> self, Class<? extends Annotation> annotationClass) {
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
    @JEP(277)
    public boolean isAnnotationPresent(Class<?> self, Class<? extends Annotation> annotationClass) {
        return self.isAnnotationPresent(annotationClass) || (annotationClass == J_L_Deprecated.class && self.isAnnotationPresent(Deprecated.class));
    }

    @Stub
    @JEP(277)
    public Annotation[] getAnnotationsByType(Class<?> self, Class<? extends Annotation> annotationClass) {
        if (annotationClass == J_L_Deprecated.class) {
            Annotation[] a = self.getAnnotationsByType(J_L_Deprecated.class);
            if (a.length > 0) {
                return a;
            }
            Annotation[] b = self.getAnnotationsByType(Deprecated.class);
            if (b.length > 0) {
                return new Annotation[] { DEFAULT_DEP };
            }
        }
        return self.getAnnotationsByType(annotationClass);
    }

    @Stub
    @JEP(277)
    public Annotation[] getDeclaredAnnotationsByType(Class<?> self, Class<? extends Annotation> annotationClass) {
        if (annotationClass == J_L_Deprecated.class) {
            Annotation[] a = self.getDeclaredAnnotationsByType(J_L_Deprecated.class);
            if (a.length > 0) {
                return a;
            }
            Annotation[] b = self.getDeclaredAnnotationsByType(Deprecated.class);
            if (b.length > 0) {
                return new Annotation[] { DEFAULT_DEP };
            }
        }
        return self.getDeclaredAnnotationsByType(annotationClass);
    }

    @Stub
    @JEP(277)
    public Annotation[] getAnnotations(Class<?> self) {
        List<Annotation> annotations = new ArrayList<>();
        for (Annotation a : self.getAnnotations()) {
            if (a.annotationType() == Deprecated.class) {
                annotations.add(DEFAULT_DEP);
            } else {
                annotations.add(a);
            }
        }
        return annotations.toArray(new Annotation[0]);
    }

    @Stub
    @JEP(277)
    public Annotation[] getDeclaredAnnotations(Class<?> self) {
        List<Annotation> annotations = new ArrayList<>();
        for (Annotation a : self.getDeclaredAnnotations()) {
            if (a.annotationType() == Deprecated.class) {
                annotations.add(DEFAULT_DEP);
            } else {
                annotations.add(a);
            }
        }
        return annotations.toArray(new Annotation[0]);
    }

}
