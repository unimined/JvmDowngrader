package xyz.wagyourtail.jvmdg.version.scan;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.version.DefaultImpl;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * {@code @DefaultImpl} method → a member redirect whose callsites are left in
 * place (except {@code INVOKESPECIAL} super-calls, which invokestatic the
 * stub) while the body is inserted into implementors lacking the method.
 */
public class DefaultImplHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return DefaultImpl.class;
    }

    @Override
    public void onMethod(Method method, Annotation a, ScanContext ctx) throws IOException {
        DefaultImpl impl = (DefaultImpl) a;
        if ("<init>".equals(impl.ref().member())) {
            throw new IllegalArgumentException("@DefaultImpl does not apply to <init> — " +
                "use a ctor retarget @Stub or @Modify");
        }
        FullyQualifiedMemberNameAndDesc target = StubHandler.resolveStubTarget(method, impl.ref());
        if (impl.ref().desc().isEmpty() && !impl.ref().value().isEmpty()) {
            // the handler's first param is always the receiver (same convention
            // as the implicit-owner form, which pops it during owner
            // derivation). With an explicit owner the receiver survives into
            // the auto-derived desc — strip it so the registered member is the
            // callsite/insertion signature, not the handler's. An explicit
            // ref.desc() is taken verbatim (the author asserts the shape).
            Type d = target.getDesc();
            Type[] args = d.getArgumentTypes();
            if (args.length == 0) {
                throw new IllegalArgumentException(method + " — @DefaultImpl needs a receiver parameter");
            }
            target = new FullyQualifiedMemberNameAndDesc(target.getOwner(), target.getName(),
                Type.getMethodType(d.getReturnType(), Arrays.copyOfRange(args, 1, args.length)));
        }
        MemberNameAndDesc member = target.toMemberNameAndDesc();
        ctx.stubMapper(target.getOwner()).addStub(member, StubEntry.defaultImpl(method));
    }
}
