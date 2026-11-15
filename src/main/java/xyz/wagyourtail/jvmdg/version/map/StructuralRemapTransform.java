package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The mechanical half of a stub pass: rewrites every structural {@code Type}
 * slot in an instruction through a {@link TypeRemap} — member insn
 * owner/desc, indy desc and bootstrap-handle fields, {@code ldc} Type
 * constants, stack-map frame slots, {@code multianewarray}, and
 * {@code TypeInsnNode} element types (including the retro-supertype
 * {@code jvmdg$instanceof}/{@code jvmdg$checkcast} rewrites).
 * <p>
 * Member-insn dispatch ({@link MemberResolver} → {@link MemberStubTransform}),
 * bootstrap-arg rewriting and {@code ConstantDynamic} stay with the caller —
 * those resolve through member registries, not the type remap.
 */
public class StructuralRemapTransform {

    private final TypeRemap remap;

    public StructuralRemapTransform(TypeRemap remap) {
        this.remap = remap;
    }

    /**
     * Whether {@code name} is generated dispatch machinery — the
     * {@code xyz/wagyourtail/jvmdg/gen/} dispatchers and the
     * {@code $jvmdg$}/{@code $Ov$} generated siblings. Their
     * {@code instanceof}/{@code checkcast}/class-query sites are literal
     * fast-paths, never user sites to rewrite: routing them would produce
     * self-recursive dispatchers (the api jar's self-downgrade rewrites the
     * emitted {@code instanceof I} inside {@code gen/gained/<I>} into a call
     * to its own {@code jvmdg$isInstance}).
     */
    static boolean isGeneratedDispatch(String name) {
        return name.startsWith("xyz/wagyourtail/jvmdg/gen/")
            || name.contains("$jvmdg$")
            || name.contains("$Ov$");
    }

