package xyz.wagyourtail.jvmdg.coverage;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CoverageChecker {

    public static void main(String[] args) throws IOException {
        var home = Path.of(System.getProperty("java.home")).resolve("lib/ct.sym");
        if (!Files.exists(home)) {
            System.err.println("Failed to find \"" + home + "\" for coverage. Please ensure you are using a JDK and not a JRE.");
            System.exit(1);
        }

        System.out.println("Found \"" + home + "\" for coverage.");

        try (var fs = Utils.openZipFileSystem(home, new HashMap<>())) {
            System.out.println("Successfully opened \"" + home + "\" for coverage.");

            var versions = new HashMap<Integer, List<Path>>();
            try (var folders = Files.newDirectoryStream(fs.getPath("/"))) {
                for (var folder : folders) {
                    folder.getFileName().toString().chars().map(c -> Character.digit(c, 36)).forEach(javaVersion -> {
                        versions.computeIfAbsent(javaVersion, k -> new ArrayList<>()).add(folder);
                    });
                }
            }

            var current = Runtime.version().feature();
            var currentModules = Path.of(URI.create("jrt:/"));
            versions.computeIfAbsent(current, k -> new ArrayList<>()).add(currentModules);

            var classes = new HashMap<String, Pair<String, ClassNode>>();

            // load initial classes
            compare(versions.get(current), classes, new ArrayList<>());

            versions.keySet().stream().sorted((a, b) -> -a.compareTo(b)).forEach(v -> {
                var missingStubs = new ArrayList<Pair<Pair<String, FullyQualifiedMemberNameAndDesc>, Boolean>>();
                var parentOnlyStubs = new ArrayList<Pair<Pair<String, FullyQualifiedMemberNameAndDesc>, Boolean>>();

                var stubVersion = v + 1;
                var availableStubCount = 0;
                var onlyOnParentStubCount = 0;
                var missingStubCount = 0;
                var versionProvider = ClassDowngrader.currentVersionDowngrader.getVersionProviderFor(stubVersion);

                if (versionProvider == null) {
                    System.out.println("No version provider for " + stubVersion);
                    return;
                }
                System.out.println("Checking version " + stubVersion);

                try {
                    var requiredStubs = new ArrayList<Pair<Boolean, Pair<String, FullyQualifiedMemberNameAndDesc>>>();
                    compare(versions.get(v), classes, requiredStubs);

                    outer:for (var staticAndStub : requiredStubs) {
                        var isStatic = staticAndStub.getFirst();
                        var modName = staticAndStub.getSecond().getFirst();
                        var stub = staticAndStub.getSecond().getSecond();

                        if (stub.getName() != null) {
                            var stubProvider = versionProvider.getStubMapper(stub.getOwner());
                            var member = stub.toMemberNameAndDesc();

                            if (stubProvider.getMethodStubMap().containsKey(stub.toMemberNameAndDesc()) || stubProvider.getMethodModifyMap().containsKey(member)) {
                                availableStubCount++;
                                continue;
                            }

                            for (var parent : stubProvider.getParents()) {
                                if (parent.getMethodStubMap().containsKey(stub.toMemberNameAndDesc()) || parent.getMethodModifyMap().containsKey(member)) {

                                    onlyOnParentStubCount++;
                                    parentOnlyStubs.add(new Pair<>(new Pair<>(modName, stub), isStatic));

                                    continue outer;
                                }
                            }
                        } else {
                            if (versionProvider.stubMappings.containsKey(stub.getOwner())) {
                                availableStubCount++;
                                continue;
                            }
                        }

                        missingStubCount++;
                        missingStubs.add(new Pair<>(new Pair<>(modName, stub), isStatic));

                    }

                    var total = availableStubCount + onlyOnParentStubCount + missingStubCount;
                    System.out.println("Version " + stubVersion + " has " + availableStubCount + " available stubs, " + onlyOnParentStubCount + " only on parent stubs, and " + missingStubCount + " missing stubs. Total: " + total);

                    if (!missingStubs.isEmpty()) {
                        var missing = Path.of("./coverage/" + stubVersion + "/missing.txt");
                        writeList(missingStubs, missing);
                    }

                    if (!parentOnlyStubs.isEmpty()) {
                        var parentOnly = Path.of("./coverage/" + stubVersion + "/parentOnly.txt");
                        writeList(parentOnlyStubs, parentOnly);
                    }

                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to compare version " + v, e);
                }
            });

        }
    }

    private static void writeList(List<Pair<Pair<String, FullyQualifiedMemberNameAndDesc>, Boolean>> missing, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());
        Files.deleteIfExists(outputFile);
        var byModule = new HashMap<String, List<String>>();
        for (var stub : missing) {
            byModule.computeIfAbsent(stub.getFirst().getFirst(), k -> new ArrayList<>()).add(stub.getFirst().getSecond().toString() + (stub.getSecond() ? "; static" : ";"));
        }
        try (var writer = Files.newBufferedWriter(outputFile)) {
            for (var entry : byModule.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                var mod = entry.getKey();
                for (var stub : entry.getValue()) {
                    writer.write(mod);
                    writer.write(';');
                    writer.write(stub);
                    writer.write('\n');
                }
            }
        }
    }

    public static void compare(List<Path> moduleHolders, Map<String, Pair<String, ClassNode>> currentVersion, List<Pair<Boolean, Pair<String, FullyQualifiedMemberNameAndDesc>>> removed) throws IOException {
        var mods = new ArrayList<Path>();
        for (var mod : moduleHolders) {
            try (var folders = Files.newDirectoryStream(mod)) {
                folders.forEach(mods::add);
            }
        }
        var newClasses = new HashMap<String, Pair<String, ClassNode>>();
        var removedClasses = new HashSet<String>();
        for (var mod : mods) {
            if (!Files.isDirectory(mod)) {
                continue;
            }
            var modName = mod.getFileName().toString();
            try (var files = Files.find(mod, Integer.MAX_VALUE, (p, a) -> a.isRegularFile())) {
                files.forEach(p -> {
                    try {
                        if (!p.toString().endsWith(".class") && !p.toString().endsWith(".sig")) return;
                        var cn = ClassDowngrader.bytesToClassNode(Files.readAllBytes(p), ClassReader.SKIP_CODE);
                        if (cn.name.startsWith("jdk/")) return;
                        if (cn.name.startsWith("sun/")) {
                            if (!cn.name.equals("sun/misc/Unsafe")) return;
                        }
                        if ((cn.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) return;
                        newClasses.put(cn.name, new Pair<>(modName, cn));
                        if (currentVersion.containsKey(cn.name)) {
                            // check to see what was "removed"
                            var old = currentVersion.get(cn.name);
                            var oldCls = old.getSecond();
                            var methods = new HashSet<MemberNameAndDesc>();
                            for (var m : oldCls.methods) {
                                if ((m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) continue;
                                if (m.name.equals("<clinit>")) continue;
                                methods.add(new MemberNameAndDesc(m.name, Type.getMethodType(m.desc)));
                            }
                            for (var m : cn.methods) {
                                var md = new MemberNameAndDesc(m.name, Type.getMethodType(m.desc));
                                methods.remove(md);
                            }
                            var ct = Type.getObjectType(cn.name);
                            for (var m : methods) {
                                removed.add(new Pair<>(false, new Pair<>(modName, m.toFullyQualified(ct))));
                            }
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to read " + p.toAbsolutePath(), e);
                    }
                });
            }
        }
        // find classes that were "removed"
        for (var cls : currentVersion.keySet()) {
            if (!newClasses.containsKey(cls)) {
                if (removedClasses.add(cls)) {
                    removed.add(new Pair<>(false, new Pair<>(currentVersion.get(cls).getFirst(), new FullyQualifiedMemberNameAndDesc(Type.getObjectType(cls), null, null))));
                }
            }
        }
        // add new classes
        currentVersion.clear();
        currentVersion.putAll(newClasses);
    }

}
