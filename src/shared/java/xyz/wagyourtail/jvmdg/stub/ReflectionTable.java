package xyz.wagyourtail.jvmdg.stub;

import xyz.wagyourtail.jvmdg.stub.all.GainedInterfaces;
import xyz.wagyourtail.jvmdg.stub.all.JvmDowngraded;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.RequiresResource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ASM-free, table-driven reflection layer — the runtime half of the
 * package-relative {@code reflection.idx} resource emitted by
 * {@code StubIndexWriter}. Replaces what the api stubs used to do by pulling
 * {@code ClassDowngrader}/{@code VersionProvider} (and transitively ASM) into
 * the running application: the downgraded app's {@code Class.forName} sites
 * resolve through this table instead of the provider graph.
 *
 * <p>{@code T} rows carry {@code source<TAB>target<TAB>since} — the stubbed
 * JDK type, the api class that stands in for it, and the class-file version
 * the source first appeared at. {@code M} rows carry
 * {@code owner<TAB>name<TAB>desc<TAB>i|s<TAB>impl-owner<TAB>impl-name<TAB>impl-desc<TAB>since<TAB>mods}
 * — the stubbed member, its receiver shape, the {@code public static} impl
 * handler, the version it appeared, and the JDK declaration flags the
 * {@code Method} view presents. Both row kinds apply for a callsite authored
 * at {@code origVersion} when {@code since ∈ (runningJvm, origVersion]}: the
 * type/member is missing on the running JDK but existed on the authoring
 * one. Both bounds matter — a type real on the running JVM must come back
 * real (identity with the live JDK surface), and a type introduced after
 * the authored version must still {@link ClassNotFoundException} the way the
 * authored JVM would have.</p>
 */
@RequiresResource(ReflectionTable.RESOURCE)
public final class ReflectionTable {

    /** classpath location of the reflection table — also the {@code @RequiresResource} tag shading keys on */
    /**
     * Api-jar-relative path of the serialized table. It lives next to this
     * class (in {@code xyz/wagyourtail/jvmdg/stub/}), not under {@code
     * META-INF}: the reader resolves it with {@code getResource}
     * relative to its own package, so a prefixed copy reads only the file in
     * its own relocated package — two shaded jars with different prefixes
     * never merge each other's rows the way a fixed {@code META-INF} path
     * would inside one loader.
     */
    public static final String RESOURCE = "xyz/wagyourtail/jvmdg/stub/reflection.idx";

    private ReflectionTable() {
    }

    /** stubbed JDK internal name → the api type standing in for it + its first-version */
    private static final class TypeRow {
        final String target;
        final int since;

        TypeRow(String target, int since) {
            this.target = target;
            this.since = since;
        }
    }

    /**
     * a stubbed member: {@code desc} is the callsite (source-namespace)
     * signature, {@code instance} marks the receiver-in-arg0 shape, the
     * {@code impl*} triple names the {@code public static} handler, and
     * {@code mods} carries the JDK declaration flags the {@code Method}
     * view reports
     */
    private static final class MemberRow {
        final String name;
        final String desc;
        final boolean instance;
        final String implOwner;
        final String implName;
        final String implDesc;
        final int since;
        final int mods;

        MemberRow(String name, String desc, boolean instance,
                  String implOwner, String implName, String implDesc, int since, int mods) {
            this.name = name;
            this.desc = desc;
            this.instance = instance;
            this.implOwner = implOwner;
            this.implName = implName;
            this.implDesc = implDesc;
            this.since = since;
            this.mods = mods;
        }
    }

    /**
     * One impl reference inside a {@link CtorRow} — {@code owner.name(desc)}
     * plus the original-arg indices feeding its params ({@code from}) and a
     * trailing-version flag ({@code version}) for the {@code @InitPost}/
     * factory shapes that take the authored class-file version.
     */
    static final class Spec {
        final String owner;
        final String name;
        final String desc;
        final int[] from;
        final boolean version;