    /**
     * Remaps the structural Type slots of {@code insn} in place.
     * @param ownerName the containing class's internal name — generated
     *        dispatch machinery skips the retro/gained site rewrites
     *        (mechanical remaps still apply)
     * @param ceiling the containing class's authored class-file version —
     *        the membership ceiling gained-supertype sites emit
     * @return a replacement insn list for the {@code TypeInsnNode}
     *         retro-supertype rewrite ({@code instanceof}/{@code checkcast} →
     *         ldc + invokestatic), or null when the insn continues as-is.
     */
    public InsnList remapInsn(AbstractInsnNode insn, Set<String> warnings, int ceiling, String ownerName) {
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            min.owner = remap.remap(Type.getObjectType(min.owner), warnings).getInternalName();
            min.desc = remap.remap(Type.getMethodType(min.desc), warnings).getDescriptor();
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode fin = (FieldInsnNode) insn;
            fin.owner = remap.remap(Type.getObjectType(fin.owner), warnings).getInternalName();
            fin.desc = remap.remap(Type.getType(fin.desc), warnings).getDescriptor();
        } else if (insn instanceof TypeInsnNode) {
            if (isGeneratedDispatch(ownerName)) {
                return null;
            }
            return remapTypeInsn((TypeInsnNode) insn, warnings, ceiling);
        } else if (insn instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
            indy.desc = remap.remap(Type.getMethodType(indy.desc), warnings).getDescriptor();
            indy.bsm = new Handle(
                indy.bsm.getTag(),
                remap.remap(Type.getObjectType(indy.bsm.getOwner()), warnings).getInternalName(),
                indy.bsm.getName(),
                remap.remap(Type.getMethodType(indy.bsm.getDesc()), warnings).getDescriptor(),
                indy.bsm.isInterface()
            );
        } else if (insn instanceof MultiANewArrayInsnNode) {
            MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) insn;
            manain.desc = remap.remap(Type.getType(manain.desc), warnings).getDescriptor();
        } else if (insn instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) insn;
            if (ldc.cst instanceof Type) {
                ldc.cst = remap.remap((Type) ldc.cst, warnings);
            }
        } else if (insn instanceof FrameNode) {
            FrameNode fn = (FrameNode) insn;
            if (fn.local != null) {
                for (int j = 0; j < fn.local.size(); j++) {
                    Object o = fn.local.get(j);
                    if (o instanceof String) {
                        fn.local.set(j, remap.remap(Type.getObjectType((String) o), warnings).getInternalName());
                    }
                }
            }
            if (fn.stack != null) {
                for (int j = 0; j < fn.stack.size(); j++) {
                    Object o = fn.stack.get(j);
                    if (o instanceof String) {
                        fn.stack.set(j, remap.remap(Type.getObjectType((String) o), warnings).getInternalName());
                    }
                }
            }
        }
        return null;
    }

    /**
     * TypeInsnNode handling: array/object element remap, and for stubbed
     * object types the retro-supertype rewrite — a {@code jvmdg$instanceof}/
     * {@code jvmdg$checkcast} hook always wins, else a retro-capable adapter
     * emits {@code RetroTypes.isInstance}/{@code cast} / the declared
     * {@code wrap} factory — returning the replacement insns, or null.
     * Retro emission gates on {@code mode} alone, never {@code implementedBy}:
     * membership lives in the {@code META-INF/jvmdg/retrotypes} runtime table,
     * so the scan and idx load paths must emit identically.
     */
    private InsnList remapTypeInsn(TypeInsnNode insn, Set<String> warnings, int ceiling) {
        Type desc = Type.getObjectType(insn.desc);
        switch (desc.getSort()) {
            case Type.ARRAY:
                Type type = desc.getElementType();
                if (insn.getOpcode() == Opcodes.INSTANCEOF && type.getSort() == Type.OBJECT) {
                    String disp = remap.gainedFor(type);
                    if (disp != null) {
                        // gained-element arrays: I[k] covariance reduces to
                        // the base element's membership — the dispatcher
                        // strips dims itself. checkcast stays native:
                        // member arrays can't be fabricated (covariance is
                        // a VM property), so its CCE is the honest answer
                        InsnList out = new InsnList();
                        out.add(intInsn(ceiling));
                        out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                            "jvmdg$isArrayInstance", "(Ljava/lang/Object;I)Z", false));
                        return out;
                    }
                }
                ClassStubEntry elementStub = remap.stubFor(type);
                if (elementStub != null && elementStub.mode == Adapter.Mode.SUPERTYPE &&
                    (insn.getOpcode() == Opcodes.INSTANCEOF || insn.getOpcode() == Opcodes.CHECKCAST)) {
                    // retro-supertype array form: element-type membership
                    // check — own-interface arrays stay on the element remap
                    // (JDK impls are not adapter instances)
                    remap.warnUnstubbed(elementStub.target, warnings);
                    InsnList out = new InsnList();
                    out.add(dispCall(elementStub.stubType, insn.getOpcode() == Opcodes.INSTANCEOF
                        ? "isArrayInstance" : "castArray",
                        insn.getOpcode() == Opcodes.INSTANCEOF));
                    if (insn.getOpcode() == Opcodes.CHECKCAST) {
                        out.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getType(
                            desc.getDescriptor().substring(0, desc.getDimensions()) +
                                elementStub.target.getDescriptor()).getInternalName()));
                    }
                    return out;
                }
                if (elementStub != null) {
                    type = elementStub.target;
                }
                if (remap.gainedFor(type) == null) {
                    // gained interfaces are real at the output — not a coverage gap
                    remap.warnUnstubbed(type, warnings);
                }
                insn.desc = Type.getType(desc.getDescriptor().substring(0, desc.getDimensions()) + type.getDescriptor()).getInternalName();
                return null;
            case Type.OBJECT:
                ClassStubEntry stub = remap.stubFor(desc);
                if (stub != null) {
                    switch (insn.getOpcode()) {
                        case Opcodes.INSTANCEOF:
                            if (stub.instanceofHook != null) {
                                // baked jvmdg$ hook from the stub class — explicit override
                                return single(hookInvoke(stub.instanceofHook, stub.itf));
                            }
                            {
                                // every class stub is retro-capable: SUPERTYPE
                                // membership is the whole point, OWN_INTERFACE
                                // rows cover the JDK implementors the stub
                                // interface itself can't see
                                return single(dispCall(stub.stubType, "isInstance", true));
                            }
                        case Opcodes.CHECKCAST:
                            if (stub.checkcastHook != null) {
                                return single(hookInvoke(stub.checkcastHook, stub.itf));
                            }
                            if (stub.mode == Adapter.Mode.SUPERTYPE) {
                                InsnList out = new InsnList();
                                out.add(dispCall(stub.stubType, "cast", false));
                                // membership is enforced; the remapped
                                // checkcast keeps the verifier's stack type
                                out.add(new TypeInsnNode(Opcodes.CHECKCAST, stub.target.getInternalName()));
                                return out;
                            }
                            if (stub.mode == Adapter.Mode.OWN_INTERFACE && stub.wrap != null) {
                                return single(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                    stub.wrap.getOwner().getInternalName(), stub.wrap.getName(),
                                    stub.wrap.getDesc().getDescriptor(),
                                    stub.wrap.getOwner().equals(stub.stubType)
                                        ? stub.itf : isInterface(stub.wrap.getOwner())));
                            }
                            if (stub.mode == Adapter.Mode.OWN_INTERFACE) {
                                // generated wrap dispatcher: adapter-typed
                                // objects pass through, declared impls adapt
                                // members, RetroTypes.wrapImpl is the table
                                // fallback — the trailing checkcast keeps
                                // the verifier's stack type. Emitted for
                                // every OWN_INTERFACE adapter so scan/idx
                                // emit identically
                                InsnList out = new InsnList();
                                out.add(dispCall(stub.stubType, "wrap", false));
                                out.add(new TypeInsnNode(Opcodes.CHECKCAST, stub.target.getInternalName()));
                                return out;
                            }
                            break;
                        default:
                    }
                    insn.desc = stub.target.getInternalName();
                    return null;
                }
                String disp = remap.gainedFor(desc);
                if (disp != null) {
                    // gained-supertype interface: real at the target so the
                    // type slot stays — only the query ops route, with the
                    // class's authored version as the membership ceiling
                    switch (insn.getOpcode()) {
                        case Opcodes.INSTANCEOF: {
                            InsnList out = new InsnList();
                            out.add(intInsn(ceiling));
                            out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                                "jvmdg$isInstance", "(Ljava/lang/Object;I)Z", false));
                            return out;
                        }
                        case Opcodes.CHECKCAST: {
                            // the dispatcher's wrap produces a real I-typed
                            // result — the verifier's stack type a raw
                            // member could never satisfy
                            InsnList out = new InsnList();
                            out.add(intInsn(ceiling));
                            out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                                "jvmdg$cast", "(Ljava/lang/Object;I)" + desc.getDescriptor(), false));
                            return out;
                        }
                        default:
                    }
                }
                remap.warnUnstubbed(desc, warnings);
                break;
        }
        return null;
    }

    /**
     * Rewrites {@code X.class.isInstance(o)}/{@code cast}/{@code isAssignableFrom}
     * calls whose receiver is an {@code ldc}'d retro-capable type. The
     * {@code ldc} is remapped to the target class like any other use, so
     * the call would lose the retro identity — instead the receiver/arg
     * pair is rearranged ({@code swap;pop;ldc name;invokestatic}) to
     * {@link xyz.wagyourtail.jvmdg.stub.RetroTypes}.
     * <p>
     * Producer analysis is a {@link SourceValue} pass over the
     * <em>pre-remap</em> bytecode — run before the per-insn loop mutates
     * the method. Only uniform producer sets rewrite (every producer an
     * {@code ldc} of the same retro type); mixed or opaque receivers keep
     * the remapped-native behavior. Returns call insn → replacement.
     */
    public Map<AbstractInsnNode, InsnList> remapClassCalls(String owner, MethodNode method, int ceiling) {
        Map<AbstractInsnNode, InsnList> out = new HashMap<>();
        if (isGeneratedDispatch(owner)) {
            // generated machinery's class queries are literal fast-paths —
            // never rewrite them into the dispatchers they implement
            return out;
        }
        boolean found = false;
        for (int i = 0; i < method.instructions.size(); i++) {
            if (isClassQuery(method.instructions.get(i))) {
                found = true;
                break;
            }
        }
        if (!found) {
            return out;
        }
        Frame<SourceValue>[] frames;
        try {
            frames = new Analyzer<>(new SourceInterpreter()).analyze(owner, method);
        } catch (AnalyzerException | RuntimeException e) {
            return out; // unverifiable input — leave it to the verifier
        }
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode insn = method.instructions.get(i);
            if (!isClassQuery(insn)) {
                continue;
            }
            Frame<SourceValue> frame = frames[i];
            if (frame == null || frame.getStackSize() < 2) {
                continue; // unreachable / malformed
            }
            Type receiver = uniformRoutableType(frame.getStack(frame.getStackSize() - 2).insns);
            if (receiver != null) {
                InsnList postfix = classQueryPostfix((MethodInsnNode) insn, receiver, ceiling);
                if (postfix != null) {
                    out.put(insn, postfix);
                }
            }
        }
        return out;
    }

    /** {@code INVOKEVIRTUAL java/lang/Class.isInstance|cast|isAssignableFrom} — the query shapes {@link #remapClassCalls} routes. */
    private static boolean isClassQuery(AbstractInsnNode insn) {
        if (!(insn instanceof MethodInsnNode) || insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
            return false;
        }
        MethodInsnNode min = (MethodInsnNode) insn;
        if (!min.owner.equals("java/lang/Class")) {
            return false;
        }
        return (min.name.equals("isInstance") && min.desc.equals("(Ljava/lang/Object;)Z")) ||
            (min.name.equals("cast") && min.desc.equals("(Ljava/lang/Object;)Ljava/lang/Object;")) ||
            (min.name.equals("isAssignableFrom") && min.desc.equals("(Ljava/lang/Class;)Z"));
    }

    /**
     * The single routable {@link Type} every producer {@code ldc}s, or
     * null — producers must be uniformly {@code ldc <type>.class} of the
     * same routable type: a retro (stubbed) receiver or a gained-supertype
     * interface (an array constant counts only when its element is
     * routable; scalar-vs-array merges mismatch and are skipped).
     */
    private Type uniformRoutableType(Set<AbstractInsnNode> producers) {
        if (producers.isEmpty()) {
            return null;
        }
        String name = null;
        for (AbstractInsnNode producer : producers) {
            if (!(producer instanceof LdcInsnNode) || !(((LdcInsnNode) producer).cst instanceof Type)) {
                return null;
            }
            Type t = (Type) ((LdcInsnNode) producer).cst;
            Type element = t.getSort() == Type.ARRAY ? t.getElementType() : t;
            boolean routable = element.getSort() == Type.OBJECT &&
                (remap.stubFor(element) != null || remap.gainedFor(element) != null);
            if (!routable || (name != null && !name.equals(t.getInternalName()))) {
                return null;
            }
            name = t.getInternalName();
        }
        return Type.getObjectType(name);
    }

    /**
     * {@code [.., X.class, arg] → [.., arg(, dims)] → X$jvmdg$retro.*} —
     * stack effect identical to the original call so the next hand-authored
     * frame stays valid. Array receivers route to the element-membership
     * forms and carry the receiver's dimension count; every retro query
     * lands on the generated per-adapter dispatcher (guaranteed by
     * {@link #uniformRetroType}, which only accepts stubbed receivers).
     */
    private InsnList classQueryPostfix(MethodInsnNode call, Type receiver, int ceiling) {
        boolean array = receiver.getSort() == Type.ARRAY;
        Type element = array ? receiver.getElementType() : receiver;
        ClassStubEntry stub = remap.stubFor(element);
        if (stub == null) {
            return gainedQueryPostfix(call, receiver, element, ceiling);
        }
        InsnList out = new InsnList();
        out.add(new InsnNode(Opcodes.SWAP));
        out.add(new InsnNode(Opcodes.POP));
        switch (call.name) {
            case "isInstance":
                out.add(dispCall(stub.stubType, array ? "isArrayInstance" : "isInstance", true));
                break;
            case "cast":
                // mirror checkcast's dispatch: OWN_INTERFACE receivers must
                // adapt members to the adapter type — cast() validates but
                // returns the raw object — so the same hook/wrap-factory/
                // generated-wrap order applies
                if (!array && stub.mode == Adapter.Mode.OWN_INTERFACE) {
                    if (stub.checkcastHook != null) {
                        out.add(hookInvoke(stub.checkcastHook, stub.itf));
                    } else if (stub.wrap != null) {
                        out.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            stub.wrap.getOwner().getInternalName(), stub.wrap.getName(),
                            stub.wrap.getDesc().getDescriptor(),
                            stub.wrap.getOwner().equals(stub.stubType)
                                ? stub.itf : isInterface(stub.wrap.getOwner())));
                    } else {
                        out.add(dispCall(stub.stubType, "wrap", false));
                    }
                } else {
                    out.add(dispCall(stub.stubType, array ? "castArray" : "cast", false));
                }
                break;
            default: // isAssignableFrom
                out.add(intInsn(array ? receiver.getDimensions() : 0));
                out.add(dispCall(stub.stubType, "isAssignableFrom", "(Ljava/lang/Class;I)Z"));
        }
        return out;
    }

    /**
     * {@code X.class.*} on a gained-supertype receiver — the same
     * swap/pop receiver rearrangement, routed to the interface's
     * dispatcher with the authored ceiling. {@code cast} on an array
     * receiver stays native (member arrays can't be fabricated); its
     * {@code null} return leaves the call alone.
     */
    private InsnList gainedQueryPostfix(MethodInsnNode call, Type receiver, Type element, int ceiling) {
        String disp = remap.gainedFor(element);
        if (disp == null) {
            return null;
        }
        boolean array = receiver.getSort() == Type.ARRAY;
        InsnList out = new InsnList();
        out.add(new InsnNode(Opcodes.SWAP));
        out.add(new InsnNode(Opcodes.POP));
        switch (call.name) {
            case "isInstance":
                out.add(intInsn(ceiling));
                out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                    array ? "jvmdg$isArrayInstance" : "jvmdg$isInstance",
                    "(Ljava/lang/Object;I)Z", false));
                break;
            case "cast":
                if (array) {
                    return null;
                }
                out.add(intInsn(ceiling));
                out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp, "jvmdg$cast",
                    "(Ljava/lang/Object;I)" + receiver.getDescriptor(), false));
                break;
            default: // isAssignableFrom
                if (array) {
                    out.add(intInsn(receiver.getDimensions()));
                    out.add(intInsn(ceiling));
                    out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                        "jvmdg$isArrayAssignableFrom", "(Ljava/lang/Class;II)Z", false));
                } else {
                    out.add(intInsn(ceiling));
                    out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                        "jvmdg$isAssignableFrom", "(Ljava/lang/Class;I)Z", false));
                }
        }
        return out;
    }

    private static AbstractInsnNode intInsn(int v) {
        if (v >= 0 && v <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + v);
        }
        return new IntInsnNode(v <= Byte.MAX_VALUE ? Opcodes.BIPUSH : Opcodes.SIPUSH, v);
    }

    private static InsnList single(AbstractInsnNode insn) {
        InsnList out = new InsnList();
        out.add(insn);
        return out;
    }

    private static MethodInsnNode hookInvoke(FullyQualifiedMemberNameAndDesc member, boolean itf) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, member.getOwner().getInternalName(),
            member.getName(), member.getDesc().getDescriptor(), itf);
    }

    /**
     * Off-adapter {@code wrap} owner fallback — the adapter's own bit comes
     * from {@link ClassStubEntry#itf}; a foreign owner's invokestatic bit
     * resolves reflectively when it can, treating unloadable classes
     * (including pre-downgrade class-file versions a runtime loader would
     * hand us) as non-interface.
     */
    private static boolean isInterface(Type owner) {
        try {
            return Class.forName(owner.getClassName(), false, StructuralRemapTransform.class.getClassLoader()).isInterface();
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * {@code invokestatic <adapter>$jvmdg$retro.<name>} — the per-adapter
     * dispatcher {@code genStubIndex} generates into the api jar for every
     * class stub (OWN_INTERFACE adapters get {@code wrap}; SUPERTYPE get
     * {@code cast}; both get the membership/array/query forms). The owner
     * is an ordinary api-class reference: the shade closure indexes the
     * edge and prefix-remapping rewrites it like any other api call.
     */
    private static MethodInsnNode dispCall(Type adapter, String name, boolean bool) {
        return dispCall(adapter, name,
            "(Ljava/lang/Object;)" + (bool ? "Z" : "Ljava/lang/Object;"));
    }

    private static MethodInsnNode dispCall(Type adapter, String name, String desc) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC,
            adapter.getInternalName() + "$jvmdg$retro", name, desc, false);
    }
}
