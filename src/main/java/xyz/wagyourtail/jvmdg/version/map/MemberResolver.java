package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.util.Set;

/**
 * Resolution oracle for member stubbing: given a call-site member (in the
 * post-remap namespace), answers which stub or {@code @Modify} handler — if
 * any — the emitter should apply.
 * <p>
 * {@link ClassMapping} implements this today as the per-owner node of the
 * provider's lazy resolution graph (direct stubs, then a members-exists
 * check, then the supertype walk with {@code excludeChild}).
 * {@link MemberStubTransform} consumes only this surface, so a flat
 * registry-backed resolver can replace the graph in Phase D without
 * touching emission.
 */
public interface MemberResolver {

    /**
     * The stub entry for {@code member}, honoring opcode gating
     * ({@code noSpecial}), {@code requiresRuntime} availability and the
     * native-member-exists short-circuit — or null. Coverage warnings for
     * unresolved members accumulate into {@code warnings}.
     */
    StubEntry getStubFor(MemberNameAndDesc member, boolean runtimeAvailable, int opcode, Set<String> warnings);

    /**
     * The ctor entry ({@link CtorFactory} or {@link CtorRewrite}) for an
     * {@code <init>} member — owner-exact, unlike {@link #getStubFor}: a
     * subclass's {@code new Sub(...)} must not resolve its parent's rewrite.
     * {@code requiresRuntime} entries resolve only when runtime is
     * available — or null when nothing is registered.
     */
    CtorEntry getCtorFor(MemberNameAndDesc member, boolean runtimeAvailable, Set<String> warnings);

    /**
     * The {@code @Modify} handler reference for {@code member}, including the
     * class-level wildcard ({@code ref.member} unset) fallback — or null.
     */
    MethodRef getModifyFor(MemberNameAndDesc member, int opcode, Set<String> warnings);

    /** Raw modify lookup for the indy bootstrap path — no wildcard fallback. */
    MethodRef rawModify(MemberNameAndDesc member);

    /** bsm member → {@code @IndyDesugar} handler lookup — indy-site only, no wildcard fallback */
    MethodRef rawIndyDesugar(MemberNameAndDesc member);

    /**
     * Rebuilds the member key with adapter (fake) desc types translated back
     * to their real JDK types — registered members are keyed in real types.
     */
    MemberNameAndDesc unstubMember(String name, Type desc);

    /** The provider whose registry and flags this resolver reads. */
    VersionProvider provider();
}
