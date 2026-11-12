package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Registered {@code @Stub}/{@code @Modify} handlers, grouped by the JDK type
 * they target. Pure storage: hierarchy walks, member-existence checks,
 * opcode gating and coverage live in the resolving consumer
 * ({@link ClassMapping} today, the Phase C {@code MemberResolver} next).
 * Entries are annotation-free data ({@link StubEntry}), so an index-based
 * producer can feed the same registry.
 * <p>
 * One registry per {@link xyz.wagyourtail.jvmdg.version.VersionProvider} —
 * providers never resolve each other's stubs, matching the per-provider
 * {@code stubMappings} scope it replaces.
 */
public class StubRegistry {

    // keyed by Type#getInternalName — String caches its hash, ASM Type
    // recomputes it over the whole name on every lookup, and this map is
    // hit per member reference during transformation
    private final Map<String, Owner> owners = new HashMap<>();

    /**
     * JDK-introduction class-version per callsite member, written by the
     * build-time index from the {@code ct.sym} membership data (0 = unknown,
     * classifies {@link xyz.wagyourtail.jvmdg.version.StubKind#MISSING_API}
     * on every target). Side data — not part of entry identity, so the
     * idx/scan parity oracle compares {@link #owners} only.
     */
    public final Map<FullyQualifiedMemberNameAndDesc, Integer> memberSince = new HashMap<>();

    /**
     * Explicit {@link xyz.wagyourtail.jvmdg.version.StubKind} overrides from
     * {@code @Stub(kind=)}/{@code @Modify(kind=)} — sparse; absent entries
     * auto-classify.
     */
    public final Map<FullyQualifiedMemberNameAndDesc, xyz.wagyourtail.jvmdg.version.StubKind> memberKind = new HashMap<>();

    /**
     * Classify a callsite entry for a downgrade to {@code targetClassVersion}:
     * an explicit {@code kind=} override wins, {@code memberModify} entries
     * are {@link xyz.wagyourtail.jvmdg.version.StubKind#LANGUAGE}, and member
     * stubs are {@link xyz.wagyourtail.jvmdg.version.StubKind#BEHAVIOR} when
     * the JDK member exists on the target ({@code since} populated by the
     * build-time {@code ct.sym} pass) else
     * {@link xyz.wagyourtail.jvmdg.version.StubKind#MISSING_API} — including
     * the reflection-scan path where {@link #memberSince} stays empty.
     */
    public xyz.wagyourtail.jvmdg.version.StubKind kindOf(
            FullyQualifiedMemberNameAndDesc siteFqn, boolean modifyEntry, int targetClassVersion) {
        xyz.wagyourtail.jvmdg.version.StubKind k = memberKind.get(siteFqn);
        if (k != null) {
            return k;
        }
        if (modifyEntry) {
            return xyz.wagyourtail.jvmdg.version.StubKind.LANGUAGE;
        }
        Integer since = memberSince.get(siteFqn);
        return since != null && since > 0 && since <= targetClassVersion
            ? xyz.wagyourtail.jvmdg.version.StubKind.BEHAVIOR
            : xyz.wagyourtail.jvmdg.version.StubKind.MISSING_API;
    }

    /** Every stub/modify entry registered for one JDK type. */
    public static final class Owner {
        public final Map<MemberNameAndDesc, StubEntry> methodStub = new HashMap<>();
        /** field member → {getter, setter}; either side may be null */
        public final Map<MemberNameAndDesc, FieldStubEntry> fieldStub = new HashMap<>();
        /** original-ctor member → {@link CtorFactory} or {@link CtorRewrite}; resolved owner-exact (a {@code new Sub()} never inherits {@code Super}'s rewrite) */
        public final Map<MemberNameAndDesc, CtorEntry> ctor = new HashMap<>();
        /** null key = class-level {@code @Modify} ({@code Ref.member} unset) — catches every member insn on the owner */
        public final Map<MemberNameAndDesc, MethodRef> memberModify = new HashMap<>();
    }

    /** Registration path — creates the owner's entry on first write. */
    /** all registered owners by internal name — index serialization + parity tooling */
    public Map<String, Owner> owners() {
        return owners;
    }

    public Owner forOwner(Type owner) {
        String key = owner.getInternalName();
        Owner o = owners.get(key);
        if (o == null) {
            o = new Owner();
            owners.put(key, o);
        }
        return o;
    }

    /** Query path — null when nothing was ever registered for the owner. */
    public Owner owner(Type owner) {
        return owners.get(owner.getInternalName());
    }
}
