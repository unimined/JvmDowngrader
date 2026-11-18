package xyz.wagyourtail.jvmdg.j17.transform;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.j17.PermittedSubClasses;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.BoundaryTransform;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * {@code PermittedSubclasses} attribute → {@link PermittedSubClasses}
 * annotation for pre-17 targets (sealing is unenforced; the set is
 * preserved for reflection stubs).
 */
public class Unseal implements BoundaryTransform {

    @Override
    public ClassNode apply(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra,
                           Function<String, ClassNode> getReadOnly, Set<String> warnings,
                           boolean enableRuntime,
                           IOFunction<Type, Set<MemberNameAndDesc>> memberResolver,
                           IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        unseal(clazz);
        return clazz;
    }

    // TODO: add bytecode to check the seal and throw
    public void unseal(ClassNode node) {
        if (node.permittedSubclasses != null) {
            AnnotationVisitor av = node.visitAnnotation(Type.getType(PermittedSubClasses.class).getDescriptor(), true);
            AnnotationVisitor values = av.visitArray("value");
            for (String s : node.permittedSubclasses) {
                values.visit(null, Type.getObjectType(s));
            }
            values.visitEnd();
            av.visitEnd();

            node.permittedSubclasses = null;
        }
    }

}
