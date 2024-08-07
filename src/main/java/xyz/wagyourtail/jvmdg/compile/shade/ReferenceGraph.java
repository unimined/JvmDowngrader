package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.asm.AnnotationUtils;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.*;
import xyz.wagyourtail.jvmdg.version.RequiresResource;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class ReferenceGraph {
    private final Logger logger;
    private final Map<Type, References> references = new ConcurrentHashMap<>();
    private final Map<Type, ClassNode> classNodes = new ConcurrentHashMap<>();
    private final boolean retainClassNodes;
    private final boolean retainInsns;

    public ReferenceGraph(Logger logger) {
        this.logger = logger.subLogger(ReferenceGraph.class);
        retainClassNodes = false;
        retainInsns = false;
    }

    public ReferenceGraph(Logger logger, boolean retainClassNodes) {
        this.logger = logger.subLogger(ReferenceGraph.class);
        this.retainClassNodes = retainClassNodes;
        this.retainInsns = false;
    }

    public ReferenceGraph(Logger logger, boolean retainClassNodes, boolean retainInsns) {
        this.logger = logger.subLogger(ReferenceGraph.class);
        this.retainClassNodes = retainClassNodes;
        this.retainInsns = retainInsns;
        if (retainInsns && !retainClassNodes) {
            throw new IllegalArgumentException("Cannot retain instructions without retaining class nodes.");
        }
    }


    /**
     * if the {@link ClassNode} is retained, this will return the {@link ClassNode} for the given type.
     * @return the {@link ClassNode} for the given type.
     */
    public ClassNode getClassFor(Type type) {
        if (!retainClassNodes) {
            throw new IllegalStateException();
        }
        return classNodes.get(type);
    }

    public void scan(Path root, Filter filter) throws IOException, ExecutionException, InterruptedException {
        scan(preScan(root), filter);
    }

    /**
     * Pre scan the root directory to find all the classes that need to be scanned.
     * @return a map of paths to types that need to be scanned.
     */
    public Map<Path, Type> preScan(final Path root) throws IOException, ExecutionException, InterruptedException {
        final Map<Path, Type> newScanTargets = new ConcurrentHashMap<>();
        AsyncUtils.visitPathsAsync(root, new IOFunction<Path, Boolean>() {
            @Override
            public Boolean apply(Path path) throws IOException {
                // exclude META-INF/versions from scanning
                // TODO: reconsider
                return !root.relativize(path).toString().startsWith("META-INF/versions");
            }
        }, new IOConsumer<Path>() {
            @Override
            public void accept(Path path) throws IOException {
                // skip module info, it doesn't have references we care about
                if (path.getFileName().toString().equals("module-info.class")) {
                    return;
                }
                String pathStr = root.relativize(path).toString();
                if (pathStr.endsWith(".class")) {
                    pathStr = pathStr.substring(0, pathStr.length() - 6);
                    Type type = Type.getObjectType(pathStr);
                    if (!references.containsKey(type)) {
                        references.put(type, new References());
                        newScanTargets.put(path, type);
                    }

                }
            }
        }).get();
        return newScanTargets;
    }

    /**
     * Scans the path references in order to determine the reference graphs for the classes.
     *
     * @param newScanTargets the paths to scan.
     * @param filter the filter to determine if a reference should be retained.
     */
    public void scan(final Map<Path, Type> newScanTargets, final Filter filter) throws IOException, ExecutionException, InterruptedException {
        AsyncUtils.forEachAsync(newScanTargets.keySet(), new IOConsumer<Path>() {
            @Override
            public void accept(Path path) throws IOException {
                try (InputStream stream = Files.newInputStream(path)) {
                    ClassNode node = ASMUtils.bytesToClassNode(Utils.readAllBytes(stream));
                    Type type = Type.getObjectType(node.name);
                    if (!type.equals(newScanTargets.get(path))) {
                        throw new IllegalStateException("Expected path to match class name: " + path + " != " + type.getInternalName());
                    }
                    if (retainClassNodes) {
                        classNodes.put(type, node);
                    }
                    references.get(type).scan(node, filter);
                }
            }
        }).get();
    }

    public void debugPrint() {
        if (!logger.is(Logger.Level.DEBUG)) return;
        logger.debug("Reference Graph:");
        for (Map.Entry<Type, References> entry : references.entrySet()) {
            logger.debug("* " + entry.getKey().getInternalName());
            References refs = entry.getValue();
            for (Type t : refs.requiredInstances) {
                System.out.println("  - " + t.getInternalName());
            }
            for (Map.Entry<MemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> e : refs.requiredForMembers.entrySet()) {
                System.out.println("  - " + e.getKey());
                for (FullyQualifiedMemberNameAndDesc f : e.getValue()) {
                    System.out.println("      " + f);
                }
            }
        }
    }

    public Set<Type> getKeys() {
        return references.keySet();
    }

    /**
     * @return all retained references from the graph.
     */
    public Set<FullyQualifiedMemberNameAndDesc> getAllRefs() {
        Set<FullyQualifiedMemberNameAndDesc> refs = new HashSet<>();
        for (References value : references.values()) {
            for (Type requiredInstance : value.requiredInstances) {
                refs.add(FullyQualifiedMemberNameAndDesc.of(requiredInstance));
            }
            for (Set<FullyQualifiedMemberNameAndDesc> requiredForMembers : value.requiredForMembers.values()) {
                refs.addAll(requiredForMembers);
            }
        }
        return refs;
    }

    /**
     * @return all retained references from the graph, and where they came from.
     */
    public Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> getAllUsagesForRefs() {
        Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> refs = new HashMap<>();
        for (Map.Entry<Type, References> entry : references.entrySet()) {
            Type owner = entry.getKey();
            References value = entry.getValue();
            for (Type requiredInstance : value.requiredInstances) {
                FullyQualifiedMemberNameAndDesc ref = FullyQualifiedMemberNameAndDesc.of(requiredInstance);
                if (!refs.containsKey(ref)) {
                    refs.put(ref, new HashSet<FullyQualifiedMemberNameAndDesc>());
                }
                refs.get(ref).add(FullyQualifiedMemberNameAndDesc.of(owner));
            }
            for (Map.Entry<MemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> e : value.requiredForMembers.entrySet()) {
                FullyQualifiedMemberNameAndDesc target = e.getKey().toFullyQualified(owner);
                for (FullyQualifiedMemberNameAndDesc ref : e.getValue()) {
                    if (!refs.containsKey(ref)) {
                        refs.put(ref, new HashSet<FullyQualifiedMemberNameAndDesc>());
                    }
                    refs.get(ref).add(target);
                }
            }
        }
        return refs;
    }

    public void debugPrintUsageRefs(Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> refs) {
        if (!logger.is(Logger.Level.DEBUG)) return;
        logger.debug("Usage Refs:");
        for (Map.Entry<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> entry : refs.entrySet()) {
            logger.debug("- " + entry.getKey());
            for (FullyQualifiedMemberNameAndDesc f : entry.getValue()) {
                logger.debug("  * " + f);
            }
        }
    }

    /**
     * Given a set of references, this will scan the current reference graph to determine which references are required
     * by the given references.
     *
     * @param starts the references to start from.
     * @return a pair of the references required by the given references, and the resources required by the given references.
     */
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
                    FullyQualifiedMemberNameAndDesc f = FullyQualifiedMemberNameAndDesc.of(t);
                    if (refs.add(f)) {
                        toAdd.add(f);
                    }
                }
            } else {
                Set<FullyQualifiedMemberNameAndDesc> nextRefs = refer.requiredForMembers.get(next.toMemberNameAndDesc());
                if (nextRefs != null) {
                    for (FullyQualifiedMemberNameAndDesc f : nextRefs) {
                        if (refs.add(f)) {
                            toAdd.add(f);
                        }
                    }
                }
            }
        }
        // aggregate resources required
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

    /**
     * Given a set of references, this will scan the current reference graph to determine which references are required
     * by the given references, and where they came from.
     * @param starts the references to start from.
     * @return a pair of the references required by the given references, and the resources required by the given references.
     */
    public Pair<Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>, Set<String>> recursivelyResolveUsagesFrom(Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> starts) {
        Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> usages = new HashMap<>(starts);
        Set<String> resources = new HashSet<>();
        Queue<FullyQualifiedMemberNameAndDesc> toScan = new ArrayDeque<>(starts.keySet());
        while (!toScan.isEmpty()) {
            FullyQualifiedMemberNameAndDesc next = toScan.poll();
            References refer = references.get(next.getOwner());
            if (refer == null) continue;
            if (next.isClassRef()) {
                for (MemberNameAndDesc instanceMember : refer.instanceMembers) {
                    FullyQualifiedMemberNameAndDesc f = instanceMember.toFullyQualified(next.getOwner());
                    if (!usages.containsKey(f)) {
                        usages.put(f, new HashSet<FullyQualifiedMemberNameAndDesc>());
                        toScan.add(f);
                    }
                    usages.get(f).add(next);
                }
                for (Type t : refer.requiredInstances) {
                    FullyQualifiedMemberNameAndDesc f = FullyQualifiedMemberNameAndDesc.of(t);
                    if (!usages.containsKey(f)) {
                        usages.put(f, new HashSet<FullyQualifiedMemberNameAndDesc>());
                        toScan.add(f);
                    }
                    usages.get(f).add(next);
                }
            } else {
                Set<FullyQualifiedMemberNameAndDesc> nextRefs = refer.requiredForMembers.get(next.toMemberNameAndDesc());
                if (nextRefs != null) {
                    for (FullyQualifiedMemberNameAndDesc f : nextRefs) {
                        if (!usages.containsKey(f)) {
                            usages.put(f, new HashSet<FullyQualifiedMemberNameAndDesc>());
                            toScan.add(f);
                        }
                        usages.get(f).add(next);
                    }
                }
            }
        }
        // aggregate resources required
        for (Map.Entry<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> entry : usages.entrySet()) {
            FullyQualifiedMemberNameAndDesc ref = entry.getKey();
            References refer = references.get(ref.getOwner());
            if (refer == null) continue;
            MemberNameAndDesc member = ref.toMemberNameAndDesc();
            if (refer.resourceList.containsKey(member)) {
                resources.addAll(Arrays.asList(refer.resourceList.get(member)));
            }
        }
        return new Pair<>(usages, resources);
    }

    /**
     * Given a reference, this will scan the current reference graph for all instructions that reference the given reference.
     */
    public Set<AbstractInsnNode> getAllInsnsFor(FullyQualifiedMemberNameAndDesc ref) {
        if (!retainInsns) {
            throw new IllegalStateException("Insns are not retained.");
        }
        Set<AbstractInsnNode> insns = new HashSet<>();
        for (References value : references.values()) {
            Set<AbstractInsnNode> ins = value.memberInsns.get(ref);
            if (ins != null) {
                insns.addAll(ins);
            }
        }
        return insns;
    }

    /**
     * A filter to determine if a reference should be retained.
     */
    public interface Filter {

        boolean shouldInclude(FullyQualifiedMemberNameAndDesc member);

    }

    private class References {
        /**
         * The required classes to construct the class.
         */
        private final List<Type> requiredInstances = new ArrayList<>();
        /**
         * The required references for each member of the class.
         */
        private final Map<MemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> requiredForMembers = new HashMap<>();

        /**
         * if retainInsns is true, this will retain the instructions for each member referenced by the class.
         */
        private final Map<FullyQualifiedMemberNameAndDesc, Set<AbstractInsnNode>> memberInsns = new HashMap<>();

        /**
         * The list of instance members in the class.
         */
        private final List<MemberNameAndDesc> instanceMembers = new ArrayList<>();
        /**
         * The required resources for each member of the class.
         * this is determined by the {@link RequiresResource} annotation.
         */
        private final Map<MemberNameAndDesc, String[]> resourceList = new HashMap<>();

        public void scan(ClassNode classNode, Filter filter) {
            Type currentType = Type.getObjectType(classNode.name);
            // super
            Type superType = Type.getObjectType(classNode.superName);
            if (filter.shouldInclude(FullyQualifiedMemberNameAndDesc.of(superType))) {
                requiredInstances.add(superType);
            }
            // interfaces
            for (String s : classNode.interfaces) {
                Type interfaceType = Type.getObjectType(s);
                if (filter.shouldInclude(FullyQualifiedMemberNameAndDesc.of(interfaceType))) {
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
                    requiresMember(fieldMember, new FullyQualifiedMemberNameAndDesc(currentType, "<clinit>", Type.getMethodType("()V")), filter, null);
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
                        requiresMember(methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(min.owner), min.name, Type.getMethodType(min.desc)), filter, insn);
                    } else if (insn instanceof TypeInsnNode) {
                        TypeInsnNode tin = (TypeInsnNode) insn;
                        requiresInstance(methodMember, Type.getObjectType(tin.desc), filter);
                    } else if (insn instanceof FieldInsnNode) {
                        FieldInsnNode fin = (FieldInsnNode) insn;
                        requiresMember(methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(fin.owner), fin.name, Type.getType(fin.desc)), filter, insn);
                    } else if (insn instanceof InvokeDynamicInsnNode) {
                        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                        requiresInstance(methodMember, Type.getMethodType(indy.desc), filter);
                        Handle bsm = indy.bsm;
                        requiresMember(methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(bsm.getOwner()), bsm.getName(), Type.getType(bsm.getDesc())), filter, insn);
                        for (Object arg : indy.bsmArgs) {
                            if (arg instanceof Type) {
                                requiresInstance(methodMember, (Type) arg, filter);
                            } else if (arg instanceof Handle) {
                                Handle h = (Handle) arg;
                                requiresMember(methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(h.getOwner()), h.getName(), Type.getType(h.getDesc())), filter, insn);
                            } else if (arg instanceof ConstantDynamic) {
                                requiresCondy(methodMember, (ConstantDynamic) arg, filter, insn);
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
                            requiresCondy(methodMember, (ConstantDynamic) ldc.cst, filter, insn);
                        }
                    }
                }
            }
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private boolean isStatic(int access) {
            return (access & Opcodes.ACC_STATIC) != 0;
        }

        private void requiresCondy(MemberNameAndDesc member, ConstantDynamic condy, Filter filter, AbstractInsnNode insn) {
            Handle bsm = condy.getBootstrapMethod();
            requiresMember(member, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(bsm.getOwner()), bsm.getName(), Type.getType(bsm.getDesc())), filter, insn);
            for (int i = 0; i < condy.getBootstrapMethodArgumentCount(); i++) {
                Object o = condy.getBootstrapMethodArgument(i);
                if (o instanceof Type) {
                    requiresInstance(member, (Type) o, filter);
                }
                if (o instanceof Handle) {
                    Handle h = (Handle) o;
                    requiresMember(member, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(h.getOwner()), h.getName(), Type.getType(h.getDesc())), filter, insn);
                }
                if (o instanceof ConstantDynamic) {
                    requiresCondy(member, (ConstantDynamic) o, filter, insn);
                }
            }
        }

        private void requiresInstance(MemberNameAndDesc member, Type required, Filter filter) {
            Set<FullyQualifiedMemberNameAndDesc> list = requiredForMembers.get(member);
            FullyQualifiedMemberNameAndDesc type;
            switch (required.getSort()) {
                case Type.ARRAY:
                    type = FullyQualifiedMemberNameAndDesc.of(required.getElementType());
                    if (filter.shouldInclude(type)) {
                        if (list == null) {
                            list = new HashSet<>();
                            requiredForMembers.put(member, list);
                        }
                        list.add(type);
                    }
                    break;
                case Type.OBJECT:
                    type = FullyQualifiedMemberNameAndDesc.of(required);
                    if (filter.shouldInclude(type)) {
                        if (list == null) {
                            list = new HashSet<>();
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

        private void requiresMember(MemberNameAndDesc member, FullyQualifiedMemberNameAndDesc required, Filter filter, AbstractInsnNode insn) {
            Set<FullyQualifiedMemberNameAndDesc> list = requiredForMembers.get(member);
            if (filter.shouldInclude(required)) {
                if (list == null) {
                    list = new HashSet<>();
                    requiredForMembers.put(member, list);
                }
                list.add(required);
                if (retainInsns && insn != null) {
                    Set<AbstractInsnNode> insns = memberInsns.get(required);
                    if (insns == null) {
                        insns = new HashSet<>();
                        memberInsns.put(required, insns);
                    }
                    insns.add(insn);
                }
            }
        }

    }

}
