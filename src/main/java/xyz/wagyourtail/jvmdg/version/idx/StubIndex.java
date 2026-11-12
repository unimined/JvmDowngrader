package xyz.wagyourtail.jvmdg.version.idx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.objectweb.asm.Type;

import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;
import xyz.wagyourtail.jvmdg.version.map.CtorEntry;
import xyz.wagyourtail.jvmdg.version.map.CtorFactory;
import xyz.wagyourtail.jvmdg.version.map.CtorRewrite;
import xyz.wagyourtail.jvmdg.version.map.FieldStubEntry;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;
import xyz.wagyourtail.jvmdg.version.map.StubRegistry;

/**
 * Binary codec for {@code META-INF/jvmdg/stubs.idx} — a deterministic,
 * versioned snapshot of each {@link VersionProvider}'s {@link StubRegistry}
 * and {@code classStubs}, serialized from the reflective scan's output so the
 * index stays parity-by-construction (the scan remains the semantic source of
 * truth; the index is a data dump of it).
 *
 * <p>Layout:
 * <pre>
 *   u8[8]  magic "JVMDGIDX"
 *   u16    format version ({@link #FORMAT_VERSION})
 *   UTF    jvmdg version the api jar was built against
 *   u16    provider section count
 *   per provider section (sorted by provider class name):
 *     UTF      provider class name
 *     u16      classStubs count (sorted by source type name)
 *       per: UTF sourceName, UTF targetName, UTF stubName, u8 keepInterface,
 *            optFQMND instanceofHook, optFQMND checkcastHook, int[] jeps
 *     u16      owner count (sorted by owner internal name)
 *       per: UTF ownerName,
 *            u16 methodStub entries (member + StubEntry),
 *            u16 fieldStub entries (member + opt StubEntry getter + opt setter),
 *            u16 ctor entries (member + u8 kind + payload),
 *            u16 memberModify entries (opt member + MethodRef)
 *     u16      memberSince count — site-form FQN + u16 class version the JDK
 *            member first appears in (ct.sym-derived; 0 = unknown)
 *     u16      memberKind count — site-form FQN + u8 {@link xyz.wagyourtail.jvmdg.version.StubKind}
 *            ordinal, explicit {@code kind=} overrides only
 * </pre>
 *
 * <p>Every map is emitted sorted by its key so identical registries produce
 * byte-identical files; the index contains no timestamps or paths.
 */
public class StubIndex {

    public static final String RESOURCE = "META-INF/jvmdg/stubs.idx";
    public static final byte[] MAGIC = {'J', 'V', 'M', 'D', 'G', 'I', 'D', 'X'};
    public static final int FORMAT_VERSION = 2;

    private static final Comparator<FullyQualifiedMemberNameAndDesc> FQ_ORDER = new Comparator<FullyQualifiedMemberNameAndDesc>() {
        @Override
        public int compare(FullyQualifiedMemberNameAndDesc a, FullyQualifiedMemberNameAndDesc b) {
            return a.toString().compareTo(b.toString());
        }
    };

    private static final Comparator<MemberNameAndDesc> MEMBER_ORDER = new Comparator<MemberNameAndDesc>() {
        @Override
        public int compare(MemberNameAndDesc a, MemberNameAndDesc b) {
            int c = a.getName().compareTo(b.getName());
            if (c != 0) return c;
            c = a.getDesc().getDescriptor().compareTo(b.getDesc().getDescriptor());
            if (c != 0) return c;
            return Boolean.compare(a.isIgnoreReturnValue(), b.isIgnoreReturnValue());
        }
    };

    private StubIndex() {
    }

    /** file header — exposed so loaders can validate before consuming sections */
    public static class Header {
        public final int formatVersion;
        public final String jvmdgVersion;

        public Header(int formatVersion, String jvmdgVersion) {
            this.formatVersion = formatVersion;
            this.jvmdgVersion = jvmdgVersion;
        }
    }

    /** one provider's deserialized section */
    public static class Snapshot {
        public final Map<String, StubRegistry.Owner> owners = new TreeMap<>();
        public final Map<FullyQualifiedMemberNameAndDesc, Integer> memberSince = new TreeMap<>(FQ_ORDER);
        public final Map<FullyQualifiedMemberNameAndDesc, xyz.wagyourtail.jvmdg.version.StubKind> memberKind = new TreeMap<>(FQ_ORDER);
        public final Map<Type, ClassStubEntry> classStubs = new TreeMap<>(new Comparator<Type>() {
            @Override
            public int compare(Type a, Type b) {
                return a.getInternalName().compareTo(b.getInternalName());
            }
        });
    }

