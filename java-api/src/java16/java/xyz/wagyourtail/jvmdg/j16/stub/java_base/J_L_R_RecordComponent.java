package xyz.wagyourtail.jvmdg.j16.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.regex.Pattern;

@Stub(ref = @Ref("Ljava/lang/reflect/RecordComponent;"))
public class J_L_R_RecordComponent {

    private final Class<?> declaring;
    private final Field field;
    private final Method accessor;

    public J_L_R_RecordComponent(Class<?> declaring, Field field) {
        this.declaring = declaring;

        this.field = field;
        for (Method m : declaring.getDeclaredMethods()) {
            if (m.getName().equals(field.getName()) && m.getReturnType() == field.getType()) {
                this.accessor = m;
                return;
            }
        }
        throw new RuntimeException("no accessor found for " + field);
    }

    public String getGenericSignature() {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            return null;
        }
        ParameterizedType pt = (ParameterizedType) genericType;
        return getGenericSignature(pt);
    }

    private String getGenericSignature(ParameterizedType pt) {
        StringBuilder sb = new StringBuilder("L");
        sb.append(pt.getRawType().getTypeName().replace('.', '/'));
        sb.append("<");
        for (Type t : pt.getActualTypeArguments()) {
            if (t instanceof ParameterizedType) {
                sb.append(getGenericSignature((ParameterizedType) t));
            } else {
                sb.append("L").append(t.getTypeName().replace('.', '/')).append(";");
            }
        }
        sb.append(">;");
        return sb.toString();
    }

    public Type getGenericType() {
        return field.getGenericType();
    }

    public AnnotatedType getAnnotatedType() {
        return field.getAnnotatedType();
    }

    public Method getAccessor() {
        return accessor;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return field.getAnnotation(annotationClass);
    }

    public Annotation[] getAnnotations() {
        return field.getAnnotations();
    }

    public Annotation[] getDeclaredAnnotations() {
        return field.getDeclaredAnnotations();
    }

    public String toString() {
        return getType().getTypeName() + " " + getName();
    }

    public String getName() {
        return field.getName();
    }

    public Class<?> getType() {
        return field.getType();
    }

    public Class<?> getDeclaringRecord() {
        return declaring;
    }


}
