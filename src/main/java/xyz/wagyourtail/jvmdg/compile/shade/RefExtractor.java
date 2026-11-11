package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

/**
 * Extracts a class's dependency record — the {@code ReferenceIndex}
 * extraction seam. The stock {@link MemberRefExtractor} covers every
 * reference shape ASM exposes; annotation-driven semantics plug in via
 * {@link MemberAnnotationExtractor} handlers (jvmdg's own
 * {@code @RequiresResource}/{@code @ReflectionReferences} included).
 * Wholly different extraction strategies (e.g. service-loader files,
 * reflective-call sites a consumer knows about) plug in through this SPI.
 */
public interface RefExtractor {

    /**
     * Extracts {@code node}'s references into a fresh record.
     *
     * @param filter decides which referenced members are retained —
     *               e.g. "owner is an api class"
     */
    ExtractedRefs extract(ClassNode node, Filter filter);

    /** Decides whether a reference is retained. */
    interface Filter {

        boolean shouldInclude(FullyQualifiedMemberNameAndDesc member);

    }

}
