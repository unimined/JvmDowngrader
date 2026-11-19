package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.compile.ManifestUtils;
import xyz.wagyourtail.jvmdg.config.ShadeReflection;
import xyz.wagyourtail.jvmdg.pipeline.StageHook;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.stub.reflection.ReflectionTable;
import xyz.wagyourtail.jvmdg.stub.runtime.RetroTypes;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The post-scan global barrier: with every post-transform class and captured
 * variant indexed, computes the api closure per jar slot and materializes
 * everything downstream stages and the writer consume —
 * <ul>
 *   <li>per-slot {@code required} closure into {@link ShadeContext#closures}
 *       (the work {@code ApiShader} does per jar version)</li>
 *   <li>inline candidates inverted to per-slot {@link ShadeContext#inlineTargets}</li>
 *   <li>per-slot prefix {@link ShadeContext#remappers}</li>
 *   <li>pruned + prefixed api {@link ClassNode}s into {@link ShadeContext#emitted}</li>
 *   <li>api resource copies into {@link TransformContext#emitResource}</li>
 *   <li>captured variants shaded in place (inline + prefix) — they can't flow
 *       through the class loop because the worklist is name-keyed</li>
 * </ul>
 * Runs on the scan stage's successor; the class list it sees is unchanged.
 */
public class ShadeClosureBarrier implements StageHook {

    /** Deterministic ordering for entry types that aren't Comparable — by their string forms. */
    private static final Comparator<FullyQualifiedMemberNameAndDesc> FQN_ORDER =
        Comparator.comparing(Object::toString);

    private static List<FullyQualifiedMemberNameAndDesc> sortFqns(Set<FullyQualifiedMemberNameAndDesc> in) {
        List<FullyQualifiedMemberNameAndDesc> out = new ArrayList<FullyQualifiedMemberNameAndDesc>(in);
        out.sort(FQN_ORDER);
        return out;
    }

    /** byType entries sorted by owner internal name — FQN/Type aren't Comparable. */
    private static List<Map.Entry<Type, Set<MemberNameAndDesc>>> sortTypes(Map<Type, Set<MemberNameAndDesc>> byType) {
        List<Map.Entry<Type, Set<MemberNameAndDesc>>> out = new ArrayList<Map.Entry<Type, Set<MemberNameAndDesc>>>(byType.entrySet());
        out.sort(Comparator.comparing(e -> e.getKey().getInternalName()));
        return out;
    }

    private final ShadeContext context;

    public ShadeClosureBarrier(ShadeContext context) {
        this.context = context;
    }

    @Override
    public void run(TransformContext ctx) {
        indexVariants(ctx);
        for (int position : context.inputIndex.versions()) {
            computePosition(ctx, position);
        }
        shadeVariants(ctx);
    }

    /** Indexes captured variants at their slots — they never reach the scan transform. */
    private void indexVariants(TransformContext ctx) {
        if (ctx.variants == null) {
            return;
        }
        for (List<ClassNode> variantSet : ctx.variants.values()) {
            for (ClassNode node : variantSet) {
                final ShadeContext shade = context;
                final int position = context.positionOf(node, ctx.flags);
                context.inputIndex.add(node, position, new RefExtractor.Filter() {
                    @Override
                    public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
                        shade.noteInputRef(position, member);
                        return shade.apiClasses.contains(member.getOwner());
                    }
                });
                ReferenceScanTransform.noteDynamicSites(shade, node, position);
            }
        }
    }

    /**
     * One jar slot's closure product — mirrors {@code ApiShader.shadeApis}'s
     * per-version body minus the writes: closure, inline candidates, prefix
     * remap, api-class emission, resource copies.
     */
    private void computePosition(TransformContext ctx, final int position) {
        Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> starts =
            new HashMap<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>();
        for (Map.Entry<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> e
                : context.inputIndex.usages(position).entrySet()) {
            starts.put(e.getKey(), new HashSet<FullyQualifiedMemberNameAndDesc>(e.getValue()));
        }
        warnDynamicReflectionSites(ctx, position);
        if (ctx.flags.shadeReflection != ShadeReflection.NONE) {
            injectReflectionRoots(ctx, position, starts);
        }
        Pair<Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>, Set<String>> required =
            context.apiIndex.closureWithUsages(starts, position);
        context.closures.put(position, required);

        Set<Type> keys = new HashSet<Type>();
        for (Map.Entry<Type, Set<Integer>> e : context.inputIndex.keys().entrySet()) {
            if (e.getValue().contains(position)) {
                keys.add(e.getKey());
            }
        }

        if (ctx.flags.shadeInlining) {
            // api member -> sole consuming input class; inverted for per-class lookup
            Map<FullyQualifiedMemberNameAndDesc, Type> candidates =
                ApiShader.inlineCandidates(ctx.logger, required.getFirst(), keys);
            Map<Type, Set<FullyQualifiedMemberNameAndDesc>> targets =
                new HashMap<Type, Set<FullyQualifiedMemberNameAndDesc>>();
            for (FullyQualifiedMemberNameAndDesc apiMember : sortFqns(candidates.keySet())) {
                required.getFirst().remove(apiMember);
                required.getFirst().remove(new FullyQualifiedMemberNameAndDesc(apiMember.getOwner(), null, null));
                Set<FullyQualifiedMemberNameAndDesc> members = targets.get(candidates.get(apiMember));
                if (members == null) {
                    members = new TreeSet<FullyQualifiedMemberNameAndDesc>(FQN_ORDER);
                    targets.put(candidates.get(apiMember), members);
                }
                members.add(apiMember);
            }
            context.inlineTargets.put(position, targets);
        }

        Map<Type, Set<MemberNameAndDesc>> byType = ApiShader.byType(required.getFirst().keySet());
        Map<String, String> remap = new HashMap<String, String>();
        for (Type type : byType.keySet()) {
            remap.put(type.getInternalName(), context.prefix + type.getInternalName());
        }
        SimpleRemapper remapper = new SimpleRemapper(remap);
        context.remappers.put(position, remapper);

        emitApiClasses(ctx, position, byType, remapper);
        emitResources(ctx, position, required.getSecond(), remapper);
    }

    /**
     * The pruned, prefixed api classes for one slot — same recipe as the jar
     * flow: copy the retained node, strip unused members (non-enum), visit
     * through the prefix remapper. The copy is new in this flow: the jar
     * code prunes the retained node itself, letting an earlier slot's
     * pruning leak into a later slot's resolution of the same type.
     */
    private void emitApiClasses(TransformContext ctx, int position, Map<Type, Set<MemberNameAndDesc>> byType, SimpleRemapper remapper) {
        for (Map.Entry<Type, Set<MemberNameAndDesc>> type : sortTypes(byType)) {
            ClassNode retained = context.apiIndex.nodeFor(type.getKey(), position);
            if (retained == null) {
                ctx.diagnostics.warn("shade", "no api class at slot " + position + " for required type " + type.getKey().getInternalName());
                continue;
            }
            ClassNode working = new ClassNode();
            retained.accept(working);
            if ((working.access & Opcodes.ACC_ENUM) == 0) {
                Set<MemberNameAndDesc> members = type.getValue();
                Iterator<MethodNode> iter = working.methods.iterator();
                while (iter.hasNext()) {
                    MethodNode method = iter.next();
                    if (!members.contains(new MemberNameAndDesc(method.name, Type.getMethodType(method.desc)))) {
                        iter.remove();
                    }
                }
                Iterator<FieldNode> fiter = working.fields.iterator();
                while (fiter.hasNext()) {
                    FieldNode field = fiter.next();
                    if (!members.contains(new MemberNameAndDesc(field.name, Type.getType(field.desc)))) {
                        fiter.remove();
                    }
                }
            }
            ClassNode prefixed = new ClassNode();
            working.accept(new ClassRemapper(prefixed, remapper));
            context.emit(prefixed, ctx.flags);
        }
    }

    /** Api resource copies for one slot — signature files never carried. */
    private void emitResources(TransformContext ctx, int position, Set<String> resources, SimpleRemapper remapper) {
        for (String resource : new TreeSet<String>(resources)) {
            if (ManifestUtils.isSignatureFile(resource) ||
                resource.startsWith("META-INF/services/")) {
                // service files route through emitServiceFiles — their
                // provider lines need the remapper, a raw copy would keep
                // unprefixed names
                continue;
            }
            for (Path apiRoot : context.apiRoots) {
                Path inPath = apiRoot.resolve(resource);
                try {
                    if (Files.exists(inPath)) {
                        byte[] bytes = Files.readAllBytes(inPath);
                        if (resource.equals(RetroTypes.RESOURCE)) {
                            // the table's api-internal names (adapter + marker
                            // classes) carry the prefix like every other api
                            // reference — JDK names map to themselves
                            bytes = remapRetroTypes(new String(bytes, StandardCharsets.UTF_8), remapper)
                                .getBytes(StandardCharsets.UTF_8);
                        } else if (resource.equals(RetroTypes.WRAP_RESOURCE)) {
                            // retro<TAB>stub<TAB>member=impl,… — key + member
                            // names are JDK (unmapped); stub + impl names are
                            // api classes and carry the prefix
                            bytes = remapWraps(new String(bytes, StandardCharsets.UTF_8), remapper)
                                .getBytes(StandardCharsets.UTF_8);
                        } else if (resource.equals(RetroTypes.GAINED_CALLS_RESOURCE) ||
                            resource.equals(RetroTypes.GAINED_CALLS_MANUAL_RESOURCE)) {
                            // I<TAB>m desc<TAB>T=since:declarer[>site];… — the
                            // gained/gained-calls interface + member names are
                            // JDK (unmapped); each site's owner is an api stub
                            // class and carries the prefix
                            bytes = remapGainedCalls(new String(bytes, StandardCharsets.UTF_8), remapper)
                                .getBytes(StandardCharsets.UTF_8);
                        } else if (resource.equals(ReflectionTable.RESOURCE)) {
                            // T<TAB>source<TAB>target<TAB>since — the source
                            // is a JDK name (unmapped); the target is the api
                            // stand-in class and carries the prefix, while rows
                            // whose target didn't make the closure are dropped —
                            // they could never resolve in this jar anyway
                            bytes = remapReflection(new String(bytes, StandardCharsets.UTF_8), remapper,
                                context.apiIndex, position, ctx.flags.shadeReflection)
                                .getBytes(StandardCharsets.UTF_8);
                            // the reader resolves the table relative to its
                            // own package — emit it under the prefixed package
                            // so two prefixed jars each read only their rows
                            ctx.emitResource(context.prefix + resource, bytes);
                            break;
                        }
                        ctx.emitResource(resource, bytes);
                        break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read api resource " + inPath, e);
                }
            }
        }
        emitServiceFiles(ctx, remapper);
    }

    /**
     * {@code META-INF/services/<iface>} copies for the slot — discovered on
     * the api roots directly rather than the requires-resource set (the SPI
     * contract is implicit, no class declares needing the file): renamed
     * when the service interface is an emitted api class, provider lines
     * prefixed for emitted api providers, dropped for pruned ones.
     */
    private void emitServiceFiles(TransformContext ctx, SimpleRemapper remapper) {
        for (Path apiRoot : context.apiRoots) {
            Path dir = apiRoot.resolve("META-INF/services");
            if (!Files.isDirectory(dir)) {
                continue;
            }
            List<Path> files = new ArrayList<Path>();
            try {
                for (Path file : Files.newDirectoryStream(dir)) {
                    if (Files.isRegularFile(file)) {
                        files.add(file);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to list api services " + dir, e);
            }
            files.sort(java.util.Comparator.naturalOrder());
            for (Path file : files) {
                try {
                    Map.Entry<String, byte[]> mapped = ApiShader.mapServiceEntry(
                        "META-INF/services/" + file.getFileName().toString(),
                        Files.readAllBytes(file), remapper, context.apiClasses);
                    if (mapped != null) {
                        ctx.emitResource(mapped.getKey(), mapped.getValue());
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read api service " + file, e);
                }
            }
        }
    }

    /** rewrite {@code <retro>\t<impl>,<impl>,…} rows so shaded member names match the prefixed classes */
    private static String remapRetroTypes(String content, SimpleRemapper remapper) {
        if (content.isEmpty()) {
            return content;
        }
        StringBuilder out = new StringBuilder(content.length());
        for (String line : content.split("\n", -1)) {
            int tab = line.indexOf('\t');
            if (tab < 0 || line.startsWith("#")) {
                out.append(line);
            } else {
                out.append(line, 0, tab + 1);
                String[] names = line.substring(tab + 1).split(",");
                for (int i = 0; i < names.length; i++) {
                    if (i > 0) {
                        out.append(',');
                    }
                    String mapped = remapper.map(names[i]);
                    out.append(mapped == null ? names[i] : mapped);
                }
            }
            out.append('\n');
        }
        out.setLength(out.length() - 1);
        return out.toString();
    }

    /**
     * rewrite {@code <I>\t<m> <desc>\t<T>=<since>:<declarer>[>owner.member(desc)][;…],…}
     * rows — every {@code >}-introduced stub site's owner (the internal name
     * before the member-joining {@code .} ahead of {@code (}) is an api class
     * and carries the prefix; interface/member/declarer names stay JDK
     */
    private static String remapGainedCalls(String content, SimpleRemapper remapper) {
        if (content.isEmpty()) {
            return content;
        }
        StringBuilder out = new StringBuilder(content.length());
        for (String line : content.split("\n", -1)) {
            if (line.startsWith("#")) {
                out.append(line);
            } else {
                StringBuilder l = new StringBuilder(line);
                int search = 0;
                int gt;
                while ((gt = l.indexOf(">", search)) >= 0) {
                    int paren = l.indexOf("(", gt);
                    if (paren < 0) {
                        break;
                    }
                    int dot = l.lastIndexOf(".", paren);
                    // a D: prefix marks the site as override-preserving
                    // dispatch — the impl owner follows it
                    int owner = l.indexOf("D:", gt + 1) == gt + 1 ? gt + 3 : gt + 1;
                    String mapped = dot > owner ? remapper.map(l.substring(owner, dot)) : null;
                    if (mapped != null) {
                        l.replace(owner, dot, mapped);
                        search = owner + mapped.length();
                    } else {
                        search = paren;
                    }
                }
                out.append(l);
            }
            out.append('\n');
        }
        out.setLength(out.length() - 1);
        return out.toString();
    }

    /**
     * rewrite {@code T\t<source-jdk>\t<target>\t<since>} rows — an api-class
     * target carries the prefix when it made the closure; an api target that
     * didn't (not referenced by bytecode edges) drops the row, since the
     * reflective lookup would only miss anyway; a JDK target stays as-is —
     * it resolves natively on the running JVM.
     */
    private static String remapReflection(String content, SimpleRemapper remapper,
                                          ReferenceIndex apiIndex, int position,
                                          ShadeReflection shadeReflection) {
        if (content.isEmpty() || shadeReflection == ShadeReflection.NONE) {
            return "";
        }
        StringBuilder out = new StringBuilder(content.length());
        for (String line : content.split("\n", -1)) {
            String[] cols = line.split("\t", -1);
            if (cols.length == 4 && "T".equals(cols[0])) {
                String mapped = remapper.map(cols[2]);
                if (mapped != null) {
                    cols[2] = mapped;
                    out.append(String.join("\t", cols));
                } else if (apiIndex.nodeFor(Type.getObjectType(cols[2]), position) == null) {
                    // not an api class at all — a JDK target; resolves natively
                    out.append(line);
                }
                // api target outside the closure → prune: the lookup
                // degrades to a native CNFE instead of an internal error
            } else if (cols.length == 10 && "M".equals(cols[0])) {
                String mapped = remapper.map(cols[5]);
                if (mapped != null) {
                    cols[5] = mapped;
                    out.append(String.join("\t", cols));
                } else if (apiIndex.nodeFor(Type.getObjectType(cols[5]), position) == null) {
                    out.append(line);
                }
                // same prune: a missing impl answers NoSuchMethodException
            } else if (cols.length == 11 && "C".equals(cols[0])) {
                // impl references sit inside spec fields — owner.name(desc):from…
                // R rows: conv specs (col 6, |-joined), pre (7), post (8);
                // F rows: the factory impl (col 4). Each owner prefix-maps
                // or the row prunes — a missing impl answers NSME.
                int[] specCols = null;
                boolean ok = true;
                if ("F".equals(cols[3])) {
                    specCols = new int[]{4};
                } else if ("R".equals(cols[3])) {
                    specCols = new int[]{6, 7, 8};
                }
                if (specCols != null) {
                    for (int c : specCols) {
                        if ("-".equals(cols[c])) {
                            continue;
                        }
                        StringBuilder specs = new StringBuilder();
                        for (String spec : cols[c].split("\\|", -1)) {
                            if (specs.length() > 0) {
                                specs.append('|');
                            }
                            int colon = spec.indexOf(':');
                            int dot = spec.lastIndexOf('.', colon < 0 ? spec.indexOf('(') : colon);
                            if (dot < 0) {
                                ok = false;
                                break;
                            }
                            String owner = spec.substring(0, dot);
                            String mapped = remapper.map(owner);
                            if (mapped != null) {
                                specs.append(mapped).append(spec.substring(dot));
                            } else if (apiIndex.nodeFor(Type.getObjectType(owner), position) == null) {
                                specs.append(spec);
                            } else {
                                ok = false;
                                break;
                            }
                        }
                        if (!ok) {
                            break;
                        }
                        cols[c] = specs.toString();
                    }
                    if (ok) {
                        out.append(String.join("\t", cols));
                    }
                } else {
                    out.append(line);
                }
            } else {
                out.append(line);
            }
            out.append('\n');
        }
        out.setLength(out.length() - 1);
        return out.toString();
    }

    /** rewrite {@code <retro>\t<stub>\t<member>=<impl>,…} rows — stub/impl are api classes, retro/member stay JDK */
    private static String remapWraps(String content, SimpleRemapper remapper) {
        if (content.isEmpty()) {
            return content;
        }
        StringBuilder out = new StringBuilder(content.length());
        for (String line : content.split("\n", -1)) {
            String[] fields = line.split("\t", -1);
            if (fields.length != 3 || line.startsWith("#")) {
                out.append(line);
            } else {
                out.append(fields[0]).append('\t');
                String mapped = remapper.map(fields[1]);
                out.append(mapped == null ? fields[1] : mapped).append('\t');
                String[] pairs = fields[2].split(",");
                for (int i = 0; i < pairs.length; i++) {
                    if (i > 0) {
                        out.append(',');
                    }
                    int eq = pairs[i].indexOf('=');
                    String impl = eq > 0 ? remapper.map(pairs[i].substring(eq + 1)) : null;
                    out.append(pairs[i], 0, eq + 1);
                    out.append(impl == null ? pairs[i].substring(eq + 1) : impl);
                }
            }
            out.append('\n');
        }
        out.setLength(out.length() - 1);
        return out.toString();
    }

    /**
     * Shades each captured variant in place — inline candidates and the
     * prefix remap for its own slot — and writes the transformed node back
     * into {@code ctx.variants} so the writer sees the shaded form.
     */
    private void shadeVariants(TransformContext ctx) {
        if (ctx.variants == null) {
            return;
        }
        for (Map.Entry<Object, List<ClassNode>> entry : ctx.variants.entrySet()) {
            List<ClassNode> nodes = entry.getValue();
            List<ClassNode> shaded = new ArrayList<ClassNode>(nodes.size());
            for (ClassNode node : nodes) {
                int position = context.positionOf(node, ctx.flags);
                ClassNode out = ShadeInlineTransform.inline(node, position, context, ctx);
                shaded.add(ShadePrefixTransform.remap(out, position, context));
            }
            nodes.clear();
            nodes.addAll(shaded);
        }
    }

    /**
     * synthetic usage marker recording that a closure root was pulled by the
     * reflection-keep rules rather than any single caller — impls rooted this
     * way are never sole-consumer, so they stay out of the inline candidates.
     */
    private static final FullyQualifiedMemberNameAndDesc REFLECTION_ROOT =
        FullyQualifiedMemberNameAndDesc.of(Type.getObjectType("jvmdg/reflection"));

    /** parsed {@code reflection.idx} row: the JDK owner/member it emulates + the impl refs it roots */
    static final class ReflRow {
        final String owner;      // JDK owner the row emulates — usedOwners matching
        final String matchName;  // owner or owner.name — Shade-Keep-Reflection matching
        final List<FullyQualifiedMemberNameAndDesc> impls = new ArrayList<FullyQualifiedMemberNameAndDesc>();

        ReflRow(String owner, String matchName) {
            this.owner = owner;
            this.matchName = matchName;
        }
    }

    private List<ReflRow> reflectionRows;

    private List<ReflRow> reflectionRows() throws IOException {
        if (reflectionRows == null) {
            reflectionRows = loadReflRows(context.apiRoots);
        }
        return reflectionRows;
    }

    /** Every {@code reflection.idx} row across the api roots — first existing file wins per root. */
    static List<ReflRow> loadReflRows(List<Path> apiRoots) throws IOException {
        List<ReflRow> rows = new ArrayList<ReflRow>();
        for (Path apiRoot : apiRoots) {
            Path p = apiRoot.resolve(ReflectionTable.RESOURCE);
            if (!Files.exists(p)) {
                continue;
            }
            for (String line : new String(Files.readAllBytes(p), StandardCharsets.UTF_8)
                    .split("\n", -1)) {
                ReflRow row = parseReflRow(line);
                if (row != null) {
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    static ReflRow parseReflRow(String line) {
        String[] cols = line.split("\t", -1);
        if (cols.length == 4 && "T".equals(cols[0])) {
            ReflRow row = new ReflRow(cols[1], cols[1]);
            row.impls.add(FullyQualifiedMemberNameAndDesc.of(Type.getObjectType(cols[2])));
            return row;
        }
        if (cols.length == 10 && "M".equals(cols[0])) {
            ReflRow row = new ReflRow(cols[1], cols[1] + "." + cols[2]);
            row.impls.add(FullyQualifiedMemberNameAndDesc.of(
                "L" + cols[5] + ";" + cols[6] + ";" + cols[7]));
            return row;
        }
        if (cols.length == 11 && "C".equals(cols[0])) {
            int[] specCols = "F".equals(cols[3]) ? new int[]{4}
                : "R".equals(cols[3]) ? new int[]{6, 7, 8} : null;
            if (specCols == null) {
                return null;
            }
            ReflRow row = new ReflRow(cols[1], cols[1] + ".<init>");
            for (int c : specCols) {
                if ("-".equals(cols[c])) {
                    continue;
                }
                for (String spec : cols[c].split("\\|", -1)) {
                    FullyQualifiedMemberNameAndDesc impl = specToFQM(spec);
                    if (impl != null) {
                        row.impls.add(impl);
                    }
                }
            }
            return row;
        }
        return null;
    }

    /** {@code owner.name(desc):from…} → the impl's {@code Lowner;name;desc} ref */
    private static FullyQualifiedMemberNameAndDesc specToFQM(String spec) {
        int colon = spec.indexOf(':');
        int paren = spec.indexOf('(');
        int dot = spec.lastIndexOf('.', colon < 0 ? paren : colon);
        if (dot < 0 || paren < 0 || (colon >= 0 && colon < paren)) {
            return null;
        }
        return FullyQualifiedMemberNameAndDesc.of(
            "L" + spec.substring(0, dot) + ";" + spec.substring(dot + 1, paren) + ";"
                + spec.substring(paren, colon < 0 ? spec.length() : colon));
    }

    /**
     * usedOwners/all closure roots: each kept {@code reflection.idx} row roots
     * its impl member(s) so the shaded jar can dispatch the reflective calls
     * its sites resolve at runtime. The marker records the pull as
     * reflection-keep — never a sole-consumer bytecode edge.
     */
    private void injectReflectionRoots(TransformContext ctx, int position,
            Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> starts) {
        List<ReflRow> rows;
        try {
            rows = reflectionRows();
        } catch (IOException e) {
            ctx.diagnostics.warn("shade", "could not read " + ReflectionTable.RESOURCE + ": " + e);
            return;
        }
        boolean all = ctx.flags.shadeReflection == ShadeReflection.ALL;
        Set<String> referencedOwners = new HashSet<String>();
        if (!all) {
            for (FullyQualifiedMemberNameAndDesc ref : context.inputRefsAt(position)) {
                Type owner = ref.getOwner();
                if (owner.getSort() == Type.OBJECT) {
                    referencedOwners.add(owner.getInternalName());
                }
            }
        }
        for (ReflRow row : rows) {
            if (!all && !referencedOwners.contains(row.owner)
                && !keepMatch(ctx.flags.shadeKeepReflection, row.matchName)) {
                continue;
            }
            for (FullyQualifiedMemberNameAndDesc impl : row.impls) {
                // only api impls root — T rows may name a real JDK target
                // (gained supertypes map to the existing interface); rooting
                // java/util/Collection would pull it into the api closure and
                // prefix every reference to it
                if (!context.apiClasses.contains(impl.getOwner())) {
                    continue;
                }
                Set<FullyQualifiedMemberNameAndDesc> users = starts.get(impl);
                if (users == null) {
                    users = new HashSet<FullyQualifiedMemberNameAndDesc>();
                    starts.put(impl, users);
                }
                users.add(REFLECTION_ROOT);
            }
        }
    }

    /** floorEntry walk over {@code Shade-Keep-Reflection} — same wildcard semantics as Ignore-Warnings-In */
    private static boolean keepMatch(TreeMap<String, Flags.WildcardType> map, String name) {
        Map.Entry<String, Flags.WildcardType> entry = map.floorEntry(name);
        while (entry != null && entry.getKey().charAt(0) == name.charAt(0)) {
            if (name.startsWith(entry.getKey()) && Flags.wildcardMatches(entry.getValue(), entry.getKey(), name)) {
                return true;
            }
            entry = map.lowerEntry(entry.getKey());
        }
        return false;
    }

    /**
     * under usedOwners/none a callsite the table can't answer falls back to
     * native lookup — warn on input members that call the dynamic entry
     * points so the operator sees the sites the keep tier governs.
     */
    private void warnDynamicReflectionSites(TransformContext ctx, int position) {
        if (ctx.flags.shadeReflection == ShadeReflection.ALL) {
            return;
        }
        for (FullyQualifiedMemberNameAndDesc site : context.dynamicSitesAt(position)) {
            ctx.diagnostics.warn("shade", "dynamic reflection site " + site
                + " — Shade-Reflection: " + ctx.flags.shadeReflection.name()
                + " limits table coverage (see --shadeKeepReflection)");
        }
    }

}
