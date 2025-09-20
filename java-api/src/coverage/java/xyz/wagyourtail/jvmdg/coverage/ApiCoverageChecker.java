package xyz.wagyourtail.jvmdg.coverage;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.CoverageIgnore;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Stub;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
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
        "jdk.internal.vm.compiler.management",
        "com.azul.crs.client",
        "com.azul.tooling"
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
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getFirst()));
                Set<FullyQualifiedMemberNameAndDesc> stubClassMethods = versionProvider.classStubs.entrySet().stream()
                    .flatMap(e -> Stream.concat(
                                Arrays.stream(e.getValue().getSecond().getFirst().getDeclaredMethods()),
                                Arrays.stream(e.getValue().getSecond().getFirst().getConstructors())
                            ).filter(m ->
                                (m.getModifiers() & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0 &&
                                    (m.getModifiers() & Opcodes.ACC_SYNTHETIC) == 0 &&
                                    !m.isAnnotationPresent(CoverageIgnore.class) &&
                                    !m.isAnnotationPresent(Stub.class) &&
                                    !m.isAnnotationPresent(Modify.class) &&
                                    !m.getName().startsWith("jvmdg$")
                            ).map(MemberNameAndDesc::fromMember)
                            .map(m -> m.toFullyQualified(e.getValue().getFirst()))
                    ).collect(Collectors.toSet());
                var unmatchedStubs = versionProvider.stubMappings.values().stream().flatMap(value ->
                    Stream.of(value.getMethodStubMap().values().stream(), value.getMethodModifyMap().values().stream())
                        .flatMap(e -> e)).map(Pair::getFirst).collect(Collectors.toList());

                try {
                    var requiredStubs = new ArrayList<MemberInfo>();
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
                            var desc = stub.getDesc();
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
                            var member = new MemberNameAndDesc(stub.getName(), Type.getMethodType(ret, descArgs));

                            if (stubClassTypes.containsKey(stub.getOwner())) {
                                if (stubClassMethods.remove(member.toFullyQualified(stubClassTypes.get(stub.getOwner())))) {
                                    availableStubCount++;
                                    continue;
                                }
                            }

                            if (stubProvider.getMethodStubMap().containsKey(member)) {
                                availableStubCount++;
                                unmatchedStubs.remove(stubProvider.getMethodStubMap().get(member).getFirst());
                                continue;
                            }

                            if (stubProvider.getMethodModifyMap().containsKey(member)) {
                                availableStubCount++;
                                unmatchedStubs.remove(stubProvider.getMethodModifyMap().get(member).getFirst());
                                continue;
                            }

                            Deque<ClassMapping> parents = new ArrayDeque<>(stubProvider.getParents());
                            while (!parents.isEmpty()) {
                                ClassMapping parent = parents.pollFirst();
                                if (parent.getMethodStubMap().containsKey(member)) {
                                    onlyOnParentStubCount++;
                                    unmatchedStubs.remove(parent.getMethodStubMap().get(member).getFirst());
                                    parentOnlyStubs.add(new MemberInfo(modName, stub, isAbstract, isStatic));
                                    continue outer;
                                } else if (parent.getMethodModifyMap().containsKey(member)) {
                                    onlyOnParentStubCount++;
                                    unmatchedStubs.remove(parent.getMethodModifyMap().get(member).getFirst());
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
                        onlyOnParentStubCount + " only on parent stubs, and " + missingStubCount + " missing stubs. Total: " + total);

                    var missing = Path.of("./coverage/" + stubVersion + "/missing.txt");
                    writeList(missingStubs, missing);

                    var parentOnly = Path.of("./coverage/" + stubVersion + "/parentOnly.txt");
                    writeList(parentOnlyStubs, parentOnly);

                    var unmatched = Path.of("./coverage/" + stubVersion + "/unmatched.txt");
                    writeList(
                        Stream.concat(
                                unmatchedStubs.stream()
                                    .filter(e -> !e.isAnnotationPresent(CoverageIgnore.class))
                                    .map(FullyQualifiedMemberNameAndDesc::of),
                                stubClassMethods.stream()
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

            if (excludedMods.contains(modName)) return;
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
                            var methods = new HashSet<MemberInfo>();
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
                                methods.add(new MemberInfo(modName, fqn, isAbstract, isStatic));
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
                                methods.remove(new MemberInfo(modName, new FullyQualifiedMemberNameAndDesc(
                                    ct, m.name, Type.getMethodType(m.desc)), isAbstract, isStatic));
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
                                    methods.remove(new MemberInfo(modName, fqn, isAbstract, false));
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
                            removedList.addAll(methods);
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
