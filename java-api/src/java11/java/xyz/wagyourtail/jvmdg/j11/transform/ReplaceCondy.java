package xyz.wagyourtail.jvmdg.j11.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.j11.NestHost;
import xyz.wagyourtail.jvmdg.j11.stub.java_base.J_L_I_ConstantBootstraps;
import xyz.wagyourtail.jvmdg.j11.NestMembers;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.BoundaryTransform;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.*;

import org.objectweb.asm.ConstantDynamic;

/** lowers {@code ldc <ConstantDynamic>} to a {@code jvmdg$condy$N} field
 * resolved once in {@code <clinit>} for targets without condy support.
 * Name-sort places it after FixPrivateMethodsInInterfaces — required,
 * since re-wrapping a LambdaMetafactory indy that carries condy bsmArgs
 * swaps its bsm owner and would hide it from the private-interface
 * handle fixup (exotic shape — javac never emits condy in lambda
 * bsmArgs — but the ordering keeps even that safe) */
public class ReplaceCondy implements BoundaryTransform {

    @Override
    public ClassNode apply(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra,
                           Function<String, ClassNode> getReadOnly, Set<String> warnings,
                           boolean enableRuntime,
                           IOFunction<Type, Set<MemberNameAndDesc>> memberResolver,
                           IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        if (clazz.name.equals("module-info")) {
            return clazz;
        }
        replaceCondy(clazz);
        return clazz;
    }

    public void replaceCondy(ClassNode clazz) {
        for (MethodNode method : clazz.methods) {
            replaceCondy(method);
        }
    }

    public void replaceCondy(MethodNode method) {
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode insn = method.instructions.get(i);
            if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof ConstantDynamic) {
                    // "upgrade" to indy
                    ConstantDynamic condy = (ConstantDynamic) ldc.cst;
                    Object[] bsmArgs = new Object[condy.getBootstrapMethodArgumentCount() + 1];
                    bsmArgs[0] = condy.getBootstrapMethod();
                    for (int j = 0; j < condy.getBootstrapMethodArgumentCount(); j++) {
                        bsmArgs[j + 1] = condy.getBootstrapMethodArgument(j);
                    }
                    InvokeDynamicInsnNode indy = new InvokeDynamicInsnNode(
                        condy.getName(),
                        "()" + condy.getDescriptor(),
                        new Handle(
                            Opcodes.H_INVOKESTATIC,
                            Type.getInternalName(J_L_I_ConstantBootstraps.class),
                            "ldcCondyToIndy",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
                            false
                        ),
                        bsmArgs
                    );
                    method.instructions.set(insn, indy);
                    insn = indy;
                }
            }
            if (insn instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                StringBuilder condyArgs = new StringBuilder();
                for (int j = 0; j < indy.bsmArgs.length; j++) {
                    if (indy.bsmArgs[j] instanceof ConstantDynamic) {
                        condyArgs.append((char) j);
                    }
                }
                if (condyArgs.length() > 0) {
                    // replace with nest indy
                    List<Object> bsmArgs = new ArrayList<>();
                    bsmArgs.add(indy.bsm);
                    bsmArgs.add(condyArgs.toString());
                    for (Object bsmArg : indy.bsmArgs) {
                        if (bsmArg instanceof ConstantDynamic) {
                            ConstantDynamic condy = (ConstantDynamic) bsmArg;
                            insertCondyArgs(condy, bsmArgs);
                        } else {
                            bsmArgs.add(bsmArg);
                        }
                    }
                    indy = new InvokeDynamicInsnNode(
                        indy.name,
                        indy.desc,
                        new Handle(
                            Opcodes.H_INVOKESTATIC,
                            Type.getInternalName(J_L_I_ConstantBootstraps.class),
                            "nestedCondyInIndy",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
                            false
                        ),
                        bsmArgs.toArray()
                    );
                    method.instructions.set(insn, indy);
                }
            }
        }
    }

    public void insertCondyArgs(ConstantDynamic condy, List<Object> bsmArgs) {
        bsmArgs.add(condy.getBootstrapMethod());
        bsmArgs.add(condy.getName());
        bsmArgs.add(Type.getType(condy.getDescriptor()));
        bsmArgs.add(condy.getBootstrapMethodArgumentCount());
        StringBuilder condyArgs = new StringBuilder();
        for (int j = 0; j < condy.getBootstrapMethodArgumentCount(); j++) {
            if (condy.getBootstrapMethodArgument(j) instanceof ConstantDynamic) {
                condyArgs.append((char) j);
            }
        }
        bsmArgs.add(condyArgs.toString());
        for (int j = 0; j < condy.getBootstrapMethodArgumentCount(); j++) {
            Object o = condy.getBootstrapMethodArgument(j);
            if (o instanceof ConstantDynamic) {
                insertCondyArgs((ConstantDynamic) o, bsmArgs);
            } else {
                bsmArgs.add(o);
            }
        }
    }


}
