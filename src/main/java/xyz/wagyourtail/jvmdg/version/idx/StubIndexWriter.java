package xyz.wagyourtail.jvmdg.version.idx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.gen.ApiGenerator;
import xyz.wagyourtail.jvmdg.version.gen.GainedDispatchGenerator;
import xyz.wagyourtail.jvmdg.version.gen.GenContext;
import xyz.wagyourtail.jvmdg.version.gen.MarkerGenerator;
import xyz.wagyourtail.jvmdg.version.gen.MemberForwarderGenerator;
import xyz.wagyourtail.jvmdg.version.gen.RetroDispatchGenerator;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;
import xyz.wagyourtail.jvmdg.version.map.CtorEntry;
import xyz.wagyourtail.jvmdg.version.map.CtorFactory;
import xyz.wagyourtail.jvmdg.version.map.CtorRewrite;
import xyz.wagyourtail.jvmdg.version.map.DispatchSites;
import xyz.wagyourtail.jvmdg.version.map.FieldStubEntry;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;
import xyz.wagyourtail.jvmdg.version.map.StubRegistry;

/**
 * Build-time entry point that writes {@code META-INF/jvmdg/stubs.idx}: runs
 * every {@link VersionProvider} found by {@link ServiceLoader} through
 * {@link VersionProvider#ensureInit} — the same reflective scan the runtime
 * uses, so the index is parity-by-construction — then serializes the
 * resulting registries via {@link StubIndex}.
 *
 * <p>Usage: {@code StubIndexWriter <output-file> <jvmdg-version> <ct.sym> [<retrotypes-file>]}.
 * The output file should be packaged into the api jar at {@link StubIndex#RESOURCE}. The
 * {@code ct.sym} arg (the {@code genCtSym} product — {@code <vercodes>/<mod>/<class>.sig}
 * version-windowed JDK API surface) feeds {@link StubRegistry#memberSince}: for every
 * callsite member it records the first JDK class version carrying it, which is what
 * lets {@code MISSING_API} vs {@code BEHAVIOR} classify per downgrade target. It also
 * feeds {@link ClassStubEntry#implementedBy}: every adapter's retro-supertype membership
 * is computed from the newest window's type graph and serialized with the index — and
 * flattened with the adapter/marker names into the optional
 * {@code META-INF/jvmdg/retrotypes} runtime table {@code RetroTypes} reads.
 * Optional arg 5 is the jar-root dir the {@link ApiGenerator}s emit classes
 * into (marker interfaces, per-adapter {@code $jvmdg$retro} dispatchers),
 * optional arg 8 the {@code .java} inspection-dump dir (build output only,
 * not jar'd).
 */
