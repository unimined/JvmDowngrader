package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
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

    /** Every stub/modify entry registered for one JDK type. */
    public static final class Owner {
        public final Map<MemberNameAndDesc, StubEntry> methodStub = new HashMap<>();
        /** field member → {getter, setter}; either side may be null */
        public final Map<MemberNameAndDesc, FieldStubEntry> fieldStub = new HashMap<>();
        /** original-ctor member → {@link CtorFactory} or {@link CtorRewrite}; resolved owner-exact (a {@code new Sub()} never inherits {@code Super}'s rewrite) */
        public final Map<MemberNameAndDesc, CtorEntry> ctor = new HashMap<>();
        /** null key = class-level {@code @Modify} ({@code Ref.member} unset) — catches every member insn on the owner */
        public final Map<MemberNameAndDesc, Method> memberModify = new HashMap<>();
    }

    /** Registration path — creates the owner's entry on first write. */
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
