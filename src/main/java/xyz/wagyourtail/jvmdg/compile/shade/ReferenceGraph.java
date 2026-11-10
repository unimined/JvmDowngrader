package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.asm.AnnotationUtils;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.AsyncUtils;
import xyz.wagyourtail.jvmdg.util.IOConsumer;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.ReflectionReferences;
import xyz.wagyourtail.jvmdg.version.RequiresResource;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;

public class ReferenceGraph {
    private final Logger logger;
    private final Set<Integer> versions = new ConcurrentSkipListSet<>();
    private final ConcurrentMap<Type, ConcurrentMap<Integer, References>> references = new ConcurrentHashMap<>();
    private final ConcurrentMap<Type, ConcurrentMap<Integer, ClassNode>> classNodes = new ConcurrentHashMap<>();
    private final boolean retainClassNodes;
    private final boolean retainInsns;

    public ReferenceGraph(Logger logger) {
        this(logger, false);
    }

    public ReferenceGraph(Logger logger, boolean retainClassNodes) {
        this(logger, retainClassNodes, false);
    }

    public ReferenceGraph(Logger logger, boolean retainClassNodes, boolean retainInsns) {
        this.logger = logger.subLogger(ReferenceGraph.class);
        this.retainClassNodes = retainClassNodes;
        this.retainInsns = retainInsns;
        if (retainInsns && !retainClassNodes) {
            throw new IllegalArgumentException("Cannot retain instructions without retaining class nodes.");
        }
        versions.add(0);
    }


    /**
     * if the {@link ClassNode} is retained, this will return the {@link ClassNode} for the given type.
     *
     * @return the {@link ClassNode} for the given type.
     */
    public ClassNode getClassFor(Type type, int version) {
        if (!retainClassNodes) {
            throw new IllegalStateException("Class nodes are not retained.");
        }
        return resolveVersioned(classNodes.get(type), version);
    }

    public void scan(Path root, Filter filter) throws IOException, ExecutionException, InterruptedException {
        scan(root, preScan(root), filter);
    }

    /**
     * Checks whether a root-relative path is inside a multi-release {@code META-INF/versions/<n>/} tree.
     * Uses path components so it works on every platform's separator.
     */
    private static boolean isMultiRelease(Path rel) {
        return rel.getNameCount() > 3
            && rel.getName(0).toString().equals("META-INF")
            && rel.getName(1).toString().equals("versions");
    }

    private static int versionOf(Path rel) {
        return isMultiRelease(rel) ? Integer.parseInt(rel.getName(2).toString()) : 0;
    }

    /**
     * Joins a path's components with forward slashes, independent of platform separator,
     * for use as a class-file resource path.
     */
    private static String internalPath(Path rel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rel.getNameCount(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(rel.getName(i));
        }
        return sb.toString();
    }

