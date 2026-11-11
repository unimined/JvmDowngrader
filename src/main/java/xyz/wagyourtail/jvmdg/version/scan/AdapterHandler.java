package xyz.wagyourtail.jvmdg.version.scan;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.annotation.Annotation;

/**
 * Class-level {@code @Adapter} → the {@code classStubs} type remap consumed by
 * {@code TypeRemap}/{@code StructuralRemapTransform}.
 */
public class AdapterHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return Adapter.class;
    }

    @Override
    public void onClass(Class<?> clazz, Annotation a, ScanContext ctx) {
        Adapter stub = (Adapter) a;
        if (stub.value().isEmpty()) {
            throw new IllegalArgumentException("Class " + clazz.getName() + ", @Adapter must have a ref");
        }

        Type value;
        if (stub.value().startsWith("L") && stub.value().endsWith(";")) {
            value = Type.getType(stub.value());
        } else {
            value = Type.getObjectType(stub.value());
        }
        Type target;
        if (stub.target().isEmpty()) {
            target = Type.getType(clazz);
        } else {
            if (stub.target().startsWith("L") && stub.target().endsWith(";")) {
                target = Type.getType(stub.target());
            } else {
                target = Type.getObjectType(stub.target());
            }
        }
        ctx.vp.classStubs.put(value, new Pair<>(target, new Pair<Class<?>, Adapter>(clazz, stub)));
    }
}
