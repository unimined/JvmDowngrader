package xyz.wagyourtail.jvmdg.j25.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.BoundaryTransform;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.*;

/** renames {@code void main(...)} instance methods to
 * {@code jvmdg$instanceMain} so {@link AddMissingMains} can bridge to
 * them without colliding with a real static main */
public class RenameInstanceMains implements BoundaryTransform {

    @Override
    public ClassNode apply(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra,
                           Function<String, ClassNode> getReadOnly, Set<String> warnings,
                           boolean enableRuntime,
                           IOFunction<Type, Set<MemberNameAndDesc>> memberResolver,
                           IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        if (clazz.name.equals("module-info")) {
            return clazz;
        }
        renameInstanceMains(clazz);
        return clazz;
    }

    @Override
    public int order() { return 1; }

    public void renameInstanceMains(ClassNode clazz) {
        for (MethodNode method : clazz.methods) {
            if (method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V") && (method.access & Opcodes.ACC_STATIC) == 0) {
                method.name = "jvmdg$instanceMain";
            }
            for (AbstractInsnNode insn : method.instructions) {
                if (insn.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    if (min.getOpcode() != Opcodes.INVOKESTATIC && min.name.equals("main") && min.desc.equals("([Ljava/lang/String;)V")) {
                        min.name = "jvmdg$instanceMain";
                    }
                }
            }
        }
    }


}
