package xyz.wagyourtail.jvmdg.compile;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.compile.shade.ReferenceGraph;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.AsyncUtils;
import xyz.wagyourtail.jvmdg.util.IOConsumer;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ApiShader {

    @Deprecated
    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        String prefix = args[1];
        File input = new File(args[2]);
        File output = new File(args[3]);
        int target = -1;
        File downgradedApi = null;
        if (args[0].matches("\\d+")) {
            target = Integer.parseInt(args[0]);
        } else {
            downgradedApi = new File(args[0]);
        }
        Flags flags = new Flags();
        flags.classVersion = target;
        shadeApis(flags, prefix, input, output, Collections.singleton(downgradedApi));
        System.out.println("Shaded in " + (System.currentTimeMillis() - start) + "ms");
    }

    public static void shadeApis(Flags flags, String prefix, File input, File output, Set<File> apiJars) throws IOException {
        if (output.exists()) {
            output.delete();
        }
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        List<FileSystem> apiFs = new ArrayList<>();
        try {
            for (Path file : resolveDowngradedApi(flags, apiJars)) {
                apiFs.add(Utils.openZipFileSystem(file, false));
            }

            try (FileSystem inputFs = Utils.openZipFileSystem(input.toPath(), false)) {
                try (FileSystem outputFs = Utils.openZipFileSystem(output.toPath(), true)) {
                    List<Path> apiRoots = new ArrayList<>();
                    for (FileSystem fs : apiFs) {
                        apiRoots.add(fs.getPath("/"));
                    }
                    Pair<ReferenceGraph, Set<Type>> api = scanApis(flags, apiRoots);
                    shadeApis(flags, prefix, inputFs.getPath("/"), outputFs.getPath("/"), apiRoots, api.getFirst(), api.getSecond());
                }
            }
        } finally {
            for (FileSystem fs : apiFs) {
                fs.close();
            }
        }
    }

    public static void shadeApis(Flags flags, String prefix, Path inputRoot, Path outputRoot, Set<File> apiJars) throws IOException {
        shadeApis(flags, Collections.singletonList(prefix), Collections.singletonList(inputRoot), Collections.singletonList(outputRoot), apiJars);
    }

    public static void shadeApis(Flags flags, List<String> prefix, List<Path> inputRoots, List<Path> outputRoots, Set<File> apiJars) throws IOException {
        for (String p : prefix) {
            if (!p.endsWith("/")) {
                throw new IllegalArgumentException("prefix \"" + p + "\" must end with /");
            }
        }
        Set<Path> downgradedApiPath = resolveDowngradedApi(flags, apiJars);
        List<FileSystem> apiFs = new ArrayList<>();
        try {
            for (Path file : downgradedApiPath) {
                apiFs.add(Utils.openZipFileSystem(file, false));
            }
            List<Path> apiRoots = new ArrayList<>();
            for (FileSystem fs : apiFs) {
                apiRoots.add(fs.getPath("/"));
            }
            Pair<ReferenceGraph, Set<Type>> api = scanApis(flags, apiRoots);
            for (int i = 0; i < inputRoots.size(); i++) {
                shadeApis(flags, prefix.get(i % prefix.size()), inputRoots.get(i), outputRoots.get(i), apiRoots, api.getFirst(), api.getSecond());
            }
        } finally {
            for (FileSystem fs : apiFs) {
                fs.close();
            }
        }
    }

    public static Set<Path> resolveDowngradedApi(Flags flags, @Nullable Set<File> apiJars) throws IOException {
        // step 1: downgrade the api to the target version
        Set<Path> downgradedApis = new HashSet<>();
        try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(flags)) {
            for (File file : apiJars == null ? flags.findJavaApi() : apiJars) {
                // if file manifest has JvmDowngrader-Downgrade-Target of target or lower...
                try (JarFile jf = new JarFile(file)) {
                    Manifest mf = jf.getManifest();
                    if (mf != null) {
                        String target = mf.getMainAttributes().getValue(ManifestUtils.DOWNGRADE_TARGET);
                        if (target != null && flags.classVersion >= Integer.parseInt(target)) {
                            downgradedApis.add(file.toPath());
                            continue;
                        }
                    }
                } catch (Throwable ignored) {}
                Path targetPath = file.toPath().resolveSibling(downgradedApiName(file, flags));
                ZipDowngrader.downgradeZip(downgrader, file.toPath(), new HashSet<URL>(), targetPath);
                downgradedApis.add(targetPath);
            }
        } catch (NoSuchAlgorithmException nse) {
            throw new IOException(nse);
        }
        return downgradedApis;
    }

    /**
     * Cache-file name for a downgraded api jar. Includes the input hash, the
     * target class version, and a hash of every flag that can change the
     * produced bytecode — otherwise two runs with different flags would silently
     * reuse each other's stale artifact.
     */
    static String downgradedApiName(File file, Flags flags) throws IOException, NoSuchAlgorithmException {
        String name = file.getName();
        int idx = name.lastIndexOf('.');
        if (idx == -1) {
            throw new IllegalArgumentException("File has no extension: " + name);
        }
        return name.substring(0, idx) + "-" + Utils.getShortSha1(file) +
            "-downgraded-" + flags.classVersion + "-" + downgradeFlagsHash(flags) +
            name.substring(idx);
    }

    /**
     * Hash of the flags that affect downgraded-api output (output-irrelevant
     * flags like log level or color settings are intentionally excluded so the
     * cache stays shared).
     */
    private static String downgradeFlagsHash(Flags flags) throws NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();
        sb.append(flags.classVersion).append('|')
            .append(flags.multiReleaseOriginal).append('|')
            .append(new TreeSet<>(flags.multiReleaseVersions)).append('|')
            .append(flags.downgradeFromMultiReleases).append('|')
            .append(flags.debugNoSynthetic).append('|')
            .append(new TreeSet<>(flags.debugSkipStubs)).append('|')
            .append(new TreeSet<>(flags.debugSkipJeps)).append('|');
        List<String> skip = new ArrayList<>();
        for (FullyQualifiedMemberNameAndDesc m : flags.debugSkipStub) {
            skip.add(m.toString());
        }
        Collections.sort(skip);
        sb.append(skip);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = digest.digest(sb.toString().getBytes());
        StringBuilder hash = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            hash.append(String.format("%02x", hashBytes[i]));
        }
        return hash.toString();
    }

    public static Pair<ReferenceGraph, Set<Type>> scanApis(Flags flags, List<Path> apiRoots) throws IOException {
        // step 2: collect classes in the api and their references
        try {
            ReferenceGraph apiRefs = new ReferenceGraph(new Logger(ApiShader.class, flags.getLogLevel(), flags.logAnsiColors, System.out), true);
            List<Pair<Path, Map<Path, Type>>> preScans = new ArrayList<>();
            final Set<Type> apiClasses = new HashSet<>();
            for (Path apiRoot : apiRoots) {
                Map<Path, Type> preScan = apiRefs.preScan(apiRoot);
                preScans.add(new Pair<>(apiRoot, preScan));
                apiClasses.addAll(preScan.values());
            }
            for (Pair<Path, Map<Path, Type>> preScan : preScans) {
                apiRefs.scan(preScan.getFirst(), preScan.getSecond(), new ReferenceGraph.Filter() {
                    @Override
                    public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
                        return apiClasses.contains(member.getOwner());
                    }
                });
            }
            return new Pair<>(apiRefs, apiClasses);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<FullyQualifiedMemberNameAndDesc, Type> inlineCandidates(Logger logger, Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> refs, Set<Type> keys) {
        Map<FullyQualifiedMemberNameAndDesc, Type> candidates = new HashMap<>();
        // find all stubs only used by one class
        for (Map.Entry<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> entry : refs.entrySet()) {
            FullyQualifiedMemberNameAndDesc key = entry.getKey();
            Set<FullyQualifiedMemberNameAndDesc> value = entry.getValue();
            Set<Type> owners = new HashSet<>();
            for (FullyQualifiedMemberNameAndDesc ref : value) {
                owners.add(ref.getOwner());
            }
            if (owners.size() == 1 && keys.contains(owners.iterator().next())) {
                candidates.put(key, owners.iterator().next());
            }
        }
        // remove if needs another stub
        for (Set<FullyQualifiedMemberNameAndDesc> value : refs.values()) {
            for (FullyQualifiedMemberNameAndDesc ref : value) {
                candidates.remove(ref);
            }
        }

        // remove if the class is not fully inlined
        Set<FullyQualifiedMemberNameAndDesc> unInlined = new HashSet<>(refs.keySet());
        unInlined.removeAll(candidates.keySet());
        Set<Type> unInlinedOwners = new HashSet<>();
        for (FullyQualifiedMemberNameAndDesc member : unInlined) {
            unInlinedOwners.add(member.getOwner());
        }
        for (FullyQualifiedMemberNameAndDesc member : new HashSet<>(candidates.keySet())) {
            if (unInlinedOwners.contains(member.getOwner())) {
                candidates.remove(member);
            }
        }
        if (logger.is(Logger.Level.DEBUG)) {
            for (Map.Entry<FullyQualifiedMemberNameAndDesc, Type> entry : candidates.entrySet()) {
                logger.debug("Candidate for inlining: " + entry.getKey() + " -> " + entry.getValue());
            }
        }
        return candidates;
    }

    private static void inlineRef(FullyQualifiedMemberNameAndDesc ref, AbstractInsnNode insn, Type target, boolean isInterface) {
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            min.owner = target.getInternalName();
            min.name = "jvmdg$inlined$" + min.name;
            min.itf = isInterface;
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode fin = (FieldInsnNode) insn;
            fin.owner = target.getInternalName();
            fin.name = "jvmdg$inlined$" + fin.name;
        } else if (insn instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
            // check bsm matches
            Handle handle = indy.bsm;
            if (FullyQualifiedMemberNameAndDesc.of(handle).equals(ref)) {
                handle = new Handle(handle.getTag(), target.getInternalName(), "jvmdg$inlined$" + handle.getName(), handle.getDesc(), isInterface);
                indy.bsm = handle;
            }
            inlineRef(ref, indy.bsmArgs, target, isInterface);
        } else if (insn instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) insn;
            if (ldc.cst instanceof Handle) {
                Handle handle = (Handle) ldc.cst;
                ldc.cst = new Handle(handle.getTag(), target.getInternalName(), handle.getName(), handle.getDesc(), isInterface);
            } else if (ldc.cst instanceof ConstantDynamic) {
                ConstantDynamic cd = (ConstantDynamic) ldc.cst;
                ldc.cst = inlineRef(ref, cd, target, isInterface);
            }
        } else {
            throw new IllegalStateException("Unknown insn type: " + insn.getClass().getName());
        }
    }

    private static ConstantDynamic inlineRef(FullyQualifiedMemberNameAndDesc ref, ConstantDynamic cd, Type target, boolean isInterface) {
        Handle handle = cd.getBootstrapMethod();
        if (FullyQualifiedMemberNameAndDesc.of(handle).equals(ref)) {
            handle = new Handle(handle.getTag(), target.getInternalName(), "jvmdg$inlined$" + handle.getName(), handle.getDesc(), isInterface);
        }
        Object[] bsmArgs = new Object[cd.getBootstrapMethodArgumentCount()];
        for (int i = 0; i < cd.getBootstrapMethodArgumentCount(); i++) {
            bsmArgs[i] = cd.getBootstrapMethodArgument(i);
        }
        inlineRef(ref, bsmArgs, target, isInterface);
        return new ConstantDynamic(
            cd.getName(),
            cd.getDescriptor(),
            handle,
            bsmArgs
        );
    }

    private static void inlineRef(FullyQualifiedMemberNameAndDesc ref, Object[] bsmArgs, Type target, boolean isInterface) {
        for (int i = 0; i < bsmArgs.length; i++) {
            Object arg = bsmArgs[i];
            if (arg instanceof Handle) {
                Handle handleArg = (Handle) arg;
                if (FullyQualifiedMemberNameAndDesc.of(handleArg).equals(ref)) {
                    handleArg = new Handle(handleArg.getTag(), target.getInternalName(), "jvmdg$inlined$" + handleArg.getName(), handleArg.getDesc(), isInterface);
                    bsmArgs[i] = handleArg;
                }
            } else if (arg instanceof ConstantDynamic) {
                ConstantDynamic cd = (ConstantDynamic) arg;
                bsmArgs[i] = inlineRef(ref, cd, target, isInterface);
            }
        }
    }

    public static void shadeApis(final Flags flags, final String prefix, final Path inputRoot, final Path outputRoot, final List<Path> apiRoots, final ReferenceGraph apiRefs, final Set<Type> apiClasses) throws IOException {
        if (!prefix.endsWith("/")) throw new IllegalArgumentException("prefix must end with /");
        Logger logger = new Logger(ApiShader.class, flags.getLogLevel(), flags.logAnsiColors, System.out);
        try {
            // step 3: traverse the input classes for references to the api
            final ReferenceGraph inputRefs = new ReferenceGraph(logger, true, flags.shadeInlining);
            inputRefs.scan(inputRoot, new ReferenceGraph.Filter() {
                @Override
                public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
                    return apiClasses.contains(member.getOwner());
                }
            });

            Set<Integer> inputVersions = inputRefs.getVersions();
            Map<Type, Set<Integer>> inputKeys = inputRefs.getKeys();

            // deterministic output: async consumers defer into these maps,
            // then a single sorted write pass produces reproducible order.
            // shared across the version loop: first writer wins, matching the
            // previous Files.exists(outPath) defer-to-lower-version check
            final ConcurrentMap<String, byte[]> writes = new ConcurrentHashMap<>();
            final ConcurrentMap<String, Path> copies = new ConcurrentHashMap<>();

            for (final int version : inputVersions) {
                // dedup of identical class bytes is per-version: sharing this
                // map across versions would skip writing a versioned entry
                // whenever its bytes happen to equal another version's
                final Map<Type, byte[]> currentOutputData = new ConcurrentHashMap<>();
                // step 4: inline candidates
                Pair<Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>, Set<String>> required = apiRefs.recursivelyResolveUsagesFrom(inputRefs.getAllUsagesForRefs(version), version);

                Set<Type> keys = new HashSet<>();
                for (Type key : inputKeys.keySet()) {
                    if (inputKeys.get(key).contains(version)) {
                        keys.add(key);
                    }
                }

                if (flags.shadeInlining) {
                    Map<FullyQualifiedMemberNameAndDesc, Type> inlineCandidates = inlineCandidates(logger, required.getFirst(), keys);
                    for (Map.Entry<FullyQualifiedMemberNameAndDesc, Type> entry : inlineCandidates.entrySet()) {
                        ClassNode node = inputRefs.getClassFor(entry.getValue(), version);
                        required.getFirst().remove(entry.getKey());
                        required.getFirst().remove(new FullyQualifiedMemberNameAndDesc(entry.getKey().getOwner(), null, null));

                        // change insn's owner to the inlined class
                        for (AbstractInsnNode insn : inputRefs.getAllInsnsFor(entry.getKey(), version)) {
                            inlineRef(entry.getKey(), insn, entry.getValue(), (node.access & Opcodes.ACC_INTERFACE) != 0);
                        }

                        // actually inline it
                        ClassNode apiNode = apiRefs.getClassFor(entry.getKey().getOwner(), version);
                        // find api method
                        MethodNode method = null;
                        for (MethodNode m : apiNode.methods) {
                            if (m.name.equals(entry.getKey().getName()) && m.desc.equals(entry.getKey().getDesc().getDescriptor())) {
                                method = m;
                                break;
                            }
                        }
                        if (method == null) {
                            throw new IllegalStateException("Method not found in class: " + entry.getKey());
                        }
                        MethodNode copy = new MethodNode((method.access & ~Opcodes.ACC_PUBLIC) | Opcodes.ACC_PRIVATE | (flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "jvmdg$inlined$" + method.name, method.desc, method.signature, method.exceptions.toArray(new String[0]));
                        method.accept(copy);
                        node.methods.add(copy);
                    }
                }

                // step 4.5: create remapper for api classes to prefixed api classes
                final Map<Type, Set<MemberNameAndDesc>> byType = byType(required.getFirst().keySet());
                final Map<String, String> remap = new HashMap<>();
                for (Type type : byType.keySet()) {
                    remap.put(type.getInternalName(), prefix + type.getInternalName());
                }
                final SimpleRemapper remapper = new SimpleRemapper(remap);

                // step 5: actually write the referenced api classes to the output, removing unused parts from them.
                Future<Void> apiWrite = AsyncUtils.forEachAsync(byType.entrySet(), new IOConsumer<Map.Entry<Type, Set<MemberNameAndDesc>>>() {
                    @Override
                    public void accept(Map.Entry<Type, Set<MemberNameAndDesc>> type) throws IOException {
                        // load api class as a ClassNode
                        ClassNode node = apiRefs.getClassFor(type.getKey(), version);
                        // get actual version of node
                        Path outPath;
                        if (node.version > flags.classVersion) {
                            outPath = outputRoot.resolve("META-INF/versions/" + Utils.classVersionToMajorVersion(node.version) + "/");
                        } else {
                            outPath = outputRoot;
                        }
                        outPath = outPath.resolve(prefix + type.getKey().getInternalName() + ".class");
                        String rel = outputRoot.relativize(outPath).toString();
                        if (writes.containsKey(rel)) {
                            // Already written by lower version, defer to lower version
                            return;
                        }
                        if ((node.access & Opcodes.ACC_ENUM) == 0) {
                            // remove unused members
                            Set<MemberNameAndDesc> members = type.getValue();
                            Iterator<MethodNode> iter = node.methods.iterator();
                            while (iter.hasNext()) {
                                MethodNode method = iter.next();
                                if (!members.contains(new MemberNameAndDesc(method.name, Type.getMethodType(method.desc)))) {
                                    iter.remove();
                                }
                            }
                            Iterator<FieldNode> fiter = node.fields.iterator();
                            while (fiter.hasNext()) {
                                FieldNode field = fiter.next();
                                if (!members.contains(new MemberNameAndDesc(field.name, Type.getType(field.desc)))) {
                                    fiter.remove();
                                }
                            }
                        }
                        // write the class to the output
                        ClassWriter writer = new ClassWriter(0);
                        node.accept(new ClassRemapper(writer, remapper));
                        byte[] data = writer.toByteArray();
                        byte[] prevData = currentOutputData.get(type.getKey());
                        if (prevData == null || !Utils.equals(prevData, 8, prevData.length, data, 8, data.length)) {
                            currentOutputData.put(type.getKey(), data);
                            writes.putIfAbsent(rel, data);
                        }
                    }

                });

                Future<Void> resourceWrite = AsyncUtils.forEachAsync(required.getSecond(), new IOConsumer<String>() {
                    @Override
                    public void accept(String resource) throws IOException {
                        // never carry signature files from the api jar — they no
                        // longer match the shaded output
                        if (ManifestUtils.isSignatureFile(resource)) {
                            return;
                        }
                        for (Path apiRoot : apiRoots) {
                            Path inPath = apiRoot.resolve(resource);
                            if (Files.exists(inPath)) {
                                copies.put(resource, inPath);
                                return;
                            }
                        }
                    }
                });

                // step 6: remap references to the api to start with prefix
                Set<Type> inputsOnVersion = new HashSet<Type>();
                for (Type key : inputKeys.keySet()) {
                    if (inputKeys.get(key).contains(version)) {
                        inputsOnVersion.add(key);
                    }
                }
                Future<Void> shadeWrite = AsyncUtils.forEachAsync(inputsOnVersion, new IOConsumer<Type>() {
                    @Override
                    public void accept(Type type) throws IOException {
                        ClassNode node = inputRefs.getClassFor(type, version);
                        Path outPath;
                        if (version != 0) {
                            outPath = outputRoot.resolve("META-INF/versions/" + version + "/");
                        } else {
                            outPath = outputRoot;
                        }
                        outPath = outPath.resolve(type.getInternalName() + ".class");
                        ClassWriter writer = new ClassWriter(0);
                        node.accept(new ClassRemapper(writer, remapper));
                        writes.put(outputRoot.relativize(outPath).toString(), writer.toByteArray());
                    }
                });

                AsyncUtils.waitForFutures(apiWrite, resourceWrite, shadeWrite).get();
            }

            // step 7: write the resources in the class file
            AsyncUtils.visitPathsAsync(inputRoot, null, new IOConsumer<Path>() {
                @Override
                public void accept(Path path) throws IOException {
                    if (path.toString().endsWith(".class")) return;
                    copies.put(inputRoot.relativize(path).toString(), path);
                }
            }).get();

            // step 8: write module-info if present
            Path modInfoInPath = inputRoot.resolve("module-info.class");
            if (Files.exists(modInfoInPath)) {
                ClassNode modInfo = ASMUtils.bytesToClassNode(Files.readAllBytes(modInfoInPath));
                if (modInfo.module.requires != null) {
                    for (ModuleRequireNode require : new ArrayList<>(modInfo.module.requires)) {
                        if (require.module.equals("xyz.wagyourtail.jvmdg.java_api")) {
                            modInfo.module.requires.remove(require);
                        }
                    }
                }
                writes.put("module-info.class", ASMUtils.classNodeToBytes(modInfo));
            }

            // single sorted write pass: deterministic entry order
            List<String> order = new ArrayList<>(writes.size() + copies.size());
            order.addAll(writes.keySet());
            order.addAll(copies.keySet());
            Collections.sort(order);
            for (String rel : order) {
                Path p = outputRoot.resolve(rel);
                Path parent = p.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                byte[] data = writes.get(rel);
                if (data != null) {
                    Files.write(p, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } else {
                    Files.copy(copies.get(rel), p, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            OutputUtils.pinTimestamps(outputRoot);

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<Type, Set<MemberNameAndDesc>> byType(Set<FullyQualifiedMemberNameAndDesc> fqns) {
        Map<Type, Set<MemberNameAndDesc>> byType = new HashMap<>();
        for (FullyQualifiedMemberNameAndDesc fqn : fqns) {
            Set<MemberNameAndDesc> list = byType.get(fqn.getOwner());
            if (list == null) {
                list = new HashSet<>();
                byType.put(fqn.getOwner(), list);
            }
            MemberNameAndDesc member = fqn.toMemberNameAndDesc();
            if (member != null) {
                list.add(member);
            }
        }
        return byType;
    }

}
