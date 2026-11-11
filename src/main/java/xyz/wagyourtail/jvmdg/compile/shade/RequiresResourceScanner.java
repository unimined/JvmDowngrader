package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.asm.AnnotationUtils;
import xyz.wagyourtail.jvmdg.version.RequiresResource;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.util.List;

/**
 * {@code @RequiresResource} scanner: the annotated element's resource paths
 * attach to it so the api closure carries them — collected into the output
 * jar only when the element itself is required.
 * <p>
 * At class level ({@code @Target} TYPE) the paths attach to the class ref,
 * so they are pulled whenever the class is required at all.
 */
public class RequiresResourceScanner implements MemberScanner {

    private static final String DESC = Type.getDescriptor(RequiresResource.class);

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
                    RequiresResource resource = AnnotationUtils.createAnnotation(annotation);
                    sink.requiresResource(resource.value());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