        Spec(String owner, String name, String desc, int[] from, boolean version) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.from = from;
            this.version = version;
        }
    }

    /**
     * A stubbed {@code <init>} — the C-row view of a {@code @InitRewrite} or
     * {@code @InitFactory}: {@code desc} is the original (callsite) ctor
     * signature. For {@link #factory} rows {@link #impl} is the static
     * factory spec; for rewrites {@link #toDesc} is the retargeted ctor's
     * signature, {@link #keep} maps each target-arg slot to its original-arg
     * index ({@code -1} = produced by {@link #convs}[i]), and {@link #pre}/
     * {@link #post} are the optional void hooks around the call.
     */
    static final class CtorRow {
        final String desc;
        final boolean factory;
        final Spec impl;
        final String toDesc;
        final int[] keep;
        final Spec[] convs;
        final Spec pre;
        final Spec post;
        final int since;
        final int mods;

        CtorRow(String desc, boolean factory, Spec impl, String toDesc, int[] keep,
                Spec[] convs, Spec pre, Spec post, int since, int mods) {
            this.desc = desc;
            this.factory = factory;
            this.impl = impl;
            this.toDesc = toDesc;
            this.keep = keep;
            this.convs = convs;
            this.pre = pre;
            this.post = post;
            this.since = since;
            this.mods = mods;
        }
    }

    /** stubbed JDK internal name → {@link TypeRow} */
    private static final Map<String, TypeRow> TYPES = new ConcurrentHashMap<>();

    /** stubbed JDK owner internal name → its stubbed members */
    private static final Map<String, List<MemberRow>> MEMBERS = new ConcurrentHashMap<>();

    /** stand-in internal name → the source names remapping onto it (reverse of {@link #TYPES}) */
    private static final Map<String, List<String>> BY_TARGET = new ConcurrentHashMap<>();

    /** stubbed JDK owner internal name → its stubbed {@code <init>} rows */
    private static final Map<String, List<CtorRow>> CTORS = new ConcurrentHashMap<>();

    /** lazily-loaded resource table, folded into {@link #TYPES} on first query */
    private static final Lazy<Void> RESOURCES = new Lazy<Void>() {
        @Override
        protected Void init() {
            loadResources();
            return null;
        }
    };

    /**
     * {@code Class.forName(String)} — resolves through the callsite's own
     * loader (carried in by the emitted {@code ldc} of the containing class),
     * so a runtime-downgrading loader transforms the target like any other
     * api reference.
     */
    public static Class<?> forName(String name, Class<?> caller, int origVersion) throws ClassNotFoundException {
        return Class.forName(remap(name, origVersion), true, caller.getClassLoader());
    }

    /** {@code Class.forName(String, boolean, ClassLoader)} — same remap, caller-supplied loader */
    public static Class<?> forName(String name, boolean initialize, ClassLoader loader, int origVersion) throws ClassNotFoundException {
        return Class.forName(remap(name, origVersion), initialize, loader);
    }

    /**
     * The type-name view of the table: {@code name} in {@code Class.forName}
     * form ({@code pkg.Name} or descriptor-form arrays like
     * {@code [Lpkg.Name;}) → the name of the class that should actually be
     * resolved, which is the api stand-in when {@code name} is a stubbed JDK
     * type inside the version window and {@code name} itself otherwise.
     */
    public static String remap(String name, int origVersion) {
        RESOURCES.get();
        if (name.isEmpty()) {
            return name;
        }
        if (name.charAt(0) == '[') {
            // descriptor-form array names — remap the element type if it is an
            // object: [Lpkg.Missing; → [Lpkg.Adapter;. Primitive/unknown
            // element forms have no stubbed source and pass through.
            int dims = 0;
            while (dims < name.length() && name.charAt(dims) == '[') {
                dims++;
            }
            if (dims < name.length() - 1 && name.charAt(dims) == 'L'
                && name.charAt(name.length() - 1) == ';') {
                String elem = name.substring(dims + 1, name.length() - 1).replace('.', '/');
                TypeRow row = TYPES.get(elem);
                if (applies(row, origVersion)) {
                    return name.substring(0, dims + 1) + row.target.replace('/', '.') + ";";
                }
            }
            return name;
        }
        TypeRow row = TYPES.get(name.replace('.', '/'));
        return applies(row, origVersion) ? row.target.replace('/', '.') : name;
    }

    /** row active when the source is missing on the running JVM but existed at the callsite's authored version */
    private static boolean applies(TypeRow row, int origVersion) {
        return row != null && row.since > Utils.getCurrentClassVersion() && row.since <= origVersion;
    }

    /**
     * One parsed row of {@code reflection.idx}: {@code T} registers a type
     * stand-in, {@code M} a member impl. Malformed rows drop silently — a
     * partial table must never break the callsites it can't answer.
     */
    static void applyRow(String[] row) {
        if ("T".equals(row[0]) && row.length == 4) {
            int since;
            try {
                since = Integer.parseInt(row[3].trim());
            } catch (NumberFormatException e) {
                return;
            }
            // first resource wins a source — the writer emits each source
            // once, but shaded jars can stack extra copies
            register(row[1].trim(), row[2].trim(), since);
        } else if ("M".equals(row[0]) && row.length == 10) {
            try {
                registerMember(row[1].trim(), row[2].trim(), row[3].trim(),
                    "i".equals(row[4].trim()), row[5].trim(), row[6].trim(), row[7].trim(),
                    Integer.parseInt(row[8].trim()), Integer.parseInt(row[9].trim()));
            } catch (NumberFormatException e) {
                // malformed window/mods — drop the row
            }
        } else if ("C".equals(row[0]) && row.length == 11) {
            try {
                int since = Integer.parseInt(row[9].trim());
                int mods = Integer.parseInt(row[10].trim());
                String owner = row[1].trim();
                String desc = row[2].trim();
                if ("F".equals(row[3].trim())) {
                    Spec impl = parseSpec(row[4]);
                    if (impl != null) {
                        registerCtor(owner, desc, true,
                            new Spec(impl.owner, impl.name, impl.desc, impl.from,
                                "v".equals(row[5].trim())),
                            null, null, null, null, null, since, mods);
                    }
                } else if ("R".equals(row[3].trim())) {
                    String toDesc = row[4].trim();
                    int[] keep = parseInts(row[5]);
                    Spec[] convs = null;
                    if (keep != null && !"-".equals(row[6].trim())) {
                        convs = new Spec[keep.length];
                        int ci = 0;
                        for (String s : row[6].trim().split("\\|")) {
                            Spec spec = parseSpec(s);
                            if (spec == null) {
                                break;
                            }
                            while (ci < keep.length && keep[ci] >= 0) {
                                ci++;
                            }
                            if (ci >= keep.length) {
                                break;
                            }
                            convs[ci++] = spec;
                        }
                    }
                    Spec pre = parseSpec(row[7]);
                    Spec post = parseSpec(row[8]);
                    if (keep != null && (convs == null || allPresent(convs))) {
                        registerCtor(owner, desc, false, null, toDesc, keep, convs, pre, post, since, mods);
                    }
                }
            } catch (NumberFormatException e) {
                // malformed window/mods — drop the row
            }
        }
    }

    private static boolean allPresent(Spec[] convs) {
        for (Spec s : convs) {
            if (s == null) {
                return false;
            }
        }
        return true;
    }

    private static int[] parseInts(String s) {
        s = s.trim();
        if (s.isEmpty() || "-".equals(s)) {
            return new int[0];
        }
        String[] parts = s.split(",");
        int[] out = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                out[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return out;
    }

    /**
     * {@code owner.name(desc):from,from[:v]} → a {@link Spec}. The
     * {@code from} list maps the impl's params to original-ctor arg
     * indices; a trailing {@code v} marks the {@code downgradeVersion} int
     * (post hooks and factories). Malformed input returns {@code null} —
     * the row drops rather than answering a broken spec.
     */
    static Spec parseSpec(String s) {
        s = s.trim();
        if (s.isEmpty() || "-".equals(s)) {
            return null;
        }
        try {
            int colon = s.indexOf(':');
            String fqm = colon < 0 ? s : s.substring(0, colon);
            String rest = colon < 0 ? "" : s.substring(colon + 1);
            boolean version = false;
            int[] from = new int[0];
            if (!rest.isEmpty()) {
                int v = rest.lastIndexOf(':');
                if (v >= 0 && "v".equals(rest.substring(v + 1))) {
                    version = true;
                    rest = rest.substring(0, v);
                }
                from = parseInts(rest);
            }
            int paren = fqm.indexOf('(');
            int close = fqm.indexOf(')', paren);
            if (paren < 0 || close < 0) {
                return null;
            }
            int dot = fqm.lastIndexOf('.', paren);
            if (dot < 0) {
                return null;
            }
            return new Spec(fqm.substring(0, dot), fqm.substring(dot + 1, paren),
                fqm.substring(paren), from, version);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Programmatic row registration — the same entry a resource {@code T}
     * row carries ({@code source}/{@code target} are internal names). First
     * registration of a source wins, matching the resource merge.
     */
    public static void register(String source, String target, int since) {
        if (TYPES.putIfAbsent(source, new TypeRow(target, since)) == null) {
            listFor(BY_TARGET, target).add(source);
        }
    }

    /**
     * Programmatic {@code M}-row registration — the same entry a resource
     * row carries. {@code desc}/{@code implDesc} are method descriptors in
     * internal-name form.
     */
    public static void registerMember(String owner, String name, String desc, boolean instance,
                                      String implOwner, String implName, String implDesc,
                                      int since, int mods) {
        listFor(MEMBERS, owner).add(new MemberRow(name, desc, instance,
            implOwner, implName, implDesc, since, mods));
    }

    /**
     * Programmatic {@code C}-row registration — the same entry a resource
     * row carries: a stubbed {@code <init>} on {@code owner} with original
     * signature {@code desc}. {@code factory} selects the {@link Spec}
     * {@code impl} path ({@code @InitFactory}); otherwise {@code toDesc}/
     * {@code keep}/{@code convs}/{@code pre}/{@code post} describe the
     * {@code @InitRewrite} retarget. Ctors aren't inherited — the lookup
     * keys on the owner exactly, no supertype walk.
     */
    public static void registerCtor(String owner, String desc, boolean factory, Spec impl,
                                    String toDesc, int[] keep, Spec[] convs, Spec pre, Spec post,
                                    int since, int mods) {
        listFor(CTORS, owner).add(new CtorRow(desc, factory, impl, toDesc, keep,
            convs, pre, post, since, mods));
    }

    private static <V> List<V> listFor(Map<String, List<V>> map, String key) {
        List<V> l = map.get(key);
        if (l == null) {
            map.putIfAbsent(key, new CopyOnWriteArrayList<V>());
            l = map.get(key);
        }
        return l;
    }

    /**
     * {@code Class.getMethods()} — real members only. Stub impls are static
     * handlers whose signatures don't match the member shape (receiver is an
     * explicit first arg) and whose declaring class is the api stub — they
     * must not surface in enumeration. Stubbed-member lookups go through the
     * {@code getMethod}/{@code getDeclaredMethod} path instead.
     */
    public static Method[] getMethods(Class<?> clazz, int origVersion) {
        return clazz.getMethods();
    }

    /** {@code Class.getDeclaredMethods()} — real members only, like {@link #getMethods} */
    public static Method[] getDeclaredMethods(Class<?> clazz, int origVersion) {
        return clazz.getDeclaredMethods();
    }

    /**
     * {@code Class.getMethod(String, Class...)} — native-first: a member
     * the running JVM resolves (possibly on a subtype — e.g.
     * {@code String.isEmpty} vs the j15 {@code CharSequence.isEmpty} row)
     * answers with the real {@code Method}; only an honest {@link
     * NoSuchMethodException} reaches the table, whose rows by construction
     * cover members the owner lacks natively. {@code getMethod} searches
     * supertypes, so the table lookup does too.
     */
    public static Method getMethod(Class<?> clazz, String name, Class<?>[] parameterTypes, int origVersion) throws NoSuchMethodException {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            Method m = memberLookup(clazz, name, parameterTypes, origVersion, false);
            if (m == null) {
                throw e;
            }
            return m;
        }
    }

    /** {@code Class.getDeclaredMethod(String, Class...)} — same ordering, declared on the class itself */
    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>[] parameterTypes, int origVersion) throws NoSuchMethodException {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            Method m = memberLookup(clazz, name, parameterTypes, origVersion, true);
            if (m == null) {
                throw e;
            }
            return m;
        }
    }

    /**
     * {@code Class.getInterfaces()} — the runtime list adjusted to the
     * authored view: transform artifacts the source never declared are
     * removed (gained interfaces injected to restore supertype membership —
     * recorded by {@link GainedInterfaces} — and legacy {@code $Ov$} dispatch
     * markers, both by name pattern since they survive shade prefixing where
     * an annotation's string would not), {@code $Marker} entries are
     * replaced by the stand-in stub they nest under (the source declared
     * that interface — the stub is what it remaps to), and interfaces a
     * class <em>gained</em> after the running JVM but no later than
     * {@code origVersion} append as stand-ins — JDK classes can't be
     * injected, so the reflective view would otherwise miss what the
     * source-version hierarchy declared. Interfaces the JDK serves
     * natively but the corpus can only name through a stand-in (the
     * type's {@code since} lies in {@code (to, origVersion]} — the
     * callsite's {@link JvmDowngraded} window)
     * swap for that stand-in — the callsite's {@code X.class} literals
     * resolve to it, so an unsubstituted real interface would fail
     * identity checks. {@code caller} is the callsite's
     * class ({@code callerClass}) — stand-ins resolve against its loader so
     * the answer is identity-equal to the classes the callsite can name.
     */
    public static Class<?>[] getInterfaces(Class<?> clazz, Class<?> caller, int origVersion) {
        RESOURCES.get();
        int corpusTarget = corpusTarget(caller, origVersion);
        Class<?>[] ifs = clazz.getInterfaces();
        String[] gained = injectedInterfaceNames(clazz);
        Map<String, Integer> gainedSince = clazz.isArray() || clazz.isPrimitive() ? null
            : RetroTypes.gainedInterfacesFor(clazz.getName().replace('.', '/'));
        List<Class<?>> out = new ArrayList<>(ifs.length + 2);
        for (Class<?> i : ifs) {
            String name = i.getName();
            if (name.endsWith("$Marker")) {
                Class<?> standIn = standInFor(
                    name.substring(0, name.length() - "$Marker".length()), clazz, caller);
                if (standIn != null && !out.contains(standIn)) {
                    out.add(standIn);
                }
            } else if (!isInjectedInterface(name, gained)) {
                Class<?> v = interfaceView(i, clazz, caller, origVersion, corpusTarget, gainedSince);
                if (v != null) {
                    out.add(v);
                }
            }
        }
        out.addAll(gainedStandIns(clazz, caller, origVersion, corpusTarget, out));
        return out.toArray(new Class<?>[out.size()]);
    }

    /**
     * {@code Class.getGenericInterfaces()} — same adjustments on the
     * generic view. Injected entries never reached the {@code Signature}
     * attribute, and stand-ins append as raw {@link Class} types (the gained
     * interfaces carry no declarable generic surface at the target).
     */
    public static java.lang.reflect.Type[] getGenericInterfaces(Class<?> clazz, Class<?> caller, int origVersion) {
        RESOURCES.get();
        int corpusTarget = corpusTarget(caller, origVersion);
        java.lang.reflect.Type[] ifs = clazz.getGenericInterfaces();
        String[] gained = injectedInterfaceNames(clazz);
        Map<String, Integer> gainedSince = clazz.isArray() || clazz.isPrimitive() ? null
            : RetroTypes.gainedInterfacesFor(clazz.getName().replace('.', '/'));
        List<java.lang.reflect.Type> out = new ArrayList<>(ifs.length + 2);
        for (java.lang.reflect.Type t : ifs) {
            Class<?> raw = t instanceof Class ? (Class<?>) t
                : t instanceof java.lang.reflect.ParameterizedType
                    ? (Class<?>) ((java.lang.reflect.ParameterizedType) t).getRawType()
                    : null;
            if (raw != null && isInjectedInterface(raw.getName(), gained)) {
                if (raw.getName().endsWith("$Marker")) {
                    Class<?> standIn = standInFor(
                        raw.getName().substring(0, raw.getName().length() - "$Marker".length()),
                        clazz, caller);
                    if (standIn != null && !out.contains(standIn)) {
                        out.add(standIn);
                    }
                }
            } else if (raw != null) {
                Class<?> v = interfaceView(raw, clazz, caller, origVersion, corpusTarget, gainedSince);
                if (v != null) {
                    out.add(v == raw ? t : genericView(t, v));
                }
            } else {
                out.add(t);
            }
        }
        out.addAll(gainedStandIns(clazz, caller, origVersion, corpusTarget, out));
        return out.toArray(new java.lang.reflect.Type[out.size()]);
    }

    /**
     * The classfile major the callsite's jar was downgraded to — the
     * {@code @JvmDowngraded(to)} stamp the transform puts on every class it
     * rewrites; the caller's {@code ldc} literals were remapped against
     * exactly that version, so it is the lower bound of the stand-in
     * vocabulary window. Untransformed callers (no stamp — native
     * classpath, bootstrap machinery) answer {@code origVersion}: the
     * window collapses and the runner's real types stay in view.
     * <p>
     * Read by name + reflective {@code to()} like
     * {@link #injectedInterfaceNames}: the caller and this table can live in
     * different loaders (agent path), where the annotation's {@code Class}
     * identity differs and a typed {@code getAnnotation} silently misses.
     */
    private static int corpusTarget(Class<?> caller, int origVersion) {
        if (caller == null) {
            return origVersion;
        }
        String annName = JvmDowngraded.class.getName();
        try {
            for (java.lang.annotation.Annotation a : caller.getDeclaredAnnotations()) {
                if (a.annotationType().getName().equals(annName)) {
                    Object v = a.annotationType().getMethod("to").invoke(a);
                    if (v instanceof Integer) {
                        return (Integer) v;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return origVersion;
    }

    /**
     * The authored view of one natively-declared interface: {@code null}
     * when {@code clazz} gained it after {@code origVersion} (it postdates
     * the authored view); the api stand-in when the corpus names the type
     * through it — the row is in {@code (corpusTarget, origVersion]}, matching
     * the window the corpus's own literals were remapped under; the
     * interface itself otherwise.
     */
    private static Class<?> interfaceView(Class<?> i, Class<?> clazz, Class<?> caller,
                                          int origVersion, int corpusTarget,
                                          Map<String, Integer> gainedSince) {
        String name = i.getName().replace('.', '/');
        Integer gained = gainedSince == null ? null : gainedSince.get(name);
        if (gained != null && gained > origVersion) {
            return null;
        }
        TypeRow row = TYPES.get(name);
        if (row != null && row.since > corpusTarget && row.since <= origVersion) {
            Class<?> standIn = standInFor(row.target.replace('/', '.'), clazz, caller);
            if (standIn != null) {
                return standIn;
            }
        }
        return i;
    }

    /** {@code t} with its raw type swapped for the stand-in — parameterized entries keep their arguments */
    private static java.lang.reflect.Type genericView(java.lang.reflect.Type t, Class<?> standIn) {
        if (t instanceof Class) {
            return standIn;
        }
        if (!(t instanceof java.lang.reflect.ParameterizedType)) {
            return t;
        }
        final java.lang.reflect.ParameterizedType p = (java.lang.reflect.ParameterizedType) t;
        return new java.lang.reflect.ParameterizedType() {
            @Override
            public java.lang.reflect.Type[] getActualTypeArguments() {
                return p.getActualTypeArguments();
            }

            @Override
            public java.lang.reflect.Type getRawType() {
                return standIn;
            }

            @Override
            public java.lang.reflect.Type getOwnerType() {
                return p.getOwnerType();
            }

            @Override
            public String getTypeName() {
                StringBuilder sb = new StringBuilder(standIn.getTypeName());
                java.lang.reflect.Type[] args = p.getActualTypeArguments();
                if (args.length > 0) {
                    sb.append('<');
                    for (int i = 0; i < args.length; i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        sb.append(args[i].getTypeName());
                    }
                    sb.append('>');
                }
                return sb.toString();
            }
        };
    }


    /**
     * Interfaces {@code clazz} gained in {@code (…, origVersion]} that the
     * runtime doesn't declare — appended in the callsite's vocabulary:
     * the stand-in when the row is in {@code (corpusTarget, origVersion]}
     * (the caller's {@code ldc}s name it that way), the real name
     * otherwise. Runner-declared entries were already handled by
     * {@link #interfaceView} and dedup through {@code existing}.
     */
    private static List<Class<?>> gainedStandIns(Class<?> clazz, Class<?> caller, int origVersion,
                                                 int corpusTarget, List<?> existing) {
        if (clazz.isArray() || clazz.isPrimitive()) {
            return Collections.emptyList();
        }
        Map<String, Integer> gained = RetroTypes.gainedInterfacesFor(clazz.getName().replace('.', '/'));
        if (gained == null) {
            return Collections.emptyList();
        }
        List<Class<?>> out = new ArrayList<>(gained.size());
        for (Map.Entry<String, Integer> e : gained.entrySet()) {
            int since = e.getValue();
            if (since > origVersion) {
                continue;
            }
            TypeRow row = TYPES.get(e.getKey());
            String name = row != null && row.since > corpusTarget
                ? row.target.replace('/', '.')
                : e.getKey().replace('/', '.');
            Class<?> standIn = standInFor(name, clazz, caller);
            if (standIn != null && !existing.contains(standIn) && !out.contains(standIn)) {
                out.add(standIn);
            }
        }
        return out;
    }

    /**
     * Resolves {@code dottedName} in the loaders that can name it — the
     * callsite's first (the returned {@code Class} must be identity-equal
     * to what the callsite's {@code ldc}s resolve, which matters under
     * downgrading/shaded loaders where this table's own copy delegates
     * parent-first), then the inspected class's, then this table's for
     * bootstrap contexts — stand-ins are api classes the bootstrap loader
     * can't see. Unresolvable names (a stand-in pruned from this jar)
     * answer null rather than throwing.
     */
    private static Class<?> standInFor(String dottedName, Class<?> context, Class<?> caller) {
        if (caller != null && caller != context) {
            Class<?> c = standInFor(dottedName, caller.getClassLoader());
            if (c != null) {
                return c;
            }
        }
        ClassLoader loader = context.getClassLoader();
        return standInFor(dottedName,
            loader != null ? loader : ReflectionTable.class.getClassLoader());
    }

    private static Class<?> standInFor(String dottedName, ClassLoader loader) {
        try {
            return Class.forName(dottedName, false, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    private static boolean isInjectedInterface(String binaryName, String[] gained) {
        // $Ov$ markers predate the indy dispatch — keep filtering them for
        // classes produced by older toolchains (multi-release, stale jars)
        if (binaryName.endsWith("$Marker") || binaryName.contains("$Ov$")) {
            return true;
        }
        if (gained != null) {
            for (String name : gained) {
                if (name.equals(binaryName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reads {@link GainedInterfaces#value()} off {@code clazz} by annotation
     * <em>name</em> rather than by {@code getAnnotation(Class)}: the class
     * under inspection and this table can live in different loaders (agent /
     * DowngradingClassLoader splits api classes onto the parent), which makes
     * the annotation's {@code Class} identity differ and the typed lookup
     * silently return null. The string name survives both the split and the
     * shade prefix remap (the constant-pool ref here is prefixed the same way
     * the emitted descriptor is). {@code value()} is invoked reflectively for
     * the same reason — the annotation instance can't be cast across loaders.
     */
    private static String[] injectedInterfaceNames(Class<?> clazz) {
        String annName = GainedInterfaces.class.getName();
        try {
            for (java.lang.annotation.Annotation a : clazz.getDeclaredAnnotations()) {
                if (a.annotationType().getName().equals(annName)) {
                    Object v = a.annotationType().getMethod("value").invoke(a);
                    if (v instanceof String[]) {
                        return (String[]) v;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * {@code Class.getConstructor(Class...)} — native-first like {@link
     * #getMethod}: only an honest {@link NoSuchMethodException} reaches the
     * table, and only for a row whose source ctor was {@code public}. The
     * returned object is the <em>target</em> ctor's real {@code Constructor},
     * view-registered so {@code newInstance}/{@code getParameterTypes}/
     * {@code getParameterCount}/{@code getModifiers} present the source
     * member — and {@code unreflectConstructor}/{@code revealDirect} keep
     * the source view on handles.
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>[] parameterTypes, int origVersion) throws NoSuchMethodException {
        try {
            return clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            Constructor<?> c = ctorView(clazz, parameterTypes, origVersion, true);
            if (c == null) {
                throw e;
            }
            return c;
        }
    }

    /** {@code Class.getDeclaredConstructor(Class...)} — same ordering, any visibility */
    public static Constructor<?> getDeclaredConstructor(Class<?> clazz, Class<?>[] parameterTypes, int origVersion) throws NoSuchMethodException {
        try {
            return clazz.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            Constructor<?> c = ctorView(clazz, parameterTypes, origVersion, false);
            if (c == null) {
                throw e;
            }
            return c;
        }
    }

    /**
     * Resolves a matching {@link CtorRow} to a view-registered target
     * {@code Constructor} — the real {@code to}-described member (which must
     * resolve natively, or the row is unanswerable and the lookup stays a
     * miss). Factory rows have no target ctor to hand out, so they never
     * resolve here — {@code @InitFactory} construction flows through
     * {@code findConstructor}/{@code unreflectConstructor} handles instead.
     */
    private static Constructor<?> ctorView(Class<?> clazz, Class<?>[] parameterTypes, int origVersion, boolean publicOnly) {
        CtorRow row = ctorLookup(clazz, parameterTypes, origVersion);
        if (row == null || row.factory || (publicOnly && !java.lang.reflect.Modifier.isPublic(row.mods))) {
            return null;
        }
        try {
            Constructor<?> to = clazz.getDeclaredConstructor(paramClasses(row.toDesc));
            // synthesizeCtor registers the source view on this instance
            return synthesizeCtor(MethodHandles.lookup(), clazz, row, origVersion, to) == null
                ? null : to;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * {@code Lookup.findClass} — the type-remap path of {@code forName},
     * scoped to the lookup class's own loader; no initialization, matching
     * native {@code findClass}. A miss stays an honest
     * {@link ClassNotFoundException}.
     */
    public static Class<?> findClass(MethodHandles.Lookup lookup, String name, int origVersion) throws ClassNotFoundException {
        ClassLoader loader = lookup.lookupClass() != null
            ? lookup.lookupClass().getClassLoader() : ReflectionTable.class.getClassLoader();
        return Class.forName(remap(name, origVersion), false, loader);
    }

    /**
     * {@code Lookup.findVirtual} — a stubbed instance member resolves to
     * the {@code DispatchRuntime} PIC handle (a methodref-handle's semantics
     * are virtual dispatch, so a user override still wins), else the impl
     * handler; both as a receiver-first static.
     */
    public static MethodHandle findVirtual(MethodHandles.Lookup lookup, Class<?> refc, String name,
                                           MethodType type, int origVersion)
        throws NoSuchMethodException, IllegalAccessException {
        try {
            return lookup.findVirtual(refc, name, type);
        } catch (NoSuchMethodException e) {
            MethodHandle h = handleLookup(lookup, refc, name, type, origVersion, true, true,
                MethodHandleInfo.REF_invokeVirtual);
            if (h == null) {
                throw e;
            }
            return h;
        }
    }

    /**
     * {@code Lookup.findSpecial} — non-virtual dispatch goes to the impl
     * handler directly (a {@code super.m()} must not bounce through the
     * override marker back to {@code this}). A present-but-inaccessible
     * member propagates the honest {@link IllegalAccessException}; only an
     * absence reaches the table.
     */
    public static MethodHandle findSpecial(MethodHandles.Lookup lookup, Class<?> refc, String name,
                                           MethodType type, Class<?> caller, int origVersion)
        throws NoSuchMethodException, IllegalAccessException {
        try {
            return lookup.findSpecial(refc, name, type, caller);
        } catch (NoSuchMethodException e) {
            MethodHandle h = handleLookup(lookup, refc, name, type, origVersion, true, false,
                MethodHandleInfo.REF_invokeSpecial);
            if (h == null) {
                throw e;
            }
            return h;
        }
    }

    /** {@code Lookup.findStatic} — a stubbed static member resolves to its impl handler */
    public static MethodHandle findStatic(MethodHandles.Lookup lookup, Class<?> refc, String name,
                                          MethodType type, int origVersion)
        throws NoSuchMethodException, IllegalAccessException {
        try {
            return lookup.findStatic(refc, name, type);
        } catch (NoSuchMethodException e) {
            MethodHandle h = handleLookup(lookup, refc, name, type, origVersion, false, false,
                MethodHandleInfo.REF_invokeStatic);
            if (h == null) {
                throw e;
            }
            return h;
        }
    }

    /**
     * {@code Lookup.findConstructor} — a stubbed {@code <init>} resolves to
     * a synthesized {@code (origArgs) -> owner} handle that reproduces the
     * callsite rewrite: the target ctor's own handle under
     * {@code @InitArg} converter filters, the original-arg permutation, and
     * {@code @InitPre}/{@code @InitPost} folds; a factory row resolves to
     * its handler directly. The produced handle carries the source view
     * ({@code REF_newInvokeSpecial}, original signature) for {@code
     * revealDirect}.
     */
    public static MethodHandle findConstructor(MethodHandles.Lookup lookup, Class<?> refc,
                                               MethodType type, int origVersion)
        throws NoSuchMethodException, IllegalAccessException {
        try {
            return lookup.findConstructor(refc, type);
        } catch (NoSuchMethodException e) {
            if (refc == null || type.returnType() != void.class) {
                throw e;
            }
            CtorRow row = ctorLookup(refc, type.parameterArray(), origVersion);
            MethodHandle h = row == null ? null : synthesizeCtor(lookup, refc, row, origVersion);
            if (h == null) {
                throw e;
            }
            return h;
        }
    }

    /**
     * {@code Lookup.unreflectConstructor} — a view-registered {@code
     * Constructor} (handed out by {@code getConstructor}/{@code
     * getDeclaredConstructor}) unreflects to the stored synthesized handle:
     * the source signature's shape, not the target ctor's. Unregistered
     * ctors resolve natively.
     */
    public static MethodHandle unreflectConstructor(MethodHandles.Lookup lookup, Constructor<?> c)
        throws IllegalAccessException {
        StubCtorView.View v = StubCtorView.viewOf(c);
        if (v == null) {
            return lookup.unreflectConstructor(c);
        }
        StubHandleView.register(v.handle, c, c.getDeclaringClass(), "<init>", v.type,
            MethodHandleInfo.REF_newInvokeSpecial, v.mods);
        return v.handle;
    }

    /**
     * {@code Lookup.unreflect} — resolution is already correct natively
     * (the Method the table hands out IS the impl, receiver-first); the
     * stub exists to register the produced handle's source view for
     * {@code revealDirect}.
     */
    public static MethodHandle unreflect(MethodHandles.Lookup lookup, Method m) throws IllegalAccessException {
        MethodHandle h = lookup.unreflect(m);
        StubMethodView.View v = StubMethodView.viewOf(m);
        if (v != null) {
            StubHandleView.register(h, m, v.declaring, v.name, v.type,
                v.instance ? MethodHandleInfo.REF_invokeVirtual : MethodHandleInfo.REF_invokeStatic,
                v.mods);
        }
        return h;
    }

    /**
     * {@code Lookup.unreflectSpecial} — {@code unreflectSpecial} requires a
     * non-static {@code Method}, but the table hands out the {@code public
     * static} impl: a view-registered {@code Method} unreflects directly —
     * the static impl <em>is</em> the non-virtual call the source API would
     * have produced. Unregistered methods keep native behavior.
     */
    public static MethodHandle unreflectSpecial(MethodHandles.Lookup lookup, Method m, Class<?> caller)
        throws IllegalAccessException {
        boolean view = StubMethodView.hasView(m);
        MethodHandle h = view ? lookup.unreflect(m) : lookup.unreflectSpecial(m, caller);
        if (view) {
            StubMethodView.View v = StubMethodView.viewOf(m);
            StubHandleView.register(h, m, v.declaring, v.name, v.type,
                MethodHandleInfo.REF_invokeSpecial, v.mods);
        }
        return h;
    }

    /**
     * {@code Lookup.revealDirect} — a produced handle presents its source
     * view (original owner/name/signature/{@code REF_*} kind); anything
     * else resolves natively, including the honest {@link
     * IllegalArgumentException} for non-direct handles.
     */
    public static MethodHandleInfo revealDirect(MethodHandles.Lookup lookup, MethodHandle h) {
        MethodHandleInfo info = StubHandleView.info(h);
        return info != null ? info : lookup.revealDirect(h);
    }

    /**
     * Handle resolution over the member table — same owner walk as
     * {@link #memberLookup} ({@code find*} searches supertypes too),
     * kind-filtered: {@code findVirtual}/{@code findSpecial} see only
     * {@code i} rows, {@code findStatic} only {@code s}. The impl
     * {@code MethodType} is built in runtime types from the source-shaped
     * impl desc.
     */
    private static MethodHandle handleLookup(MethodHandles.Lookup lookup, Class<?> refc, String name,
                                             MethodType mt, int origVersion, boolean instance, boolean dispatch,
                                             int kind) {
        RESOURCES.get();
        if (MEMBERS.isEmpty() || refc == null) {
            return null;
        }
        List<String> owners = new ArrayList<>();
        owners.add(refc.getName().replace('.', '/'));
        collectSupertypes(refc, owners);
        for (int i = 0; i < owners.size(); i++) {
            List<MemberRow> rows = MEMBERS.get(owners.get(i));
            if (rows != null) {
                for (MemberRow row : rows) {
                    if (row.instance == instance && row.name.equals(name)
                        && applies(row.since, origVersion)
                        && paramsMatch(row.desc, mt.parameterArray())) {
                        MethodHandle h = resolveHandle(lookup, row, owners.get(i), refc, origVersion, dispatch, kind);
                        if (h != null) {
                            return h;
                        }
                    }
                }
            }
            List<String> sources = BY_TARGET.get(owners.get(i));
            if (sources != null) {
                owners.addAll(sources);
            }
        }
        return null;
    }

    private static ClassLoader lookupLoader(MethodHandles.Lookup lookup) {
        return lookup.lookupClass() != null
            ? lookup.lookupClass().getClassLoader() : ReflectionTable.class.getClassLoader();
    }

    private static MethodHandle resolveHandle(MethodHandles.Lookup lookup, MemberRow row, String owner,
                                              Class<?> clazz, int origVersion, boolean dispatch, int kind) {
        try {
            ClassLoader loader = lookupLoader(lookup);
            Method implMethod = resolveImpl(row, owner, clazz, origVersion, loader);
            if (implMethod == null) {
                return null;
            }
            MethodType implMt = MethodType.methodType(
                implMethod.getReturnType(), implMethod.getParameterTypes());
            Class<?> declaring = declaringClass(row, owner);
            if (kind == MethodHandleInfo.REF_invokeVirtual && declaring != null && declaring.isInterface()) {
                kind = MethodHandleInfo.REF_invokeInterface;
            }
            MethodType sourceMt = MethodType.methodType(
                classFor(returnFrag(row.desc)), paramClasses(row.desc));
            MethodHandle h = null;
            if (dispatch && declaring != null) {
                // findVirtual keeps virtual-dispatch semantics — the same
                // PIC the indy callsites use, as a receiver-first handle
                // with the impl's trailing transform extras bound to the
                // lookup's context (caller class / authored version)
                int memberParams = sourceMt.parameterCount();
                int extras = implMt.parameterCount() - memberParams - 1;
                if (extras >= 0) {
                    Object[] binds = new Object[extras];
                    boolean ok = true;
                    for (int k = 0; k < extras; k++) {
                        Class<?> t = implMt.parameterType(memberParams + 1 + k);
                        if (t == Class.class) {
                            binds[k] = lookup.lookupClass() != null
                                ? lookup.lookupClass() : ReflectionTable.class;
                        } else if (t == int.class) {
                            binds[k] = origVersion;
                        } else {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        try {
                            MethodHandle impl = lookup.findStatic(
                                implMethod.getDeclaringClass(), row.implName, implMt);
                            if (extras > 0) {
                                impl = MethodHandles.insertArguments(impl, memberParams + 1, binds);
                            }
                            h = DispatchRuntime.dispatchHandle(lookup, row.name, sourceMt,
                                MethodType.methodType(implMt.returnType(),
                                    implMt.parameterList().subList(0, memberParams + 1)),
                                impl);
                        } catch (Throwable ignore) {
                            // unresolvable impl — the plain handle below still answers
                        }
                    }
                }
            }
            if (h == null) {
                h = lookup.findStatic(implMethod.getDeclaringClass(), row.implName, implMt);
            }
            StubHandleView.register(h, implMethod, declaring, row.name, sourceMt, kind, row.mods);
            return h;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String returnFrag(String desc) {
        return desc.substring(desc.indexOf(')') + 1);
    }

    /**
     * Finds a stubbed member visible to {@code clazz} inside the
     * {@code (running, origVersion]} window. Candidate owners are the class
     * plus its supertypes ({@code getMethod} only) and the stubbed source
     * names that resolve onto each — a {@code forName} stand-in like
     * {@code J_L_Module} reaches rows registered under
     * {@code java/lang/Module} through {@link #BY_TARGET}.
     */
    private static Method memberLookup(Class<?> clazz, String name, Class<?>[] params, int origVersion, boolean declared) {
        RESOURCES.get();
        if (MEMBERS.isEmpty()) {
            return null;
        }
        List<String> owners = new ArrayList<>();
        owners.add(clazz.getName().replace('.', '/'));
        if (!declared) {
            collectSupertypes(clazz, owners);
        }
        for (int i = 0; i < owners.size(); i++) {
            List<MemberRow> rows = MEMBERS.get(owners.get(i));
            if (rows != null) {
                for (MemberRow row : rows) {
                    if (row.name.equals(name) && applies(row.since, origVersion)
                        && paramsMatch(row.desc, params)) {
                        Method m = resolveImpl(row, owners.get(i), clazz, origVersion);
                        if (m != null) {
                            return m;
                        }
                    }
                }
            }
            List<String> sources = BY_TARGET.get(owners.get(i));
            if (sources != null) {
                owners.addAll(sources);
            }
        }
        return null;
    }

    private static void collectSupertypes(Class<?> clazz, List<String> out) {
        Deque<Class<?>> queue = new ArrayDeque<>();
        queue.add(clazz);
        for (Class<?> c = queue.poll(); c != null; c = queue.poll()) {
            if (c.getSuperclass() != null) queue.add(c.getSuperclass());
            for (Class<?> i : c.getInterfaces()) queue.add(i);
            out.add(c.getName().replace('.', '/'));
        }
    }

    /** row active when the member is missing on the running JVM but existed at the callsite's authored version */
    private static boolean applies(int since, int origVersion) {
        return since > Utils.getCurrentClassVersion() && since <= origVersion;
    }

    /** the caller's {@code Class[]} against a row's source-namespace param desc, type by type */
    private static boolean paramsMatch(String desc, Class<?>[] params) {
        int i = desc.indexOf('(') + 1;
        int pi = 0;
        while (desc.charAt(i) != ')') {
            if (params == null || pi >= params.length) {
                return false;
            }
            int start = i;
            while (desc.charAt(i) == '[') i++;
            if (desc.charAt(i) == 'L') {
                i = desc.indexOf(';', i) + 1;
            } else {
                i++;
            }
            if (!typeMatches(desc.substring(start, i), params[pi++])) {
                return false;
            }
        }
        return params == null || pi == params.length;
    }

    /**
     * One desc fragment vs one {@code Class}: the leaf name must match
     * literally, or match through a {@code T} row in reverse — authored code
     * holding a stand-in (e.g. {@code J_L_Module.class}) passes it where the
     * source signature names {@code java/lang/Module}.
     */
    private static boolean typeMatches(String frag, Class<?> c) {
        int d = 0;
        while (d < frag.length() && frag.charAt(d) == '[') d++;
        Class<?> e = c;
        for (int k = 0; k < d; k++) {
            if (!e.isArray()) {
                return false;
            }
            e = e.getComponentType();
        }
        if (e.isArray()) {
            return false;
        }
        if (frag.charAt(d) == 'L') {
            String internal = frag.substring(d + 1, frag.length() - 1);
            if (internal.equals(e.getName().replace('.', '/'))) {
                return true;
            }
            List<String> sources = BY_TARGET.get(e.getName().replace('.', '/'));
            return sources != null && sources.contains(internal);
        }
        return e.isPrimitive() && primLetter(e) == frag.charAt(d);
    }

    private static Method resolveImpl(MemberRow row, String owner, Class<?> clazz, int origVersion) {
        return resolveImpl(row, owner, clazz, origVersion, ReflectionTable.class.getClassLoader());
    }

    private static Method resolveImpl(MemberRow row, String owner, Class<?> clazz, int origVersion,
                                      ClassLoader loader) {
        try {
            Class<?> impl = Class.forName(row.implOwner.replace('/', '.'), false, loader);
            Class<?>[] implParams = paramClasses(row.implDesc);
            Method m = impl.getDeclaredMethod(row.implName, implParams);
            try {
                m.setAccessible(true);
            } catch (Throwable ignore) {
                // api impls are public; belt-and-braces for a package-private handler
            }
            Object[] tail = transformTail(row, implParams, clazz, origVersion);
            if (tail == null) {
                // impl takes transform args the lookup can't fabricate — an
                // unresolvable row must not conjure a broken view
                return null;
            }
            StubMethodView.register(m, declaringClass(row, owner), row.name, row.instance,
                row.mods, MethodType.methodType(
                    classFor(returnFrag(row.desc)), paramClasses(row.desc)), tail);
            return m;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Args the impl handler takes past the member signature — the receiver
     * (instance rows) plus transform-injected params the callsite would have
     * carried as {@code ldc}s: a trailing {@code int} gets the authored
     * version ({@code downgradeVersion}), a {@code Class} the looked-up
     * class ({@code callerClass}'s best local answer — reflection has no
     * true caller). Any other extra shape can't be fabricated.
     */
    private static Object[] transformTail(MemberRow row, Class<?>[] implParams, Class<?> clazz, int origVersion) {
        int base = implParams.length - (row.instance ? 1 : 0);
        // params shared with the source desc — the impl may widen/narrow
        // arity only by the injected tail, so walk from the member end
        int extra = base - arity(row.desc);
        if (extra < 0) {
            return null;
        }
        Object[] tail = new Object[extra];
        for (int k = 0; k < extra; k++) {
            Class<?> t = implParams[arity(row.desc) + (row.instance ? 1 : 0) + k];
            if (t == int.class) {
                tail[k] = origVersion;
            } else if (t == Class.class) {
                tail[k] = clazz;
            } else {
                return null;
            }
        }
        return tail;
    }

    private static int arity(String desc) {
        int i = desc.indexOf('(') + 1, n = 0;
        while (desc.charAt(i) != ')') {
            while (desc.charAt(i) == '[') i++;
            i = desc.charAt(i) == 'L' ? desc.indexOf(';', i) + 1 : i + 1;
            n++;
        }
        return n;
    }

    /** the {@code Class} a {@code getDeclaringClass} view returns — the runtime-facing stand-in for the row's owner */
    private static Class<?> declaringClass(MemberRow row, String owner) {
        try {
            return Class.forName(remap(owner.replace('/', '.'), row.since), false,
                ReflectionTable.class.getClassLoader());
        } catch (Throwable t) {
            return null;
        }
    }

    private static Class<?>[] paramClasses(String desc) throws ClassNotFoundException {
        return paramClasses(desc, ReflectionTable.class.getClassLoader());
    }

    /** same parse, resolved through a supplied loader — for impls that live in a different loader than this table */
    static Class<?>[] paramClasses(String desc, ClassLoader loader) throws ClassNotFoundException {
        List<Class<?>> out = new ArrayList<>();
        int i = desc.indexOf('(') + 1;
        while (desc.charAt(i) != ')') {
            int start = i;
            while (desc.charAt(i) == '[') i++;
            if (desc.charAt(i) == 'L') {
                i = desc.indexOf(';', i) + 1;
            } else {
                i++;
            }
            out.add(classFor(desc.substring(start, i), loader));
        }
        return out.toArray(new Class<?>[0]);
    }

    private static Class<?> classFor(String d) throws ClassNotFoundException {
        return classFor(d, ReflectionTable.class.getClassLoader());
    }

    private static Class<?> classFor(String d, ClassLoader loader) throws ClassNotFoundException {
        int dims = 0;
        while (d.charAt(dims) == '[') dims++;
        char c = d.charAt(dims);
        Class<?> e;
        if (c == 'L') {
            // impl/member descs carry source-shape names, but api classes
            // are themselves downgraded: their declared signatures hold
            // the stand-in on EVERY runtime, so the source→stand-in map
            // applies unconditionally — unlike the forName window, which
            // answers the running JDK's view (a type the runner has
            // natively still has only the stand-in in the api signature)
            RESOURCES.get();
            String source = d.substring(dims + 1, d.length() - 1);
            TypeRow row = TYPES.get(source);
            e = Class.forName((row != null ? row.target : source).replace('/', '.'),
                false, loader);
        } else {
            e = primClass(c);
        }
        for (int k = 0; k < dims; k++) {
            e = java.lang.reflect.Array.newInstance(e, 0).getClass();
        }
        return e;
    }

    private static char primLetter(Class<?> c) {
        if (c == boolean.class) return 'Z';
        if (c == byte.class) return 'B';
        if (c == char.class) return 'C';
        if (c == short.class) return 'S';
        if (c == int.class) return 'I';
        if (c == long.class) return 'J';
        if (c == float.class) return 'F';
        if (c == double.class) return 'D';
        return 'V';
    }

    private static Class<?> primClass(char c) {
        switch (c) {
            case 'Z': return boolean.class;
            case 'B': return byte.class;
            case 'C': return char.class;
            case 'S': return short.class;
            case 'I': return int.class;
            case 'J': return long.class;
            case 'F': return float.class;
            case 'D': return double.class;
            default: return void.class;
        }
    }

    /**
     * The {@code CtorRow} for {@code owner.<init>(params)} inside the
     * {@code (running, origVersion]} window, or {@code null}. Ctors aren't
     * inherited — the lookup is owner-exact, no supertype walk, and no
     * {@code BY_TARGET} sources either: a rewrite's owner is by construction
     * a real runtime class (the retarget ctor lives on it).
     */
    private static CtorRow ctorLookup(Class<?> owner, Class<?>[] params, int origVersion) {
        RESOURCES.get();
        if (CTORS.isEmpty() || owner == null) {
            return null;
        }
        List<CtorRow> rows = CTORS.get(owner.getName().replace('.', '/'));
        if (rows == null) {
            return null;
        }
        for (CtorRow row : rows) {
            if (applies(row.since, origVersion) && paramsMatch(row.desc, params)) {
                return row;
            }
        }
        return null;
    }

    /**
     * Builds the {@code (origArgs) -> owner} handle for a rewrite row:
     * the target {@code <init>} under {@code @InitArg} converter filters
     * (right-to-left so positions stay stable), permuted onto the original
     * arg list, then the {@code @InitPre}/{@code @InitPost} void hooks
     * folded around it — the handle-algebra mirror of {@code
     * MemberStubTransform.rewriteCtor}'s emitted sequence. Any resolution
     * failure returns {@code null} — an unanswerable row degrades to the
     * caller's native miss.
     */
    private static MethodHandle synthesizeCtor(MethodHandles.Lookup lookup, Class<?> owner,
                                               CtorRow row, int origVersion) {
        return synthesizeCtor(lookup, owner, row, origVersion, null);
    }

    /**
     * The {@link Member} the produced handle's {@code reflectAs} answers —
     * supplied by callers that already resolved the target ctor (so the
     * object {@code getConstructor} hands out and the one {@code reflectAs}
     * answers stay the same instance); {@code null} resolves a fresh
     * {@code Constructor} for rewrite rows / the impl {@code Method} for
     * factories.
     */
    private static MethodHandle synthesizeCtor(MethodHandles.Lookup lookup, Class<?> owner,
                                               CtorRow row, int origVersion,
                                               java.lang.reflect.Member reflectAs) {
        try {
            ClassLoader loader = lookupLoader(lookup);
            if (row.factory) {
                MethodHandle f = findImpl(lookup, row.impl, loader);
                if (row.impl.version) {
                    f = MethodHandles.insertArguments(f, f.type().parameterCount() - 1, origVersion);
                }
                StubHandleView.register(f,
                    reflectAs == null ? implMember(row.impl, loader) : reflectAs, owner, "<init>",
                    MethodType.methodType(void.class, paramClasses(row.desc)),
                    MethodHandleInfo.REF_newInvokeSpecial, row.mods);
                return f;
            }
            Class<?>[] origArgs = paramClasses(row.desc);
            Class<?>[] toArgs = paramClasses(row.toDesc);
            Constructor<?> toCtor = reflectAs instanceof Constructor
                ? (Constructor<?>) reflectAs : owner.getDeclaredConstructor(toArgs);
            MethodHandle h = lookup.findConstructor(owner, MethodType.methodType(void.class, toArgs));
            // a kept slot whose target type is a supertype of the orig arg
            // needs a casting filter — permuteArguments wants exact types
            for (int i = row.keep.length - 1; i >= 0; i--) {
                if (row.keep[i] >= 0 && toArgs[i] != origArgs[row.keep[i]]) {
                    h = MethodHandles.filterArguments(h, i, MethodHandles.identity(toArgs[i])
                        .asType(MethodType.methodType(toArgs[i], origArgs[row.keep[i]])));
                }
            }
            // converter slots produce the target args — right-to-left so
            // each filter's position is still its to-desc slot
            for (int i = row.keep.length - 1; i >= 0; i--) {
                if (row.keep[i] < 0) {
                    MethodHandle conv = findImpl(lookup, row.convs[i], loader);
                    if (conv.type().returnType() != toArgs[i]) {
                        conv = conv.asType(MethodType.methodType(toArgs[i],
                            conv.type().parameterArray()));
                    }
                    h = MethodHandles.filterArguments(h, i, conv);
                }
            }
            // the filtered signature interleaves kept slots and converter
            // param lists — permute it onto the original arg list
            int[] reorder = new int[h.type().parameterCount()];
            int pos = 0;
            for (int i = 0; i < row.keep.length; i++) {
                if (row.keep[i] >= 0) {
                    reorder[pos++] = row.keep[i];
                } else {
                    for (int f : row.convs[i].from) {
                        reorder[pos++] = f;
                    }
                }
            }
            h = MethodHandles.permuteArguments(h, MethodType.methodType(owner, origArgs), reorder);
            if (row.pre != null) {
                MethodHandle p = findImpl(lookup, row.pre, loader);
                // void combiner — consumes the leading prefix of the folded
                // target's args; span it wide enough to cover every index
                int width = 0;
                for (int f : row.pre.from) {
                    width = Math.max(width, f + 1);
                }
                Class<?>[] prefix = new Class<?>[width];
                System.arraycopy(origArgs, 0, prefix, 0, width);
                p = MethodHandles.permuteArguments(p,
                    MethodType.methodType(void.class, prefix), row.pre.from);
                h = MethodHandles.foldArguments(h, p);
            }
            if (row.post != null) {
                MethodHandle p = findImpl(lookup, row.post, loader);
                if (row.post.version) {
                    p = MethodHandles.insertArguments(p, p.type().parameterCount() - 1, origVersion);
                }
                if (p.type().parameterType(0) != owner) {
                    p = p.asType(p.type().changeParameterType(0, owner));
                }
                // post(obj, origSubset[, ver])void → (obj, origArgs)void
                Class<?>[] postSig = new Class<?>[origArgs.length + 1];
                postSig[0] = owner;
                System.arraycopy(origArgs, 0, postSig, 1, origArgs.length);
                int[] pm = new int[p.type().parameterCount()];
                for (int i = 0; i < row.post.from.length; i++) {
                    pm[i + 1] = row.post.from[i] + 1;
                }
                p = MethodHandles.permuteArguments(p, MethodType.methodType(void.class, postSig), pm);
                // (obj, origArgs) -> obj doing { post(); return obj; }
                MethodHandle ret = MethodHandles.foldArguments(
                    MethodHandles.dropArguments(MethodHandles.identity(owner), 1, origArgs), p);
                h = MethodHandles.foldArguments(ret, h);
            }
            StubHandleView.register(h, toCtor, owner, "<init>",
                MethodType.methodType(void.class, origArgs),
                MethodHandleInfo.REF_newInvokeSpecial, row.mods);
            // the target ctor the table hands out (directly or via
            // reflectAs) reports the source signature and invokes through
            // this handle — registered once here so both handouts agree
            StubCtorView.register(toCtor, h, origArgs,
                MethodType.methodType(void.class, origArgs), row.mods);
            return h;
        } catch (Throwable t) {
            if (System.getProperty("jvmdg.debug.ctorSynth") != null) {
                t.printStackTrace();
            }
            return null;
        }
    }

    /** the {@code public static} impl a {@link Spec} names — its desc is source-shaped, remapped to runtime types here */
    private static MethodHandle findImpl(MethodHandles.Lookup lookup, Spec s, ClassLoader loader)
        throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        Class<?> impl = Class.forName(s.owner.replace('/', '.'), false, loader);
        return lookup.findStatic(impl, s.name, MethodType.methodType(
            classFor(returnFrag(s.desc)), paramClasses(s.desc)));
    }

    /** the {@code Member} a factory spec's {@code reflectAs} answers — its impl {@code Method} */
    private static java.lang.reflect.Member implMember(Spec s, ClassLoader loader) {
        try {
            Class<?> impl = Class.forName(s.owner.replace('/', '.'), false, loader);
            return impl.getDeclaredMethod(s.name, paramClasses(s.desc));
        } catch (Throwable t) {
            return null;
        }
    }

    private static void loadResources() {
        // leaf-only name → resolved against this class's own package (see
        // RESOURCE): the unprefixed api jar answers the same file the path
        // constant names, a prefixed copy answers its own relocated file.
        String leaf = RESOURCE.substring(RESOURCE.lastIndexOf('/') + 1);
        try {
            InputStream in = Utils.openResource(ReflectionTable.class, leaf);
            if (in == null) {
                return;
            }
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    applyRow(line.split("\t"));
                }
                r.close();
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed reading " + RESOURCE, e);
        }
    }
}