public class StubIndexWriter {

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 11) {
            throw new IllegalArgumentException("usage: StubIndexWriter <output-file> <jvmdg-version> <ct.sym> [<retrotypes-file> [<gen-classes-dir> [<excludes-file> [<wraps-file> [<gen-src-dir> [<gained-file> [<gained-calls-file> [<reflection-file>]]]]]]]]");
        }
        Path out = Paths.get(args[0]);
        Path ctSym = Paths.get(args[2]);
        List<VersionProvider> providers = new ArrayList<>();
        for (VersionProvider vp : ServiceLoader.load(VersionProvider.class)) {
            providers.add(vp);
        }
        ClassDowngrader downgrader = ClassDowngrader.downgradeTo(new Flags());
        for (VersionProvider vp : providers) {
            vp.ensureInit(downgrader);
        }
        populateMemberSince(ctSym, providers);
        JdkSurface graph = JdkSurface.read(ctSym);
        populateImplementedBy(graph, providers);
        Map<VersionProvider, Map<String, Set<String>>> derived = populateExcludeChildren(graph, providers);
        Map<String, Map<String, Integer>> gained = gainedInterfaces(graph);
        StubIndex.write(out, args[1], providers);
        if (args.length > 3) {
            writeRetroTypes(Paths.get(args[3]), providers);
        }
        Map<String, Map<String, Map<String, List<GainedRes>>>> gainedDecls =
            args.length > 9 || args.length > 4 ? gainedCallDecls(graph, gained) : null;
        Map<String, Map<String, StubEntry>> gainedStubs =
            gainedDecls == null ? null : memberStubLookup(providers);
        GenContext gen = new GenContext(providers, StubIndexWriter.class.getClassLoader(),
            args.length > 4 ? Paths.get(args[4]) : null,
            args.length > 7 ? Paths.get(args[7]) : null);
        if (args.length > 4) {
            gen.gainedMethods = gainedIfaceMethods(graph, gained);
            if (gainedDecls != null) {
                gen.gainedSites = resolveGainedSites(graph, gainedDecls, gainedStubs);
            }
            new MarkerGenerator().generate(gen);
            new RetroDispatchGenerator().generate(gen);
            new GainedDispatchGenerator().generate(gen);
            new MemberForwarderGenerator().generate(gen);
        }
        if (args.length > 5) {
            writeExcludes(Paths.get(args[5]), derived);
        }
        if (args.length > 6) {
            writeWraps(Paths.get(args[6]), gen.flattenedWraps());
        }
        if (args.length > 8) {
            writeGained(Paths.get(args[8]), gained, graph);
        }
        if (args.length > 9) {
            writeGainedCalls(Paths.get(args[9]), graph, gained, gainedDecls, gainedStubs);
        }
        if (args.length > 10) {
            writeReflection(Paths.get(args[10]), providers, graph);
        }
    }

    /**
     * {@code xyz/wagyourtail/jvmdg/stub/reflection.idx} — the type-remap rows the
     * ASM-free {@code ReflectionTable} consults at runtime:
     * {@code T<TAB>source<TAB>target<TAB>since}, one row per class-stubbed
     * JDK type. The target is the registered runtime stand-in: the api
     * adapter class for {@code OWN_INTERFACE} entries, the existing supertype
     * for {@code SUPERTYPE} ones — both loadable on the running JVM, and the
     * supertype keeps {@code isAssignableFrom}-style checks answering
     * correctly. {@code since} is the source's ct.sym first-version, so the
     * runtime window {@code (runningJvm, origVersion]} needs no per-boundary
     * rows. First registration wins a source — provider-chain order matches
     * callsite resolution.
     */
    private static void writeReflection(Path out, List<VersionProvider> providers, JdkSurface g) throws IOException {
        Map<String, Map.Entry<String, Integer>> types = new TreeMap<>();
        Map<String, String> members = new TreeMap<>();
        for (VersionProvider vp : providers) {
            for (Map.Entry<Type, ClassStubEntry> e : vp.classStubs.entrySet()) {
                String source = e.getKey().getInternalName();
                if (types.containsKey(source)) {
                    continue;
                }
                Integer since = g.classMin.get(source);
                types.put(source, new AbstractMap.SimpleImmutableEntry<>(
                    e.getValue().target.getInternalName(), since == null ? 0 : since));
            }
            for (Map.Entry<String, StubRegistry.Owner> oe : vp.stubRegistry.owners().entrySet()) {
                for (Map.Entry<MemberNameAndDesc, StubEntry> me : oe.getValue().methodStub.entrySet()) {
                    StubEntry entry = me.getValue();
                    MemberNameAndDesc m = me.getKey();
                    // impls gated behind a condition (live runtime downgrader,
                    // preview mode) can't be answered reflectively — exclude
                    // the row entirely
                    if (entry.condition != xyz.wagyourtail.jvmdg.version.StubCondition.ALWAYS || !m.isMethod()) {
                        continue;
                    }
                    String key = oe.getKey() + ' ' + m.getName() + ' ' + m.getDesc().getDescriptor();
                    if (members.containsKey(key)) {
                        continue;
                    }
                    Type[] h = entry.handler.member.getDesc().getArgumentTypes();
                    boolean instance = h.length != 0 && isRef(h[0])
                        && h.length == m.getDesc().getArgumentTypes().length + 1
                        + (entry.callerClass ? 1 : 0) + (entry.downgradeVersion ? 1 : 0);
                    Integer since = vp.stubRegistry.memberSince.get(
                        new FullyQualifiedMemberNameAndDesc(Type.getObjectType(oe.getKey()),
                            m.getName(), m.getDesc()));
                    Map<String, Integer> ownerFlags = g.memberFlags.get(oe.getKey());
                    Integer mods = ownerFlags == null ? null : ownerFlags.get(m.getName() + ' ' + m.getDesc().getDescriptor());
                    MethodRef impl = entry.handler;
                    members.put(key, "M\t" + oe.getKey() + '\t' + m.getName() + '\t'
                        + m.getDesc().getDescriptor() + '\t' + (instance ? 'i' : 's') + '\t'
                        + impl.member.getOwner().getInternalName() + '\t' + impl.member.getName() + '\t'
                        + impl.member.getDesc().getDescriptor() + '\t' + (since == null ? 0 : since)
                        + '\t' + (mods == null ? Opcodes.ACC_PUBLIC : mods));
                }
                for (Map.Entry<MemberNameAndDesc, CtorEntry> m : oe.getValue().ctor.entrySet()) {
                    CtorEntry ce = m.getValue();
                    if (ce.condition != xyz.wagyourtail.jvmdg.version.StubCondition.ALWAYS) {
                        continue;
                    }
                    String key = oe.getKey() + ' ' + m.getKey().getDesc().getDescriptor();
                    if (members.containsKey(key)) {
                        continue;
                    }
                    Integer since = vp.stubRegistry.memberSince.get(
                        new FullyQualifiedMemberNameAndDesc(Type.getObjectType(oe.getKey()),
                            m.getKey().getName(), m.getKey().getDesc()));
                    Map<String, Integer> ownerFlags = g.memberFlags.get(oe.getKey());
                    Integer mods = ownerFlags == null ? null
                        : ownerFlags.get(m.getKey().getName() + ' ' + m.getKey().getDesc().getDescriptor());
                    String row = null;
                    if (ce instanceof CtorFactory) {
                        CtorFactory f = (CtorFactory) ce;
                        row = "C\t" + oe.getKey() + '\t' + m.getKey().getDesc().getDescriptor()
                            + "\tF\t" + fqmn(f.handler) + '\t' + (f.downgradeVersion ? "v" : "-")
                            + "\t-\t-\t-";
                    } else if (ce instanceof CtorRewrite) {
                        CtorRewrite r = (CtorRewrite) ce;
                        StringBuilder keep = new StringBuilder();
                        StringBuilder convs = new StringBuilder();
                        for (int i = 0; i < r.keep.length; i++) {
                            if (i > 0) {
                                keep.append(',');
                            }
                            keep.append(r.keep[i]);
                            if (r.keep[i] < 0) {
                                if (convs.length() > 0) {
                                    convs.append('|');
                                }
                                convs.append(spec(r.argMethods[i], r.argFrom[i]));
                            }
                        }
                        row = "C\t" + oe.getKey() + '\t' + m.getKey().getDesc().getDescriptor()
                            + "\tR\t" + Type.getMethodDescriptor(Type.VOID_TYPE, r.toArgs) + '\t'
                            + keep + '\t' + (convs.length() == 0 ? "-" : convs.toString()) + '\t'
                            + (r.pre == null ? "-" : spec(r.pre, r.preFrom)) + '\t'
                            + (r.post == null ? "-" : spec(r.post, r.postFrom) + (r.downgradeVersion ? ":v" : ""));
                    }
                    if (row != null) {
                        members.put(key, row + '\t' + (since == null ? 0 : since)
                            + '\t' + (mods == null ? Opcodes.ACC_PUBLIC : mods));
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder("# jvmdg reflection table — T<source>\t<target>\t<since>"
            + " | M<owner>\t<name>\t<desc>\t<i|s>\t<impl-owner>\t<impl-name>\t<impl-desc>\t<since>\t<mods>"
            + " | C<owner>\t<ctor-desc>\t<R|F>\t<t1..t5>\t<since>\t<mods>\n");
        for (Map.Entry<String, Map.Entry<String, Integer>> row : types.entrySet()) {
            sb.append("T\t").append(row.getKey()).append('\t')
                .append(row.getValue().getKey()).append('\t')
                .append(row.getValue().getValue()).append('\n');
        }
        for (String row : members.values()) {
            sb.append(row).append('\n');
        }
        Files.createDirectories(out.getParent());
        Files.write(out, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isRef(Type t) {
        return t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY;
    }

    /** {@code owner.name(desc)} — the C-row impl token; owner/name/desc carry no {@code :|,} so the row's own delimiters stay unambiguous */
    private static String fqmn(MethodRef ref) {
        return ref.member.getOwner().getInternalName() + '.'
            + ref.member.getName() + ref.member.getDesc().getDescriptor();
    }

    /** {@code implFQMD:origArgIndex,…} — one converter/pre/post spec in a C row */
    private static String spec(MethodRef ref, int[] from) {
        StringBuilder sb = new StringBuilder(fqmn(ref)).append(':');
        for (int i = 0; i < from.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(from[i]);
        }
        return sb.toString();
    }

    /**
     * For every registered callsite member, find the earliest JDK version
     * whose {@code ct.sym} window carries it and record the class version in
     * {@code vp.stubRegistry.memberSince}, keyed by the <em>site-form</em>
     * FQN (owner + callsite name + callsite desc — the
     * {@code downgradeVersion} argument expansion is stripped). A member the
     * sym data doesn't know keeps {@code since=0} and classifies
     * {@link xyz.wagyourtail.jvmdg.version.StubKind#MISSING_API} on all targets.
     */
    /**
     * Compute {@code memberSince} for every registered member from the ct.sym
     * surface index — the minimum JDK window containing the member. Idempotent
     * per provider; used by the build-time writer and by the coverage checker
     * (whose classpath doesn't carry stubs.idx) so kind classification is
     * populated on both paths.
     */
    public static void populateMemberSince(Path ctSym, List<VersionProvider> providers) throws IOException {
        // wanted: owner internal name -> {siteFQN -> jdk member key}
        Map<String, Map<FullyQualifiedMemberNameAndDesc, MemberNameAndDesc>> wanted = new HashMap<>();
        for (VersionProvider vp : providers) {
            for (Map.Entry<String, StubRegistry.Owner> e : vp.stubRegistry.owners().entrySet()) {
                String owner = e.getKey();
                StubRegistry.Owner o = e.getValue();
                for (Map.Entry<MemberNameAndDesc, StubEntry> m : o.methodStub.entrySet()) {
                    addWanted(wanted, owner, m.getKey(), m.getValue().downgradeVersion);
                }
                for (Map.Entry<MemberNameAndDesc, ?> m : o.fieldStub.entrySet()) {
                    addWanted(wanted, owner, m.getKey(), false);
                }
                for (Map.Entry<MemberNameAndDesc, CtorEntry> m : o.ctor.entrySet()) {
                    CtorEntry ce = m.getValue();
                    boolean expanded = ce instanceof CtorFactory
                        ? ((CtorFactory) ce).downgradeVersion
                        : ce instanceof CtorRewrite && ((CtorRewrite) ce).downgradeVersion;
                    addWanted(wanted, owner, m.getKey(), expanded);
                }
            }
        }
        if (wanted.isEmpty()) return;
        Map<String, List<ZipEntry>> sigs = new HashMap<>();
        try (ZipFile zip = new ZipFile(ctSym.toFile())) {
            for (ZipEntry e : Collections.list(zip.entries())) {
                if (e.getName().endsWith(".sig")) {
                    // path is <vercodes>/<mod>/<class>.sig — class name is
                    // everything after the second '/', minus the .sig suffix
                    int p2 = e.getName().indexOf('/', e.getName().indexOf('/') + 1);
                    String cls = e.getName().substring(p2 + 1, e.getName().length() - 4);
                    if (wanted.containsKey(cls)) {
                        sigs.computeIfAbsent(cls, k -> new ArrayList<ZipEntry>()).add(e);
                    }
                }
            }
            for (VersionProvider vp : providers) {
                for (Map.Entry<String, StubRegistry.Owner> e : vp.stubRegistry.owners().entrySet()) {
                    String owner = e.getKey();
                    Map<FullyQualifiedMemberNameAndDesc, MemberNameAndDesc> members = wanted.get(owner);
                    List<ZipEntry> entries = sigs.get(owner);
                    if (members == null || entries == null) continue;
                    for (ZipEntry entry : entries) {
                        int windowMin = decodeVersion(entry.getName().charAt(0)) + 44;
                        ClassNode cn;
                        try (InputStream is = zip.getInputStream(entry)) {
                            cn = new ClassNode();
                            new ClassReader(is).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                        }
                        for (Map.Entry<FullyQualifiedMemberNameAndDesc, MemberNameAndDesc> m : members.entrySet()) {
                            if (hasMember(cn, m.getValue())) {
                                Integer cur = vp.stubRegistry.memberSince.get(m.getKey());
                                if (cur == null || windowMin < cur) {
                                    vp.stubRegistry.memberSince.put(m.getKey(), windowMin);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The JDK API surface from ct.sym, one live-enumeration pass: class
     * supertypes + declared member keys ({@code "name desc"}) at each
     * class's newest window, plus each member's first-seen window.
     * Internal impls ({@code jdk/}, {@code com/azul/}) and
     * {@code @PreviewFeature} classes are dropped; package-private API
     * classes stay — that's what the runtime name-keyed lookups need.
     * <p>
     * Entries stream on the live enumeration: the archive carries duplicate
     * names (rewrite-in-place appends), and {@code getInputStream}
     * re-resolves by name once the enumeration is exhausted, mixing the
     * stored entry's csize with the canonical occurrence's data → ZLIB EOF.
     * Rewrites share a window, so last-occurrence-wins per class (matching
     * {@code getEntry} semantics: the canonical entry is the last).
     */
    private static class JdkSurface {
        /** class → supertype internal names at its newest window */
        final Map<String, List<String>> edges = new HashMap<>();
        /** class → member keys ({@code name desc}) declared at its newest window */
        final Map<String, Set<String>> newestDecl = new HashMap<>();
        /** class → member key → earliest class-file version declaring it */
        final Map<String, Map<String, Integer>> memberMin = new HashMap<>();
        /** class → member key → access flags at its earliest-declaring window */
        final Map<String, Map<String, Integer>> memberFlags = new HashMap<>();
        /** class → ct.sym window code → that window's record — every window, for the gained-supertype diff + declarer resolution */
        final Map<String, Map<String, Window>> windows = new HashMap<>();
        /** internal names carrying {@code ACC_INTERFACE} at a non-excluded window */
        final Set<String> interfaceNames = new HashSet<>();
        /** class → earliest class-file version it exists at (non-excluded windows) */
        final Map<String, Integer> classMin = new HashMap<>();

        /** one window's view: the declared supertypes + member keys ({@code name desc}) */
        static final class Window {
            final List<String> supers;
            final Set<String> decl;

            Window(List<String> supers, Set<String> decl) {
                this.supers = supers;
                this.decl = decl;
            }
        }

        static JdkSurface read(Path ctSym) throws IOException {
            JdkSurface g = new JdkSurface();
            Map<String, Integer> edgeVer = new HashMap<>();
            try (ZipFile zip = new ZipFile(ctSym.toFile())) {
                for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements();) {
                    ZipEntry e = en.nextElement();
                    if (!e.getName().endsWith(".sig")) continue;
                    int p1 = e.getName().indexOf('/');
                    int p2 = e.getName().indexOf('/', p1 + 1);
                    String cls = e.getName().substring(p2 + 1, e.getName().length() - 4);
                    int max = 0;
                    for (int i = 0; i < p1; i++) {
                        max = Math.max(max, decodeVersion(e.getName().charAt(i)));
                    }
                    int min = decodeVersion(e.getName().charAt(0)) + 44;
                    ClassNode cn;
                    try (InputStream is = zip.getInputStream(e)) {
                        cn = new ClassNode();
                        new ClassReader(is).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    }
                    Map<String, Integer> mins = g.memberMin.computeIfAbsent(cls, k -> new HashMap<String, Integer>());
                    Map<String, Integer> fls = g.memberFlags.computeIfAbsent(cls, k -> new HashMap<String, Integer>());
                    if (cn.methods != null) {
                        for (MethodNode m : cn.methods) {
                            String key = m.name + " " + m.desc;
                            Integer cur = mins.get(key);
                            if (cur == null || min < cur) {
                                mins.put(key, min);
                                fls.put(key, m.access);
                            }
                        }
                    }
                    if (cn.fields != null) {
                        for (FieldNode f : cn.fields) {
                            String key = f.name + " " + f.desc;
                            Integer cur = mins.get(key);
                            if (cur == null || min < cur) {
                                mins.put(key, min);
                                fls.put(key, f.access);
                            }
                        }
                    }
                    List<String> supers = new ArrayList<>();
                    if (cn.superName != null) {
                        supers.add(cn.superName);
                    }
                    if (cn.interfaces != null) {
                        supers.addAll(cn.interfaces);
                    }
                    boolean excluded = cn.name.startsWith("jdk/") || cn.name.startsWith("com/azul/") || preview(cn);
                    if (!excluded) {
                        // windowed records — every window, not just the
                        // newest: an interface gained mid-life is invisible
                        // in `edges`, and gained-call resolution needs the
                        // declaring class at the binding's window.
                        // Duplicates share the window code (rewrite-in-
                        // place), so last-wins per window.
                        Set<String> winDecl = new HashSet<>();
                        if (cn.methods != null) {
                            for (MethodNode m : cn.methods) winDecl.add(m.name + " " + m.desc);
                        }
                        if (cn.fields != null) {
                            for (FieldNode f : cn.fields) winDecl.add(f.name + " " + f.desc);
                        }
                        g.windows.computeIfAbsent(cls, k -> new HashMap<String, JdkSurface.Window>())
                            .put(e.getName().substring(0, p1), new JdkSurface.Window(supers, winDecl));
                        Integer cm = g.classMin.get(cls);
                        if (cm == null || min < cm) {
                            g.classMin.put(cls, min);
                        }
                        if ((cn.access & Opcodes.ACC_INTERFACE) != 0) {
                            g.interfaceNames.add(cls);
                        }
                    }
                    Integer cur = edgeVer.get(cls);
                    if (cur != null && cur > max) {
                        continue; // an older window — keep the newest declarations
                    }
                    edgeVer.put(cls, max);
                    if (excluded) {
                        g.edges.remove(cls);
                        g.newestDecl.remove(cls);
                        g.windows.remove(cls);
                        g.interfaceNames.remove(cls);
                        g.classMin.remove(cls);
                        continue;
                    }
                    g.edges.put(cls, supers);
                    Set<String> decl = new HashSet<>();
                    if (cn.methods != null) {
                        for (MethodNode m : cn.methods) decl.add(m.name + " " + m.desc);
                    }
                    if (cn.fields != null) {
                        for (FieldNode f : cn.fields) decl.add(f.name + " " + f.desc);
                    }
                    g.newestDecl.put(cls, decl);
                }
            }
            // a class excluded at its newest window may have older
            // non-excluded windows recorded before the exclusion was
            // seen — `edges` is the canonical surviving set
            g.windows.keySet().retainAll(g.edges.keySet());
            g.classMin.keySet().retainAll(g.edges.keySet());
            g.interfaceNames.retainAll(g.edges.keySet());
            return g;
        }
    }

    /**
     * For every {@code @Adapter} with an empty annotation-declared membership,
     * compute {@code implementedBy} from the ct.sym type graph: the set of all
     * JDK class+interface internal names whose supertype closure contains the
     * adapter's source type — interfaces included, since an implementor's
     * hierarchy names them too ({@code ArrayList} → {@code java/util/List}).
     * Each class is taken at its <em>newest</em> window (the latest supertypes
     * it ever had), so a later-version implementor is still a member when the
     * call site flows through an earlier provider's retro stub.
     */
    public static void populateImplementedBy(JdkSurface graph, List<VersionProvider> providers) {
        Map<String, List<ClassStubEntry>> wanted = new HashMap<>();
        for (VersionProvider vp : providers) {
            for (Map.Entry<Type, ClassStubEntry> e : vp.classStubs.entrySet()) {
                wanted.computeIfAbsent(e.getKey().getInternalName(), k -> new ArrayList<ClassStubEntry>())
                    .add(e.getValue());
            }
        }
        if (wanted.isEmpty()) return;
        for (Map.Entry<String, List<ClassStubEntry>> w : wanted.entrySet()) {
            Set<String> impls = new LinkedHashSet<>();
            Map<String, Boolean> memo = new HashMap<>();
            for (String cls : graph.edges.keySet()) {
                if (containsType(cls, w.getKey(), graph.edges, memo)) {
                    impls.add(cls);
                }
            }
            // declared membership unions with the derivation — the
            // annotation lists members derivation can't see, never
            // a replacement or a veto; collect before the snapshots so
            // same-key entries in other providers see each other's extras
            for (ClassStubEntry entry : w.getValue()) {
                impls.addAll(entry.implementedBy);
            }
            for (ClassStubEntry entry : w.getValue()) {
                entry.setImplementedBy(impls);
            }
        }
    }

    /**
     * Derives {@code excludeChild} for every registered member stub: the JDK
     * classes that are subtypes of the stub's owner AND natively declare the
     * member at the provider's input version — the semantic the manual
     * attribute encodes ("{@code Random.nextLong} has its own impl — don't
     * detour through {@code RandomGenerator}'s backport"). Derived children
     * union with the declared ones; the declared attribute stays as the
     * override for cases the type graph can't see.
     * <p>
     * A class {@code D} qualifies when {@code owner ∈ closure(D)} (so a
     * callsite on D reaches the parent's stub), {@code D} declares the
     * member at its newest window, and the member exists on the downgrade
     * target ({@code memberMin ≤ outputVersion}) — the exclusion keeps the
     * callsite's native invoke, which only resolves if the member is real
     * on the output JDK. Descendants of D are covered by the resolver's
     * {@code isAssignableFrom} check, so only declaring types are listed.
     *
     * @return per-provider derived children, keyed by site-form member —
     *         what the {@code excludes} resource ships for the scan path
     */
    public static Map<VersionProvider, Map<String, Set<String>>> populateExcludeChildren(JdkSurface graph, List<VersionProvider> providers) {
        Map<VersionProvider, Map<String, Set<String>>> derived = new HashMap<>();
        // member name+args key -> (declaring class, exact key) at the
        // newest window — inverted index so per-member lookup skips the
        // all-classes scan. Grouped on name+args, not exact desc: the
        // resolver collides name+args-equal members across a supertype
        // boundary regardless of return type (covariant returns and
        // erased generic self-types — TypeDescriptor$OfMethod.returnType
        // vs MethodType.returnType), so an exact-desc index misses the
        // child that actually serves the callsite.
        Map<String, List<Map.Entry<String, String>>> declBy = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : graph.newestDecl.entrySet()) {
            for (String key : e.getValue()) {
                declBy.computeIfAbsent(nameArgs(key), k -> new ArrayList<Map.Entry<String, String>>())
                    .add(new AbstractMap.SimpleImmutableEntry<String, String>(e.getKey(), key));
            }
        }
        for (VersionProvider vp : providers) {
            Map<String, Set<String>> vpDerived = new HashMap<>();
            derived.put(vp, vpDerived);
            // adapter-owner stubs register under the adapter type — JDK
            // classes never have it in their supertype closure, so the
            // subtype check runs against the adapter's source type. The
            // resource key keeps the registered owner: the scan path's
            // lookup names the same registry slot.
            Map<String, String> stubToSource = new HashMap<>();
            for (Map.Entry<Type, ClassStubEntry> e : vp.classStubs.entrySet()) {
                stubToSource.put(e.getValue().stubType.getInternalName(), e.getKey().getInternalName());
            }
            Map<String, Set<String>> manualOnly = new TreeMap<>();
            for (Map.Entry<String, StubRegistry.Owner> o : vp.stubRegistry.owners().entrySet()) {
                String owner = o.getKey();
                String source = stubToSource.getOrDefault(owner, owner);
                Map<String, Boolean> memo = new HashMap<>();
                for (Map.Entry<MemberNameAndDesc, StubEntry> m : o.getValue().methodStub.entrySet()) {
                    Set<String> children = derivedChildren(vp, m.getKey(), graph, declBy, owner, source, memo, vpDerived);
                    reportManualOnly(manualOnly, owner, m.getKey(), m.getValue().excludeChild, children);
                    if (children != null) {
                        m.setValue(m.getValue().withExcludeChild(union(m.getValue().excludeChild, children)));
                    }
                }
                for (Map.Entry<MemberNameAndDesc, FieldStubEntry> m : o.getValue().fieldStub.entrySet()) {
                    FieldStubEntry f = m.getValue();
                    Set<String> children = derivedChildren(vp, m.getKey(), graph, declBy, owner, source, memo, vpDerived);
                    if (f.getter != null) {
                        reportManualOnly(manualOnly, owner, m.getKey(), f.getter.excludeChild, children);
                    }
                    if (f.setter != null) {
                        reportManualOnly(manualOnly, owner, m.getKey(), f.setter.excludeChild, children);
                    }
                    if (children == null) {
                        continue;
                    }
                    if (f.getter != null) {
                        f = f.withGetter(f.getter.withExcludeChild(union(f.getter.excludeChild, children)));
                    }
                    if (f.setter != null) {
                        f = f.withSetter(f.setter.withExcludeChild(union(f.setter.excludeChild, children)));
                    }
                    m.setValue(f);
                }
            }
            // migration check: every declared child should be one the
            // derivation reproduces — a miss means the manual entry is an
            // override for something the type graph can't see (kept — the
            // union adds it back), or a stale annotation to revisit.
            for (Map.Entry<String, Set<String>> e : manualOnly.entrySet()) {
                System.out.println("[StubIndexWriter] excludeChild not derivable on " +
                    vp.getClass().getSimpleName() + " " + e.getKey() + ": " + e.getValue());
            }
        }
        return derived;
    }

    private static void reportManualOnly(Map<String, Set<String>> acc, String owner, MemberNameAndDesc member,
                                         String[] manual, Set<String> derived) {
        if (manual.length == 0) {
            return;
        }
        Set<String> missing = new TreeSet<>();
        for (String child : manual) {
            String c = child;
            if (c.startsWith("L") && c.endsWith(";")) {
                c = c.substring(1, c.length() - 1);
            }
            if (derived == null || !derived.contains(c)) {
                missing.add(c);
            }
        }
        if (!missing.isEmpty()) {
            String site = member.toFullyQualified(Type.getObjectType(owner)).toString();
            acc.computeIfAbsent(site, k -> new TreeSet<String>()).addAll(missing);
        }
    }

    /** manual {@code excludeChild} ∪ derived children, sorted for determinism */
    private static String[] union(String[] manual, Set<String> derived) {
        Set<String> out = new TreeSet<>(Arrays.asList(manual));
        out.addAll(derived);
        return out.toArray(new String[0]);
    }

    /**
     * The derived child types for one member stub — also records them in
     * {@code vpDerived} under the site-form key for the {@code excludes}
     * resource — or null when nothing qualifies.
     */
    /**
     * Normalizes a {@code name desc} member key to {@code name (args} for
     * methods — the resolver's collision domain (name + argument types,
     * return type ignored) — so a child whose declaration differs from
     * the stub only in return type still counts as natively declaring
     * the member. Field keys carry no args and pass through whole.
     */
    private static String nameArgs(String memberKey) {
        int rparen = memberKey.indexOf(')');
        return rparen < 0 ? memberKey : memberKey.substring(0, rparen + 1);
    }

    /**
     * Whether the child's declaration serves callsites that name+args
     * collide with the stub: the declaration's return type must be
     * compatible — equal (the common case) or a subtype of the stub's
     * return (covariant overrides and erased generic self-types —
     * {@code TypeDescriptor$OfMethod.returnType → OfField} against
     * {@code MethodType.returnType → Class}). A child redeclaring the
     * member with an unrelated return does not serve the parent's
     * callsite; excluding it would leave a native invoke the target
     * can't resolve. Fields always serve — name-exact is their
     * whole signature.
     */
    private static boolean serves(MemberNameAndDesc stubMember, String childKey, JdkSurface graph, Map<String, Boolean> memo) {
        if (!stubMember.isMethod()) {
            return true;
        }
        Type stubRet = stubMember.getDesc().getReturnType();
        Type childRet = Type.getReturnType(childKey.substring(childKey.indexOf(' ') + 1));
        return retServes(childRet, stubRet, graph, memo);
    }

    private static boolean retServes(Type childRet, Type stubRet, JdkSurface graph, Map<String, Boolean> memo) {
        if (childRet.equals(stubRet)) {
            return true;
        }
        if (childRet.getSort() == Type.ARRAY && stubRet.getSort() == Type.ARRAY
            && childRet.getDimensions() == stubRet.getDimensions()) {
            return retServes(childRet.getElementType(), stubRet.getElementType(), graph, memo);
        }
        if (childRet.getSort() != Type.OBJECT || stubRet.getSort() != Type.OBJECT) {
            return false;
        }
        return containsType(childRet.getInternalName(), stubRet.getInternalName(), graph.edges, memo);
    }

    private static Set<String> derivedChildren(VersionProvider vp, MemberNameAndDesc member,
                                     JdkSurface graph, Map<String, List<Map.Entry<String, String>>> declBy,
                                     String owner, String source, Map<String, Boolean> memo, Map<String, Set<String>> vpDerived) {
        String key = nameArgs(member.getName() + " " + member.getDesc().getDescriptor());
        List<Map.Entry<String, String>> decls = declBy.get(key);
        if (decls == null) {
            return null;
        }
        Set<String> children = null;
        Map<String, Boolean> retMemo = new HashMap<>();
        for (Map.Entry<String, String> dd : decls) {
            String d = dd.getKey();
            // the source owner itself trivially declares the member —
            // listing it would exclude every subtype callsite
            // (isAssignableFrom(source) is always true for them). memberMin
            // is gated on outputVersion: the exclusion keeps a *native*
            // call, which only resolves if the member exists on the
            // downgrade target's class — a member newer than the target is
            // exactly the one being stubbed.
            Integer min = graph.memberMin.get(d).get(dd.getValue());
            if (min != null && !serves(member, dd.getValue(), graph, retMemo)) {
                continue;
            }
            if (d.equals(source) || min == null || min > vp.outputVersion || !containsType(d, source, graph.edges, memo)) {
                continue;
            }
            if (children == null) {
                children = new TreeSet<>();
            }
            children.add(d);
        }
        if (children == null) {
            return null;
        }
        String site = member.toFullyQualified(Type.getObjectType(owner)).toString();
        vpDerived.computeIfAbsent(site, k -> new TreeSet<String>()).addAll(children);
        return children;
    }

    /**
     * Derived gained-supertype rows: {@code T → I → since} where interface
     * {@code I} enters {@code T}'s transitive supertype closure at
     * class-file version {@code since} and {@code T} already existed
     * ({@code classMin[T] < since}) — an existing JDK class <em>gaining</em>
     * an interface, not a new type born with one ({@code HttpClient} →
     * {@code AutoCloseable} @65, {@code ExecutorService} @63). Computed by
     * diffing each class's interface closure across consecutive ct.sym
     * windows — {@link JdkSurface#edges} (newest-window) can't see these:
     * a binding added mid-life leaves the newest edges looking like it was
     * always there.
     */
    public static Map<String, Map<String, Integer>> gainedInterfaces(JdkSurface g) {
        // version domain: every window code is a boundary candidate —
        // interface sets only change where a window starts
        Set<Integer> domain = new TreeSet<>();
        for (Map<String, JdkSurface.Window> w : g.windows.values()) {
            for (String win : w.keySet()) {
                for (int i = 0; i < win.length(); i++) {
                    domain.add(decodeVersion(win.charAt(i)) + 44);
                }
            }
        }
        Map<String, Set<String>> prev = new HashMap<>();
        Map<String, Map<String, Integer>> out = new HashMap<>();
        for (int v : domain) {
            Map<String, Set<String>> memo = new HashMap<>();
            for (String cls : g.windows.keySet()) {
                ifaceClosure(g, cls, v, memo);
            }
            for (Map.Entry<String, Set<String>> e : memo.entrySet()) {
                Set<String> old = prev.get(e.getKey());
                for (String i : e.getValue()) {
                    if (old == null || !old.contains(i)) {
                        Integer tMin = g.classMin.get(e.getKey());
                        // a brand-new type's interfaces aren't "gained" —
                        // the binding has to postdate the type itself
                        if (tMin != null && tMin < v) {
                            Map<String, Integer> row = out.computeIfAbsent(e.getKey(), k -> new HashMap<String, Integer>());
                            if (!row.containsKey(i)) {
                                row.put(i, v); // ascending domain: first diff is the earliest binding
                            }
                        }
                    }
                }
            }
            prev = memo;
        }
        return out;
    }

    /**
     * {@code cls}'s interface closure at class-file version {@code v}:
     * itself when it's an interface, plus every interface reachable
     * through that window's supertype edges. Memoized per version —
     * shared sets are safe: each node's set is complete before it's
     * stored, and parents copy via {@code addAll}.
     */
    private static Set<String> ifaceClosure(JdkSurface g, String cls, int v, Map<String, Set<String>> memo) {
        Set<String> hit = memo.get(cls);
        if (hit != null) {
            return hit;
        }
        Set<String> out = new HashSet<>();
        List<String> supers = edgesAt(g, cls, v);
        if (supers != null) {
            for (String s : supers) {
                out.addAll(ifaceClosure(g, s, v, memo));
            }
        }
        if (g.interfaceNames.contains(cls)) {
            out.add(cls);
        }
        memo.put(cls, out);
        return out;
    }

    /** {@code cls}'s window record containing class-file version {@code v}, or null when absent */
    private static JdkSurface.Window windowAt(JdkSurface g, String cls, int v) {
        Map<String, JdkSurface.Window> w = g.windows.get(cls);
        if (w == null) {
            return null;
        }
        for (Map.Entry<String, JdkSurface.Window> e : w.entrySet()) {
            String win = e.getKey();
            for (int i = 0; i < win.length(); i++) {
                if (decodeVersion(win.charAt(i)) + 44 == v) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    /** the supertype list of {@code cls}'s window containing class-file version {@code v}, or null when absent */
    private static List<String> edgesAt(JdkSurface g, String cls, int v) {
        JdkSurface.Window w = windowAt(g, cls, v);
        return w == null ? null : w.supers;
    }

    /**
     * Resolved impl sites for the gained-supertype dispatch:
     * {@code I → member name → member desc → T → [(firstCeiling, declarer)]}
     * — for every {@code (T, I, since)} binding and every abstract method
     * {@code m} declared on {@code I} at-or-below the ceiling domain, the
     * JDK member an {@code invokeinterface I.m} on a {@code T} would
     * resolve to under {@code ceiling} semantics — the first declarer in
     * {@code T}'s superclass-first closure at that window (matching JVM
     * resolution: class hierarchy beats interface defaults). Entries are
     * keyed by the ceiling at which the resolution <em>changes</em>
     * (a member re-declaring {@code m} after the binding), ascending —
     * the dispatcher picks the last entry {@code <= ceiling}.
     */
    /** one resolution entry: the declarer an {@code I.m} site resolves to at ceilings {@code >= ver} */
    public static final class GainedRes {
        public final int ver;
        /** internal name of the class/interface declaring the member, or {@code null} (unresolvable) */
        public final String declarer;

        GainedRes(int ver, String declarer) {
            this.ver = ver;
            this.declarer = declarer;
        }
    }

    /** every class-file version any window covers — the boundary candidates interface sets change at */
    private static TreeSet<Integer> windowDomain(JdkSurface g) {
        TreeSet<Integer> domain = new TreeSet<>();
        for (Map<String, JdkSurface.Window> w : g.windows.values()) {
            for (String win : w.keySet()) {
                for (int i = 0; i < win.length(); i++) {
                    domain.add(decodeVersion(win.charAt(i)) + 44);
                }
            }
        }
        return domain;
    }

    public static Map<String, Map<String, Map<String, List<GainedRes>>>> gainedCallDecls(
        JdkSurface g, Map<String, Map<String, Integer>> gained) {
        TreeSet<Integer> domain = windowDomain(g);
        Map<String, Map<String, Map<String, List<GainedRes>>>> out = new HashMap<>();
        int newest = domain.last();
        for (Map.Entry<String, Map<String, Integer>> t : gained.entrySet()) {
            String cls = t.getKey();
            for (Map.Entry<String, Integer> i : t.getValue().entrySet()) {
                String iface = i.getKey();
                int since = i.getValue();
                JdkSurface.Window win = windowAt(g, iface, newest);
                if (win == null) {
                    continue;
                }
                for (String key : win.decl) {
                    if (!key.substring(key.indexOf(' ') + 1).startsWith("(")) {
                        continue; // fields aren't invokeinterface sites
                    }
                    Integer mMin = g.memberMin.get(iface) == null ? null : g.memberMin.get(iface).get(key);
                    if (mMin == null) {
                        continue;
                    }
                    List<GainedRes> res = new ArrayList<>();
                    String last = null;
                    boolean have = false;
                    for (int v : domain.tailSet(Math.max(since, mMin))) {
                        String d = declarer(g, cls, key, v);
                        if (!Objects.equals(d, last) || !have) {
                            res.add(new GainedRes(v, d));
                            last = d;
                            have = true;
                        }
                    }
                    if (res.isEmpty()) {
                        continue;
                    }
                    out.computeIfAbsent(iface, k -> new TreeMap<String, Map<String, List<GainedRes>>>())
                        .computeIfAbsent(key, k -> new TreeMap<String, List<GainedRes>>())
                        .put(cls, res);
                }
            }
        }
        // pseudo-row T=I on every member: a receiver implementing I
        // natively has I in its name closure, so gainedMemberName lands
        // here when no gained-T row matched — declarer resolves to I (or
        // a superinterface), letting a member absent on the running JDK
        // fall through to I's registered stub instead of AME
        for (Map.Entry<String, Map<String, Map<String, List<GainedRes>>>> iface : out.entrySet()) {
            String i = iface.getKey();
            Map<String, Integer> memberMins = g.memberMin.get(i);
            for (Map.Entry<String, Map<String, List<GainedRes>>> member
                    : new ArrayList<>(iface.getValue().entrySet())) {
                if (member.getValue().containsKey(i)) {
                    continue;
                }
                Integer mMin = memberMins == null ? null : memberMins.get(member.getKey());
                if (mMin == null) {
                    continue;
                }
                List<GainedRes> res = new ArrayList<>();
                String last = null;
                boolean have = false;
                for (int v : domain.tailSet(mMin)) {
                    String d = declarer(g, i, member.getKey(), v);
                    if (!Objects.equals(d, last) || !have) {
                        res.add(new GainedRes(v, d));
                        last = d;
                        have = true;
                    }
                }
                if (!res.isEmpty()) {
                    member.getValue().put(i, res);
                }
            }
        }
        return out;
    }

    /**
     * The member in {@code cls}'s closure declaring {@code name desc} at
     * window {@code v}, superclass-first then interfaces breadth-first —
     * the JDK member {@code I.m} resolves to on a {@code cls} instance.
     * Internal name, or null when nothing declares it.
     */
    private static String declarer(JdkSurface g, String cls, String key, int v) {
        // superclass chain first — concrete methods always win
        for (String c = cls; c != null; ) {
            JdkSurface.Window w = windowAt(g, c, v);
            if (w == null) {
                break;
            }
            if (w.decl.contains(key)) {
                return c;
            }
            String sup = null;
            for (String s : w.supers) {
                if (!g.interfaceNames.contains(s)) {
                    sup = s;
                    break;
                }
            }
            c = sup;
        }
        // interface defaults — breadth-first over the whole supertype
        // closure (a member's interface can be several classes deep),
        // first declarer wins
        Deque<String> walk = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        walk.add(cls);
        while (!walk.isEmpty()) {
            String c = walk.removeFirst();
            JdkSurface.Window w = windowAt(g, c, v);
            if (w == null) {
                continue;
            }
            for (String s : w.supers) {
                if (!seen.add(s)) {
                    continue;
                }
                JdkSurface.Window sw = windowAt(g, s, v);
                if (sw == null) {
                    continue;
                }
                if (g.interfaceNames.contains(s) && sw.decl.contains(key)) {
                    return s;
                }
                walk.add(s);
            }
        }
        return null;
    }

    /**
     * Every interface with a gained binding → its callable member keys at
     * the newest window — the dispatcher/wrapper method set for
     * {@link GainedDispatchGenerator}. Method-keys only (fields can't be
     * gained-dispatched); statics/private methods ride along — a delegate
     * for one is just dead code an {@code invokestatic} site never hits.
     */
    private static Map<String, Set<String>> gainedIfaceMethods(JdkSurface g,
                                                             Map<String, Map<String, Integer>> gained) {
        Set<String> ifaces = new TreeSet<>();
        for (Map<String, Integer> row : gained.values()) {
            ifaces.addAll(row.keySet());
        }
        Map<String, Set<String>> out = new TreeMap<>();
        TreeSet<Integer> domain = windowDomain(g);
        for (String iface : ifaces) {
            // union across windows: a member gained then removed still has
            // to dispatch for sites authored at the earlier ceiling —
            // extra methods on the wrapper are dead code, missing ones
            // are NoSuchMethodErrors
            Set<String> methods = new TreeSet<>();
            for (int v : domain) {
                JdkSurface.Window w = windowAt(g, iface, v);
                if (w == null) {
                    continue;
                }
                for (String key : w.decl) {
                    // methods only — fields' descs don't start with '(', and
                    // <init>/<clinit> are never interface members; a leaked
                    // <clinit> produces unloadable <clinit>$<desc> dispatchers
                    if (!key.startsWith("<") && key.substring(key.indexOf(' ') + 1).startsWith("(")) {
                        methods.add(key);
                    }
                }
            }
            out.put(iface, methods);
        }
        return out;
    }

    /**
     * {@code META-INF/jvmdg/gained-calls} — the per-member impl table for
     * gained-supertype dispatch:
     * {@code I<TAB>name desc<TAB>T=ver:D[>stubSite]|ver:D[>stubSite],…} —
     * resolutions separate on {@code |} since stub-site descriptors carry
     * {@code ;}.
     * {@code D} is the declarer the call resolves to at ceilings
     * {@code >= ver} (a {@code -} marks an unresolvable ceiling — an
     * {@link AbstractMethodError} in the dispatcher); {@code stubSite} is
     * the registered member stub {@code owner.name(desc)} the runtime
     * falls back to when {@code D}'s member doesn't exist on the target —
     * the same site {@code MemberStubTransform} would emit for a direct
     * {@code D.m} site. Provider-independent data: the stub class is in
     * the api jar whichever boundary authored the site.
     */
    /**
     * {@code declarer → member key → registered stub entry} across every
     * provider — the first provider to register (D, key) owns it; sites are
     * identical across providers so the lookup is shared between the
     * {@code gained-calls} row writer and the dispatcher's
     * {@code @ReflectionReferences} data.
     */
    private static Map<String, Map<String, StubEntry>> memberStubLookup(List<VersionProvider> providers) {
        Map<String, Map<String, StubEntry>> stubs = new HashMap<>();
        for (VersionProvider vp : providers) {
            for (Map.Entry<String, StubRegistry.Owner> e : vp.stubRegistry.owners().entrySet()) {
                Map<String, StubEntry> byKey = stubs.computeIfAbsent(e.getKey(),
                    k -> new HashMap<String, StubEntry>());
                for (Map.Entry<MemberNameAndDesc, StubEntry> m : e.getValue().methodStub.entrySet()) {
                    if (m.getValue().handler != null) {
                        byKey.putIfAbsent(m.getKey().getName() + " " + m.getKey().getDesc().getDescriptor(), m.getValue());
                    }
                }
            }
        }
        return stubs;
    }

    /**
     * The stub {@link MethodRef} a gained-call row's declarer resolves to,
     * or null. Exact-desc match first; on a miss the declarer's own
     * window decls are scanned for same-name keys — covariant overrides
     * declare the member under a bridge desc (e.g.
     * {@code List.reversed()Ljava/util/List;} serving the interface's
     * {@code ()Ljava/util/SequencedCollection;} site), and the stub is
     * registered under the declarer's real desc.
     */
    private static StubEntry gainedSite(JdkSurface g, Map<String, Map<String, StubEntry>> stubs,
                                        GainedRes r, String memberKey) {
        if (r.declarer == null) {
            return null;
        }
        Map<String, StubEntry> byKey = stubs.get(r.declarer);
        if (byKey == null) {
            return null;
        }
        StubEntry exact = byKey.get(memberKey);
        if (exact != null) {
            return exact;
        }
        JdkSurface.Window w = windowAt(g, r.declarer, r.ver);
        if (w == null) {
            return null;
        }
        String prefix = memberKey.substring(0, memberKey.indexOf(' ') + 1) + "(";
        for (String key : new TreeSet<>(w.decl)) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            StubEntry ref = byKey.get(key);
            if (ref != null) {
                return ref;
            }
        }
        return null;
    }

    /**
     * The site a gained-call row's {@code >stub} column names: the handler
     * impl, {@code D:}-prefixed when the entry is a dispatchable instance
     * stub — the prefix tells {@code RetroTypes.invokeStub} to run the
     * override-then-impl resolution through {@code DispatchRuntime}
     * .dispatchStub, so a user override of the member still wins over the
     * stub body when the fallback runs.
     */
    private static String gainedDispatchSite(StubEntry e, String memberKey) {
        MethodRef h = e.handler;
        int sp = memberKey.indexOf(' ');
        MemberNameAndDesc member = new MemberNameAndDesc(
            memberKey.substring(0, sp), Type.getMethodType(memberKey.substring(sp + 1)));
        return (DispatchSites.isInstanceStub(e, member) ? "D:" : "") +
            h.member.getOwner().getInternalName() + "." + h.member.getName() +
            h.member.getDesc().getDescriptor();
    }

    /**
     * {@code I → member key → sorted set of stub site refs} — every stub
     * site the gained-call rows for that member resolve to. Fed to
     * {@code GenContext.gainedSites} so generated dispatchers can declare the
     * members shading must keep: {@code RetroTypes.gainedCall} reaches them
     * reflectively, leaving no bytecode edge for the closure to follow.
     */
    private static Map<String, Map<String, Set<MethodRef>>> resolveGainedSites(
            JdkSurface g,
            Map<String, Map<String, Map<String, List<GainedRes>>>> decls,
            Map<String, Map<String, StubEntry>> stubs) {
        Map<String, Map<String, Set<MethodRef>>> out = new TreeMap<>();
        for (Map.Entry<String, Map<String, Map<String, List<GainedRes>>>> iface : decls.entrySet()) {
            Map<String, Set<MethodRef>> byMember = new TreeMap<>();
            for (Map.Entry<String, Map<String, List<GainedRes>>> member : iface.getValue().entrySet()) {
                // entries dedupe on the registry object so one site renders
                // once even when several rows resolve to it
                Set<StubEntry> resolved = new LinkedHashSet<>();
                for (List<GainedRes> res : member.getValue().values()) {
                    for (GainedRes r : res) {
                        StubEntry stub = gainedSite(g, stubs, r, member.getKey());
                        if (stub != null) {
                            resolved.add(stub);
                        }
                    }
                }
                Set<MethodRef> sites = new LinkedHashSet<>();
                for (StubEntry stub : resolved) {
                    sites.add(stub.handler);
                }
                if (!sites.isEmpty()) {
                    byMember.put(member.getKey(), sites);
                }
            }
            if (!byMember.isEmpty()) {
                out.put(iface.getKey(), byMember);
            }
        }
        return out;
    }

    private static void writeGainedCalls(Path out, JdkSurface g,
                                         Map<String, Map<String, Integer>> gained,
                                         Map<String, Map<String, Map<String, List<GainedRes>>>> decls,
                                         Map<String, Map<String, StubEntry>> stubs) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Map<String, List<GainedRes>>>> iface : new TreeMap<>(decls).entrySet()) {
            for (Map.Entry<String, Map<String, List<GainedRes>>> member : iface.getValue().entrySet()) {
                sb.append(iface.getKey()).append('\t').append(member.getKey()).append('\t');
                boolean firstT = true;
                for (Map.Entry<String, List<GainedRes>> t : member.getValue().entrySet()) {
                    if (!firstT) {
                        sb.append(',');
                    }
                    firstT = false;
                    sb.append(t.getKey()).append('=');
                    boolean firstRes = true;
                    for (GainedRes r : t.getValue()) {
                        if (!firstRes) {
                            sb.append('|');
                        }
                        firstRes = false;
                        sb.append(r.ver).append(':').append(r.declarer == null ? "-" : r.declarer);
                        StubEntry stub = gainedSite(g, stubs, r, member.getKey());
                        if (stub != null) {
                            sb.append('>').append(gainedDispatchSite(stub, member.getKey()));
                        }
                    }
                }
                sb.append('\n');
            }
        }
        Files.createDirectories(out.getParent());
        Files.write(out, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@code META-INF/jvmdg/gained} — the gained-supertype table serving
     * transform-site routing AND runtime membership:
     * {@code I<TAB>I-min<TAB>T:since,T:since,…} in class-file versions.
     * {@link DerivedGained} load-filters per provider (interface exists at
     * output, member {@code output < since ≤ input}) for site routing;
     * {@code RetroTypes} keeps the full {@code since} map so emitted sites
     * evaluate membership at their authoring-version ceiling. Sorted for
     * byte-identical output.
     */
    private static void writeGained(Path out, Map<String, Map<String, Integer>> gained, JdkSurface graph) throws IOException {
        Map<String, Map<String, Integer>> byIface = new TreeMap<>();
        for (Map.Entry<String, Map<String, Integer>> t : gained.entrySet()) {
            for (Map.Entry<String, Integer> i : t.getValue().entrySet()) {
                byIface.computeIfAbsent(i.getKey(), k -> new TreeMap<String, Integer>())
                    .put(t.getKey(), i.getValue());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Integer>> row : byIface.entrySet()) {
            Integer imin = graph.classMin.get(row.getKey());
            sb.append(row.getKey()).append('\t').append(imin == null ? 0 : imin).append('\t');
            boolean first = true;
            for (Map.Entry<String, Integer> m : row.getValue().entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(m.getKey()).append(':').append(m.getValue());
            }
            sb.append('\n');
        }
        Files.createDirectories(out.getParent());
        Files.write(out, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** {@code cls}'s transitive supertype closure (itself included) contains {@code target} */
    private static boolean containsType(String cls, String target, Map<String, List<String>> edges, Map<String, Boolean> memo) {
        Boolean hit = memo.get(cls);
        if (hit != null) {
            return hit;
        }
        if (cls.equals(target)) {
            memo.put(cls, Boolean.TRUE);
            return true;
        }
        List<String> supers = edges.get(cls);
        boolean result = false;
        if (supers != null) {
            for (String s : supers) {
                if (containsType(s, target, edges, memo)) {
                    result = true;
                    break;
                }
            }
        }
        memo.put(cls, result);
        return result;
    }

    private static boolean preview(ClassNode cn) {
        if (cn.invisibleAnnotations == null) {
            return false;
        }
        for (AnnotationNode a : cn.invisibleAnnotations) {
            if (a.desc.equals("Ljdk/internal/javac/PreviewFeature;") || a.desc.equals("Ljdk/internal/PreviewFeature;")) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code META-INF/jvmdg/retrotypes} — the runtime membership table
     * {@code RetroTypes} reads: one {@code retro<TAB>name,name,…} row per
     * classStub source, merging the derived/declared implementors with the
     * adapter's own type and its {@code $Marker} interface (both implicit
     * members). Sorted for byte-identical output.
     */
    private static void writeRetroTypes(Path out, List<VersionProvider> providers) throws IOException {
        Map<String, Set<String>> rows = new TreeMap<>();
        for (VersionProvider vp : providers) {
            for (Map.Entry<Type, ClassStubEntry> e : vp.classStubs.entrySet()) {
                Set<String> names = rows.computeIfAbsent(e.getKey().getInternalName(), k -> new TreeSet<String>());
                names.addAll(e.getValue().implementedBy);
                String stub = e.getValue().stubType.getInternalName();
                names.add(stub);
                names.add(stub + "$Marker");
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> row : rows.entrySet()) {
            sb.append(row.getKey()).append('\t');
            boolean first = true;
            for (String name : row.getValue()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(name);
            }
            sb.append('\n');
        }
        Files.createDirectories(out.getParent());
        Files.write(out, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@code META-INF/jvmdg/excludes} — the scan path's copy of the derived
     * {@code excludeChild} data: {@code provider<TAB>member<TAB>child,…}
     * rows, provider-scoped because the {@code inputVersion} gate differs per
     * provider. The idx path needs nothing here — its entries already carry
     * the union — but the reflective scan builds annotations-only entries
     * and unions this resource in at {@code ensureInit}, keeping the two
     * load paths' resolution identical.
     */
    private static void writeExcludes(Path out, Map<VersionProvider, Map<String, Set<String>>> derived) throws IOException {
        Map<String, Map<String, Set<String>>> sorted = new TreeMap<>();
        for (Map.Entry<VersionProvider, Map<String, Set<String>>> e : derived.entrySet()) {
            sorted.put(e.getKey().getClass().getName(), new TreeMap<>(e.getValue()));
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Set<String>>> p : sorted.entrySet()) {
            for (Map.Entry<String, Set<String>> m : p.getValue().entrySet()) {
                sb.append(p.getKey()).append('\t').append(m.getKey()).append('\t');
                boolean first = true;
                for (String child : m.getValue()) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;
                    sb.append(child);
                }
                sb.append('\n');
            }
        }
        Files.createDirectories(out.getParent());
        Files.write(out, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@code META-INF/jvmdg/wraps} — OWN_INTERFACE adaptation rows
     * ({@code retro<TAB>stubAdapter<TAB>member=impl,…}, ordered) from
     * {@link GenContext#flattenedWraps()}, which shares the pull-up
     * ordering with the generated {@code $jvmdg$retro} dispatchers so the
     * table and the bytecode can't drift.
     */
    private static void writeWraps(Path out, Map<String, GenContext.FlatWrap> rows) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, GenContext.FlatWrap> row : rows.entrySet()) {
            sb.append(row.getKey()).append('\t').append(row.getValue().stub).append('\t');
            List<String[]> pairs = row.getValue().pairs;
            for (int i = 0; i < pairs.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(pairs.get(i)[0]).append('=').append(pairs.get(i)[1]);
            }
            sb.append('\n');
        }
        Files.createDirectories(out.getParent());
        Files.write(out, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Register {@code owner + member} for the since pass; {@code expanded}
     * strips the trailing {@code int} input-version arg of a
     * {@code downgradeVersion} member so the lookup names the real JDK member.
     */
    private static void addWanted(Map<String, Map<FullyQualifiedMemberNameAndDesc, MemberNameAndDesc>> wanted,
                                  String owner, MemberNameAndDesc member, boolean expanded) {
        MemberNameAndDesc jdk = member;
        if (expanded && member.isMethod()) {
            Type d = member.getDesc();
            Type[] args = d.getArgumentTypes();
            if (args.length > 0 && args[args.length - 1] == Type.INT_TYPE) {
                Type[] stripped = new Type[args.length - 1];
                System.arraycopy(args, 0, stripped, 0, stripped.length);
                jdk = new MemberNameAndDesc(member.getName(),
                    Type.getMethodType(d.getReturnType(), stripped));
            }
        }
        wanted.computeIfAbsent(owner, k -> new HashMap<FullyQualifiedMemberNameAndDesc, MemberNameAndDesc>())
            .put(new FullyQualifiedMemberNameAndDesc(Type.getObjectType(owner), jdk.getName(), jdk.getDesc()), jdk);
    }

    /** ct.sym window code: '0'-'9' → 0-9, 'A'-'Z' → 10-35 (first char is the window's min version). */
    private static int decodeVersion(char c) {
        return c <= '9' ? c - '0' : 10 + (c - 'A');
    }

    private static boolean hasMember(ClassNode cn, MemberNameAndDesc member) {
        String desc = member.getDesc().getDescriptor();
        if (desc.charAt(0) == '(') {
            for (MethodNode m : cn.methods) {
                if (m.name.equals(member.getName()) && m.desc.equals(desc)) return true;
            }
        } else {
            for (FieldNode f : cn.fields) {
                if (f.name.equals(member.getName()) && f.desc.equals(desc)) return true;
            }
        }
        return false;
    }

    private StubIndexWriter() {
    }
}
