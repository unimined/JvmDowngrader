package xyz.wagyourtail.jvmdg.j7.transform;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.BoundaryTransform;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.ReflectionReferences;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.indy.IndyShapes;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.*;

/** invokedynamic/constant-handle lowering for pre-7 targets: each indy site
 * becomes a {@code jvmdg$indy$N} static method on the class (invoked via
 * INVOKESTATIC at the old site), each ldc'd Handle becomes a
 * {@code jvmdg$handle$N} static field, and method-type ldc becomes a
 * {@code jvmdg$methodtype$N} field — all resolved once in {@code <clinit>}
 * through the lookup stub. {@code jvmdg$lookup} caches the Lookup. Also
 * used by {@code --noIndy} as the generic fallback lowering (the shared
 * {@code InvokeDynamicType}/{@code IndyMethod}/{@code IndyField}/
 * {@code MethodTypeField}/{@code HandleField} shapes live in
 * {@link xyz.wagyourtail.jvmdg.version.indy.IndyShapes}, referenced by
 * {@code NoIndyTransform}) */
public class IndyDesugars implements BoundaryTransform {

    @Override
    public ClassNode apply(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra,
                           Function<String, ClassNode> getReadOnly, Set<String> warnings,
                           boolean enableRuntime,
                           IOFunction<Type, Set<MemberNameAndDesc>> memberResolver,
                           IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        InsnList addToClinit = new InsnList();
        Type handleType = provider.stubClass(Type.getObjectType("java/lang/invoke/MethodHandle"), warnings);
        Type callSiteType = provider.stubClass(Type.getObjectType("java/lang/invoke/CallSite"), warnings);

        Type handlesType = provider.stubClass(Type.getObjectType("java/lang/invoke/MethodHandles"), warnings);
        Type lookupType = provider.stubClass(Type.getObjectType("java/lang/invoke/MethodHandles$Lookup"), warnings);
        Type methodType = provider.stubClass(Type.getObjectType("java/lang/invoke/MethodType"), warnings);

        MethodNode clinit = null;
        List<FullyQualifiedMemberNameAndDesc> reflectionRefList = new ArrayList<FullyQualifiedMemberNameAndDesc>();
        for (MethodNode method : new ArrayList<MethodNode>(clazz.methods)) {
            if (method.name.equals("<clinit>")) {
                clinit = method;
            }
            if (method.instructions != null) {
                for (int i = 0; i < method.instructions.size(); i++) {
                    AbstractInsnNode insn = method.instructions.get(i);
                    if (insn.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
                        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                        String name = IndyShapes.indyToMethod(method, indy, clazz, addToClinit, callSiteType, handleType, lookupType, methodType, reflectionRefList);

                        method.instructions.set(indy, new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            clazz.name,
                            name,
                            indy.desc,
                            false
                        ));
                    }
                    if (insn.getType() == AbstractInsnNode.LDC_INSN) {
                        assert insn instanceof LdcInsnNode;
                        Object cst = ((LdcInsnNode) insn).cst;
                        if (cst instanceof Handle) {
                            String name = IndyShapes.handleToLookupField((Handle) cst, clazz, addToClinit, handleType, lookupType, methodType, reflectionRefList);
                            method.instructions.set(insn, new FieldInsnNode(Opcodes.GETSTATIC, clazz.name, name, handleType.getDescriptor()));
                        } else if (cst instanceof Type && ((Type) cst).getSort() == Type.METHOD) {
                            String name = IndyShapes.methodDescToLookupField((Type) cst, clazz, addToClinit, methodType);
                            method.instructions.set(insn, new FieldInsnNode(Opcodes.GETSTATIC, clazz.name, name, methodType.getDescriptor()));
                        }
                    }
                }
            }
        }
        if (addToClinit.size() > 0) {
            if (clinit == null) {
                clinit = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                clinit.visitCode();
                clinit.visitInsn(Opcodes.RETURN);
                clinit.visitMaxs(0, 0);
                clinit.visitEnd();
                clazz.methods.add(clinit);
            }
            if (clinit.visibleAnnotations == null) {
                clinit.visibleAnnotations = new ArrayList<AnnotationNode>();
            }
            String reflectionRefs = Type.getType(ReflectionReferences.class).getDescriptor();
            String refType = Type.getType(Ref.class).getDescriptor();
            AnnotationNode node = null;
            for (AnnotationNode a : clinit.visibleAnnotations) {
                if (a.desc.equals(reflectionRefs)) {
                    node = a;
                    break;
                }
            }
            if (node == null) {
                node = new AnnotationNode(reflectionRefs);
                clinit.visibleAnnotations.add(node);
                node.values = new ArrayList<Object>(Arrays.asList("value", new ArrayList<AnnotationNode>()));
            }
            List<AnnotationNode> refs = ((List<AnnotationNode>) node.values.get(1));
            for (FullyQualifiedMemberNameAndDesc ref : reflectionRefList) {
                AnnotationNode refAnn = new AnnotationNode(refType);
                refAnn.visit("value", ref.getOwner().getInternalName());
                refAnn.visit("member", ref.getName());
                refAnn.visit("desc", ref.getDesc().toString());
                refAnn.visitEnd();
                refs.add(refAnn);
            }
            Handle lookup = provider.stubHandle(clazz, clinit, extra, null, null, enableRuntime, memberResolver, superTypeResolver, warnings,
                new Handle(
                    Opcodes.H_INVOKESTATIC,
                    handlesType.getInternalName(),
                    "lookup",
                    "()" + lookupType.getDescriptor(),
                    false
                )
            );
            clazz.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "jvmdg$lookup", lookupType.getDescriptor(), null, null);
            InsnList lookupInsns = new InsnList();
            lookupInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, lookup.getOwner(), lookup.getName(), lookup.getDesc(), false));
            lookupInsns.add(new FieldInsnNode(Opcodes.PUTSTATIC, clazz.name, "jvmdg$lookup", lookupType.getDescriptor()));

            addToClinit.insert(lookupInsns);
            clinit.instructions.insert(addToClinit);
        }
        return clazz;
    }

    @Override
    public int order() { return 0; }

}
