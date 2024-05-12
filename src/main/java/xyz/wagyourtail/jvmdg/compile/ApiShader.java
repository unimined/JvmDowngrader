package xyz.wagyourtail.jvmdg.compile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.compile.shade.ReferenceGraph;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class ApiShader {

    public static void main(String[] args) throws IOException {
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
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        shadeApis(target, prefix, input, output, downgradedApi);
    }

    public static void downgradedApi(int target, Path api, Path targetPath) throws IOException {
        ZipDowngrader.downgradeZip(target, api, new HashSet<URL>(), targetPath);
    }

    public static void shadeApis(int target, String prefix, File input, File output, File downgradedApi) throws IOException {
        Path downgradedApiPath;
        if (downgradedApi == null) {
            // step 1: downgrade the api to the target version
            Path temp = Files.createTempDirectory(".jvmdg");
            Path api = temp.resolve("api.jar");
            try (InputStream stream = ClassDowngrader.javaApi.openStream()) {
                Files.copy(stream, api, StandardCopyOption.REPLACE_EXISTING);
            }
            downgradedApiPath = temp.resolve("downgraded-api.jar");
            downgradedApi(target, api, downgradedApiPath);
        } else {
            downgradedApiPath = downgradedApi.toPath();
        }
        shadeApis(prefix, input.toPath(), output.toPath(), downgradedApiPath);
    }

    public static void shadeApis(final String prefix, Path input, Path output, Path downgradedApi) throws IOException {
        if (!prefix.endsWith("/")) throw new IllegalArgumentException("prefix must end with /");
        // step 2: collect classes in the api and their references
        ReferenceGraph refs = new ReferenceGraph();
        final Set<Type> apiClasses;
        try (FileSystem apiFs = Utils.openZipFileSystem(downgradedApi, new HashMap<String, Object>())) {
            Path apiRoot = apiFs.getPath("/");
            Map<Path, Type> preScan = refs.preScan(apiFs.getPath("/"));
            apiClasses = new HashSet<>(preScan.values());
            refs.scan(preScan, new ReferenceGraph.Filter() {
                @Override
                public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
                    return apiClasses.contains(member.getOwner());
                }
            });
            try (final FileSystem inputFs = Utils.openZipFileSystem(input, new HashMap<String, Object>())) {
                final Path inputRoot = inputFs.getPath("/");
                // step 3: traverse the input classes for references to the api
                ReferenceGraph inputRefs = new ReferenceGraph();
                inputRefs.scan(inputFs.getPath("/"), new ReferenceGraph.Filter() {
                    @Override
                    public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
                        return apiClasses.contains(member.getOwner());
                    }
                });
                Map<String, Object> env = new HashMap<>();
                env.put("create", "true");
                try (final FileSystem outputFs = Utils.openZipFileSystem(output, env)) {
                    final Path outputRoot = outputFs.getPath("/");
                    // step 4: create remapper for api classes to prefixed api classes
                    Pair<Set<FullyQualifiedMemberNameAndDesc>, Set<String>> required = refs.recursiveResolveFrom(inputRefs.getAllRefs());
                    final Map<Type, Set<MemberNameAndDesc>> byType = byType(required.getFirst());
                    final Map<String, String> remap = new HashMap<>();
                    for (Type type : byType.keySet()) {
                        remap.put(type.getInternalName(), prefix + type.getInternalName());
                    }
                    final SimpleRemapper remapper = new SimpleRemapper(remap);
                    // step 5: actually write the referenced api classes to the output, removing unused parts from them.
                    for (Map.Entry<Type, Set<MemberNameAndDesc>> type : byType.entrySet()) {
                        Path inPath = apiRoot.resolve(type.getKey().getInternalName() + ".class");
                        Path outPath = outputRoot.resolve(prefix + type.getKey().getInternalName() + ".class");
                        Path parent = outPath.getParent();
                        if (parent != null) {
                            Files.createDirectories(outputRoot.resolve(parent));
                        }
                        // load api class as a ClassNode
                        ClassNode node = new ClassNode();
                        try (InputStream stream = Files.newInputStream(inPath)) {
                            new ClassReader(stream).accept(node, 0);
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
                        Files.write(outPath, writer.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    }

                    for (String resource : required.getSecond()) {
                        Path inPath = apiRoot.resolve(resource);
                        Path outPath = outputRoot.resolve(resource);
                        Path parent = outPath.getParent();
                        if (parent != null) {
                            Files.createDirectories(outputRoot.resolve(parent));
                        }
                        Files.copy(inPath, outPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    // step 6: remap references to the api to start with prefix
                    Files.walkFileTree(inputFs.getPath("/"), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            Path output = outputRoot.resolve(inputRoot.relativize(dir));
                            Files.createDirectories(output);
                            return super.preVisitDirectory(dir, attrs);
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Path output = outputRoot.resolve(inputRoot.relativize(file));
                            if (file.toString().endsWith(".class")) {
                                byte[] data = Files.readAllBytes(file);
                                ClassWriter writer = new ClassWriter(0);
                                new ClassReader(data).accept(new ClassRemapper(writer, remapper), 0);
                                Files.write(output, writer.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                            } else {
                                Files.copy(file, output, StandardCopyOption.REPLACE_EXISTING);
                            }
                            return super.visitFile(file, attrs);
                        }
                    });
                }
            }
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
