package xyz.wagyourtail.jvmdg.compile;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.compile.jar.FusedJarRun;
import xyz.wagyourtail.jvmdg.compile.jar.JarStep;
import xyz.wagyourtail.jvmdg.compile.jar.ManifestStep;
import xyz.wagyourtail.jvmdg.compile.jar.PipelineJarStep;
import xyz.wagyourtail.jvmdg.compile.jar.SignatureStripStep;
import xyz.wagyourtail.jvmdg.compile.jar.StepContext;
import xyz.wagyourtail.jvmdg.compile.shade.IndexScanner;
import xyz.wagyourtail.jvmdg.compile.shade.RefExtractor;
import xyz.wagyourtail.jvmdg.compile.shade.ReferenceIndex;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.AsyncUtils;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ApiShader {

    private static final Object API_DOWNGRADE_LOCK = new Object();

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
        new Logger(ApiShader.class, flags.getLogLevel(), flags.logAnsiColors, System.out)
            .info("Shaded in " + (System.currentTimeMillis() - start) + "ms");
    }

    public static void shadeApis(Flags flags, String prefix, File input, File output, Set<File> apiJars) throws IOException {
        if (output.exists() && Files.isSameFile(input.toPath(), output.toPath())) {
            throw new IOException("input and output are the same file: " + input);
        }
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
                    Pair<ReferenceIndex, Set<Type>> api = scanApis(flags, apiRoots);
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
            Pair<ReferenceIndex, Set<Type>> api = scanApis(flags, apiRoots);
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
                // the sha1+flags hash in the name makes the path
                // content-addressed, so a hit is reusable; guard + lock since
                // parallel transforms/tasks would otherwise race writing the
                // same file
                Path targetPath = file.toPath().resolveSibling(downgradedApiName(file, flags));
                if (!Files.exists(targetPath)) {
                    synchronized (API_DOWNGRADE_LOCK) {
                        if (!Files.exists(targetPath)) {
                            Path tmp = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");
                            ZipDowngrader.downgradeZip(downgrader, file.toPath(), new HashSet<URL>(), tmp);
                            Files.move(tmp, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
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
            .append(flags.noIndy).append('|')
            .append(flags.stubPolicy).append('|')
            .append(flags.onMissingApi).append('|')
            .append(flags.allowPreview).append('|')
            .append(flags.ignoreEmbeddedRules).append('|')
            .append(flags.keepEmbeddedRules).append('|')
            .append(flags.scopedRules).append('|')
            .append(flags.debugNoFuse).append('|')
            .append(flags.debugNoStubIdx).append('|')
            .append(new TreeSet<>(flags.debugSkipStubs)).append('|')
            .append(new TreeSet<>(flags.debugSkipJeps)).append('|');
        List<String> include = new ArrayList<>();
        for (FullyQualifiedMemberNameAndDesc m : flags.includeStub) {
            include.add(m.toString());
        }
        include.sort(java.util.Comparator.naturalOrder());
        sb.append(include).append('|');
        List<String> skip = new ArrayList<>();
        for (FullyQualifiedMemberNameAndDesc m : flags.debugSkipStub) {
            skip.add(m.toString());
        }
        skip.sort(java.util.Comparator.naturalOrder());
        sb.append(skip);
        try {
            // first 14 hex chars (was: first 7 digest bytes)
            return Utils.getSha1(sb.toString().getBytes()).substring(0, 14);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Pair<ReferenceIndex, Set<Type>> scanApis(Flags flags, List<Path> apiRoots) throws IOException {
        // step 2: collect classes in the api and their references
        try {
            ReferenceIndex apiRefs = new ReferenceIndex(new Logger(ApiShader.class, flags.getLogLevel(), flags.logAnsiColors, System.out), true);
            // collectClassTypes runs inside visitPathsAsync — concurrent add site
            final Set<Type> apiClasses = Collections.newSetFromMap(new ConcurrentHashMap<Type, Boolean>());
            // pass 1: collect every api class name across the roots —
            // the filter needs the complete set before refs resolve
            for (Path apiRoot : apiRoots) {
                collectClassTypes(apiRoot, apiClasses);
            }
            for (Path apiRoot : apiRoots) {
                IndexScanner.scanRoot(apiRefs, apiRoot, new RefExtractor.Filter() {
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

    private static void collectClassTypes(final Path root, final Set<Type> out) throws IOException, ExecutionException, InterruptedException {
        AsyncUtils.visitPathsAsync(root, null, path -> {
            if (path.getFileName().toString().equals("module-info.class")) {
                return;
            }
            Path rel = root.relativize(path);
            if (IndexScanner.isMultiRelease(rel)) {
                rel = rel.subpath(3, rel.getNameCount());
            }
            String pathStr = IndexScanner.internalPath(rel);
            if (pathStr.endsWith(".class")) {
                out.add(Type.getObjectType(pathStr.substring(0, pathStr.length() - 6)));
            }
        }).get();
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

    /** Rewrites {@code insn} (and nested bsm args/consts) referencing {@code ref} to call the copy
     *  appended to {@code target} as {@code jvmdg$inlined$<name>} — used by the in-pipeline
     *  inline transform as well as the jar-level flow. */
    public static void inlineRef(FullyQualifiedMemberNameAndDesc ref, AbstractInsnNode insn, Type target, boolean isInterface) {
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

    public static void shadeApis(final Flags flags, final String prefix, final Path inputRoot, final Path outputRoot, final List<Path> apiRoots, final ReferenceIndex apiRefs, final Set<Type> apiClasses) throws IOException {
        if (!prefix.endsWith("/")) throw new IllegalArgumentException("prefix must end with /");
        Logger logger = new Logger(ApiShader.class, flags.getLogLevel(), flags.logAnsiColors, System.out);
        // one fused pass: classes collect during traversal, then a single
        // pipeline (scan → closure → inline/prefix → emit) runs at endFused
        List<JarStep> steps = new ArrayList<>();
        // shaded class bytes invalidate jar signatures — strip them (and the
        // manifest's Sealed attrs) the same way the downgrade chain does
        steps.add(new SignatureStripStep());
        steps.add(new ManifestStep());
        steps.add(new PipelineJarStep(prefix, apiRefs, apiClasses, apiRoots));
        StepContext ctx = new StepContext(flags, logger, null,
            Collections.singletonList(inputRoot), null);
        FusedJarRun.run(inputRoot, steps, ctx, outputRoot);
    }

    /** Groups required members by owner type — null-member (class-level) refs contribute just the type. */
    public static Map<Type, Set<MemberNameAndDesc>> byType(Set<FullyQualifiedMemberNameAndDesc> fqns) {
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

    /**
     * One shaded output entry for a {@code META-INF/services/<iface>} provider
     * file, or null when the file can't survive the shade: the service
     * interface is an api class the closure pruned (nobody can load it), or
     * no provider line remains. Provider lines naming an emitted api class
     * carry the prefix; lines naming a pruned api class are dropped (the
     * class is absent — keeping the line would crash {@code ServiceLoader});
     * {@code #} comments, blanks and names outside the api set pass through.
     * The file is renamed when the interface itself is an emitted api class
     * so {@code ServiceLoader.load(prefixedInterface)} still resolves it.
     */
    public static Map.Entry<String, byte[]> mapServiceEntry(String name, byte[] bytes,
            @Nullable SimpleRemapper remapper, Set<Type> apiClasses) {
        String ifaceInternal = name.substring("META-INF/services/".length()).replace('.', '/');
        String outName = name;
        if (apiClasses.contains(Type.getObjectType(ifaceInternal))) {
            String mapped = remapper == null ? null : remapper.map(ifaceInternal);
            if (mapped == null) {
                return null;
            }
            outName = "META-INF/services/" + mapped.replace('/', '.');
        }
        String content = new String(bytes, StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder(content.length());
        Set<String> seen = new HashSet<>();
        for (String line : content.split("\n", -1)) {
            int hash = line.indexOf('#');
            String provider = (hash < 0 ? line : line.substring(0, hash)).trim();
            if (provider.isEmpty()) {
                out.append(line).append('\n');
                continue;
            }
            String internal = provider.replace('.', '/');
            String mapped = remapper == null ? null : remapper.map(internal);
            if (mapped != null) {
                provider = mapped.replace('/', '.');
            } else if (apiClasses.contains(Type.getObjectType(internal))) {
                continue;
            }
            if (!seen.add(provider)) {
                continue;
            }
            out.append(provider);
            if (hash >= 0) {
                out.append(' ').append(line.substring(hash));
            }
            out.append('\n');
        }
        if (seen.isEmpty()) {
            return null;
        }
        out.setLength(out.length() - 1);
        return new AbstractMap.SimpleImmutableEntry<>(
            outName, out.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Union of two provider files for the same service — the input jar and an
     * api jar can both carry {@code META-INF/services/<iface>} as separate
     * classpath resources, but a merged output jar holds exactly one file, so
     * the provider sets merge: {@code first}'s providers lead (input precedes
     * the api jars on a classpath, preserving {@code ServiceLoader} order),
     * deduped by provider name. Comment lines are dropped — they carry no
     * semantics.
     */
    public static byte[] mergeServices(byte[] first, byte[] second) {
        StringBuilder out = new StringBuilder(first.length + second.length);
        Set<String> seen = new LinkedHashSet<>();
        for (byte[] bytes : new byte[][]{first, second}) {
            for (String line : new String(bytes, StandardCharsets.UTF_8).split("\n", -1)) {
                int hash = line.indexOf('#');
                String provider = (hash < 0 ? line : line.substring(0, hash)).trim();
                if (!provider.isEmpty() && seen.add(provider)) {
                    out.append(provider).append('\n');
                }
            }
        }
        if (out.length() > 0) {
            out.setLength(out.length() - 1);
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

}
