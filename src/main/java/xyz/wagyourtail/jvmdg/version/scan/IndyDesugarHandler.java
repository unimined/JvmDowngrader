package xyz.wagyourtail.jvmdg.version.scan;

import xyz.wagyourtail.jvmdg.version.IndyDesugar;
import xyz.wagyourtail.jvmdg.version.ModifyContext;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@code @IndyDesugar} method → an indy-site desugar in the
 * {@code indyDesugars} registry, keyed by the bootstrap member it
 * replaces. Unlike {@link ModifyHandler}, the handler signature is fixed —
 * {@code (ModifyContext)} — and registration never intercepts direct
 * invoke sites on the member.
 */
public class IndyDesugarHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return IndyDesugar.class;
    }

    @Override
    public void onMethod(Method method, Annotation a, ScanContext ctx) throws IOException {
        IndyDesugar desugar = (IndyDesugar) a;
        Class<?>[] params = method.getParameterTypes();
        if (params.length != 1 || params[0] != ModifyContext.class) {
            throw new IllegalArgumentException("Class " + ctx.clazz.getName() +
                ", @IndyDesugar " + method + " must have signature (ModifyContext)");
        }
        FullyQualifiedMemberNameAndDesc target = ModifyHandler.resolveModifyTarget(method, desugar.ref());
        MemberNameAndDesc member = target.toMemberNameAndDesc();
        if (member == null || member.getName() == null) {
            throw new IllegalArgumentException("Class " + ctx.clazz.getName() +
                ", @IndyDesugar " + method + " requires a member in ref — class-level desugars are not supported");
        }
        ctx.stubMapper(target.getOwner()).addIndyDesugar(member, method);
    }
}
