package xyz.wagyourtail.jvmdg.version.scan;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

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
        ClassStubEntry entry = new ClassStubEntry(target, clazz, stub.keepInterface(), stub.mode(),
            new LinkedHashSet<>(Arrays.asList(stub.implementedBy())),
            stub.wrap().isEmpty() ? null : FullyQualifiedMemberNameAndDesc.of(stub.wrap()),
            new LinkedHashSet<>(Arrays.asList(stub.signaturePolymorphic())));
        if (!ctx.vp.shouldSkip(entry)) {
            ctx.vp.classStubs.put(value, entry);
            List<String[]> impls = new ArrayList<>();
            for (Class<?> impl : stub.impls()) {
                impls.add(new String[]{memberType(clazz, impl), Type.getInternalName(impl)});
            }
            if (!impls.isEmpty()) {
                ctx.vp.adapterImpls.put(value.getInternalName(), impls);
            }
        }
    }

    /**
     * The member type an {@code impls} entry wraps — the parameter of its
     * single-arg constructor. Validates the impl actually implements the
     * adapter so wrong-class typos fail at registration.
     */
    private static String memberType(Class<?> adapter, Class<?> impl) {
        if (!adapter.isAssignableFrom(impl)) {
            throw new IllegalArgumentException(impl.getName() + " does not implement adapter " + adapter.getName());
        }
        Constructor<?> ctor = null;
        for (Constructor<?> c : impl.getDeclaredConstructors()) {
            if (c.getParameterCount() == 1) {
                if (ctor != null) {
                    throw new IllegalArgumentException(impl.getName() + " has multiple single-argument constructors — @Adapter impls need exactly one (the member type)");
                }
                ctor = c;
            }
        }
        if (ctor == null) {
            throw new IllegalArgumentException(impl.getName() + " has no single-argument constructor — @Adapter impls wrap one member type");
        }
        return Type.getInternalName(ctor.getParameterTypes()[0]);
    }
}
