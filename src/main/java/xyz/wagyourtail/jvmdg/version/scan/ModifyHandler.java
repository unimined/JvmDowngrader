package xyz.wagyourtail.jvmdg.version.scan;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.ModifyUtils;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * {@code @Modify} method → an insn-level rewrite handler in the modify
 * registry. Only the {@link Method} is stored — the annotation's {@code ref}
 * is fully consumed at registration.
 */
public class ModifyHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return Modify.class;
    }

    @Override
    public void onMethod(Method method, Annotation a, ScanContext ctx) throws IOException {
        Modify modify = (Modify) a;
        FullyQualifiedMemberNameAndDesc target = resolveModifyTarget(method, modify.ref());
        Type owner = target.getOwner();
        MemberNameAndDesc member = target.toMemberNameAndDesc();
        // ensure method parameters are valid
        try {
            ModifyUtils.matchModifyParams(method);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Class " + ctx.clazz.getName() + ", " + e.getMessage(), e);
        }
        ctx.stubMapper(owner).addModify(member, method);
    }

    /**
     * Resolves the {@code @Modify} target: the ref's owner is required, a
     * missing {@code member} produces a class-level ref (the wildcard
     * {@code @Modify} that intercepts every member insn on the owner).
     */
    public static FullyQualifiedMemberNameAndDesc resolveModifyTarget(Member member, Ref ref) {
        if (member instanceof Method) {
            Type owner;
            String name;
            Type desc;
            if (ref.value().isEmpty()) {
                throw new IllegalArgumentException("ref must have a value");
            } else {
                if (ref.value().startsWith("L") && ref.value().endsWith(";")) {
                    owner = Type.getType(ref.value());
                } else {
                    owner = Type.getObjectType(ref.value());
                }
            }
            if (ref.member().isEmpty()) {
                return new FullyQualifiedMemberNameAndDesc(owner, null, null);
            } else {
                name = ref.member();
            }
            if (ref.desc().isEmpty()) {
                throw new IllegalArgumentException("ref must have a desc");
            } else {
                desc = Type.getMethodType(ref.desc());
            }
            return new FullyQualifiedMemberNameAndDesc(owner, name, desc);
        } else {
            throw new IllegalArgumentException("member must be a method");
        }
    }
}