    // ---------- write ----------

    /** serialize the given providers' registries; providers are emitted sorted by class name */
    public static void write(Path out, String jvmdgVersion, List<VersionProvider> providers) throws IOException {
        List<VersionProvider> sorted = new ArrayList<>(providers);
        sorted.sort(new Comparator<VersionProvider>() {
            @Override
            public int compare(VersionProvider a, VersionProvider b) {
                return a.getClass().getName().compareTo(b.getClass().getName());
            }
        });
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        DataOutputStream o = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(out)));
        try {
            o.write(MAGIC);
            o.writeShort(FORMAT_VERSION);
            o.writeUTF(jvmdgVersion);
            o.writeShort(sorted.size());
            for (VersionProvider vp : sorted) {
                writeProvider(o, vp);
            }
        } finally {
            o.close();
        }
    }

    private static void writeProvider(DataOutputStream o, VersionProvider vp) throws IOException {
        o.writeUTF(vp.getClass().getName());
        List<Map.Entry<Type, ClassStubEntry>> stubs = new ArrayList<>(vp.classStubs.entrySet());
        stubs.sort(new Comparator<Map.Entry<Type, ClassStubEntry>>() {
            @Override
            public int compare(Map.Entry<Type, ClassStubEntry> a, Map.Entry<Type, ClassStubEntry> b) {
                return a.getKey().getInternalName().compareTo(b.getKey().getInternalName());
            }
        });
        o.writeShort(stubs.size());
        for (Map.Entry<Type, ClassStubEntry> e : stubs) {
            ClassStubEntry s = e.getValue();
            o.writeUTF(e.getKey().getInternalName());
            o.writeUTF(s.target.getInternalName());
            o.writeUTF(s.stubType.getInternalName());
            o.writeByte(s.keepInterface ? 1 : 0);
            writeOptFQMND(o, s.instanceofHook);
            writeOptFQMND(o, s.checkcastHook);
            writeInts(o, s.jeps);
        }
        Map<String, StubRegistry.Owner> owners = new TreeMap<>(vp.stubRegistry.owners());
        o.writeShort(owners.size());
        for (Map.Entry<String, StubRegistry.Owner> e : owners.entrySet()) {
            o.writeUTF(e.getKey());
            StubRegistry.Owner owner = e.getValue();
            o.writeShort(owner.methodStub.size());
            for (Map.Entry<MemberNameAndDesc, StubEntry> m : sorted(owner.methodStub)) {
                writeMemberKey(o, m.getKey());
                writeStubEntry(o, m.getValue());
            }
            o.writeShort(owner.fieldStub.size());
            for (Map.Entry<MemberNameAndDesc, FieldStubEntry> m : sorted(owner.fieldStub)) {
                writeMemberKey(o, m.getKey());
                writeOptStubEntry(o, m.getValue().getter);
                writeOptStubEntry(o, m.getValue().setter);
            }
            o.writeShort(owner.ctor.size());
            for (Map.Entry<MemberNameAndDesc, CtorEntry> m : sorted(owner.ctor)) {
                writeMemberKey(o, m.getKey());
                writeCtorEntry(o, m.getValue());
            }
            o.writeShort(owner.memberModify.size());
            List<Map.Entry<MemberNameAndDesc, MethodRef>> modifies = new ArrayList<>(owner.memberModify.entrySet());
            modifies.sort(new Comparator<Map.Entry<MemberNameAndDesc, MethodRef>>() {
                @Override
                public int compare(Map.Entry<MemberNameAndDesc, MethodRef> a, Map.Entry<MemberNameAndDesc, MethodRef> b) {
                    if (a.getKey() == null) return b.getKey() == null ? 0 : -1;
                    if (b.getKey() == null) return 1;
                    return MEMBER_ORDER.compare(a.getKey(), b.getKey());
                }
            });
            for (Map.Entry<MemberNameAndDesc, MethodRef> m : modifies) {
                if (m.getKey() == null) {
                    o.writeByte(0);
                } else {
                    o.writeByte(1);
                    writeMemberKey(o, m.getKey());
                }
                writeMethodRef(o, m.getValue());
            }
        }
        List<Map.Entry<FullyQualifiedMemberNameAndDesc, Integer>> since =
            new ArrayList<>(vp.stubRegistry.memberSince.entrySet());
        since.sort(new Comparator<Map.Entry<FullyQualifiedMemberNameAndDesc, Integer>>() {
            @Override
            public int compare(Map.Entry<FullyQualifiedMemberNameAndDesc, Integer> a,
                               Map.Entry<FullyQualifiedMemberNameAndDesc, Integer> b) {
                return FQ_ORDER.compare(a.getKey(), b.getKey());
            }
        });
        o.writeShort(since.size());
        for (Map.Entry<FullyQualifiedMemberNameAndDesc, Integer> e : since) {
            writeFQMND(o, e.getKey());
            o.writeShort(e.getValue());
        }
        List<Map.Entry<FullyQualifiedMemberNameAndDesc, xyz.wagyourtail.jvmdg.version.StubKind>> kinds =
            new ArrayList<>(vp.stubRegistry.memberKind.entrySet());
        kinds.sort(new Comparator<Map.Entry<FullyQualifiedMemberNameAndDesc, xyz.wagyourtail.jvmdg.version.StubKind>>() {
            @Override
            public int compare(Map.Entry<FullyQualifiedMemberNameAndDesc, xyz.wagyourtail.jvmdg.version.StubKind> a,
                               Map.Entry<FullyQualifiedMemberNameAndDesc, xyz.wagyourtail.jvmdg.version.StubKind> b) {
                return FQ_ORDER.compare(a.getKey(), b.getKey());
            }
        });
        o.writeShort(kinds.size());
        for (Map.Entry<FullyQualifiedMemberNameAndDesc, xyz.wagyourtail.jvmdg.version.StubKind> e : kinds) {
            writeFQMND(o, e.getKey());
            o.writeByte(e.getValue().ordinal());
        }
    }

    private static <V> List<Map.Entry<MemberNameAndDesc, V>> sorted(Map<MemberNameAndDesc, V> map) {
        List<Map.Entry<MemberNameAndDesc, V>> list = new ArrayList<>(map.entrySet());
        list.sort(new Comparator<Map.Entry<MemberNameAndDesc, V>>() {
            @Override
            public int compare(Map.Entry<MemberNameAndDesc, V> a, Map.Entry<MemberNameAndDesc, V> b) {
                return MEMBER_ORDER.compare(a.getKey(), b.getKey());
            }
        });
        return list;
    }

    private static void writeMemberKey(DataOutputStream o, MemberNameAndDesc m) throws IOException {
        o.writeUTF(m.getName());
        o.writeUTF(m.getDesc().getDescriptor());
        o.writeByte(m.isIgnoreReturnValue() ? 1 : 0);
    }

    private static void writeStubEntry(DataOutputStream o, StubEntry e) throws IOException {
        writeMethodRef(o, e.handler);
        o.writeByte((e.downgradeVersion ? 1 : 0) | (e.requiresRuntime ? 2 : 0)
            | (e.abstractDefault ? 4 : 0) | (e.noSpecial ? 8 : 0));
        writeStrings(o, e.excludeChild);
    }

    private static void writeOptStubEntry(DataOutputStream o, StubEntry e) throws IOException {
        o.writeByte(e == null ? 0 : 1);
        if (e != null) writeStubEntry(o, e);
    }

    private static void writeCtorEntry(DataOutputStream o, CtorEntry e) throws IOException {
        if (e instanceof CtorFactory) {
            o.writeByte(0);
            CtorFactory f = (CtorFactory) e;
            writeMethodRef(o, f.handler);
            o.writeByte((f.downgradeVersion ? 1 : 0) | (f.requiresRuntime ? 2 : 0));
        } else {
            o.writeByte(1);
            CtorRewrite r = (CtorRewrite) e;
            o.writeByte((r.downgradeVersion ? 1 : 0) | (r.requiresRuntime ? 2 : 0));
            o.writeByte(r.toArgs.length);
            for (Type t : r.toArgs) o.writeUTF(t.getDescriptor());
            writeInts(o, r.keep);
            o.writeByte(r.argMethods.length);
            for (int i = 0; i < r.argMethods.length; i++) {
                writeOptMethodRef(o, r.argMethods[i]);
                writeOptInts(o, r.argFrom[i]);
            }
            writeOptMethodRef(o, r.pre);
            writeInts(o, r.preFrom);
            writeOptMethodRef(o, r.post);
            writeInts(o, r.postFrom);
        }
    }

    private static void writeMethodRef(DataOutputStream o, MethodRef r) throws IOException {
        writeFQMND(o, r.member);
        o.writeInt(r.modifiers);
        o.writeByte(r.itf ? 1 : 0);
        writeInts(o, r.jeps);
    }

    private static void writeOptMethodRef(DataOutputStream o, MethodRef r) throws IOException {
        o.writeByte(r == null ? 0 : 1);
        if (r != null) writeMethodRef(o, r);
    }

    private static void writeFQMND(DataOutputStream o, FullyQualifiedMemberNameAndDesc f) throws IOException {
        o.writeUTF(f.getOwner().getInternalName());
        o.writeUTF(f.getName());
        o.writeUTF(f.getDesc().getDescriptor());
        o.writeByte(f.isIgnoreReturnValue() ? 1 : 0);
    }

    private static void writeOptFQMND(DataOutputStream o, FullyQualifiedMemberNameAndDesc f) throws IOException {
        o.writeByte(f == null ? 0 : 1);
        if (f != null) writeFQMND(o, f);
    }

    private static void writeInts(DataOutputStream o, int[] ints) throws IOException {
        o.writeShort(ints.length);
        for (int i : ints) o.writeInt(i);
    }

    private static void writeOptInts(DataOutputStream o, int[] ints) throws IOException {
        o.writeByte(ints == null ? 0 : 1);
        if (ints != null) writeInts(o, ints);
    }

    private static void writeStrings(DataOutputStream o, String[] strings) throws IOException {
        o.writeShort(strings.length);
        for (String s : strings) o.writeUTF(s);
    }

    // ---------- read ----------

    /** read + validate the magic; returns the header for the caller to version-check */
    public static Header readHeader(DataInputStream in) throws IOException {
        byte[] magic = new byte[MAGIC.length];
        in.readFully(magic);
        for (int i = 0; i < MAGIC.length; i++) {
            if (magic[i] != MAGIC[i]) throw new IOException("not a stubs.idx (bad magic)");
        }
        return new Header(in.readShort() & 0xFFFF, in.readUTF());
    }

    /** read all provider sections after the header; {@code classLookup} is handed to lazy {@link MethodRef}s */
    public static Map<String, Snapshot> read(DataInputStream in, Function<String, Class<?>> classLookup) throws IOException {
        int providers = in.readShort() & 0xFFFF;
        Map<String, Snapshot> out = new HashMap<>();
        for (int i = 0; i < providers; i++) {
            String name = in.readUTF();
            Snapshot s = readProvider(in, classLookup);
            if (out.put(name, s) != null) throw new IOException("duplicate provider section " + name);
        }
        return out;
    }

    /** convenience: read a complete index file (header + sections) */
    public static Map<String, Snapshot> read(InputStream raw, Function<String, Class<?>> classLookup) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(raw));
        readHeader(in);
        return read(in, classLookup);
    }

    private static Snapshot readProvider(DataInputStream in, Function<String, Class<?>> classLookup) throws IOException {
        Snapshot s = new Snapshot();
        int classStubs = in.readShort() & 0xFFFF;
        for (int i = 0; i < classStubs; i++) {
            Type source = Type.getObjectType(in.readUTF());
            Type target = Type.getObjectType(in.readUTF());
            Type stubType = Type.getObjectType(in.readUTF());
            boolean keepInterface = in.readByte() != 0;
            FullyQualifiedMemberNameAndDesc io = readOptFQMND(in);
            FullyQualifiedMemberNameAndDesc cc = readOptFQMND(in);
            int[] jeps = readInts(in);
            if (s.classStubs.put(source, new ClassStubEntry(target, stubType, keepInterface, io, cc, jeps)) != null) {
                throw new IOException("duplicate classStub for " + source.getInternalName());
            }
        }
        int owners = in.readShort() & 0xFFFF;
        for (int i = 0; i < owners; i++) {
            String ownerName = in.readUTF();
            StubRegistry.Owner owner = new StubRegistry.Owner();
            int methodStubs = in.readShort() & 0xFFFF;
            for (int j = 0; j < methodStubs; j++) {
                owner.methodStub.put(readMemberKey(in), readStubEntry(in, classLookup));
            }
            int fieldStubs = in.readShort() & 0xFFFF;
            for (int j = 0; j < fieldStubs; j++) {
                MemberNameAndDesc key = readMemberKey(in);
                owner.fieldStub.put(key, new FieldStubEntry(
                    readOptStubEntry(in, classLookup), readOptStubEntry(in, classLookup)));
            }
            int ctors = in.readShort() & 0xFFFF;
            for (int j = 0; j < ctors; j++) {
                owner.ctor.put(readMemberKey(in), readCtorEntry(in, classLookup));
            }
            int modifies = in.readShort() & 0xFFFF;
            for (int j = 0; j < modifies; j++) {
                MemberNameAndDesc key = in.readByte() != 0 ? readMemberKey(in) : null;
                owner.memberModify.put(key, readMethodRef(in, classLookup));
            }
            if (s.owners.put(ownerName, owner) != null) throw new IOException("duplicate owner " + ownerName);
        }
        int sinceCount = in.readShort() & 0xFFFF;
        for (int i = 0; i < sinceCount; i++) {
            s.memberSince.put(readFQMND(in), in.readShort() & 0xFFFF);
        }
        int kindCount = in.readShort() & 0xFFFF;
        for (int i = 0; i < kindCount; i++) {
            s.memberKind.put(readFQMND(in),
                xyz.wagyourtail.jvmdg.version.StubKind.values()[in.readByte()]);
        }
        return s;
    }

    private static MemberNameAndDesc readMemberKey(DataInputStream in) throws IOException {
        MemberNameAndDesc m = new MemberNameAndDesc(in.readUTF(), Type.getType(in.readUTF()));
        return in.readByte() != 0 ? m.ignoreReturnValue() : m;
    }

    private static StubEntry readStubEntry(DataInputStream in, Function<String, Class<?>> classLookup) throws IOException {
        MethodRef handler = readMethodRef(in, classLookup);
        int flags = in.readByte();
        return new StubEntry(handler, (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0, (flags & 8) != 0, readStrings(in));
    }

    private static StubEntry readOptStubEntry(DataInputStream in, Function<String, Class<?>> classLookup) throws IOException {
        return in.readByte() != 0 ? readStubEntry(in, classLookup) : null;
    }

    private static CtorEntry readCtorEntry(DataInputStream in, Function<String, Class<?>> classLookup) throws IOException {
        int kind = in.readByte();
        if (kind == 0) {
            MethodRef handler = readMethodRef(in, classLookup);
            int flags = in.readByte();
            return new CtorFactory(handler, (flags & 1) != 0, (flags & 2) != 0);
        }
        int flags = in.readByte();
        int argCount = in.readByte() & 0xFF;
        Type[] toArgs = new Type[argCount];
        for (int i = 0; i < argCount; i++) toArgs[i] = Type.getType(in.readUTF());
        int[] keep = readInts(in);
        int slots = in.readByte() & 0xFF;
        MethodRef[] argMethods = new MethodRef[slots];
        int[][] argFrom = new int[slots][];
        for (int i = 0; i < slots; i++) {
            argMethods[i] = readOptMethodRef(in, classLookup);
            argFrom[i] = readOptInts(in);
        }
        MethodRef pre = readOptMethodRef(in, classLookup);
        int[] preFrom = readInts(in);
        MethodRef post = readOptMethodRef(in, classLookup);
        int[] postFrom = readInts(in);
        CtorRewrite r = new CtorRewrite(toArgs, keep, argMethods, argFrom, pre, preFrom, post, postFrom,
            (flags & 1) != 0, (flags & 2) != 0);
        return r;
    }

    private static MethodRef readMethodRef(DataInputStream in, Function<String, Class<?>> classLookup) throws IOException {
        FullyQualifiedMemberNameAndDesc member = readFQMND(in);
        int modifiers = in.readInt();
        boolean itf = in.readByte() != 0;
        int[] jeps = readInts(in);
        return new MethodRef(member, modifiers, itf, jeps, classLookup);
    }

    private static MethodRef readOptMethodRef(DataInputStream in, Function<String, Class<?>> classLookup) throws IOException {
        return in.readByte() != 0 ? readMethodRef(in, classLookup) : null;
    }

    private static FullyQualifiedMemberNameAndDesc readFQMND(DataInputStream in) throws IOException {
        FullyQualifiedMemberNameAndDesc f = new FullyQualifiedMemberNameAndDesc(
            Type.getObjectType(in.readUTF()), in.readUTF(), Type.getType(in.readUTF()));
        return in.readByte() != 0 ? f.ignoreReturnValue() : f;
    }

    private static FullyQualifiedMemberNameAndDesc readOptFQMND(DataInputStream in) throws IOException {
        return in.readByte() != 0 ? readFQMND(in) : null;
    }

    private static int[] readInts(DataInputStream in) throws IOException {
        int[] ints = new int[in.readShort() & 0xFFFF];
        for (int i = 0; i < ints.length; i++) ints[i] = in.readInt();
        return ints;
    }

    private static int[] readOptInts(DataInputStream in) throws IOException {
        return in.readByte() != 0 ? readInts(in) : null;
    }

    private static String[] readStrings(DataInputStream in) throws IOException {
        String[] strings = new String[in.readShort() & 0xFFFF];
        for (int i = 0; i < strings.length; i++) strings[i] = in.readUTF();
        return strings;
    }

    /**
     * install a loaded snapshot into a provider's registry, applying the same
     * {@link VersionProvider#shouldSkip} filtering the reflective scan applies
     * at registration time (so {@code debugSkipJeps}/{@code debugSkipStub}
     * behave identically on both paths)
     */
    public static void apply(Snapshot snapshot, VersionProvider vp) {
        vp.stubRegistry.memberSince.putAll(snapshot.memberSince);
        vp.stubRegistry.memberKind.putAll(snapshot.memberKind);
        for (Map.Entry<Type, ClassStubEntry> e : snapshot.classStubs.entrySet()) {
            if (!vp.shouldSkip(e.getValue())) {
                vp.classStubs.put(e.getKey(), e.getValue());
            }
        }
        for (Map.Entry<String, StubRegistry.Owner> e : snapshot.owners.entrySet()) {
            StubRegistry.Owner target = null;
            for (Map.Entry<MemberNameAndDesc, StubEntry> m : e.getValue().methodStub.entrySet()) {
                if (vp.shouldSkip(m.getValue().handler)) continue;
                if (target == null) target = vp.stubRegistry.forOwner(Type.getObjectType(e.getKey()));
                target.methodStub.put(m.getKey(), m.getValue());
            }
            for (Map.Entry<MemberNameAndDesc, FieldStubEntry> m : e.getValue().fieldStub.entrySet()) {
                StubEntry getter = m.getValue().getter != null && !vp.shouldSkip(m.getValue().getter.handler) ? m.getValue().getter : null;
                StubEntry setter = m.getValue().setter != null && !vp.shouldSkip(m.getValue().setter.handler) ? m.getValue().setter : null;
                if (getter == null && setter == null) continue;
                if (target == null) target = vp.stubRegistry.forOwner(Type.getObjectType(e.getKey()));
                target.fieldStub.put(m.getKey(), new FieldStubEntry(getter, setter));
            }
            for (Map.Entry<MemberNameAndDesc, CtorEntry> m : e.getValue().ctor.entrySet()) {
                if (ctorSkipped(m.getValue(), vp)) continue;
                if (target == null) target = vp.stubRegistry.forOwner(Type.getObjectType(e.getKey()));
                target.ctor.put(m.getKey(), m.getValue());
            }
            for (Map.Entry<MemberNameAndDesc, MethodRef> m : e.getValue().memberModify.entrySet()) {
                if (vp.shouldSkip(m.getValue())) continue;
                if (target == null) target = vp.stubRegistry.forOwner(Type.getObjectType(e.getKey()));
                target.memberModify.put(m.getKey(), m.getValue());
            }
        }
    }

    /** a ctor entry is dropped if any handler it references is filtered — the scan would fail its link validation */
    private static boolean ctorSkipped(CtorEntry entry, VersionProvider vp) {
        for (MethodRef r : entry.methods()) {
            if (r != null && vp.shouldSkip(r)) return true;
        }
        return false;
    }
}
