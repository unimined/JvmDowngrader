package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.asm.AnnotationUtils;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.RequiresResource;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class ReferenceGraph {
    private final Map<Type, References> references = new HashMap<>();

    public void scan(Path root, Filter filter) throws IOException {
        scan(preScan(root), filter);
    }

    public Map<Path, Type> preScan(final Path root) throws IOException {
        final Map<Path, Type> newScanTargets = new HashMap<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String path = root.relativize(file).toString();
                if (path.endsWith(".class")) {
                    path = path.substring(0, path.length() - 6);
                    Type type = Type.getObjectType(path);
                    if (!references.containsKey(type)) {
                        references.put(type, new References());
                        newScanTargets.put(file, type);
                    }

                }
                return FileVisitResult.CONTINUE;
            }
        });
        return newScanTargets;
    }

    public void scan(final Map<Path, Type> newScanTargets, Filter filter) throws IOException {
        for (Path path : newScanTargets.keySet()) {
            try (InputStream stream = Files.newInputStream(path)) {
                ClassNode node = ASMUtils.bytesToClassNode(Utils.readAllBytes(stream));
                Type type = Type.getObjectType(node.name);
                if (!type.equals(newScanTargets.get(path))) {
                    throw new IllegalStateException("Expected path to match class name");
                }
                references.get(type).scan(node, filter);
            }
        }
    }

    public void debugPrint() {
        for (Map.Entry<Type, References> entry : references.entrySet()) {
            System.out.println(entry.getKey().getInternalName());
            References refs = entry.getValue();
            for (Type t : refs.requiredInstances) {
                System.out.println("  " + t.getInternalName());
            }
            for (Map.Entry<MemberNameAndDesc, List<FullyQualifiedMemberNameAndDesc>> e : refs.requiredForMembers.entrySet()) {
                System.out.println("  " + e.getKey());
                for (FullyQualifiedMemberNameAndDesc f : e.getValue()) {
                    System.out.println("    " + f);
                }
            }
        }
    }

    public Set<FullyQualifiedMemberNameAndDesc> getAllRefs() {
        Set<FullyQualifiedMemberNameAndDesc> refs = new HashSet<>();
        for (References value : references.values()) {
            for (Type requiredInstance : value.requiredInstances) {
                refs.add(FullyQualifiedMemberNameAndDesc.ofType(requiredInstance));
            }
            for (List<FullyQualifiedMemberNameAndDesc> requiredForMembers : value.requiredForMembers.values()) {
                refs.addAll(requiredForMembers);
            }
        }
        return refs;
    }

    public Pair<Set<FullyQualifiedMemberNameAndDesc>, Set<String>> recursiveResolveFrom(Set<FullyQualifiedMemberNameAndDesc> starts) {
        Set<FullyQualifiedMemberNameAndDesc> refs = new HashSet<>(starts);
        Set<String> resources = new HashSet<>();
        Queue<FullyQualifiedMemberNameAndDesc> toAdd = new ArrayDeque<>(starts);
        while (!toAdd.isEmpty()) {
            FullyQualifiedMemberNameAndDesc next = toAdd.poll();
            References refer = references.get(next.getOwner());
            if (refer == null) continue;
            if (next.isClassRef()) {
                for (MemberNameAndDesc instanceMember : refer.instanceMembers) {
                    FullyQualifiedMemberNameAndDesc f = instanceMember.toFullyQualified(next.getOwner());
                    if (refs.add(f)) {
                        toAdd.add(f);
                    }
                }
                for (Type t : refer.requiredInstances) {
                    FullyQualifiedMemberNameAndDesc f = FullyQualifiedMemberNameAndDesc.ofType(t);
                    if (refs.add(f)) {
                        toAdd.add(f);
                    }
                }
            } else {
                List<FullyQualifiedMemberNameAndDesc> nextRefs = refer.requiredForMembers.get(next.toMemberNameAndDesc());
                if (nextRefs != null) {
                    for (FullyQualifiedMemberNameAndDesc f : nextRefs) {
                        if (refs.add(f)) {
                            toAdd.add(f);
                        }
                    }
                }
            }
        }
        for (FullyQualifiedMemberNameAndDesc ref : refs) {
            References refer = references.get(ref.getOwner());
            if (refer == null) continue;
            MemberNameAndDesc member = ref.toMemberNameAndDesc();
            if (refer.resourceList.containsKey(member)) {
                resources.addAll(Arrays.asList(refer.resourceList.get(member)));
            }
        }
        return new Pair<>(refs, resources);
    }

    public interface Filter {

        boolean shouldInclude(FullyQualifiedMemberNameAndDesc member);

    }

    private static class References {
        private List<Type> requiredInstances = new ArrayList<>();
        private Map<MemberNameAndDesc, List<FullyQualifiedMemberNameAndDesc>> requiredForMembers = new HashMap<>();
        private List<MemberNameAndDesc> instanceMembers = new ArrayList<>();
        private Map<MemberNameAndDesc, String[]> resourceList = new HashMap<>();

        public void scan(ClassNode classNode, Filter filter) {
            Type currentType = Type.getObjectType(classNode.name);
            // super
            Type superType = Type.getObjectType(classNode.superName);
            if (filter.shouldInclude(FullyQualifiedMemberNameAndDesc.ofType(superType))) {
                requiredInstances.add(superType);
            }
            // interfaces
            for (String s : classNode.interfaces) {
                Type interfaceType = Type.getObjectType(s);
                if (filter.shouldInclude(FullyQualifiedMemberNameAndDesc.ofType(interfaceType))) {
                    requiredInstances.add(interfaceType);
                }
            }

            boolean hasClinit = false;
            for (MethodNode method : classNode.methods) {
                if ("<clinit>".equals(method.name)) {
                    hasClinit = true;
                    break;
                }
            }

            // fields
            for (FieldNode field : classNode.fields) {
                MemberNameAndDesc fieldMember = new MemberNameAndDesc(field.name, Type.getType(field.desc));
                requiresInstance(fieldMember, Type.getType(field.desc), filter);
                if (field.value == null && hasClinit) {
                    requiresMember(fieldMember, new FullyQualifiedMemberNameAndDesc(currentType, "<clinit>", Type.getMethodType("()V")), filter);
                }
                if (!isStatic(field.access)) {
                    requiresInstance(fieldMember, currentType, filter);
                }

                if (field.invisibleAnnotations != null) {
                    for (AnnotationNode annotation : field.invisibleAnnotations) {
                        if (annotation.desc.equals(Type.getDescriptor(RequiresResource.class))) {
                            try {
                                RequiresResource resource = AnnotationUtils.createAnnotation(annotation);
                                resourceList.put(fieldMember, resource.value());
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }

            // methods
            for (MethodNode method : classNode.methods) {
                Type methodType = Type.getMethodType(method.desc);
                final MemberNameAndDesc methodMember = new MemberNameAndDesc(method.name, Type.getMethodType(method.desc));
                requiresInstance(methodMember, methodType, filter);
                if (!isStatic(method.access)) {
                    requiresInstance(methodMember, currentType, filter);
                    instanceMembers.add(methodMember);
                }

                if (method.invisibleAnnotations != null) {
                    for (AnnotationNode annotation : method.invisibleAnnotations) {
                        if (annotation.desc.equals(Type.getDescriptor(RequiresResource.class))) {
                            try {
                                RequiresResource resource = AnnotationUtils.createAnnotation(annotation);
                                resourceList.put(methodMember, resource.value());
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }

                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode min = (MethodInsnNode) insn;
                        requiresMember(methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(min.owner), min.name, Type.getMethodType(min.desc)), filter);
                    } else if (insn instanceof TypeInsnNode) {
                        TypeInsnNode tin = (TypeInsnNode) insn;
                        requiresInstance(methodMember, Type.getObjectType(tin.desc), filter);
                    } else if (insn instanceof FieldInsnNode) {
                        FieldInsnNode fin = (FieldInsnNode) insn;
                        requiresMember(methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(fin.owner), fin.name, Type.getType(fin.desc)), filter);
                    } else if (insn instanceof InvokeDynamicInsnNode) {
                        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                        requiresInstance(methodMember, Type.getMethodType(indy.desc), filter);
                        Handle bsm = indy.bsm;
                        requiresMember(methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(bsm.getOwner()), bsm.getName(), Type.getType(bsm.getDesc())), filter);
                        for (Object arg : indy.bsmArgs) {
                            if (arg instanceof Type) {
                                requiresInstance(methodMember, (Type) arg, filter);
                            } else if (arg instanceof Handle) {
                                Handle h = (Handle) arg;
                                requiresMember(methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(h.getOwner()), h.getName(), Type.getType(h.getDesc())), filter);
                            } else if (arg instanceof ConstantDynamic) {
                                requiresCondy(methodMember, (ConstantDynamic) arg, filter);
                            }
                        }
                    } else if (insn instanceof MultiANewArrayInsnNode) {
                        MultiANewArrayInsnNode mana = (MultiANewArrayInsnNode) insn;
                        requiresInstance(methodMember, Type.getType(mana.desc), filter);
                    } else if (insn instanceof LdcInsnNode) {
                        LdcInsnNode ldc = (LdcInsnNode) insn;
                        if (ldc.cst instanceof Type) {
                            requiresInstance(methodMember, (Type) ldc.cst, filter);
                        } else if (ldc.cst instanceof ConstantDynamic) {
                            requiresCondy(methodMember, (ConstantDynamic) ldc.cst, filter);
                        }
                    }
                }
            }
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private boolean isStatic(int access) {
            return (access & Opcodes.ACC_STATIC) != 0;
        }

        private void requiresCondy(MemberNameAndDesc member, ConstantDynamic condy, Filter filter) {
            Handle bsm = condy.getBootstrapMethod();
            requiresMember(member, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(bsm.getOwner()), bsm.getName(), Type.getType(bsm.getDesc())), filter);
            for (int i = 0; i < condy.getBootstrapMethodArgumentCount(); i++) {
                Object o = condy.getBootstrapMethodArgument(i);
                if (o instanceof Type) {
                    requiresInstance(member, (Type) o, filter);
                } if (o instanceof Handle) {
                    Handle h = (Handle) o;
                    requiresMember(member, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(h.getOwner()), h.getName(), Type.getType(h.getDesc())), filter);
                } if (o instanceof ConstantDynamic) {
                    requiresCondy(member, (ConstantDynamic) o, filter);
                }
            }
        }

        private void requiresInstance(MemberNameAndDesc member, Type required, Filter filter) {
            List<FullyQualifiedMemberNameAndDesc> list = requiredForMembers.get(member);
            FullyQualifiedMemberNameAndDesc type;
            switch (required.getSort()) {
                case Type.ARRAY:
                    type = FullyQualifiedMemberNameAndDesc.ofType(required.getElementType());
                    if (filter.shouldInclude(type)) {
                        if (list == null) {
                            list = new ArrayList<>();
                            requiredForMembers.put(member, list);
                        }
                        list.add(type);
                    }
                    break;
                case Type.OBJECT:
                    type = FullyQualifiedMemberNameAndDesc.ofType(required);
                    if (filter.shouldInclude(type)) {
                        if (list == null) {
                            list = new ArrayList<>();
                            requiredForMembers.put(member, list);
                        }
                        list.add(type);
                    }
                    break;
                case Type.METHOD:
                    requiresInstance(member, required.getReturnType(), filter);
                    for (Type t : required.getArgumentTypes()) {
                        requiresInstance(member, t, filter);
                    }
                    break;
                default:
                    break;
            }
        }

        private void requiresMember(MemberNameAndDesc member, FullyQualifiedMemberNameAndDesc required, Filter filter) {
            List<FullyQualifiedMemberNameAndDesc> list = requiredForMembers.get(member);
            if (filter.shouldInclude(required)) {
                if (list == null) {
                    list = new ArrayList<>();
                    requiredForMembers.put(member, list);
                }
                list.add(required);
            }
        }

    }

}
