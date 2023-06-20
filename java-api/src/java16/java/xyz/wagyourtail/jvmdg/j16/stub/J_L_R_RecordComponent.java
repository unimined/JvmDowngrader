package xyz.wagyourtail.jvmdg.j16.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

@Stub(opcVers = Opcodes.V16, ref = @Ref("Ljava/lang/reflect/RecordComponent;"))
public class J_L_R_RecordComponent {

    private final Class<?> declaring;
    private final String name;
    private final String signature;

    private final Field field;
    private final Method accessor;

    public J_L_R_RecordComponent(Class<?> declaring, String descriptor) {
        this.declaring = declaring;

        String[] parts = descriptor.split(" ");
        this.name = parts[0];
        if (parts[1].equals("null")) {
            this.signature = null;
        } else {
            this.signature = parts[1];
        }

        try {
            this.field = declaring.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        try {
            this.accessor = declaring.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public String getGenericSignature() {
        return signature;
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
        return name;
    }

    public Class<?> getType() {
        return field.getType();
    }

    public Class<?> getDeclaringRecord() {
        return declaring;
    }


}
