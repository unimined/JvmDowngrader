package xyz.wagyourtail.jvmdg.coverage;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.CoverageIgnore;
import xyz.wagyourtail.jvmdg.version.Internal;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Stub;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.FieldStubEntry;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;
import xyz.wagyourtail.jvmdg.version.idx.StubIndexWriter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiCoverageChecker {

    private static final Set<String> excludedMods = Set.of(
        "jdk.internal.vm.compiler",
        "com.azul.",
        "javafx"
    );

    public static void main(String[] args) throws IOException, URISyntaxException {
//        var home = Path.of(System.getProperty("java.home")).resolve("lib/ct.sym");
//        if (!Files.exists(home)) {
//            System.err.println("Failed to find \"" + home + "\" for coverage. Please ensure you are using a JDK and not a JRE.");
//            System.exit(1);
//        }
//
//        System.out.println("Found \"" + home + "\" for coverage.");

        File ctSym = new File(args[0]);
        if (!ctSym.exists()) {
            System.err.println("Failed to find \"" + ctSym + "\" for coverage. Please provide as first arg.");
            System.exit(1);
        }

        URL sym = ctSym.toURI().toURL();
        System.out.println("Found \"" + sym + "\" for coverage.");

        Path home = Paths.get(sym.toURI());

        try (var fs = Utils.openZipFileSystem(home, false)) {
            System.out.println("Successfully opened \"" + home + "\" for coverage.");

            var versions = new HashMap<Integer, List<Path>>();
            try (var folders = Files.newDirectoryStream(fs.getPath("/"))) {
                for (var folder : folders) {
                    folder.getFileName().toString().chars().map(c -> Character.digit(c, 36))
                        .forEach(javaVersion -> versions.computeIfAbsent(javaVersion, k -> new ArrayList<>()).add(folder));
                }
            }

//            var current = Runtime.version().feature();
//            var currentModules = Path.of(URI.create("jrt:/"));
//            versions.computeIfAbsent(current, k -> new ArrayList<>()).add(currentModules);

            var classes = new HashMap<String, Pair<String, ClassNode>>();

            var max = versions.keySet().stream().max(Integer::compareTo).orElseThrow();
            // load initial classes
            compare(versions.get(max), classes, new ArrayList<>());

            versions.keySet().stream().sorted((a, b) -> -a.compareTo(b)).skip(1).forEach(v -> {
                var missingStubs = new ArrayList<MemberInfo>();
                var parentOnlyStubs = new ArrayList<MemberInfo>();

                var stubVersion = v + 1;
                var availableStubCount = 0;
                var onlyOnParentStubCount = 0;
                var missingStubCount = 0;
                var versionProvider = ClassDowngrader.getCurrentVersionDowngrader().getVersionProviderFor(stubVersion);
                if (versionProvider != null && versionProvider.stubRegistry.memberSince.isEmpty()) {
                    // the coverage classpath carries no stubs.idx — derive memberSince
                    // from the same ct.sym the writer uses, independently
                    try {
                        StubIndexWriter.populateMemberSince(ctSym.toPath(), List.of(versionProvider));
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to populate memberSince for " + stubVersion, e);
                    }
                }

                if (versionProvider == null) {
                    System.out.println("No version provider for " + stubVersion);
                    try {
                        compare(versions.get(v), classes, new ArrayList<>());
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to compare version " + v, e);
                    }
                    return;
                }
                System.out.println("Checking version " + stubVersion);

                Map<Type, Type> stubClassTypes = versionProvider.classStubs.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().target));
                // stubClassTypes is JDK-source → adapter; unmatched members need the inverse
                Map<Type, Type> adapterToJdk = versionProvider.classStubs.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getValue().target, Map.Entry::getKey));
                Set<FullyQualifiedMemberNameAndDesc> stubClassMethods = versionProvider.classStubs.entrySet().stream()
                    .flatMap(e -> Stream.concat(
                                Arrays.stream(e.getValue().clazz().getDeclaredMethods()),
                                Arrays.stream(e.getValue().clazz().getConstructors())
                            ).filter(m ->
                                (m.getModifiers() & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0 &&
                                    (m.getModifiers() & Opcodes.ACC_SYNTHETIC) == 0 &&
                                    !m.isAnnotationPresent(Internal.class) &&
                                    !m.isAnnotationPresent(CoverageIgnore.class) &&
                                    !m.isAnnotationPresent(Stub.class) &&
                                    !m.isAnnotationPresent(Modify.class) &&
                                    !m.getName().startsWith("jvmdg$")
                            ).map(MemberNameAndDesc::fromMember)
                            .map(m -> m.toFullyQualified(e.getValue().target))
                    ).collect(Collectors.toSet());
                var unmatchedStubs = versionProvider.stubMappings.values().stream().flatMap(val ->
                    Stream.of(
                            val.getMethodStubMap().values().stream().map(e -> e.handler.method()),
                            val.getFieldStubMap().values().stream().flatMap(f -> f.getter == null ? Stream.<Method>empty() : Stream.of(f.getter.handler.method())),
                            val.getCtorMap().values().stream().flatMap(e -> Stream.of(e.methods()).map(MethodRef::method)),
                            val.getMethodModifyMap().values().stream().map(MethodRef::method))
                        .flatMap(e -> e)).collect(Collectors.toList());

                // impl method → the JDK member it is registered against; used to auto-exclude
                // registered entries whose member exists on the target surface (BEHAVIOR kind) or
                // resolves through supertypes with a covariant return — neither is "unmatched".
                var implSite = new IdentityHashMap<Method, FullyQualifiedMemberNameAndDesc>();
                var kindCounts = new EnumMap<xyz.wagyourtail.jvmdg.version.StubKind, Integer>(
                    xyz.wagyourtail.jvmdg.version.StubKind.class);
                versionProvider.stubMappings.forEach((owner, val) -> {
                    java.util.function.Function<MemberNameAndDesc, FullyQualifiedMemberNameAndDesc> site =
                        m -> new FullyQualifiedMemberNameAndDesc(owner, m.getName(),
                            remapDesc(m.getDesc(), adapterToJdk));
                    val.getMethodStubMap().forEach((m, e) -> {
                        var s = site.apply(m);
                        implSite.put(e.handler.method(), s);
                        kindCounts.merge(versionProvider.stubRegistry.kindOf(s, false, v + 44), 1, Integer::sum);
                    });
                    val.getFieldStubMap().forEach((m, f) -> {
                        var s = site.apply(m);
                        if (f.getter != null) implSite.put(f.getter.handler.method(), s);
                        if (f.setter != null) implSite.put(f.setter.handler.method(), s);
                        kindCounts.merge(versionProvider.stubRegistry.kindOf(s, false, v + 44), 1, Integer::sum);
                    });
                    val.getCtorMap().forEach((m, e) -> {
                        var s = site.apply(m);
                        for (MethodRef r : e.methods()) {
                            implSite.put(r.method(), s);
                        }
                        kindCounts.merge(versionProvider.stubRegistry.kindOf(s, false, v + 44), 1, Integer::sum);
                    });
                    val.getMethodModifyMap().forEach((m, e) -> {
                        if (m == null) {
                            kindCounts.merge(xyz.wagyourtail.jvmdg.version.StubKind.LANGUAGE, 1, Integer::sum);
                        } else {
                            var s = site.apply(m);
                            implSite.put(e.method(), s);
                            kindCounts.merge(versionProvider.stubRegistry.kindOf(s, true, v + 44), 1, Integer::sum);
                        }
                    });
                });

                try {
                    var requiredStubs = new ArrayList<MemberInfo>();
                    // classes currently holds stubVersion's own surface (compare replaces it with
                    // the target-v surface) — adapter members are existence-checked against the
                    // API they mirror, registered sites against the target they downgrade to
                    var stubVersionSurface = new HashMap<>(classes);
                    compare(versions.get(v), classes, requiredStubs);

                    outer:
                    for (var staticAndStub : requiredStubs) {
                        var isStatic = staticAndStub.isStatic();
                        var isAbstract = staticAndStub.isAbstract();
                        var modName = staticAndStub.module();
                        var stub = staticAndStub.fqm();

                        if (stub.getName() != null) {
                            Set<String> warnings = new HashSet<>();
                            var stubProvider = versionProvider.getStubMapper(stub.getOwner(), warnings);
                            if (!warnings.isEmpty()) {
                                for (var warning : warnings) {
                                    System.err.println(warning);
                                }
                            }
                            // map classes in desc
                            var desc = remapDesc(stub.getDesc(), stubClassTypes);

                            var member = new MemberNameAndDesc(stub.getName(), desc);
                            if (stubClassTypes.containsKey(stub.getOwner())) {
                                if (stubClassMethods.remove(member.toFullyQualified(stubClassTypes.get(stub.getOwner())))) {
                                    availableStubCount++;
                                    continue;
                                }
                            }

                            if (stubProvider.getMethodStubMap().containsKey(member)) {
                                availableStubCount++;
                                unmatchedStubs.remove(stubProvider.getMethodStubMap().get(member).handler.method());
                                continue;
                            }

                            if (stubProvider.getFieldStubMap().containsKey(member)) {
                                availableStubCount++;
                                FieldStubEntry fe = stubProvider.getFieldStubMap().get(member);
                                unmatchedStubs.remove(fe.getter == null ? null : fe.getter.handler.method());
                                continue;
                            }

                            if (stubProvider.getMethodModifyMap().containsKey(member)) {
                                availableStubCount++;
                                unmatchedStubs.remove(stubProvider.getMethodModifyMap().get(member).method());
                                continue;
                            }

                            if (stubProvider.getCtorMap().containsKey(member)) {
                                availableStubCount++;
                                for (MethodRef hm : stubProvider.getCtorMap().get(member).methods()) {
                                    unmatchedStubs.remove(hm.method());
                                }
                                continue;
                            }

                            Deque<ClassMapping> parents = new ArrayDeque<>(stubProvider.getParents());
                            while (!parents.isEmpty()) {
                                ClassMapping parent = parents.pollFirst();
                                if (parent.getMethodStubMap().containsKey(member)) {
                                    onlyOnParentStubCount++;
                                    unmatchedStubs.remove(parent.getMethodStubMap().get(member).handler.method());
                                    parentOnlyStubs.add(new MemberInfo(modName, stub, isAbstract, isStatic));
                                    continue outer;
                                } else if (parent.getFieldStubMap().containsKey(member)) {
                                    onlyOnParentStubCount++;
                                    FieldStubEntry fe = parent.getFieldStubMap().get(member);
                                    unmatchedStubs.remove(fe.getter == null ? null : fe.getter.handler.method());
                                    parentOnlyStubs.add(new MemberInfo(modName, stub, isAbstract, isStatic));
                                    continue outer;
                                } else if (parent.getMethodModifyMap().containsKey(member)) {
                                    onlyOnParentStubCount++;
                                    unmatchedStubs.remove(parent.getMethodModifyMap().get(member).method());
                                    parentOnlyStubs.add(new MemberInfo(modName, stub, isAbstract, isStatic));
                                    continue outer;
                                }
                                for (ClassMapping par : parent.getParents()) {
                                    parents.addFirst(par);
                                }
                            }
                        } else {
                            if (versionProvider.classStubs.containsKey(stub.getOwner())) {
                                availableStubCount++;
                                continue;
                            }
                        }

                        missingStubCount++;
                        missingStubs.add(new MemberInfo(modName, stub, isAbstract, isStatic));

                    }

                    var total = availableStubCount + onlyOnParentStubCount + missingStubCount;
                    System.out.println("Version " + stubVersion + " has " + availableStubCount + " available stubs, " +
                        onlyOnParentStubCount + " only on parent stubs, and " + missingStubCount + " missing stubs. Total: " + total +
                        " (kinds: missingApi=" + kindCounts.getOrDefault(xyz.wagyourtail.jvmdg.version.StubKind.MISSING_API, 0) +
                        ", behavior=" + kindCounts.getOrDefault(xyz.wagyourtail.jvmdg.version.StubKind.BEHAVIOR, 0) +
                        ", language=" + kindCounts.getOrDefault(xyz.wagyourtail.jvmdg.version.StubKind.LANGUAGE, 0) + ")");

                    var missing = Path.of("./coverage/" + stubVersion + "/missing.txt");
                    writeList(missingStubs, missing);

                    var parentOnly = Path.of("./coverage/" + stubVersion + "/parentOnly.txt");
                    writeList(parentOnlyStubs, parentOnly);

                    var unmatched = Path.of("./coverage/" + stubVersion + "/unmatched.txt");
                    writeList(
                        Stream.concat(
                                unmatchedStubs.stream()
                                    .filter(e -> !e.isAnnotationPresent(Internal.class) &&
                                        !e.isAnnotationPresent(CoverageIgnore.class) &&
                                        !existsInSurface(classes, implSite.get(e)))
                                    .map(FullyQualifiedMemberNameAndDesc::of),
                                stubClassMethods.stream()
                                    .filter(e -> !existsInSurface(stubVersionSurface,
                                        new FullyQualifiedMemberNameAndDesc(
                                            adapterToJdk.getOrDefault(e.getOwner(), e.getOwner()), e.getName(),
                                            remapDesc(e.getDesc(), adapterToJdk))))
                            ).map(e -> new MemberInfo("unknown", e, false, false))
                            .collect(Collectors.toList()),
                        unmatched
                    );
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to compare version " + v, e);
                }
            });

        }
    }

    private static void writeList(List<MemberInfo> missing, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());
        Files.deleteIfExists(outputFile);
        if (missing.isEmpty()) return;
        var byModule = new HashMap<String, List<String>>();
        for (var stub : missing) {
            String sta = stub.isStatic() ? "; static" : ";";
            sta = stub.isAbstract() ? "; abstract" : sta;
            byModule.computeIfAbsent(stub.module(), k -> new ArrayList<>()).add(stub.fqm().toString() + sta);
        }
        try (var writer = Files.newBufferedWriter(outputFile)) {
            for (var entry : byModule.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                var mod = entry.getKey();
                for (var stub : entry.getValue().stream().sorted().toList()) {
                    writer.write(mod);
                    writer.write(';');
                    writer.write(stub);
                    writer.write('\n');
                }
            }
        }
    }

    public static ClassNode findClass(String name, List<Path> mods) throws IOException {
        for (Path path : mods) {
            var scn = path.resolve(name + ".sig");
            if (Files.exists(scn)) {
                return ASMUtils.bytesToClassNode(Files.readAllBytes(scn), ClassReader.SKIP_CODE);
            }
        }
        return null;
    }

    /** remap jvmdg adapter types in a desc to their JDK targets via {@code stubClassTypes}. */
    private static Type remapDesc(Type desc, Map<Type, Type> stubClassTypes) {
        if (desc.getSort() == Type.METHOD) {
            var descArgs = desc.getArgumentTypes();
            for (int i = 0; i < descArgs.length; i++) {
                var arg = descArgs[i];
                if (arg.getSort() == Type.OBJECT) {
                    var stubCls = stubClassTypes.get(arg);
                    if (stubCls != null) {
                        descArgs[i] = stubCls;
                    }
                } else if (arg.getSort() == Type.ARRAY) {
                    var dims = arg.getDimensions();
                    var elem = arg.getElementType();
                    if (elem.getSort() == Type.OBJECT) {
                        var stubCls = stubClassTypes.get(elem);
                        if (stubCls != null) {
                            descArgs[i] = Type.getType("[".repeat(dims) + stubCls.getDescriptor());
                        }
                    }
                }
            }
            var ret = desc.getReturnType();
            if (ret.getSort() == Type.OBJECT) {
                var stubCls = stubClassTypes.get(ret);
                if (stubCls != null) {
                    ret = stubCls;
                }
            } else if (ret.getSort() == Type.ARRAY) {
                var dims = ret.getDimensions();
                var elem = ret.getElementType();
                if (elem.getSort() == Type.OBJECT) {
                    var stubCls = stubClassTypes.get(elem);
                    if (stubCls != null) {
                        ret = Type.getType("[".repeat(dims) + stubCls.getDescriptor());
                    }
                }
            }
            return Type.getMethodType(ret, descArgs);
        } else if (desc.getSort() == Type.OBJECT) {
            var stubCls = stubClassTypes.get(desc);
            if (stubCls != null) {
                return stubCls;
            }
        } else if (desc.getSort() == Type.ARRAY) {
            var dims = desc.getDimensions();
            var elem = desc.getElementType();
            if (elem.getSort() == Type.OBJECT) {
                var stubCls = stubClassTypes.get(elem);
                if (stubCls != null) {
                    return Type.getType("[".repeat(dims) + stubCls.getDescriptor());
                }
            }
        }
        return desc;
    }

    /**
     * Does {@code fqn} exist in {@code surface}? Exact member match on the owning class or an
     * inherited declaration — for methods, a supertype declaration with the same name+args and a
     * return the site return is assignable to counts (covariant redeclarations).
     */
    private static boolean existsInSurface(Map<String, Pair<String, ClassNode>> surface,
                                           FullyQualifiedMemberNameAndDesc fqn) {
        if (fqn == null) return false;
        return memberInHierarchy(surface, fqn, new HashSet<>());
    }

    private static boolean memberInHierarchy(Map<String, Pair<String, ClassNode>> surface,
                                             FullyQualifiedMemberNameAndDesc fqn, Set<String> seen) {
        if (!seen.add(fqn.getOwner().getInternalName())) return false;
        var p = surface.get(fqn.getOwner().getInternalName());
        if (p == null) return false;
        var cn = p.getSecond();
        if (fqn.getDesc().getSort() == Type.METHOD) {
            for (MethodNode m : cn.methods) {
                if ((m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) continue;
                if (!m.name.equals(fqn.getName())) continue;
                if (m.desc.equals(fqn.getDesc().getDescriptor())) return true;
                var md = Type.getMethodType(m.desc);
                if (Arrays.equals(md.getArgumentTypes(), fqn.getDesc().getArgumentTypes()) &&
                    isSubtype(surface, fqn.getDesc().getReturnType(), md.getReturnType())) {
                    return true;
                }
            }
        } else {
            for (FieldNode f : cn.fields) {
                if ((f.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) continue;
                if (f.name.equals(fqn.getName()) && f.desc.equals(fqn.getDesc().getDescriptor())) {
                    return true;
                }
            }
        }
        if (cn.superName != null && memberInHierarchy(surface, new FullyQualifiedMemberNameAndDesc(
            Type.getObjectType(cn.superName), fqn.getName(), fqn.getDesc()), seen)) {
            return true;
        }
        if (cn.interfaces != null) {
            for (var itf : cn.interfaces) {
                if (memberInHierarchy(surface, new FullyQualifiedMemberNameAndDesc(
                    Type.getObjectType(itf), fqn.getName(), fqn.getDesc()), seen)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** is {@code sub} assignable to {@code sup} within the type graph reachable via {@code surface}? */
    private static boolean isSubtype(Map<String, Pair<String, ClassNode>> surface, Type sub, Type sup) {
        if (sub.equals(sup)) return true;
        if (sub.getSort() != Type.OBJECT || sup.getSort() != Type.OBJECT) return false;
        var seen = new HashSet<String>();
        var stack = new ArrayDeque<String>();
        stack.push(sub.getInternalName());
        while (!stack.isEmpty()) {
            var n = stack.pop();
            if (!seen.add(n)) continue;
            if (n.equals(sup.getInternalName())) return true;
            var p = surface.get(n);
            if (p == null) {
                // type outside the measured surface — only java/lang/Object is a safe conclusion
                if (sup.getInternalName().equals("java/lang/Object")) return true;
                continue;
            }
            if (p.getSecond().superName != null) stack.push(p.getSecond().superName);
            if (p.getSecond().interfaces != null) stack.addAll(p.getSecond().interfaces);
        }
        return false;
    }

    public static void compare(List<Path> moduleHolders, Map<String, Pair<String, ClassNode>> currentVersion, List<MemberInfo> removed) throws IOException {
        var mods = new ArrayList<Path>();
        for (var mod : moduleHolders) {
            try (var folders = Files.newDirectoryStream(mod)) {
                folders.forEach(mods::add);
            }
        }
        var newClasses = new ConcurrentHashMap<String, Pair<String, ClassNode>>();
        var removedClasses = new ConcurrentSkipListSet<String>();
        var removedList = new ConcurrentLinkedQueue<MemberInfo>();
        mods.parallelStream().forEach(mod -> {
            if (!Files.isDirectory(mod)) {
                return;
            }
            var modName = mod.getFileName().toString();

            if (excludedMods.stream().anyMatch(e -> modName.startsWith(e))) return;
            try (var files = Files.find(mod, Integer.MAX_VALUE, (p, a) -> !a.isDirectory())) {
                files.parallel().forEach(p -> {
                    try {
                        if (!p.toString().endsWith(".class") && !p.toString().endsWith(".sig")) {
                            System.err.println("Skipping " + p.toAbsolutePath());
                            return;
                        }
                        var cn = ASMUtils.bytesToClassNode(Files.readAllBytes(p), ClassReader.SKIP_CODE);
                        if (cn.name.startsWith("jdk/")) return;
                        if (cn.name.startsWith("com/azul/")) return;
                        if (cn.name.startsWith("sun/")) {
                            if (!cn.name.equals("sun/misc/Unsafe")) return;
                        }
                        // if class is annotated with @PreviewFeature, skip
                        if (cn.invisibleAnnotations != null) {
                            for (var a : cn.invisibleAnnotations) {
                                if (a.desc.equals("Ljdk/internal/javac/PreviewFeature;") || a.desc.equals("Ljdk/internal/PreviewFeature;")) {
                                    return;
                                }
                            }
                        }
                        if ((cn.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) return;
                        newClasses.put(cn.name, new Pair<>(modName, cn));
                        if (currentVersion.containsKey(cn.name)) {
                            // check to see what was "removed"
                            var old = currentVersion.get(cn.name);
                            var oldCls = old.getSecond();
                            var members = new HashSet<MemberInfo>();
                            var ct = Type.getObjectType(cn.name);
                            outerA:
                            for (var m : oldCls.methods) {
                                if ((m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0 || (m.access & Opcodes.ACC_SYNTHETIC) != 0)
                                    continue;
                                if (m.name.equals("<clinit>")) continue;
                                if (m.invisibleAnnotations != null) {
                                    for (var a : m.invisibleAnnotations) {
                                        if (a.desc.equals("Ljdk/internal/javac/PreviewFeature;") || a.desc.equals("Ljdk/internal/PreviewFeature;")) {
                                            continue outerA;
                                        }
                                    }
                                }
                                var isStatic = (m.access & Opcodes.ACC_STATIC) != 0;
                                var isAbstract = (m.access & Opcodes.ACC_ABSTRACT) != 0;
                                var fqn = new FullyQualifiedMemberNameAndDesc(ct, m.name, Type.getMethodType(m.desc));
                                members.add(new MemberInfo(modName, fqn, isAbstract, isStatic));
                            }
                            outerA2:
                            for (var f : oldCls.fields) {
                                if ((f.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0 || (f.access & Opcodes.ACC_SYNTHETIC) != 0)
                                    continue;
                                if (f.invisibleAnnotations != null) {
                                    for (var a : f.invisibleAnnotations) {
                                        if (a.desc.equals("Ljdk/internal/javac/PreviewFeature;") || a.desc.equals("Ljdk/internal/PreviewFeature;")) {
                                            continue outerA2;
                                        }
                                    }
                                }
                                var isStatic = (f.access & Opcodes.ACC_STATIC) != 0;
                                var fqn = new FullyQualifiedMemberNameAndDesc(ct, f.name, Type.getType(f.desc));
                                members.add(new MemberInfo(modName, fqn, false, isStatic));
                            }
                            outerB:
                            for (var m : cn.methods) {
                                // is preview feature?
                                if (m.invisibleAnnotations != null) {
                                    for (var a : m.invisibleAnnotations) {
                                        if (a.desc.equals("Ljdk/internal/javac/PreviewFeature;") || a.desc.equals("Ljdk/internal/PreviewFeature;")) {
                                            continue outerB;
                                        }
                                    }
                                }
                                var isStatic = (m.access & Opcodes.ACC_STATIC) != 0;
                                var isAbstract = (m.access & Opcodes.ACC_ABSTRACT) != 0;
                                var fqn = new FullyQualifiedMemberNameAndDesc(ct, m.name, Type.getMethodType(m.desc));
                                members.remove(new MemberInfo(modName, fqn, isAbstract, isStatic));
                            }
                            outerB2:
                            for (var f : cn.fields) {
                                if (f.invisibleAnnotations != null) {
                                    for (var a : f.invisibleAnnotations) {
                                        if (a.desc.equals("Ljdk/internal/javac/PreviewFeature;") || a.desc.equals("Ljdk/internal/PreviewFeature;")) {
                                            continue outerB2;
                                        }
                                    }
                                }
                                var isStatic = (f.access & Opcodes.ACC_STATIC) != 0;
                                var fqn = new FullyQualifiedMemberNameAndDesc(ct, f.name, Type.getType(f.desc));
                                members.remove(new MemberInfo(modName, fqn, false, isStatic));
                            }

                            // check if method(s) still exist on parent (include interfaces in traversal)
                            Deque<ClassNode> parents = new ArrayDeque<>();
                            for (var iface : cn.interfaces) {
                                var cls = findClass(iface, mods);
                                if (cls != null) {
                                    parents.add(cls);
                                }
                            }
                            {
                                var cls = findClass(cn.superName, mods);
                                if (cls != null) {
                                    parents.add(cls);
                                }
                            }
                            while (!parents.isEmpty()) {
                                var superCls = parents.pollFirst();
                                if (superCls == null) continue;
                                for (var m : superCls.methods) {
                                    if (m.name.equals("<clinit>") || m.name.equals("<init>")) continue;
                                    var isStatic = (m.access & Opcodes.ACC_STATIC) != 0;
                                    if (isStatic) continue;
                                    var isAbstract = (m.access & Opcodes.ACC_ABSTRACT) != 0;
                                    var fqn = new FullyQualifiedMemberNameAndDesc(ct, m.name, Type.getMethodType(m.desc));
                                    members.remove(new MemberInfo(modName, fqn, isAbstract, false));
                                }
                                if (!superCls.name.equals("java/lang/Object")) {
                                    for (var iface : superCls.interfaces) {
                                        var cls = findClass(iface, mods);
                                        if (cls != null) {
                                            parents.add(cls);
                                        }
                                    }
                                    var cls = findClass(superCls.superName, mods);
                                    if (cls != null) {
                                        parents.add(cls);
                                    }
                                }
                            }
                            removedList.addAll(members);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to read " + p.toAbsolutePath(), e);
                    } catch (RuntimeException e) {
                        if (p.toString().endsWith("/module-info.sig")) {
                            // ASM library may fail to read some module-info.sig
                            System.err.println("Failed to read " + p.toAbsolutePath());
                        } else throw new RuntimeException("Failed to read " + p.toAbsolutePath(), e);
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to list " + mod, e);
            }
        });
        removed.addAll(removedList);
        // find classes that were "removed"
        for (var cls : currentVersion.entrySet()) {
            if (!newClasses.containsKey(cls.getKey())) {
                if (removedClasses.add(cls.getKey())) {
                    removed.add(new MemberInfo(cls.getValue().getFirst(), new FullyQualifiedMemberNameAndDesc(Type.getObjectType(cls.getKey()), null, null), false, false));
                    // add all methods
                    var oldCls = cls.getValue().getSecond();
                    outer:
                    for (var method : oldCls.methods) {
                        if ((method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0 || (method.access & Opcodes.ACC_SYNTHETIC) != 0)
                            continue;
                        if (method.name.equals("<clinit>")) continue;
                        if (method.invisibleAnnotations != null) {
                            for (var a : method.invisibleAnnotations) {
                                if (a.desc.equals("Ljdk/internal/javac/PreviewFeature;") || a.desc.equals("Ljdk/internal/PreviewFeature;")) {
                                    continue outer;
                                }
                            }
                        }
                        var isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
                        var isAbstract = (method.access & Opcodes.ACC_ABSTRACT) != 0;
                        removed.add(new MemberInfo(cls.getValue().getFirst(), new FullyQualifiedMemberNameAndDesc(Type.getObjectType(cls.getKey()), method.name, Type.getMethodType(method.desc)), isAbstract, isStatic));
                    }
                    // add all fields
                    outer2:
                    for (var field : oldCls.fields) {
                        if ((field.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0 || (field.access & Opcodes.ACC_SYNTHETIC) != 0)
                            continue;
                        if (field.invisibleAnnotations != null) {
                            for (var a : field.invisibleAnnotations) {
                                if (a.desc.equals("Ljdk/internal/javac/PreviewFeature;") || a.desc.equals("Ljdk/internal/PreviewFeature;")) {
                                    continue outer2;
                                }
                            }
                        }
                        var isStatic = (field.access & Opcodes.ACC_STATIC) != 0;
                        removed.add(new MemberInfo(cls.getValue().getFirst(), new FullyQualifiedMemberNameAndDesc(Type.getObjectType(cls.getKey()), field.name, Type.getType(field.desc)), false, isStatic));
                    }
                }
            }
        }
        // add new classes
        currentVersion.clear();
        currentVersion.putAll(newClasses);
    }

    public record MemberInfo(String module, FullyQualifiedMemberNameAndDesc fqm, boolean isAbstract, boolean isStatic) {
    }

}
