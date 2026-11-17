package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The stock {@link RefExtractor}: every reference shape reachable from a
 * {@link ClassNode} — supertypes, signatures, annotations (including nested
 * values and enum constants), class metadata (inner/outer/nest/permitted),
 * record components, member descriptors/exceptions/try-catch, every
 * instruction kind, invokedynamic + condy bootstrap graphs.
 * <p>
 * Semantic extraction beyond the bytecode shapes is delegated to
 * {@link MemberScanner} hooks run once per class/field/method — jvmdg's
 * {@code @RequiresResource} and {@code @ReflectionReferences} are the two
 * stock scanners; {@link #MemberRefExtractor(boolean, List)} registers more
 * (annotation walkers, instruction-pattern scans, consumer conventions).
 * <p>
 * Non-static members additionally record a requirement on the owning class
 * itself (an instance must exist to reach them); non-static methods are
 * listed in {@link ExtractedRefs#instanceMembers} — the expansion set a
 * class ref pulls on closure. A non-constant field in a class with a
 * {@code <clinit>} records a requirement on {@code <clinit>} (lazy init).
 */
public final class MemberRefExtractor implements RefExtractor {

    private final boolean retainInsns;
    private final List<MemberScanner> memberScanners = new ArrayList<MemberScanner>();

    /** The two stock scanners: {@code @RequiresResource}, {@code @ReflectionReferences}. */
    public MemberRefExtractor(boolean retainInsns) {
        this(retainInsns, null);
    }

    /**
     * Additional member scanners appended after the stock two — to compose
     * differently (e.g. drop the jvmdg annotations), build a custom
     * {@link RefExtractor} instead.
     */
    public MemberRefExtractor(boolean retainInsns, List<MemberScanner> additionalScanners) {
        this.retainInsns = retainInsns;
        memberScanners.add(new RequiresResourceScanner());
        memberScanners.add(new ReflectionReferencesScanner());
        if (additionalScanners != null) {
            memberScanners.addAll(additionalScanners);
        }
    }

    @Override
    public ExtractedRefs extract(ClassNode classNode, Filter filter) {
        ExtractedRefs out = new ExtractedRefs();
        scan(classNode, out, filter);
        return out;
    }

    private void scan(ClassNode classNode, ExtractedRefs out, Filter filter) {
        Type currentType = Type.getObjectType(classNode.name);
        // super
        requiresInstance(out, null, Type.getObjectType(classNode.superName), filter);
        // interfaces
        for (String s : classNode.interfaces) {
            requiresInstance(out, null, Type.getObjectType(s), filter);
        }

        // generic signature (type params/args reference class types)
        scanSignature(out, classNode.signature, null, filter);

        // inner/outer/nest/permitted class metadata
        if (classNode.innerClasses != null) {
            for (InnerClassNode inner : classNode.innerClasses) {
                if (inner.name != null) {
                    requiresInstance(out, null, Type.getObjectType(inner.name), filter);
                }
                if (inner.outerName != null) {
                    requiresInstance(out, null, Type.getObjectType(inner.outerName), filter);
                }
            }
        }
        if (classNode.outerClass != null) {
            requiresInstance(out, null, Type.getObjectType(classNode.outerClass), filter);
        }
        if (classNode.outerMethod != null) {
            requiresInstance(out, null, Type.getObjectType(classNode.outerMethod), filter);
        }
        if (classNode.outerMethodDesc != null) {
            requiresInstance(out, null, Type.getMethodType(classNode.outerMethodDesc), filter);
        }
        if (classNode.nestHostClass != null) {
            requiresInstance(out, null, Type.getObjectType(classNode.nestHostClass), filter);
        }
        if (classNode.nestMembers != null) {
            for (String nestMember : classNode.nestMembers) {
                requiresInstance(out, null, Type.getObjectType(nestMember), filter);
            }
        }
        if (classNode.permittedSubclasses != null) {
            for (String permitted : classNode.permittedSubclasses) {
                requiresInstance(out, null, Type.getObjectType(permitted), filter);
            }
        }

        // class-level annotations (types and values can reference classes)
        scanAnnotations(out, classNode.visibleAnnotations, null, filter);
        scanAnnotations(out, classNode.invisibleAnnotations, null, filter);
        scanAnnotations(out, classNode.visibleTypeAnnotations, null, filter);
        scanAnnotations(out, classNode.invisibleTypeAnnotations, null, filter);

        // record components
        if (classNode.recordComponents != null) {
            for (RecordComponentNode rc : classNode.recordComponents) {
                requiresInstance(out, null, Type.getType(rc.descriptor), filter);
                scanSignature(out, rc.signature, null, filter);
                scanAnnotations(out, rc.visibleAnnotations, null, filter);
                scanAnnotations(out, rc.invisibleAnnotations, null, filter);
                scanAnnotations(out, rc.visibleTypeAnnotations, null, filter);
                scanAnnotations(out, rc.invisibleTypeAnnotations, null, filter);
            }
        }

        boolean hasClinit = false;
        for (MethodNode method : classNode.methods) {
            if ("<clinit>".equals(method.name)) {
                hasClinit = true;
                break;
            }
        }

        for (MemberScanner scanner : memberScanners) {
            scanner.scanClass(classNode, new Sink(out, null, filter));
        }

        // fields
        for (FieldNode field : classNode.fields) {
            MemberNameAndDesc fieldMember = new MemberNameAndDesc(field.name, Type.getType(field.desc));
            requiresInstance(out, fieldMember, Type.getType(field.desc), filter);
            if (field.value == null && hasClinit) {
                requiresMember(out, fieldMember, new FullyQualifiedMemberNameAndDesc(currentType, "<clinit>", Type.getMethodType("()V")), filter, null);
            }
            if (!isStatic(field.access)) {
                requiresInstance(out, fieldMember, currentType, filter);
            }

            scanSignature(out, field.signature, fieldMember, filter);
            scanAnnotations(out, field.visibleAnnotations, fieldMember, filter);
            scanAnnotations(out, field.invisibleAnnotations, fieldMember, filter);
            scanAnnotations(out, field.visibleTypeAnnotations, fieldMember, filter);
            scanAnnotations(out, field.invisibleTypeAnnotations, fieldMember, filter);

            for (MemberScanner scanner : memberScanners) {
                scanner.scanField(field, fieldMember, new Sink(out, fieldMember, filter));
            }
        }

        // methods
        for (MethodNode method : classNode.methods) {
            Type methodType = Type.getMethodType(method.desc);
            final MemberNameAndDesc methodMember = new MemberNameAndDesc(method.name, Type.getMethodType(method.desc));
            requiresInstance(out, methodMember, methodType, filter);
            if (!isStatic(method.access)) {
                requiresInstance(out, methodMember, currentType, filter);
                out.instanceMembers.add(methodMember);
            }

            scanSignature(out, method.signature, methodMember, filter);
            if (method.exceptions != null) {
                for (String exception : method.exceptions) {
                    requiresInstance(out, methodMember, Type.getObjectType(exception), filter);
                }
            }
            scanAnnotations(out, method.visibleAnnotations, methodMember, filter);
            scanAnnotations(out, method.invisibleAnnotations, methodMember, filter);
            scanAnnotations(out, method.visibleTypeAnnotations, methodMember, filter);
            scanAnnotations(out, method.invisibleTypeAnnotations, methodMember, filter);
            if (method.visibleParameterAnnotations != null) {
                for (List<AnnotationNode> parameterAnnotations : method.visibleParameterAnnotations) {
                    scanAnnotations(out, parameterAnnotations, methodMember, filter);
                }
            }
            if (method.invisibleParameterAnnotations != null) {
                for (List<AnnotationNode> parameterAnnotations : method.invisibleParameterAnnotations) {
                    scanAnnotations(out, parameterAnnotations, methodMember, filter);
                }
            }
            if (method.annotationDefault != null) {
                scanAnnotationValue(out, method.annotationDefault, methodMember, filter);
            }
            if (method.tryCatchBlocks != null) {
                for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
                    if (tryCatch.type != null) {
                        requiresInstance(out, methodMember, Type.getObjectType(tryCatch.type), filter);
                    }
                    scanAnnotations(out, tryCatch.visibleTypeAnnotations, methodMember, filter);
                    scanAnnotations(out, tryCatch.invisibleTypeAnnotations, methodMember, filter);
                }
            }

            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    requiresMember(out, methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(min.owner), min.name, Type.getMethodType(min.desc)), filter, insn);
                } else if (insn instanceof TypeInsnNode) {
                    TypeInsnNode tin = (TypeInsnNode) insn;
                    requiresInstance(out, methodMember, Type.getObjectType(tin.desc), filter);
                } else if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fin = (FieldInsnNode) insn;
                    requiresMember(out, methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(fin.owner), fin.name, Type.getType(fin.desc)), filter, insn);
                } else if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                    requiresInstance(out, methodMember, Type.getMethodType(indy.desc), filter);
                    Handle bsm = indy.bsm;
                    requiresMember(out, methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(bsm.getOwner()), bsm.getName(), Type.getType(bsm.getDesc())), filter, insn);
                    for (Object arg : indy.bsmArgs) {
                        if (arg instanceof Type) {
                            requiresInstance(out, methodMember, (Type) arg, filter);
                        } else if (arg instanceof Handle) {
                            Handle h = (Handle) arg;
                            requiresMember(out, methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(h.getOwner()), h.getName(), Type.getType(h.getDesc())), filter, insn);
                        } else if (arg instanceof ConstantDynamic) {
                            requiresCondy(out, methodMember, (ConstantDynamic) arg, filter, insn);
                        }
                    }
                } else if (insn instanceof MultiANewArrayInsnNode) {
                    MultiANewArrayInsnNode mana = (MultiANewArrayInsnNode) insn;
                    requiresInstance(out, methodMember, Type.getType(mana.desc), filter);
                } else if (insn instanceof LdcInsnNode) {
                    LdcInsnNode ldc = (LdcInsnNode) insn;
                    if (ldc.cst instanceof Type) {
                        requiresInstance(out, methodMember, (Type) ldc.cst, filter);
                    } else if (ldc.cst instanceof ConstantDynamic) {
                        requiresCondy(out, methodMember, (ConstantDynamic) ldc.cst, filter, insn);
                    } else if (ldc.cst instanceof Handle) {
                        Handle h = (Handle) ldc.cst;
                        requiresMember(out, methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(h.getOwner()), h.getName(), Type.getType(h.getDesc())), filter, insn);
                    }
                }
            }

            for (MemberScanner scanner : memberScanners) {
                scanner.scanMethod(method, methodMember, new Sink(out, methodMember, filter));
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    private void requiresCondy(ExtractedRefs out, MemberNameAndDesc member, ConstantDynamic condy, Filter filter, AbstractInsnNode insn) {
        requiresInstance(out, member, Type.getType(condy.getDescriptor()), filter);
        Handle bsm = condy.getBootstrapMethod();
        requiresMember(out, member, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(bsm.getOwner()), bsm.getName(), Type.getType(bsm.getDesc())), filter, insn);
        for (int i = 0; i < condy.getBootstrapMethodArgumentCount(); i++) {
            Object o = condy.getBootstrapMethodArgument(i);
            if (o instanceof Type) {
                requiresInstance(out, member, (Type) o, filter);
            }
            if (o instanceof Handle) {
                Handle h = (Handle) o;
                requiresMember(out, member, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(h.getOwner()), h.getName(), Type.getType(h.getDesc())), filter, insn);
            }
            if (o instanceof ConstantDynamic) {
                requiresCondy(out, member, (ConstantDynamic) o, filter, insn);
            }
        }
    }

    private void requiresInstance(ExtractedRefs out, MemberNameAndDesc member, Type required, Filter filter) {
        switch (required.getSort()) {
            case Type.ARRAY:
                requiresInstance(out, member, required.getElementType(), filter);
                break;
            case Type.OBJECT: {
                FullyQualifiedMemberNameAndDesc type = FullyQualifiedMemberNameAndDesc.of(required);
                if (filter.shouldInclude(type)) {
                    if (member == null) {
                        out.requiredInstances.add(required);
                    } else {
                        Set<FullyQualifiedMemberNameAndDesc> list = out.requiredForMembers.get(member);
                        if (list == null) {
                            list = new HashSet<FullyQualifiedMemberNameAndDesc>();
                            out.requiredForMembers.put(member, list);
                        }
                        list.add(type);
                    }
                }
                break;
            }
            case Type.METHOD:
                requiresInstance(out, member, required.getReturnType(), filter);
                for (Type t : required.getArgumentTypes()) {
                    requiresInstance(out, member, t, filter);
                }
                break;
            default:
                break;
        }
    }

    private void scanAnnotations(ExtractedRefs out, List<? extends AnnotationNode> annotations, MemberNameAndDesc member, Filter filter) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            requiresInstance(out, member, Type.getType(annotation.desc), filter);
            if (annotation.values != null) {
                // values are stored as (name, value) pairs
                for (int i = 1; i < annotation.values.size(); i += 2) {
                    scanAnnotationValue(out, annotation.values.get(i), member, filter);
                }
            }
        }
    }

    /** Binds one element (member + record + filter) for scanner callbacks. */
    private final class Sink implements MemberScanner.MemberSink {

        private final ExtractedRefs out;
        private final MemberNameAndDesc member;
        private final Filter filter;

        private Sink(ExtractedRefs out, MemberNameAndDesc member, Filter filter) {
            this.out = out;
            this.member = member;
            this.filter = filter;
        }

        @Override
        public MemberNameAndDesc member() {
            return member;
        }

        @Override
        public void requiresInstance(Type type) {
            MemberRefExtractor.this.requiresInstance(out, member, type, filter);
        }

        @Override
        public void requiresMember(FullyQualifiedMemberNameAndDesc ref) {
            MemberRefExtractor.this.requiresMember(out, member, ref, filter, null);
        }

        @Override
        public void requiresMember(FullyQualifiedMemberNameAndDesc ref, AbstractInsnNode insn) {
            MemberRefExtractor.this.requiresMember(out, member, ref, filter, insn);
        }

        @Override
        public void requiresResource(String... resources) {
            String[] existing = out.resourceList.get(member);
            if (existing == null) {
                out.resourceList.put(member, resources);
            } else {
                String[] merged = Arrays.copyOf(existing, existing.length + resources.length);
                System.arraycopy(resources, 0, merged, existing.length, resources.length);
                out.resourceList.put(member, merged);
            }
        }

    }

    @SuppressWarnings("unchecked")
    private void scanAnnotationValue(ExtractedRefs out, Object value, MemberNameAndDesc member, Filter filter) {
        if (value instanceof Type) {
            requiresInstance(out, member, (Type) value, filter);
        } else if (value instanceof AnnotationNode) {
            AnnotationNode nested = (AnnotationNode) value;
            requiresInstance(out, member, Type.getType(nested.desc), filter);
            if (nested.values != null) {
                for (int i = 1; i < nested.values.size(); i += 2) {
                    scanAnnotationValue(out, nested.values.get(i), member, filter);
                }
            }
        } else if (value instanceof List) {
            for (Object o : (List<Object>) value) {
                scanAnnotationValue(out, o, member, filter);
            }
        } else if (value instanceof String[]) {
            // enum constant reference: {desc, name}
            requiresInstance(out, member, Type.getType(((String[]) value)[0]), filter);
        }
    }

    private void scanSignature(final ExtractedRefs out, String signature, final MemberNameAndDesc member, final Filter filter) {
        if (signature == null) return;
        new SignatureReader(signature).accept(new SignatureVisitor(Opcodes.ASM9) {
            @Override
            public void visitClassType(String name) {
                requiresInstance(out, member, Type.getObjectType(name), filter);
            }

            @Override
            public void visitInnerClassType(String name) {
                requiresInstance(out, member, Type.getObjectType(name), filter);
            }
        });
    }

    private void requiresMember(ExtractedRefs out, MemberNameAndDesc member, FullyQualifiedMemberNameAndDesc required, Filter filter, AbstractInsnNode insn) {
        Set<FullyQualifiedMemberNameAndDesc> list = out.requiredForMembers.get(member);
        if (filter.shouldInclude(required)) {
            if (list == null) {
                list = new HashSet<FullyQualifiedMemberNameAndDesc>();
                out.requiredForMembers.put(member, list);
            }
            list.add(required);
            if (retainInsns && insn != null) {
                Set<AbstractInsnNode> insns = out.memberInsns.get(required);
                if (insns == null) {
                    insns = new HashSet<AbstractInsnNode>();
                    out.memberInsns.put(required, insns);
                }
                insns.add(insn);
            }
        }
    }

}
