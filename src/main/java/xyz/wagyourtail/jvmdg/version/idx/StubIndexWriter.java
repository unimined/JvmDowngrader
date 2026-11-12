package xyz.wagyourtail.jvmdg.version.idx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.CtorEntry;
import xyz.wagyourtail.jvmdg.version.map.CtorFactory;
import xyz.wagyourtail.jvmdg.version.map.CtorRewrite;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;
import xyz.wagyourtail.jvmdg.version.map.StubRegistry;

/**
 * Build-time entry point that writes {@code META-INF/jvmdg/stubs.idx}: runs
 * every {@link VersionProvider} found by {@link ServiceLoader} through
 * {@link VersionProvider#ensureInit} — the same reflective scan the runtime
 * uses, so the index is parity-by-construction — then serializes the
 * resulting registries via {@link StubIndex}.
 *
 * <p>Usage: {@code StubIndexWriter <output-file> <jvmdg-version> <ct.sym>}. The output
 * file should be packaged into the api jar at {@link StubIndex#RESOURCE}. The
 * {@code ct.sym} arg (the {@code genCtSym} product — {@code <vercodes>/<mod>/<class>.sig}
 * version-windowed JDK API surface) feeds {@link StubRegistry#memberSince}: for every
 * callsite member it records the first JDK class version carrying it, which is what
 * lets {@code MISSING_API} vs {@code BEHAVIOR} classify per downgrade target.
 */
public class StubIndexWriter {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("usage: StubIndexWriter <output-file> <jvmdg-version> <ct.sym>");
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
        StubIndex.write(out, args[1], providers);
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
