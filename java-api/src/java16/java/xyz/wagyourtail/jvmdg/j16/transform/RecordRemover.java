package xyz.wagyourtail.jvmdg.j16.transform;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.RecordComponentNode;
import xyz.wagyourtail.jvmdg.j16.RecordComponents;
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
 * {@code ACC_RECORD} + {@code Record} attribute removal for pre-16 targets —
 * the components are re-expressed as the runtime-readable
 * {@link RecordComponents} annotation so reflection stubs still see them.
 */
public class RecordRemover implements BoundaryTransform {

    @Override
    public ClassNode apply(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra,
                           Function<String, ClassNode> getReadOnly, Set<String> warnings,
                           boolean enableRuntime,
                           IOFunction<Type, Set<MemberNameAndDesc>> memberResolver,
                           IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        recordRemover(clazz);
        return clazz;
    }

    public void recordRemover(ClassNode node) {
        if ((node.access & Opcodes.ACC_RECORD) != 0) {
            node.access &= ~Opcodes.ACC_RECORD;
//            StringBuilder value = new StringBuilder();
//            for (int i = 0; i < node.recordComponents.size(); i++) {
//                RecordComponentNode recordComponent = node.recordComponents.get(i);
//                value.append(recordComponent.name).append(' ').append(recordComponent.signature).append(';');
//            }
//            value.deleteCharAt(value.length() - 1);
//            node.recordComponents = null;
//            node.visitField(
//                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
//                "jvmdowngrader$recordComponents",
//                "Ljava/lang/String;",
//                null,
//                value.toString()
//            ).visitEnd();
        }
        if (node.recordComponents != null) {
            AnnotationVisitor av = node.visitAnnotation(Type.getType(RecordComponents.class).getDescriptor(), true);
            AnnotationVisitor values = av.visitArray("value");

            for (RecordComponentNode rcn : node.recordComponents) {
                AnnotationVisitor value = values.visitAnnotation(null, Type.getType(RecordComponents.Value.class).getDescriptor());
                value.visit("name", rcn.name);
                value.visit("type", Type.getType(rcn.descriptor));
                value.visitEnd();
            }
            values.visitEnd();
            av.visitEnd();

            node.recordComponents = null;
        }
    }

}
