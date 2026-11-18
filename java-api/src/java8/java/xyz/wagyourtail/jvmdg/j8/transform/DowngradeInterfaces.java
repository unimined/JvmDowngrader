package xyz.wagyourtail.jvmdg.j8.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.BoundaryTransform;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.*;

/** interface default-method materialization for pre-8 targets: moves
 * default bodies into {@code <I>$jvmdg$StaticDefaults} companions and
 * re-points invokeinterface/invokespecial sites at them */
public class DowngradeInterfaces implements BoundaryTransform {

    @Override
    public ClassNode apply(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra,
                           Function<String, ClassNode> getReadOnly, Set<String> warnings,
                           boolean enableRuntime,
                           IOFunction<Type, Set<MemberNameAndDesc>> memberResolver,
                           IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        List<ClassNode> classes = new ArrayList<>(extra);
        classes.add(clazz);
        for (ClassNode cls : classes) {
            try {
                downgradeInterfaces(provider, cls, extra, getReadOnly, warnings);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return clazz;
    }

    public void downgradeInterfaces(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly, Set<String> warnings) throws IOException {
        if ((clazz.access & Opcodes.ACC_INTERFACE) != 0) {
            downgradeInterfaceMethods(provider, clazz, extra, getReadOnly, warnings);
        } else {
            downgradeInterfaceAccesses(provider, clazz, extra, getReadOnly, warnings);
        }
    }

    private void downgradeInterfaceMethods(final VersionProvider provider, final ClassNode clazz, Set<ClassNode> extra, final Function<String, ClassNode> getReadOnly, Set<String> warnings) throws IOException {
        ClassNode interfaceStaticDefaults = new ClassNode();
        boolean removed = false;
        Iterator<MethodNode> mnodes = clazz.methods.iterator();
        Set<MethodNode> toAdd = new HashSet<>();
        while (mnodes.hasNext()) {
            MethodNode mnode = mnodes.next();
            if ((mnode.access & Opcodes.ACC_ABSTRACT) == 0) {
                mnodes.remove();
                removed = true;
                if ((mnode.access & Opcodes.ACC_STATIC) == 0) {
                    // public instance method
                    if ((mnode.access & Opcodes.ACC_PUBLIC) != 0) {
                        // write back an abstract version
                        toAdd.add(new MethodNode(
                            Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC | (provider.downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC),
                            mnode.name,
                            mnode.desc,
                            mnode.signature,
                            mnode.exceptions.toArray(new String[0])
                        ));
                    }
                    // move to StaticDefault
                    Type[] args = Type.getArgumentTypes(mnode.desc);
                    Type[] newArgs = new Type[args.length + 1];
                    newArgs[0] = Type.getObjectType(clazz.name);
                    System.arraycopy(args, 0, newArgs, 1, args.length);
                    mnode.desc = Type.getMethodDescriptor(Type.getReturnType(mnode.desc), newArgs);
                    mnode.access |= Opcodes.ACC_STATIC;

                }
                interfaceStaticDefaults.methods.add(mnode);
            }
        }
        clazz.methods.addAll(toAdd);
        if (removed) {
            interfaceStaticDefaults.visit(
                Opcodes.V1_7,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                clazz.name + "$jvmdg$StaticDefaults",
                null,
                "java/lang/Object",
                new String[0]
            );
            clazz.visitInnerClass(
                clazz.name + "$jvmdg$StaticDefaults",
                clazz.name,
                "jvmdg$StaticDefaults",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
            );
            interfaceStaticDefaults.visitOuterClass(clazz.name, null, null);
            extra.add(interfaceStaticDefaults);
            downgradeInterfaceAccesses(provider, interfaceStaticDefaults, extra, new Function<String, ClassNode>() {
                @Override
                public ClassNode apply(String s) {
                    return s.equals(clazz.name) ? clazz : getReadOnly.apply(s);
                }
            }, warnings);
        }
    }

    private void downgradeInterfaceAccesses(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly, Set<String> warnings) throws IOException {
        Map<MemberNameAndDesc, Type> members = new HashMap<>();
        if (!clazz.name.endsWith("$jvmdg$StaticDefaults")) {
            ClassMapping stubMapper = provider.getStubMapper(Type.getObjectType(clazz.name), (clazz.access & Opcodes.ACC_INTERFACE) != 0, warnings);
            for (Map.Entry<MemberNameAndDesc, Pair<Boolean, Type>> member : stubMapper.getMembers().entrySet()) {
                if (member.getValue().getFirst() && member.getKey().isMethod()) {
                    members.put(member.getKey(), member.getValue().getSecond());
                }
            }
        }
        for (MethodNode mnode : clazz.methods) {
            for (AbstractInsnNode insn : mnode.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    if (min.itf) {
                        if (min.owner.startsWith("java/") ||
                            min.owner.startsWith("sun/") ||
                            min.owner.startsWith("jdk/") ||
                            min.owner.startsWith("com/sun/")
                        ) {
                            if (min.getOpcode() == Opcodes.INVOKESTATIC || min.getOpcode() == Opcodes.INVOKESPECIAL) {
                                warnings.add("Found java interface missing stub: " + FullyQualifiedMemberNameAndDesc.of(min));
                            }
                            continue;
                        }
                        if (min.getOpcode() == Opcodes.INVOKESTATIC) {
                            min.owner = min.owner + "$jvmdg$StaticDefaults";
                            min.itf = false;
                        } else if (min.getOpcode() == Opcodes.INVOKESPECIAL) {
                            // super calls, and private methods
                            Type[] args = Type.getArgumentTypes(min.desc);
                            Type[] newArgs = new Type[args.length + 1];
                            newArgs[0] = Type.getObjectType(min.owner);
                            System.arraycopy(args, 0, newArgs, 1, args.length);
                            min.owner = min.owner + "$jvmdg$StaticDefaults";
                            min.desc = Type.getMethodDescriptor(Type.getReturnType(min.desc), newArgs);
                            min.itf = false;
                            min.setOpcode(Opcodes.INVOKESTATIC);
                        }
                    }
                } else if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                    for (int i = 0; i < indy.bsmArgs.length; i++) {
                        Object arg = indy.bsmArgs[i];
                        if (arg instanceof Handle) {
                            Handle handle = (Handle) arg;
                            if (handle.isInterface()) {
                                if (handle.getOwner().startsWith("java/") ||
                                    handle.getOwner().startsWith("sun/") ||
                                    handle.getOwner().startsWith("jdk/") ||
                                    handle.getOwner().startsWith("com/sun/")) {
                                    if (handle.getTag() == Opcodes.H_INVOKESTATIC || handle.getTag() == Opcodes.H_INVOKESPECIAL) {
                                        warnings.add("Found java interface missing stub: " + FullyQualifiedMemberNameAndDesc.of(handle));
                                    }
                                    continue;
                                }
                                if (handle.getTag() == Opcodes.H_INVOKESTATIC) {
                                    handle = new Handle(
                                        Opcodes.H_INVOKESTATIC,
                                        handle.getOwner() + "$jvmdg$StaticDefaults",
                                        handle.getName(),
                                        handle.getDesc(),
                                        false
                                    );
                                    indy.bsmArgs[i] = handle;
                                } else if (handle.getTag() == Opcodes.H_INVOKESPECIAL) {
                                    Type[] args = Type.getArgumentTypes(handle.getDesc());
                                    Type[] newArgs = new Type[args.length + 1];
                                    newArgs[0] = Type.getObjectType(handle.getOwner());
                                    System.arraycopy(args, 0, newArgs, 1, args.length);
                                    handle = new Handle(
                                        Opcodes.H_INVOKESTATIC,
                                        handle.getOwner() + "$jvmdg$StaticDefaults",
                                        handle.getName(),
                                        Type.getMethodDescriptor(Type.getReturnType(handle.getDesc()), newArgs),
                                        false
                                    );
                                    indy.bsmArgs[i] = handle;
                                }
                            }
                        }
                    }
                }
            }
            members.remove(MemberNameAndDesc.fromNode(mnode));
        }

        // insert missing defaults.
        for (Map.Entry<MemberNameAndDesc, Type> member : members.entrySet()) {
            String internalName = member.getValue().getInternalName();
            if (internalName.startsWith("java/") ||
                internalName.startsWith("sun/") ||
                internalName.startsWith("jdk/") ||
                internalName.startsWith("com/sun/")) {
                warnings.add("[Java8 Interface Downgrader] Found java interface default missing implementation: " + member.getKey().toFullyQualified(member.getValue()));
                continue;
            }
            // create method redirecting to static default
            MethodVisitor mn = clazz.visitMethod(
                Opcodes.ACC_PUBLIC | (provider.downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC),
                member.getKey().getName(),
                member.getKey().getDesc().getDescriptor(),
                null,
                null
            );
            mn.visitCode();
            mn.visitVarInsn(Opcodes.ALOAD, 0);
            Type[] args = Type.getArgumentTypes(member.getKey().getDesc().getDescriptor());
            int j = 1;
            for (Type arg : args) {
                mn.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), j);
                j += arg.getSize();
            }
            Type[] newArgs = new Type[args.length + 1];
            newArgs[0] = member.getValue();
            System.arraycopy(args, 0, newArgs, 1, args.length);

            mn.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                member.getValue().getInternalName() + "$jvmdg$StaticDefaults",
                member.getKey().getName(),
                Type.getMethodDescriptor(member.getKey().getDesc().getReturnType(), newArgs),
                false
            );
            mn.visitInsn(Type.getReturnType(member.getKey().getDesc().getDescriptor()).getOpcode(Opcodes.IRETURN));
            mn.visitMaxs(0, 0);
            mn.visitEnd();
        }
    }

}