    /**
     * Pre scan the root directory to find all the classes that need to be scanned.
     *
     * @return a map of paths to types that need to be scanned.
     */
    public Map<Path, Type> preScan(final Path root) throws IOException, ExecutionException, InterruptedException {
        final Map<Path, Type> newScanTargets = new ConcurrentHashMap<>();
        AsyncUtils.visitPathsAsync(root, null, new IOConsumer<Path>() {
            @Override
            public void accept(Path path) throws IOException {
                // skip module info, it doesn't have references we care about
                if (path.getFileName().toString().equals("module-info.class")) {
                    return;
                }
                Path rel = root.relativize(path);
                int version = versionOf(rel);
                if (version != 0) {
                    rel = rel.subpath(3, rel.getNameCount());
                }
                String pathStr = internalPath(rel);
                versions.add(version);
                if (pathStr.endsWith(".class")) {
                    pathStr = pathStr.substring(0, pathStr.length() - 6);
                    Type type = Type.getObjectType(pathStr);
                    ConcurrentMap<Integer, References> typeRefs = references.get(type);
                    if (typeRefs == null) {
                        classNodes.putIfAbsent(type, new ConcurrentHashMap<Integer, ClassNode>());
                        references.putIfAbsent(type, new ConcurrentHashMap<Integer, References>());
                        typeRefs = references.get(type);
                    }
                    // putIfAbsent keeps a second file mapping to the same
                    // (type, version) from clobbering an in-progress References
                    if (typeRefs.putIfAbsent(version, new References(version)) == null) {
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
     * @param filter         the filter to determine if a reference should be retained.
     */
    public void scan(final Path rootPath, final Map<Path, Type> newScanTargets, final Filter filter) throws IOException, ExecutionException, InterruptedException {
        AsyncUtils.forEachAsync(newScanTargets.keySet(), new IOConsumer<Path>() {
            @Override
            public void accept(Path path) throws IOException {
                int version = versionOf(rootPath.relativize(path));
                try (InputStream stream = Files.newInputStream(path)) {
                    ClassNode node = ASMUtils.bytesToClassNode(Utils.readAllBytes(stream));
                    Type type = Type.getObjectType(node.name);
                    if (!type.equals(newScanTargets.get(path))) {
                        throw new IllegalStateException("Expected path to match class name: " + path + " != " + type.getInternalName());
                    }
                    if (retainClassNodes) {
                        classNodes.get(type).put(version, node);
                    }
                    references.get(type).get(version).scan(node, filter);
                }
            }
        }).get();
    }

    public void debugPrint() {
        if (!logger.is(Logger.Level.DEBUG)) return;
        logger.debug("Reference Graph:");
        for (Map.Entry<Type, ConcurrentMap<Integer, References>> entry : references.entrySet()) {
            for (Map.Entry<Integer, References> e : entry.getValue().entrySet()) {
                logger.debug("* " + e.getKey() + "/" + entry.getKey().getInternalName());
                References refs = e.getValue();
                for (Type t : refs.requiredInstances) {
                    logger.debug("  - " + t.getInternalName());
                }
                for (Map.Entry<MemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> e2 : refs.requiredForMembers.entrySet()) {
                    logger.debug("  - " + e2.getKey());
                    for (FullyQualifiedMemberNameAndDesc f : e2.getValue()) {
                        logger.debug("      " + f);
                    }
                }
            }
        }
    }

    public Set<Integer> getVersions() {
        return new TreeSet<>(versions);
    }

    public Map<Type, Set<Integer>> getKeys() {
        Map<Type, Set<Integer>> keys = new HashMap<>();
        for (Map.Entry<Type, ConcurrentMap<Integer, References>> entry : references.entrySet()) {
            keys.put(entry.getKey(), new HashSet<>(entry.getValue().keySet()));
        }
        return keys;
    }

    /**
     * @return all retained references from the graph.
     */
    public Set<FullyQualifiedMemberNameAndDesc> getAllRefs(int version) {
        Set<FullyQualifiedMemberNameAndDesc> refs = new HashSet<>();
        for (Map.Entry<Type, ConcurrentMap<Integer, References>> entry : references.entrySet()) {
            References value = resolveVersioned(entry.getValue(), version);
            for (Type requiredInstance : value.requiredInstances) {
                refs.add(FullyQualifiedMemberNameAndDesc.of(requiredInstance));
            }
            for (Map.Entry<MemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> entry2 : value.requiredForMembers.entrySet()) {
                refs.addAll(entry2.getValue());
            }
        }
        return refs;
    }

    public <T> T resolveVersioned(Map<Integer, T> map, int version) {
        if (map == null) return null;
        for (int i = version; i >= 0; i--) {
            T t = map.get(i);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    /**
     * @return all retained references from the graph, and where they came from.
     */
    public Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> getAllUsagesForRefs(int version) {
        Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> refs = new HashMap<>();
        for (Map.Entry<Type, ConcurrentMap<Integer, References>> entry : references.entrySet()) {
            Type owner = entry.getKey();
            References value = resolveVersioned(entry.getValue(), version);
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
    public Pair<Set<FullyQualifiedMemberNameAndDesc>, Set<String>> recursiveResolveFrom(Set<FullyQualifiedMemberNameAndDesc> starts, int version) {
        Set<FullyQualifiedMemberNameAndDesc> refs = new HashSet<>(starts);
        Set<String> resources = new HashSet<>();
        Queue<FullyQualifiedMemberNameAndDesc> toAdd = new ArrayDeque<>(starts);
        while (!toAdd.isEmpty()) {
            FullyQualifiedMemberNameAndDesc next = toAdd.poll();
            References refer = resolveVersioned(references.get(next.getOwner()), version);
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
            References refer = resolveVersioned(references.get(ref.getOwner()), version);
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
     *
     * @param starts the references to start from.
     * @return a pair of the references required by the given references, and the resources required by the given references.
     */
    public Pair<Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>, Set<String>> recursivelyResolveUsagesFrom(Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> starts, int version) {
        Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> usages = new HashMap<>(starts);
        Set<String> resources = new HashSet<>();
        Queue<FullyQualifiedMemberNameAndDesc> toScan = new ArrayDeque<>(starts.keySet());
        while (!toScan.isEmpty()) {
            FullyQualifiedMemberNameAndDesc next = toScan.poll();
            References refer = resolveVersioned(references.get(next.getOwner()), version);
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
            References refer = resolveVersioned(references.get(ref.getOwner()), version);
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
    public Set<AbstractInsnNode> getAllInsnsFor(FullyQualifiedMemberNameAndDesc ref, int version) {
        if (!retainInsns) {
            throw new IllegalStateException("Insns are not retained.");
        }
        Set<AbstractInsnNode> insns = new HashSet<>();
        for (ConcurrentMap<Integer, References> value : references.values()) {
            References v = value.get(version);
            // unlike all the other methods, we only want the insns for the given version.
            if (v == null) continue;
            Set<AbstractInsnNode> ins = v.memberInsns.get(ref);
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
         * The multi-release version of the class.
         */
        private final int version;

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

        public References(int version) {
            this.version = version;
        }

        /**
         * Synchronized because two input paths can resolve to the same
         * (type, version) — e.g. duplicate class files — in which case
         * concurrent workers would otherwise race on this instance's maps.
         */
        public synchronized void scan(ClassNode classNode, Filter filter) {
            Type currentType = Type.getObjectType(classNode.name);
            // super
            requiresInstance(null, Type.getObjectType(classNode.superName), filter);
            // interfaces
            for (String s : classNode.interfaces) {
                requiresInstance(null, Type.getObjectType(s), filter);
            }

            // generic signature (type params/args reference class types)
            scanSignature(classNode.signature, null, filter);

            // inner/outer/nest/permitted class metadata
            if (classNode.innerClasses != null) {
                for (InnerClassNode inner : classNode.innerClasses) {
                    if (inner.name != null) {
                        requiresInstance(null, Type.getObjectType(inner.name), filter);
                    }
                    if (inner.outerName != null) {
                        requiresInstance(null, Type.getObjectType(inner.outerName), filter);
                    }
                }
            }
            if (classNode.outerClass != null) {
                requiresInstance(null, Type.getObjectType(classNode.outerClass), filter);
            }
            if (classNode.outerMethod != null) {
                requiresInstance(null, Type.getObjectType(classNode.outerMethod), filter);
            }
            if (classNode.outerMethodDesc != null) {
                requiresInstance(null, Type.getMethodType(classNode.outerMethodDesc), filter);
            }
            if (classNode.nestHostClass != null) {
                requiresInstance(null, Type.getObjectType(classNode.nestHostClass), filter);
            }
            if (classNode.nestMembers != null) {
                for (String nestMember : classNode.nestMembers) {
                    requiresInstance(null, Type.getObjectType(nestMember), filter);
                }
            }
            if (classNode.permittedSubclasses != null) {
                for (String permitted : classNode.permittedSubclasses) {
                    requiresInstance(null, Type.getObjectType(permitted), filter);
                }
            }

            // class-level annotations (types and values can reference classes)
            scanAnnotations(classNode.visibleAnnotations, null, filter);
            scanAnnotations(classNode.invisibleAnnotations, null, filter);
            scanAnnotations(classNode.visibleTypeAnnotations, null, filter);
            scanAnnotations(classNode.invisibleTypeAnnotations, null, filter);

            // record components
            if (classNode.recordComponents != null) {
                for (RecordComponentNode rc : classNode.recordComponents) {
                    requiresInstance(null, Type.getType(rc.descriptor), filter);
                    scanSignature(rc.signature, null, filter);
                    scanAnnotations(rc.visibleAnnotations, null, filter);
                    scanAnnotations(rc.invisibleAnnotations, null, filter);
                    scanAnnotations(rc.visibleTypeAnnotations, null, filter);
                    scanAnnotations(rc.invisibleTypeAnnotations, null, filter);
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

                scanSignature(field.signature, fieldMember, filter);
                scanAnnotations(field.visibleAnnotations, fieldMember, filter);
                scanAnnotations(field.invisibleAnnotations, fieldMember, filter);
                scanAnnotations(field.visibleTypeAnnotations, fieldMember, filter);
                scanAnnotations(field.invisibleTypeAnnotations, fieldMember, filter);

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

                scanSignature(method.signature, methodMember, filter);
                if (method.exceptions != null) {
                    for (String exception : method.exceptions) {
                        requiresInstance(methodMember, Type.getObjectType(exception), filter);
                    }
                }
                scanAnnotations(method.visibleAnnotations, methodMember, filter);
                scanAnnotations(method.invisibleAnnotations, methodMember, filter);
                scanAnnotations(method.visibleTypeAnnotations, methodMember, filter);
                scanAnnotations(method.invisibleTypeAnnotations, methodMember, filter);
                if (method.visibleParameterAnnotations != null) {
                    for (List<AnnotationNode> parameterAnnotations : method.visibleParameterAnnotations) {
                        scanAnnotations(parameterAnnotations, methodMember, filter);
                    }
                }
                if (method.invisibleParameterAnnotations != null) {
                    for (List<AnnotationNode> parameterAnnotations : method.invisibleParameterAnnotations) {
                        scanAnnotations(parameterAnnotations, methodMember, filter);
                    }
                }
                if (method.annotationDefault != null) {
                    scanAnnotationValue(method.annotationDefault, methodMember, filter);
                }
                if (method.tryCatchBlocks != null) {
                    for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
                        if (tryCatch.type != null) {
                            requiresInstance(methodMember, Type.getObjectType(tryCatch.type), filter);
                        }
                        scanAnnotations(tryCatch.visibleTypeAnnotations, methodMember, filter);
                        scanAnnotations(tryCatch.invisibleTypeAnnotations, methodMember, filter);
                    }
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
                        if (annotation.desc.equals(Type.getDescriptor(ReflectionReferences.class))) {
                            try {
                                ReflectionReferences refs = AnnotationUtils.createAnnotation(annotation);
                                for (Ref ref : refs.value()) {
                                    requiresMember(methodMember, FullyQualifiedMemberNameAndDesc.of(ref) , filter, null);
                                }
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
                        } else if (ldc.cst instanceof Handle) {
                            Handle h = (Handle) ldc.cst;
                            requiresMember(methodMember, new FullyQualifiedMemberNameAndDesc(Type.getObjectType(h.getOwner()), h.getName(), Type.getType(h.getDesc())), filter, insn);
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
            requiresInstance(member, Type.getType(condy.getDescriptor()), filter);
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
            switch (required.getSort()) {
                case Type.ARRAY:
                    requiresInstance(member, required.getElementType(), filter);
                    break;
                case Type.OBJECT: {
                    FullyQualifiedMemberNameAndDesc type = FullyQualifiedMemberNameAndDesc.of(required);
                    if (filter.shouldInclude(type)) {
                        if (member == null) {
                            requiredInstances.add(required);
                        } else {
                            Set<FullyQualifiedMemberNameAndDesc> list = requiredForMembers.get(member);
                            if (list == null) {
                                list = new HashSet<>();
                                requiredForMembers.put(member, list);
                            }
                            list.add(type);
                        }
                    }
                    break;
                }
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

        private void scanAnnotations(List<? extends AnnotationNode> annotations, MemberNameAndDesc member, Filter filter) {
            if (annotations == null) return;
            for (AnnotationNode annotation : annotations) {
                requiresInstance(member, Type.getType(annotation.desc), filter);
                if (annotation.values != null) {
                    // values are stored as (name, value) pairs
                    for (int i = 1; i < annotation.values.size(); i += 2) {
                        scanAnnotationValue(annotation.values.get(i), member, filter);
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void scanAnnotationValue(Object value, MemberNameAndDesc member, Filter filter) {
            if (value instanceof Type) {
                requiresInstance(member, (Type) value, filter);
            } else if (value instanceof AnnotationNode) {
                AnnotationNode nested = (AnnotationNode) value;
                requiresInstance(member, Type.getType(nested.desc), filter);
                if (nested.values != null) {
                    for (int i = 1; i < nested.values.size(); i += 2) {
                        scanAnnotationValue(nested.values.get(i), member, filter);
                    }
                }
            } else if (value instanceof List) {
                for (Object o : (List<Object>) value) {
                    scanAnnotationValue(o, member, filter);
                }
            } else if (value instanceof String[]) {
                // enum constant reference: {desc, name}
                requiresInstance(member, Type.getType(((String[]) value)[0]), filter);
            }
        }

        private void scanSignature(String signature, final MemberNameAndDesc member, final Filter filter) {
            if (signature == null) return;
            new SignatureReader(signature).accept(new SignatureVisitor(Opcodes.ASM9) {
                @Override
                public void visitClassType(String name) {
                    requiresInstance(member, Type.getObjectType(name), filter);
                }

                @Override
                public void visitInnerClassType(String name) {
                    requiresInstance(member, Type.getObjectType(name), filter);
                }
            });
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
