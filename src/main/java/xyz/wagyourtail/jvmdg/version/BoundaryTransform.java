package xyz.wagyourtail.jvmdg.version;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A named, discoverable unit of per-boundary transform work — what
 * {@link VersionProvider#otherTransforms} used to keep inline as private
 * methods. Lives under the provider's {@code j<N>} package so the build-time
 * scan can enumerate it into the generated provider config; the provider's
 * {@code boundaryTransforms} list orders the units the same way the former
 * method calls sequenced them.
 * <p>
 * {@link #order()} breaks ordering ties when the config is generated from a
 * classpath scan — lower runs earlier; equal orders fall back to class-name
 * sort so emission is deterministic. Values only need to be right within
 * one provider's list — each boundary's transforms run at their own
 * version step, so numbers never compare across {@code j<N>} scopes.
 * The sort at the end of {@code ensureInit} normalizes the list on both
 * load paths, so the emitted {@code registerBoundaryTransforms} add order
 * is cosmetic — {@code order()} + name is the sole authority.
 */
public interface BoundaryTransform {

    ClassNode apply(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra,
                    Function<String, ClassNode> getReadOnly, Set<String> warnings,
                    boolean enableRuntime,
                    IOFunction<Type, Set<MemberNameAndDesc>> memberResolver,
                    IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException;

    default int order() {
        return 0;
    }

}
