package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

/**
 * Member-level extension point for {@link MemberRefExtractor}: receives each
 * whole class/field/method node and records whatever extra dependencies it
 * can prove — annotation-driven metadata, instruction-pattern scans
 * (reflective-call sites, service lookups), naming conventions, anything a
 * consumer knows about its own classes.
 * <p>
 * All three hooks run once per element per extraction; implementations
 * no-op the levels they don't use (Java 7 — no default methods). The stock
 * scanners ({@code @RequiresResource}, {@code @ReflectionReferences}) are
 * ordinary annotation walkers built on this seam; richer bytecode analysis
 * (e.g. commons-asm's reflection/resource scanning) plugs in the same way.
 */
public interface MemberScanner {

    /**
     * Class-level scan — runs once per class. {@link MemberSink#member()} is
     * {@code null} here, so contributed records behave like class-level refs.
     */
    void scanClass(ClassNode node, MemberSink sink);

    /** Field-level scan — runs once per field. */
    void scanField(FieldNode field, MemberNameAndDesc member, MemberSink sink);

    /** Method-level scan — runs once per method (instructions included). */
    void scanMethod(MethodNode method, MemberNameAndDesc member, MemberSink sink);

    /**
     * Where a scanner records what it found — the enclosing member plus the
     * record builders. The scan's reference filter is applied internally;
     * resources are recorded unconditionally (like the jar-level flow).
     */
    interface MemberSink {

        /** The enclosing member — {@code null} from {@link #scanClass}. */
        MemberNameAndDesc member();

        /** Requires an instance of {@code type} (class-level dependency). */
        void requiresInstance(Type type);

        /** Requires {@code ref} — a member this element needs kept. */
        void requiresMember(FullyQualifiedMemberNameAndDesc ref);

        /**
         * Requires {@code ref}, retaining {@code insn} as a referencing site —
         * for scanners proving a call from the instruction stream. Retained
         * insns feed {@code ReferenceIndex#insnsFor}, e.g. inline rewriting.
         */
        void requiresMember(FullyQualifiedMemberNameAndDesc ref, AbstractInsnNode insn);

        /** Attaches resource paths, collected when {@link #member()} is required. */
        void requiresResource(String... resources);

    }

}
