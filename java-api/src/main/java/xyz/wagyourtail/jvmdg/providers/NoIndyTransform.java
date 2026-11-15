package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.pipeline.ClassTransform;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.DowngradeContext;
import xyz.wagyourtail.jvmdg.version.ModifyContext;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.ReflectionReferences;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;
import xyz.wagyourtail.jvmdg.version.map.StubRegistry;

import java.io.IOException;
import java.util.*;

/**
 * Post-chain sweep for {@code --noIndy}: guarantees output classes contain
 * no {@link InvokeDynamicInsnNode} or {@link ConstantDynamic} regardless of
 * target. Runs at {@code AFTER_VERSION_CHAIN}, so it only sees sites that
 * survived the boundary desugars — i.e. bootstrap APIs that exist natively
 * on the output target.
 * <p>
 * Per indy site, dispatch order is:
 * <ol>
 *   <li>registered {@code @IndyDesugar} handlers, unioned across <b>all</b>
 *   providers (newest {@code inputVersion} first) — applied in sequence
 *   until the insn stops being an indy (a normalizer like j23's
 *   {@code typeSwitch} desc fix can run ahead of j21's real desugar);</li>
 *   <li>generic {@code jvmdg$indy$} lowering — the same shape the j7→6
 *   boundary produces: a static {@code CallSite} field resolved once in
 *   {@code <clinit>} by invokestatic'ing the (already stub-remapped) bsm,
 *   and a synthetic static method doing {@code getTarget().invokeExact}.
 *   Works for any statically-invocable bsm, including user bsms.</li>
 * </ol>
 * {@code ldc <ConstantDynamic>} and condy-valued bsm args resolve into
 * {@code jvmdg$condy$} static fields — the bsm is invoked once in
 * {@code <clinit>} and the ldc/arg becomes a {@code GETSTATIC} (matching
 * condy's once-per-callsite evaluation semantics).
 */
public class NoIndyTransform implements ClassTransform {

    private static final Type CALLSITE = Type.getObjectType("java/lang/invoke/CallSite");
    private static final Type HANDLE = Type.getObjectType("java/lang/invoke/MethodHandle");
    private static final Type HANDLES = Type.getObjectType("java/lang/invoke/MethodHandles");
    private static final Type LOOKUP = Type.getObjectType("java/lang/invoke/MethodHandles$Lookup");
    private static final Type METHOD_TYPE = Type.getObjectType("java/lang/invoke/MethodType");

    private final Java7Downgrader generic = new Java7Downgrader();
    private final Map<MemberNameAndDesc, List<MethodRef>> desugars;
    private final List<String> initFailures;

