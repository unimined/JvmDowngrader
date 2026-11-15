package xyz.wagyourtail.jvmdg.version.gen;

import java.io.IOException;

/**
 * One generated-artifact family in the api jar build: a generator derives
 * classes/resources from the post-{@code ensureInit} provider registries
 * (and shared {@link GenContext} computations like {@link
 * GenContext#flattenedWraps}) and emits them through {@link GenContext#emit}.
 * Implementations are plain functions of the context inputs — the
 * deterministic-output invariant means no global counters, no
 * iteration-order-dependent naming: every emitted name is a pure function
 * of the registry data.
 * <p>
 * Bytecode is authoritative; generators also write a source-shaped
 * {@code .java} inspection dump next to each class so the result is
 * readable/diffable without decompiling.
 */
public interface ApiGenerator {

    /** short id for diagnostics */
    String name();

    /** emit this family's artifacts into {@code ctx}'s sinks */
    void generate(GenContext ctx) throws IOException;

}
