package xyz.wagyourtail.jvmdg.j11.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.j11.NestHost;
import xyz.wagyourtail.jvmdg.j11.NestMembers;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.BoundaryTransform;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.*;

/** rewrites self-targeted private interface method calls
 * ({@code INVOKEINTERFACE} sites + lambda-metafactory handle args) to
 * {@code INVOKESPECIAL} for pre-11 targets */
public class FixPrivateMethodsInInterfaces implements BoundaryTransform {

    @Override
    public ClassNode apply(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra,
                           Function<String, ClassNode> getReadOnly, Set<String> warnings,
                           boolean enableRuntime,
                           IOFunction<Type, Set<MemberNameAndDesc>> memberResolver,
                           IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        if (clazz.name.equals("module-info")) {
            return clazz;
        }
        fixPrivateMethodsInInterfaces(clazz);
        return clazz;
    }

    public void fixPrivateMethodsInInterfaces(ClassNode node) {
        if ((node.access & Opcodes.ACC_INTERFACE) == 0) return;

        List<String> privateMethods = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_PRIVATE) != 0) {
                privateMethods.add(method.name + method.desc);
            }
        }

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    if (min.getOpcode() == Opcodes.INVOKEINTERFACE && min.owner.equals(node.name) && privateMethods.contains(min.name + min.desc)) {
                        min.setOpcode(Opcodes.INVOKESPECIAL);
                    }
                } else if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                    if (indy.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
                        if (indy.bsmArgs[1] instanceof Handle) {
                            Handle lambda = (Handle) indy.bsmArgs[1];
                            if (lambda.getOwner().equals(node.name) &&
                                lambda.getTag() == Opcodes.H_INVOKEINTERFACE &&
                                privateMethods.contains(lambda.getName() + lambda.getDesc())
                            ) {
                                indy.bsmArgs[1] = new Handle(Opcodes.H_INVOKESPECIAL, lambda.getOwner(), lambda.getName(), lambda.getDesc(), lambda.isInterface());
                            }
                        }
                    }
                }
            }
        }
    }


}
