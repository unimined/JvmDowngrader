package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.asm.AnnotationUtils;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.ReflectionReferences;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.util.List;

/**
 * {@code @ReflectionReferences} scanner: each declared {@code @Ref} is
 * recorded as a member the enclosing member uses — the members reflection
 * reaches that static analysis can't see, so shading keeps them.
 */
public class ReflectionReferencesScanner implements MemberScanner {

    private static final String DESC = Type.getDescriptor(ReflectionReferences.class);

    @Override
    public void scanClass(ClassNode node, MemberSink sink) {
        scan(node.invisibleAnnotations, sink);
    }

    @Override
    public void scanField(FieldNode field, MemberNameAndDesc member, MemberSink sink) {
        scan(field.invisibleAnnotations, sink);
    }

    @Override
    public void scanMethod(MethodNode method, MemberNameAndDesc member, MemberSink sink) {
        scan(method.invisibleAnnotations, sink);
    }

    private void scan(List<AnnotationNode> annotations, MemberSink sink) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(DESC)) {
                try {
                    ReflectionReferences refs = AnnotationUtils.createAnnotation(annotation);
                    for (Ref ref : refs.value()) {
                        sink.requiresMember(FullyQualifiedMemberNameAndDesc.of(ref));
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
