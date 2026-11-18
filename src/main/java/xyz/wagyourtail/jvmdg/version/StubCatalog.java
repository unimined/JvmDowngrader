package xyz.wagyourtail.jvmdg.version;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.IOSupplier;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.StubRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The provider's stub-data half — every table the reflective scan
 * ({@link StubScanner}) or the {@code stubs.idx} load populates, and the
 * resolver emission code queries. Exactly one {@link VersionProvider}
 * owns a catalog; both load paths build the same object, so scan≡idx
 * parity is a property of this class's construction rather than a
 * convention scattered across loaders.
 * <p>
 * {@link VersionProvider} keeps {@code public final} aliases to each map
 * for SPI compatibility — they point at these same objects, never
 * copies.
 */
public class StubCatalog {

    /**
     * Owning provider — the catalog cannot exist standalone: registry
     * resolution reaches back for {@code coverage}, {@code logger}, and
     * the provider itself (as the {@link ClassMapping} backref). Public
     * so the {@code version.idx} loaders can read provider context
     * (flags, boundary versions, classloader) through the catalog.
     */
    public final VersionProvider provider;

    public final Map<Type, ClassStubEntry> classStubs = new LinkedHashMap<>();
    /**
     * {@code @Adapter(impls=…)} declared wrapper pairs per classStub source
     * (internal name → ordered {@code [memberInternalName, implInternalName]}
     * entries, member type ctor-derived). Build-time data for the
     * {@code META-INF/jvmdg/wraps} writer — emitted checkcasts route through
     * {@code RetroTypes.wrap} unconditionally, so nothing idx-serializes this
     * and the runtime reads only the resource.
     */
    public final Map<String, List<String[]>> adapterImpls = new LinkedHashMap<>();
    /**
     * Gained-supertypes view from {@code DerivedGained}: interface internal
     * name → member internal names whose binding crosses this provider's
     * boundary ({@code output < since ≤ input}). Site routing only — the
     * full {@code since} map stays in the resource; emitted dispatchers
     * evaluate membership at the callsite class's own version ceiling.
     */
    public final Map<String, Set<String>> gained = new LinkedHashMap<>();
    /**
     * Member-call view from {@code DerivedGained}: interface internal name
     * → every {@code "name desc"} key the interface's generated dispatcher
     * declares — the {@code gained-calls} member universe, un-windowed
     * (the dispatcher is one class serving all ceilings). An
     * {@code invokeinterface} on a gained interface only routes when the
     * site's member is in this set — inherited members like
     * {@code SequencedCollection.size} stay on the member-stub/native
     * path instead of calling a method the dispatcher never generated.
     */
    public final Map<String, Set<String>> gainedMembers = new LinkedHashMap<>();
    /**
     * {@code I → "name desc" → stub-site impls} every gained-calls row can
     * resolve to at any ceiling ({@code owner.name(desc)} in registered
     * vocabulary). Gained indy sites resolve these reflectively in
     * {@code GainedRuntime}/{@code RetroTypes}, leaving no bytecode edge —
     * emission declares them via {@code @ReflectionReferences} so the
     * shade closure keeps the impl class.
     */
    public final Map<String, Map<String, Set<String>>> gainedStubSites = new LinkedHashMap<>();
    public final Map<Type, ClassMapping> stubMappings = new LinkedHashMap<>();
    /** registered @Stub/@Modify entries for this provider's input version, keyed by stubbed owner */
    public final StubRegistry stubRegistry = new StubRegistry();

    /**
     * Class names {@code init()} registered, in call order — recorded only
     * while {@link VersionProvider} runs the reflective scan ({@code
     * recordingInit}); the generated {@code META-INF/jvmdg/stubs/j<N>}
     * resource is emitted from this list so replay registers the identical
     * set in the identical order. Gen-time data — empty on the replay path.
     */
    public final List<String> initRegistrations = new ArrayList<>();

    public StubCatalog(VersionProvider provider) {
        this.provider = provider;
    }

    public ClassMapping getStubMapper(Type type, final boolean isInterface, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        return getStubMapper(type, new IOSupplier<Boolean>() {
            @Override
            public Boolean get() {
                return isInterface;
            }
        }, memberResolver, superTypeResolver);
    }

    public synchronized ClassMapping getStubMapper(final Type type, final IOSupplier<Boolean> isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, final IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        if (stubMappings.containsKey(type)) {
            return stubMappings.get(type);
        }
        if (type.getSort() == Type.ARRAY) {
            return new ClassMapping(provider.coverage, new Lazy<List<ClassMapping>>() {
                @Override
                public List<ClassMapping> init() {
                    try {
                        return Collections.singletonList(getStubMapper(Type.getObjectType("java/lang/Object"), false, memberResolver, superTypeResolver));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, type, isInterface.get(), memberResolver, provider);
        }
        if (type.getInternalName().equals("java/lang/Object")) {
            ClassMapping mapping = new ClassMapping(provider.coverage, new Lazy<List<ClassMapping>>() {
                @Override
                public List<ClassMapping> init() {
                    return Collections.emptyList();
                }
            }, type, false, memberResolver, provider);
            stubMappings.put(type, mapping);
            return mapping;
        }
        ClassMapping mapping = new ClassMapping(provider.coverage, new Lazy<List<ClassMapping>>() {
            @Override
            public List<ClassMapping> init() {
                try {
                    List<Pair<Type, Boolean>> types = superTypeResolver.apply(type);
                    if (types == null) {
                        provider.logger.error("Could not find class " + type.getInternalName());
                        types = Collections.emptyList();
                    }
                    List<ClassMapping> superTypes = new ArrayList<>();
                    for (Pair<Type, Boolean> superType : types) {
                        superTypes.add(getStubMapper(superType.getFirst(), superType.getSecond(), memberResolver, superTypeResolver));
                    }
                    return superTypes;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, type, isInterface.get(), memberResolver, provider);
        stubMappings.put(type, mapping);
        return mapping;
    }

}
