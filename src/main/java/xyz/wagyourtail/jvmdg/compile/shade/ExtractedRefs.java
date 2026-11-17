package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One class's extracted dependency record — what a {@link RefExtractor}
 * produces and what {@link ReferenceIndex} stores per (type, version) slot.
 * <p>
 * References split into two shapes: a <i>class ref</i> (a member-less
 * {@link FullyQualifiedMemberNameAndDesc} — "an instance of this type is
 * needed") and a <i>member ref</i> (fully-qualified member — "this specific
 * method/field is needed"). Class refs live in {@link #requiredInstances}
 * (class-level) or as member ref values; member refs populate
 * {@link #requiredForMembers} keyed by the needing member of this class.
 */
public final class ExtractedRefs {

    /** Class-level requirements — supertypes, annotations, signature, nest/inner/record metadata. */
    public final List<Type> requiredInstances = new ArrayList<Type>();

    /** Per-member requirements — which refs each declared member needs. */
    public final Map<MemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> requiredForMembers =
        new HashMap<MemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>();

    /** Non-static methods — the members a class ref expands to on closure. */
    public final List<MemberNameAndDesc> instanceMembers = new ArrayList<MemberNameAndDesc>();

    /** Retained insns per referenced member — only when the index keeps insns. */
    public final Map<FullyQualifiedMemberNameAndDesc, Set<AbstractInsnNode>> memberInsns =
        new HashMap<FullyQualifiedMemberNameAndDesc, Set<AbstractInsnNode>>();

    /** {@code @RequiresResource} payloads per member. */
    public final Map<MemberNameAndDesc, String[]> resourceList = new HashMap<MemberNameAndDesc, String[]>();

}
