package xyz.wagyourtail.jvmdg.version.scan;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.version.InitFactory;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.CtorFactory;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * {@code @InitFactory} method → a ctor-to-factory entry in the stub registry:
 * verified {@code NEW}/{@code DUP} construction sites become
 * {@code INVOKESTATIC} of the annotated method. The transform proves the
 * pairing per call site and warns + leaves shapes it can't verify.
 */
public class InitFactoryHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return InitFactory.class;
    }

    @Override
    public void onMethod(Method method, Annotation a, ScanContext ctx) throws IOException {
        InitFactory factory = (InitFactory) a;
        FullyQualifiedMemberNameAndDesc target = resolveFactoryTarget(method, factory.ref());
        if (factory.downgradeVersion()) {
            target = StubHandler.applyDowngradeVersion(method, target);
        }
        MemberNameAndDesc member = target.toMemberNameAndDesc();
        ctx.stubMapper(target.getOwner()).addCtor(member,
            new CtorFactory(method, factory.downgradeVersion(),
                xyz.wagyourtail.jvmdg.version.StubCondition.of(
                    factory.requiresRuntime(), factory.condition(), "@InitFactory " + method)));
    }

    /**
     * Resolves an {@code @InitFactory} target: {@code ref.member()} must be
     * {@code "<init>"}, the owner defaults to the method's (constructed-object)
     * return type when {@code ref.value} is empty, and the method must be
     * static returning an object type. Validated here so malformed factories
     * fail at registration rather than per-callsite at transform time.
     */
    public static FullyQualifiedMemberNameAndDesc resolveFactoryTarget(Method method, Ref ref) {
        if (!"<init>".equals(ref.member())) {
            throw new IllegalArgumentException("@InitFactory requires ref(member=\"<init>\") — " +
                "other members are plain @Stub targets");
        }
        FullyQualifiedMemberNameAndDesc target = StubHandler.resolveStubTarget(method, ref);
        Type ret = Type.getReturnType(method);
        if (!Modifier.isStatic(method.getModifiers()) || ret.getSort() != Type.OBJECT) {
            throw new IllegalArgumentException(target.getOwner().getDescriptor() + "<init>;" +
                target.getDesc().getDescriptor() + " ctor factory stubs must be static and " +
                "return the constructed object");
        }
        return target;
    }
}
