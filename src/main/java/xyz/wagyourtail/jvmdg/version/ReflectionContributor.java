package xyz.wagyourtail.jvmdg.version;

/**
 * ServiceLoader SPI ({@code META-INF/services/...ReflectionContributor})
 * owning the reflection-stub surface — the set of {@code stub()}/
 * {@code modify()} registrations that intercept {@code java.lang.Class},
 * {@code MethodHandles$Lookup} and {@code java.lang.reflect.*} members at
 * every provider boundary. Previously this list lived inline in
 * {@code AllVersionDowngrader.preInit}; pulling it behind a contributor
 * means one declaration site (the api jar's services file) and lets
 * third-party reflection layers register without touching providers.
 * <p>
 * Invoked once per provider's init — registrations are per-boundary by
 * design (each boundary's {@code StubMethodsTransform} consults its own
 * registry), so a contributor registers unconditionally; the version
 * window for the stubbed behavior lives in {@code reflection.idx}'s
 * since/until columns, not in which provider asked.
 */
public interface ReflectionContributor {

    /** Register this contributor's stubs/mods into the provider's registry. */
    void contribute(VersionProvider provider);
}
