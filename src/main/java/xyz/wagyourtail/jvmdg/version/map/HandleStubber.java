package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Handle/constant-slot rewriting into stub vocabulary — the emission
 * machinery {@link VersionProvider#stubMethod} and indy/condy bootstrap
 * args flow through. Split out of the provider so the registry side
 * (scan/idx state, resolver factories) and the bytecode-emitting side
 * aren't the same file; {@code provider} supplies the registry queries
 * ({@code stubClass}, {@code getStubMapper}) and run flags.
 */
public class HandleStubber {

    private final VersionProvider provider;

    public HandleStubber(VersionProvider provider) {
        this.provider = provider;
    }

    public void stubBSMArgs(ClassNode owner, MethodNode method, Set<ClassNode> extra, Handle bsm, String indyDesc, Object[] bsmArgs, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings) throws IOException {
        // typeSwitch labels keep their source types: the emitted switch
        // routes stubbed labels through RetroTypes by name, so the retro
        // identity must survive to J_L_R_SwitchBootstraps.makeTypeSwitch.
        // Accept the stubbed owner name too — pipeline order decides
        // whether the bsm handle is already remapped when this runs.
        boolean preserveLabels = bsm != null
            && bsm.getName().equals("typeSwitch")
            && bsm.getOwner().endsWith("SwitchBootstraps");
        for (int j = 0; j < bsmArgs.length; j++) {
            Object arg = bsmArgs[j];
            if (arg instanceof Handle) {
                arg = stubHandle(owner, method, extra, bsm, indyDesc, enableRuntime, memberResolver, superTypeResolver, warnings, (Handle) arg);
            } else if (arg instanceof Type) {
                arg = preserveLabels ? arg : provider.stubClass((Type) arg, warnings);
            } else if (arg instanceof ConstantDynamic) {
                arg = stubCondy(owner, method, extra, (ConstantDynamic) arg, enableRuntime, memberResolver, superTypeResolver, warnings);
            }
            bsmArgs[j] = arg;
        }
    }

    public Handle stubHandle(ClassNode owner, MethodNode method, Set<ClassNode> extra, Handle bsm, String indyDesc, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings, Handle handle) throws IOException {
        handle = new Handle(
            handle.getTag(),
            provider.stubClass(Type.getObjectType(handle.getOwner()), warnings).getInternalName(),
            handle.getName(),
            provider.stubClass(Type.getType(handle.getDesc()), warnings).getDescriptor(),
            handle.isInterface()
        );
        Type[] captured = null;
        if (bsm != null && bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
            captured = Type.getMethodType(indyDesc).getArgumentTypes();
        }
        Type hOwner = Type.getObjectType(handle.getOwner());
        if (hOwner.getSort() == Type.ARRAY) {
            return handle;
        }
        Type hDesc = Type.getType(handle.getDesc());
        MemberNameAndDesc member = new MemberNameAndDesc(handle.getName(), hDesc);
        ClassMapping stubMapper = provider.getStubMapper(hOwner, handle.isInterface(), memberResolver, superTypeResolver);
        StubEntry min = stubMapper.getStubFor(member, enableRuntime, handle.getTag(), warnings);
        if (min != null) {
            if (!min.abstractDefault) {
                // the descriptor as if it's an INVOKESTATIC
                Type hStaticDesc;
                switch (handle.getTag()) {
                    case Opcodes.H_GETFIELD:
                        hStaticDesc = Type.getMethodType(Type.getType(hDesc.getDescriptor()), Type.getObjectType(handle.getOwner()));
                        break;
                    case Opcodes.H_GETSTATIC:
                        hStaticDesc = Type.getMethodType("()" + hDesc.getDescriptor());
                        break;
                    case Opcodes.H_PUTFIELD:
                        hStaticDesc = Type.getMethodType(Type.VOID_TYPE, Type.getObjectType(handle.getOwner()), Type.getType(hDesc.getDescriptor()));
                        break;
                    case Opcodes.H_PUTSTATIC:
                        hStaticDesc = Type.getMethodType(Type.VOID_TYPE, Type.getType(hDesc.getDescriptor()));
                        break;
                    case Opcodes.H_INVOKESTATIC:
                        hStaticDesc = hDesc;
                        break;
                    case Opcodes.H_NEWINVOKESPECIAL:
                        warnings.add("@Stub should not be used for NEWINVOKESPECIAL in" + owner.name + ";" + method.name + ";" + method.desc + "; targetting " + handle.getOwner() + ";" + handle.getName() + ";" + handle.getDesc());
                    case Opcodes.H_INVOKEVIRTUAL:
                    case Opcodes.H_INVOKESPECIAL:
                    case Opcodes.H_INVOKEINTERFACE:
                        Type[] params = new Type[hDesc.getArgumentCount() + 1];
                        params[0] = hOwner;
                        System.arraycopy(hDesc.getArgumentTypes(), 0, params, 1, hDesc.getArgumentCount());
                        if (captured != null) {
                            // replace actual with capture, fixes invokeInterface on LambdaMetafactory
                            System.arraycopy(captured, 0, params, 0, captured.length);
                        }
                        hStaticDesc = Type.getMethodType(hDesc.getReturnType(), params);
                        break;
                    default:
                        throw new IllegalStateException("unknown tag: " + handle.getTag());
                }

                MethodRef m = min.handler;
                // virtual-family handles on a dispatchable instance stub get a
                // wrapper carrying the indy dispatch — a methodref's semantics
                // are virtual dispatch, so a user override still wins; special
                // stays on the impl (non-virtual ≈ the impl call)
                boolean dispatch = DispatchSites.isInstanceStub(min, member)
                    && (handle.getTag() == Opcodes.H_INVOKEVIRTUAL || handle.getTag() == Opcodes.H_INVOKEINTERFACE);
                String implOwner = m.member.getOwner().getInternalName();
                String newOwner = implOwner;
                String name = m.member.getName();
                String desc = m.member.getDesc().getDescriptor();
                boolean intf = m.itf;
                if (dispatch || !desc.equals(hStaticDesc.getDescriptor())) {
                    // create wrapper as desc should exactly match.
                    newOwner = owner.name;
                    desc = hStaticDesc.getDescriptor();
                    intf = (owner.access & Opcodes.ACC_INTERFACE) != 0;
                    MethodNode found = null;
                    int num = 0;
                    for (MethodNode mn : owner.methods) {
                        if (mn instanceof HandleMethodNode) {
                            Handle h = ((HandleMethodNode) mn).ref;
                            if (h.getTag() == handle.getTag() &&
                                h.getOwner().equals(handle.getOwner()) &&
                                h.getName().equals(handle.getName()) &&
                                h.getDesc().equals(handle.getDesc()) &&
                                h.isInterface() == handle.isInterface()) {
                                if (!mn.desc.equals(hStaticDesc.getDescriptor())) {
                                    num++;
                                } else {
                                    found = mn;
                                    break;
                                }
                            }
                        }
                    }
                    if (found != null) {
                        name = found.name;
                    } else {
                        HandleMethodNode mv = new HandleMethodNode(method.name, handle, num);
                        mv.access = Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | (provider.downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC);
                        mv.desc = hStaticDesc.getDescriptor();
                        mv.visitCode();
                        Type returnType = hStaticDesc.getReturnType();
                        Type[] arguments = hStaticDesc.getArgumentTypes();
                        Type actualReturnType = m.member.getDesc().getReturnType();
                        int k = 0;
                        for (Type argument : arguments) {
                            mv.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), k);
                            k += argument.getSize();
                        }
                        // transform-injected args the callsite would have
                        // carried as ldcs — the wrapper's own class is the
                        // call's containing class / authored version
                        if (min.callerClass) {
                            mv.visitLdcInsn(Type.getObjectType(owner.name));
                        }
                        if (min.downgradeVersion) {
                            mv.visitLdcInsn(owner.version);
                        }
                        if (dispatch) {
                            // the impl resolves reflectively — declare it for
                            // shading's closure like an authored
                            // @ReflectionReferences
                            MemberStubTransform.addReflectionRef(mv, m, provider.downgrader, provider.inputVersion, warnings);
                            mv.visitInvokeDynamicInsn(member.getName(),
                                m.member.getDesc().getDescriptor(), DispatchSites.DISPATCH_BSM,
                                provider.stubClass(member.getDesc(), warnings),
                                provider.stubClass(Type.getObjectType(implOwner), warnings),
                                m.member.getName() + " " + m.member.getDesc().getDescriptor());
                        } else {
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, implOwner, m.member.getName(),
                                m.member.getDesc().getDescriptor(), m.itf);
                        }
                        if (!actualReturnType.equals(returnType)) {
                            mv.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
                        }
                        mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));

                        mv.visitMaxs(0, 0);
                        mv.visitEnd();
                        owner.methods.add(mv);
                        name = mv.name;
                    }
                }
                handle = new Handle(
                    Opcodes.H_INVOKESTATIC,
                    newOwner,
                    name,
                    desc,
                    intf
                );
            }
        } else {
            MethodRef mod = stubMapper.getModifyFor(member, handle.getTag(), warnings);
            CtorEntry ce = null;
            if (mod == null && member.getName().equals("<init>") &&
                handle.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
                ce = stubMapper.getCtorFor(member, enableRuntime, warnings);
            }
            if (mod != null || ce != null) {
                if (handle.getTag() != Opcodes.H_NEWINVOKESPECIAL) {
                    warnings.add("Invalid modify for indy handle: " + handle.getOwner() + "." + handle.getName() + handle.getDesc());
                } else {
                    Type returnType = Type.getObjectType(handle.getOwner());
                    Type[] arguments = hDesc.getArgumentTypes();

                    String name;
                    String desc = Type.getMethodDescriptor(returnType, arguments);
                    boolean intf = (owner.access & Opcodes.ACC_INTERFACE) != 0;

                    MethodNode found = null;
                    int num = 0;
                    for (MethodNode mn : owner.methods) {
                        if (mn instanceof HandleMethodNode) {
                            Handle h = ((HandleMethodNode) mn).ref;
                            if (h.getTag() == handle.getTag() &&
                                h.getOwner().equals(handle.getOwner()) &&
                                h.getName().equals(handle.getName()) &&
                                h.getDesc().equals(handle.getDesc()) &&
                                h.isInterface() == handle.isInterface()) {
                                if (!mn.desc.equals(desc)) {
                                    num++;
                                } else {
                                    found = mn;
                                    break;
                                }
                            }
                        }
                    }

                    if (found != null) {
                        name = found.name;
                    } else {
                        // construct wrapper
                        HandleMethodNode mn = new HandleMethodNode(method.name, handle, num);
                        mn.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
                        mn.desc = desc;
                        mn.visitCode();
                        int k = 0;
                        if (ce instanceof CtorFactory) {
                            CtorFactory factory = (CtorFactory) ce;
                            for (Type argument : arguments) {
                                mn.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), k);
                                k += argument.getSize();
                            }
                            if (factory.downgradeVersion) {
                                mn.visitLdcInsn(stubMapper.provider().inputVersion);
                            }
                            MethodRef fm = factory.handler;
                            mn.visitMethodInsn(Opcodes.INVOKESTATIC,
                                fm.member.getOwner().getInternalName(),
                                fm.member.getName(), fm.member.getDesc().getDescriptor(),
                                fm.itf);
                            if (!fm.member.getDesc().getReturnType().equals(returnType)) {
                                mn.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
                            }
                            mn.visitInsn(Opcodes.ARETURN);
                        } else {
                            mn.visitTypeInsn(Opcodes.NEW, returnType.getInternalName());
                            mn.visitInsn(Opcodes.DUP);
                            for (Type argument : arguments) {
                                mn.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), k);
                                k += argument.getSize();
                            }
                            mn.visitMethodInsn(Opcodes.INVOKESPECIAL, returnType.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, arguments), false);
                            mn.visitInsn(Opcodes.ARETURN);
                        }
                        mn.visitMaxs(0, 0);
                        mn.visitEnd();
                        owner.methods.add(mn);

                        if (ce instanceof CtorRewrite) {
                            // same rewrite as a plain NEW/DUP callsite — the wrapper
                            // emits exactly that shape for it to operate on
                            int iIndex = mn.instructions.size() - 2;
                            MemberStubTransform.rewriteCtor(mn, mn.instructions.get(iIndex),
                                iIndex, owner, returnType.getInternalName(), member,
                                (CtorRewrite) ce, stubMapper, warnings);
                        } else if (mod != null) {
                            // invoke modify
                            try {
                                Method m = mod.method();
                                List<Object> modifyArgs = Arrays.asList(mn, mn.instructions.size() - 2, owner, extra);
                                m.invoke(null, modifyArgs.subList(0, m.getParameterTypes().length).toArray());
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }

                        name = mn.name;
                    }

                    handle = new Handle(
                        Opcodes.H_INVOKESTATIC,
                        owner.name,
                        name,
                        desc,
                        intf
                    );

                }
            }
        }
        return handle;
    }

    public ConstantDynamic stubCondy(ClassNode owner, MethodNode method, Set<ClassNode> extra, ConstantDynamic condy, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings) throws IOException {
        Handle bsm = condy.getBootstrapMethod();
        Object[] bsmArgs = new Object[condy.getBootstrapMethodArgumentCount()];
        for (int i = 0; i < condy.getBootstrapMethodArgumentCount(); i++) {
            bsmArgs[i] = condy.getBootstrapMethodArgument(i);
        }
        Type desc = provider.stubClass(Type.getType(condy.getDescriptor()), warnings);
        stubBSMArgs(owner, method, extra, bsm, desc.getDescriptor(), bsmArgs, enableRuntime, memberResolver, superTypeResolver, warnings);
        bsm = stubHandle(owner, method, extra, bsm, desc.getDescriptor(), enableRuntime, memberResolver, superTypeResolver, warnings, bsm);
        return new ConstantDynamic(
            condy.getName(),
            desc.getDescriptor(),
            bsm,
            bsmArgs
        );
    }

    public static class HandleMethodNode extends MethodNode {
        public final Handle ref;

        public HandleMethodNode(String caller, Handle ref, int num) {
            super(Opcodes.ASM9);
            caller = caller.replace("<", "$").replace(">", "$");
            this.ref = ref;
            if (ref.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
                name = "jvmdowngrader$construct$";
            } else {
                name = "jvmdowngrader$call$";
            }
            name += caller + "$" + num;
        }

    }

}
