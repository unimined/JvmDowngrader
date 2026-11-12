package xyz.wagyourtail.jvmdg.version.scan;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Coerce;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code @Stub} method → a member redirect in the stub registry (method
 * stubs, field get/put stubs, and the two declarative {@code <init>} forms).
 */
public class StubHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return Stub.class;
    }

    @Override
    public void onMethod(Method method, Annotation a, ScanContext ctx) throws IOException {
        Stub stub = (Stub) a;
        FullyQualifiedMemberNameAndDesc target = resolveStubTarget(method, stub.ref(), stub);
        Type owner = target.getOwner();
        MemberNameAndDesc member = target.toMemberNameAndDesc();
        if (stub.downgradeVersion()) {
            member = applyDowngradeVersion(method, target).toMemberNameAndDesc();
        }
        ctx.stubMapper(owner).addStub(member, new StubEntry(method, stub));
        if (stub.kind() != xyz.wagyourtail.jvmdg.version.StubKind.AUTO) {
            ctx.stubMapper(owner).putKind(target, stub.kind());
        }
    }

    /**
     * Resolves the {@code @Stub} target: params/return carrying
     * {@code @Coerce} or an {@code @Adapter}-annotated class register as the
     * real JDK type (never the fake adapter type — pre-transform call sites
     * carry real types), the ref overrides owner/member/desc, and
     * {@code <init>} targets are allowed in two declarative forms — a stub
     * with {@code init} retargets the callsite to another ctor of the same
     * class and tail-calls the stub with {@code (Self, droppedArgs…)}; a stub
     * without {@code init} is a ctor-to-factory redirect ({@code new X(args)}
     * becomes {@code X stub(args)}), validated per-callsite by the transform
     * (it warns and leaves shapes it can't prove, e.g. delegation).
     */
    public static FullyQualifiedMemberNameAndDesc resolveStubTarget(Method method, Ref ref) {
        return resolveStubTarget(method, ref, null);
    }

    /**
     * Strips the trailing {@code int} that {@code downgradeVersion} appends to
     * the handler signature — the transform pushes the input version at the
     * call site, so the registered key must match the pre-transform signature.
     */
    public static FullyQualifiedMemberNameAndDesc applyDowngradeVersion(Method method, FullyQualifiedMemberNameAndDesc target) {
        Type d = target.getDesc();
        Type[] args = d.getArgumentTypes();
        if (args.length == 0 || !args[args.length - 1].equals(Type.INT_TYPE)) {
            throw new IllegalArgumentException(method + " downgradeVersion stub must end with an int parameter");
        }
        args = Arrays.copyOf(args, args.length - 1);
        return new FullyQualifiedMemberNameAndDesc(target.getOwner(), target.getName(),
            Type.getMethodType(d.getReturnType(), args));
    }

    /**
     * The method's param types with {@code @Coerce}/{@code @Adapter}-typed
     * params resolved to the real JDK types — the signature a handler's
     * params bind against, and what member lookups match.
     */
    public static Type[] coercedParamTypes(Method method) {
        List<Type> params = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(method)));

        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < params.size(); i++) {
            Annotation[] param = annotations[i];
            for (Annotation ann : param) {
                if (ann instanceof Coerce) {
                    Coerce c = (Coerce) ann;
                    params.set(i, Type.getType(c.value()));
                }
            }
        }

        Class<?>[] paramClasses = method.getParameterTypes();
        for (int i = 0; i < params.size(); i++) {
            // walk array types to the component: annotations don't propagate
            // to the synthesized array class, and reflectively the signature
            // carries the adapter (fake) type after api downgrading
            int dims = 0;
            Class<?> p = paramClasses[i];
            while (p.isArray()) {
                p = p.getComponentType();
                dims++;
            }
            Adapter a = p.getAnnotation(Adapter.class);
            if (a != null) {
                String aRef = a.value();
                Type t;
                if (aRef.startsWith("L") && aRef.endsWith(";")) {
                    t = Type.getType(aRef);
                } else {
                    t = Type.getObjectType(aRef);
                }
                for (int d = 0; d < dims; d++) {
                    t = Type.getType("[" + t.getDescriptor());
                }

                // always record the real (JDK) type, not just when the adapter
                // has already been registered — the member lookup is matched
                // against pre-transform call sites, which carry real types, and
                // registration order is otherwise load-order dependent
                params.set(i, t);
            }
        }
        return params.toArray(new Type[0]);
    }

    /** the method's return type with an {@code @Adapter}-typed class resolved to the real JDK type */
    public static Type coercedReturnType(Method method) {
        Type ret = Type.getReturnType(method);
        // walk arrays to the component, as with params: an adapter
        // (fake) return type must register as the real JDK type so the
        // member desc matches pre-transform call sites
        int dims = 0;
        Class<?> r = method.getReturnType();
        while (r.isArray()) {
            r = r.getComponentType();
            dims++;
        }
        Adapter a = r.getAnnotation(Adapter.class);
        if (a != null) {
            String aRef = a.value();
            Type t;
            if (aRef.startsWith("L") && aRef.endsWith(";")) {
                t = Type.getType(aRef);
            } else {
                t = Type.getObjectType(aRef);
            }
            for (int d = 0; d < dims; d++) {
                t = Type.getType("[" + t.getDescriptor());
            }
            ret = t;
        }
        return ret;
    }

    public static FullyQualifiedMemberNameAndDesc resolveStubTarget(Method method, Ref ref, Stub stub) {
        Type owner;
        String name;
        List<Type> params = new ArrayList<>(Arrays.asList(coercedParamTypes(method)));
        Type ret = coercedReturnType(method);
        // ctor refs always name <init> explicitly (a Java method can't be
        // called that). A <init> @Stub has no meaning of its own any more —
        // construction replacement is @InitFactory, signature-change is
        // @InitRewrite — so the only <init> resolve that survives is the
        // no-stub (factory) form, whose owner derives from the return type
        boolean ctor = "<init>".equals(ref.member());
        if (ctor && stub != null) {
            throw new IllegalArgumentException(
                "<init> @Stub " + method + " — use @InitFactory for construction " +
                "replacement, @InitRewrite for ctor retargets, or @Modify");
        }
        if (ref.value().isEmpty()) {
            if (ctor) {
                owner = ret;
            } else {
                if (params.isEmpty()) {
                    throw new IllegalArgumentException("cannot derive owner for " + method + " — declare ref.value()");
                }
                owner = params.remove(0);
            }
        } else {
            if (ref.value().startsWith("L") && ref.value().endsWith(";")) {
                owner = Type.getType(ref.value());
            } else {
                owner = Type.getObjectType(ref.value());
            }
        }
        if (ref.member().isEmpty()) {
            name = method.getName();
        } else {
            name = ref.member();
        }
        Type desc;
        if (ref.desc().isEmpty()) {
            // <init> descs are always void-returning — the stub's own return
            // (the factory's product) or tail signature isn't the ctor desc
            desc = Type.getMethodType(ctor ? Type.VOID_TYPE : ret, params.toArray(new Type[0]));
        } else {
            desc = Type.getType(ref.desc());
        }
        if (name.equals("<init>")) {
            if (!Type.VOID_TYPE.equals(desc.getReturnType())) {
                throw new IllegalArgumentException(owner.getDescriptor() + "<init>;" + desc +
                    " must have a void descriptor");
            }
        }
        // re-assemble desc
        return new FullyQualifiedMemberNameAndDesc(owner, name, desc);
    }
}
