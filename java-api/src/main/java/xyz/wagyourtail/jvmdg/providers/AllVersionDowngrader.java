package xyz.wagyourtail.jvmdg.providers;

import xyz.wagyourtail.jvmdg.version.ReflectionContributor;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

public abstract class AllVersionDowngrader extends VersionProvider {

    /**
     * ServiceLoader-discovered {@link ReflectionContributor}s — the
     * reflection-stub surface's single declaration point, applied at every
     * provider boundary (registries are per-boundary by design). Sorted by
     * class name for deterministic registration order.
     */
    private static final List<ReflectionContributor> CONTRIBUTORS = loadContributors();

    private static List<ReflectionContributor> loadContributors() {
        List<ReflectionContributor> out = new ArrayList<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = AllVersionDowngrader.class.getClassLoader();
        }
        for (ReflectionContributor c : ServiceLoader.load(ReflectionContributor.class, loader)) {
            out.add(c);
        }
        Collections.sort(out, new Comparator<ReflectionContributor>() {
            @Override
            public int compare(ReflectionContributor a, ReflectionContributor b) {
                return a.getClass().getName().compareTo(b.getClass().getName());
            }
        });
        return Collections.unmodifiableList(out);
    }

    protected AllVersionDowngrader(int inputVersion, int outputVersion, int priority) {
        super(inputVersion, outputVersion, priority);
    }

    @Override
    public void preInit() {
        for (ReflectionContributor contributor : CONTRIBUTORS) {
            contributor.contribute(this);
        }
    }

}