    public NoIndyTransform(Map<MemberNameAndDesc, List<MethodRef>> desugars,
                         List<String> initFailures) {
        this.desugars = desugars;
        this.initFailures = initFailures;
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) throws IOException {
        DowngradeContext dc = DowngradeContext.get(ctx);
        Flags flags = dc.downgrader.flags;
        boolean found = false;
        for (MethodNode m : node.methods) {
            if (m.instructions == null) continue;
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof InvokeDynamicInsnNode ||
                    (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof ConstantDynamic)) {
                    found = true;
                    break;
                }
            }
            if (found) break;
        }
        if (!found) return node;

        for (String failure : initFailures) {
            ctx.diagnostics.warn("noIndy:provider:" + failure, failure);
        }

        // the newest provider's reverse map covers the whole chain below
        // it — handlers use it to recover source-vocabulary names from
        // already-remapped site types
        VersionProvider unstubProvider = null;
        for (java.util.Map.Entry<Integer, VersionProvider> e : dc.downgrader.providers().entrySet()) {
            if (unstubProvider == null || e.getKey() > unstubProvider.inputVersion) {
                unstubProvider = e.getValue();
            }
        }
        final VersionProvider unstubVp = unstubProvider;

        InsnList addToClinit = new InsnList();
        List<FullyQualifiedMemberNameAndDesc> reflectionRefs = new ArrayList<>();
        Map<ConstantDynamic, FieldNode> condyFieldMap = new HashMap<>();
        Map<Java7Downgrader.InvokeDynamicType, String> indyCache = new HashMap<>();

        // snapshot: handlers and the generic lowering append synthetic
        // methods (jvmdowngrader$* helpers, jvmdg$indy$ dispatchers) to
        // node.methods mid-sweep — those carry no indy sites so they
        // don't need revisiting
        for (MethodNode method : new ArrayList<>(node.methods)) {
            if (method.instructions == null) continue;
            for (int i = 0; i < method.instructions.size(); i++) {
                AbstractInsnNode insn = method.instructions.get(i);
                if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                    MemberNameAndDesc member = new MemberNameAndDesc(indy.bsm.getName(),
                        Type.getMethodType(indy.bsm.getDesc()));
                    List<MethodRef> handlers = desugars.get(member);
                    if (handlers != null) {
                        for (MethodRef handler : handlers) {
                            if (!(method.instructions.get(i) instanceof InvokeDynamicInsnNode)) {
                                break;
                            }
                            try {
                                handler.method().invoke(null, new ModifyContext(flags, node, method, i,
                                    ctx.extras.asSet(), flags.debugNoSynthetic, indy,
                                    unstubVp == null ? null : unstubVp::unstubClass));
                            } catch (Throwable t) {
                                String ownerName = handler.member.getOwner().getInternalName();
                                ctx.diagnostics.warn("indyDesugar " +
                                    ownerName.substring(ownerName.lastIndexOf('/') + 1) + "." +
                                    handler.member.getName() + " failed on " + member.getName() +
                                    member.getDesc() + ": " + t);
                            }
                        }
                    }
                    if (method.instructions.get(i) instanceof InvokeDynamicInsnNode) {
                        if (handlers == null) {
                            String bsmMember = indy.bsm.getOwner() + "." + indy.bsm.getName() +
                                indy.bsm.getDesc();
                            ctx.diagnostics.warn("noIndy:" + bsmMember,
                                "--noIndy: no registered desugar for bsm " + bsmMember +
                                    " — falling back to generic jvmdg$indy$ lowering");
                        }
                        String name = lowerIndy(method, indy, node, addToClinit, condyFieldMap,
                            indyCache, reflectionRefs);
                        method.instructions.set(indy, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            node.name, name, indy.desc, false));
                    }
                } else if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof ConstantDynamic) {
                    FieldNode field = condyField((ConstantDynamic) ((LdcInsnNode) insn).cst, node,
                        addToClinit, condyFieldMap, reflectionRefs);
                    method.instructions.set(insn, new FieldInsnNode(Opcodes.GETSTATIC, node.name,
                        field.name, field.desc));
                }
            }
        }

        if (addToClinit.size() > 0) {
            MethodNode clinit = null;
            for (MethodNode m : node.methods) {
                if (m.name.equals("<clinit>")) {
                    clinit = m;
                    break;
                }
            }
            if (clinit == null) {
                clinit = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                clinit.visitCode();
                clinit.visitInsn(Opcodes.RETURN);
                clinit.visitMaxs(0, 0);
                clinit.visitEnd();
                node.methods.add(clinit);
            }
            if (clinit.visibleAnnotations == null) {
                clinit.visibleAnnotations = new ArrayList<>();
            }
            String reflectionRefsDesc = Type.getType(ReflectionReferences.class).getDescriptor();
            String refDesc = Type.getType(Ref.class).getDescriptor();
            AnnotationNode refAnn = null;
            for (AnnotationNode a : clinit.visibleAnnotations) {
                if (a.desc.equals(reflectionRefsDesc)) {
                    refAnn = a;
                    break;
                }
            }
            if (refAnn == null) {
                refAnn = new AnnotationNode(reflectionRefsDesc);
                clinit.visibleAnnotations.add(refAnn);
                refAnn.values = new ArrayList<Object>(Arrays.asList("value", new ArrayList<AnnotationNode>()));
            }
            List<AnnotationNode> refs = (List<AnnotationNode>) refAnn.values.get(1);
            for (FullyQualifiedMemberNameAndDesc ref : reflectionRefs) {
                AnnotationNode ann = new AnnotationNode(refDesc);
                ann.visit("value", ref.getOwner().getInternalName());
                ann.visit("member", ref.getName());
                ann.visit("desc", ref.getDesc().toString());
                ann.visitEnd();
                refs.add(ann);
            }

            node.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "jvmdg$lookup", LOOKUP.getDescriptor(), null, null);
            InsnList lookupInsns = new InsnList();
            lookupInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HANDLES.getInternalName(),
                "lookup", "()" + LOOKUP.getDescriptor(), false));
            lookupInsns.add(new FieldInsnNode(Opcodes.PUTSTATIC, node.name, "jvmdg$lookup",
                LOOKUP.getDescriptor()));
            addToClinit.insert(lookupInsns);
            clinit.instructions.insert(addToClinit);
        }
        return node;
    }

    /**
     * The union of every provider's {@code indyDesugars}, ordered newest
     * {@code inputVersion} first per member — a normalizer registered on a
     * newer provider runs before the real desugar on an older one, matching
     * chain order. All providers are initialized lazily here: a surviving
     * indy was authored at &ge; its bootstrap's API version, so the desugar
     * for it may live on a provider below the run's chain (e.g. j9's concat
     * desugar for a 15&rarr;9 run). Runs at stage-list time (from
     * {@link NoIndyContributor}) so off-chain provider init mutates the
     * resolver graph consistently with chain init rather than mid-run.
     * A provider that fails to initialize in this environment — e.g.
     * classpath-resolution gaps when the api jar itself is the input —
     * is skipped with a warning recorded in {@code initFailures}: its
     * desugars just don't fire and those sites still get the generic
     * lowering, so the zero-indy invariant holds either way.
     */
    public static Map<MemberNameAndDesc, List<MethodRef>> unionDesugars(ClassDowngrader downgrader,
                                                                      List<String> initFailures) {
        Map<MemberNameAndDesc, List<MethodRef>> union = new HashMap<>();
        List<Map.Entry<Integer, VersionProvider>> providers =
            new ArrayList<>(downgrader.providers().entrySet());
        providers.sort(new Comparator<Map.Entry<Integer, VersionProvider>>() {
            @Override
            public int compare(Map.Entry<Integer, VersionProvider> a, Map.Entry<Integer, VersionProvider> b) {
                return b.getKey() - a.getKey();
            }
        });
        for (Map.Entry<Integer, VersionProvider> e : providers) {
            try {
                e.getValue().ensureInit(downgrader);
            } catch (Exception ex) {
                initFailures.add("--noIndy: skipping indy desugars from provider for input version " +
                    e.getKey() + " — provider failed to initialize here: " + ex);
                continue;
            }
            for (Map.Entry<String, StubRegistry.Owner> owner : e.getValue().stubRegistry.owners().entrySet()) {
                for (Map.Entry<MemberNameAndDesc, MethodRef> d : owner.getValue().indyDesugars.entrySet()) {
                    List<MethodRef> list = union.get(d.getKey());
                    if (list == null) {
                        list = new ArrayList<>();
                        union.put(d.getKey(), list);
                    }
                    list.add(d.getValue());
                }
            }
        }
        return union;
    }

    /**
     * Generic indy lowering — same shape as the j7&rarr;6 boundary: a
     * {@code jvmdg$indy$} static {@code CallSite} field resolved once in
     * {@code <clinit>} by invoking the bsm, and a synthetic
     * {@code jvmdg$indy$} method that does {@code field.getTarget()
     * .invokeExact(siteArgs)}.
     */
    private String lowerIndy(MethodNode method, InvokeDynamicInsnNode indy, ClassNode node,
                             InsnList addToClinit, Map<ConstantDynamic, FieldNode> condyFieldMap,
                             Map<Java7Downgrader.InvokeDynamicType, String> indyCache,
                             List<FullyQualifiedMemberNameAndDesc> reflectionRefs) {
        Java7Downgrader.InvokeDynamicType it = new Java7Downgrader.InvokeDynamicType(indy);
        String cached = indyCache.get(it);
        if (cached != null) {
            return cached;
        }
        int count = node.fields.size();
        Java7Downgrader.IndyField indyField = new Java7Downgrader.IndyField(it, CALLSITE, count);
        node.fields.add(indyField);

        addToClinit.add(new FieldInsnNode(Opcodes.GETSTATIC, node.name, "jvmdg$lookup",
            LOOKUP.getDescriptor()));
        addToClinit.add(new LdcInsnNode(indy.name));
        addToClinit.add(Java7Downgrader.methodDescToMethodType(Type.getMethodType(indy.desc), METHOD_TYPE));
        pushBsmArgs(indy.bsm, indy.bsmArgs, node, addToClinit, condyFieldMap, reflectionRefs);
        addToClinit.add(new MethodInsnNode(Opcodes.INVOKESTATIC, indy.bsm.getOwner(),
            indy.bsm.getName(), indy.bsm.getDesc(), indy.bsm.isInterface()));
        addToClinit.add(new FieldInsnNode(Opcodes.PUTSTATIC, node.name, indyField.name, indyField.desc));

        MethodNode indyMethod = new Java7Downgrader.IndyMethod(it, count);
        node.methods.add(indyMethod);
        indyMethod.visitCode();
        indyMethod.visitFieldInsn(Opcodes.GETSTATIC, node.name, indyField.name, CALLSITE.getDescriptor());
        indyMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CALLSITE.getInternalName(), "getTarget",
            Type.getMethodDescriptor(HANDLE), false);
        int argIdx = 0;
        for (Type arg : Type.getArgumentTypes(indy.desc)) {
            indyMethod.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), argIdx);
            argIdx += arg.getSize();
        }
        indyMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, HANDLE.getInternalName(), "invokeExact",
            indy.desc, false);
        indyMethod.visitInsn(Type.getReturnType(indy.desc).getOpcode(Opcodes.IRETURN));
        indyMethod.visitMaxs(0, 0);
        indyMethod.visitEnd();
        indyCache.put(it, indyMethod.name);
        return indyMethod.name;
    }

    /**
     * Resolves a {@link ConstantDynamic} into a {@code jvmdg$condy$} static
     * field: {@code <clinit>} invokes the condy's bsm once and stores the
     * result; nested condy args resolve to their own fields first.
     */
    private FieldNode condyField(ConstantDynamic condy, ClassNode node, InsnList addToClinit,
                                 Map<ConstantDynamic, FieldNode> condyFieldMap,
                                 List<FullyQualifiedMemberNameAndDesc> reflectionRefs) {
        FieldNode existing = condyFieldMap.get(condy);
        if (existing != null) {
            return existing;
        }
        Type fieldType = Type.getType(condy.getDescriptor());
        FieldNode field = new FieldNode(Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC |
            Opcodes.ACC_FINAL, "jvmdg$condy$" + condyFieldMap.size(), fieldType.getDescriptor(),
            null, null);
        condyFieldMap.put(condy, field);
        node.fields.add(field);

        addToClinit.add(new FieldInsnNode(Opcodes.GETSTATIC, node.name, "jvmdg$lookup",
            LOOKUP.getDescriptor()));
        addToClinit.add(new LdcInsnNode(condy.getName()));
        addToClinit.add(new LdcInsnNode(fieldType));
        int argCount = condy.getBootstrapMethodArgumentCount();
        Object[] args = new Object[argCount];
        for (int i = 0; i < argCount; i++) {
            args[i] = condy.getBootstrapMethodArgument(i);
        }
        pushBsmArgs(condy.getBootstrapMethod(), args, node, addToClinit, condyFieldMap,
            reflectionRefs);
        addToClinit.add(new MethodInsnNode(Opcodes.INVOKESTATIC, condy.getBootstrapMethod().getOwner(),
            condy.getBootstrapMethod().getName(), condy.getBootstrapMethod().getDesc(),
            condy.getBootstrapMethod().isInterface()));
        Type bsmReturn = Type.getReturnType(condy.getBootstrapMethod().getDesc());
        if (!bsmReturn.equals(fieldType)) {
            if (fieldType.getSort() < Type.ARRAY) {
                addToClinit.add(new TypeInsnNode(Opcodes.CHECKCAST,
                    ASMUtils.getBoxFor(fieldType).getInternalName()));
                Handle unbox = ASMUtils.unboxType(fieldType);
                addToClinit.add(new MethodInsnNode(unbox.getTag() == Opcodes.H_INVOKESTATIC
                    ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL, unbox.getOwner(), unbox.getName(),
                    unbox.getDesc(), unbox.isInterface()));
            } else {
                addToClinit.add(new TypeInsnNode(Opcodes.CHECKCAST, fieldType.getInternalName()));
            }
        }
        addToClinit.add(new FieldInsnNode(Opcodes.PUTSTATIC, node.name, field.name, field.desc));
        return field;
    }

    /**
     * Pushes a bsm's trailing args (position 3+, after lookup/name/type)
     * onto the clinit stack, packing into an array when the bsm's last
     * parameter is varargs. {@link ConstantDynamic} args resolve to their
     * {@code jvmdg$condy$} fields.
     */
    private void pushBsmArgs(Handle bsm, Object[] args, ClassNode node, InsnList addToClinit,
                             Map<ConstantDynamic, FieldNode> condyFieldMap,
                             List<FullyQualifiedMemberNameAndDesc> reflectionRefs) {
        Type[] params = Type.getArgumentTypes(bsm.getDesc());
        int fixed = params.length - 3;
        boolean varargs = params.length > 0 && params[params.length - 1].getSort() == Type.ARRAY;
        if (varargs) {
            fixed--;
        }
        int i = 0;
        for (; i < Math.min(fixed, args.length); i++) {
            pushBsmArg(args[i], params[3 + i], node, addToClinit, condyFieldMap, reflectionRefs);
        }
        if (varargs) {
            Type element = params[params.length - 1].getElementType();
            int remaining = args.length - i;
            addToClinit.add(new LdcInsnNode(remaining));
            addToClinit.add(new TypeInsnNode(Opcodes.ANEWARRAY, element.getInternalName()));
            for (int j = 0; j < remaining; j++) {
                addToClinit.add(new InsnNode(Opcodes.DUP));
                addToClinit.add(new LdcInsnNode(j));
                pushBsmArg(args[i + j], element, node, addToClinit, condyFieldMap, reflectionRefs);
                addToClinit.add(new InsnNode(element.getOpcode(Opcodes.IASTORE)));
            }
        }
    }

    private void pushBsmArg(Object arg, Type expected, ClassNode node, InsnList addToClinit,
                            Map<ConstantDynamic, FieldNode> condyFieldMap,
                            List<FullyQualifiedMemberNameAndDesc> reflectionRefs) {
        if (arg instanceof ConstantDynamic) {
            FieldNode field = condyField((ConstantDynamic) arg, node, addToClinit, condyFieldMap,
                reflectionRefs);
            addToClinit.add(new FieldInsnNode(Opcodes.GETSTATIC, node.name, field.name, field.desc));
        } else if (arg instanceof Type && ((Type) arg).getSort() == Type.METHOD) {
            addToClinit.add(Java7Downgrader.methodDescToMethodType((Type) arg, METHOD_TYPE));
        } else if (arg instanceof Handle) {
            addToClinit.add(generic.handleToLookupStack((Handle) arg, node, HANDLE, LOOKUP, METHOD_TYPE,
                reflectionRefs));
        } else {
            addToClinit.add(new LdcInsnNode(arg));
            // constant-pool ints/longs/floats/doubles need boxing when the
            // param expects an object (varargs elements, Object params)
            if (expected.getSort() >= Type.ARRAY) {
                Type primitive = arg instanceof Integer ? Type.INT_TYPE
                    : arg instanceof Long ? Type.LONG_TYPE
                    : arg instanceof Float ? Type.FLOAT_TYPE
                    : arg instanceof Double ? Type.DOUBLE_TYPE : null;
                if (primitive != null) {
                    Handle box = ASMUtils.boxType(primitive);
                    addToClinit.add(new MethodInsnNode(Opcodes.INVOKESTATIC, box.getOwner(),
                        box.getName(), box.getDesc(), false));
                }
            }
        }
    }

}
