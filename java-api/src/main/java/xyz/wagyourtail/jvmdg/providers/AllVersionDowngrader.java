package xyz.wagyourtail.jvmdg.providers;

import xyz.wagyourtail.jvmdg.version.VersionProvider;

/**
 * Base type of the bundled {@code Java<N>Downgrader} providers. Carries no
 * behavior of its own — the {@link xyz.wagyourtail.jvmdg.version.ReflectionContributor}
 * application used to live here but moved to {@link VersionProvider#preInit()}
 * so generic and external providers get it too. Retained so generated
 * provider classes and external subclasses keep a stable base type.
 */
public abstract class AllVersionDowngrader extends VersionProvider {

    protected AllVersionDowngrader(int inputVersion, int outputVersion, int priority) {
        super(inputVersion, outputVersion, priority);
    }

}
