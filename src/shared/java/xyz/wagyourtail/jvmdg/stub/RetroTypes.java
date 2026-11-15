package xyz.wagyourtail.jvmdg.stub;

import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.RequiresResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime membership oracle for <em>retroactive supertypes</em>: JDK types
 * (usually interfaces) that existing JDK classes only started implementing in
 * a later release — {@code java/util/SequencedCollection} on {@code List},
 * {@code RandomGenerator} on {@code Random}, and so on. Downgraded code has
 * the type remapped to a real supertype, so {@code instanceof}/
 * {@code checkcast} can't ask the JVM; the transform emits calls here
 * instead.
 *
 * <p>Membership is name-keyed: a runtime class is a member when its
 * superclass/interface closure contains any registered internal name —
 * covering package-private JDK impls (e.g. {@code LinkedHashMap}'s view
 * classes) and jvmdg <em>marker</em> interfaces a downgraded user class is
 * given. No {@code Class.forName}, so unknown/absent JDK impls can't break
 * older runtimes.</p>
 *
 * <p>Data arrives two ways, merged: every
 * {@code META-INF/jvmdg/retrotypes} classpath resource
 * ({@code <retroInternalName><TAB><name>,<name>,…} lines, {@code #} comments)
 * loaded lazily on first use, and {@link #register} — which generated/api
 * code and tests may call before first use.</p>
 */
@RequiresResource({RetroTypes.RESOURCE, RetroTypes.WRAP_RESOURCE,
    RetroTypes.GAINED_RESOURCE, RetroTypes.GAINED_MANUAL_RESOURCE,
    RetroTypes.GAINED_CALLS_RESOURCE, RetroTypes.GAINED_CALLS_MANUAL_RESOURCE})
public final class RetroTypes {

    /** classpath location of the runtime membership table — also the {@code @RequiresResource} tag shading keys on */
    public static final String RESOURCE = "META-INF/jvmdg/retrotypes";

    /** classpath location of the OWN_INTERFACE wrap table ({@code retro<TAB>stub<TAB>member=impl,…} rows) */
    public static final String WRAP_RESOURCE = "META-INF/jvmdg/wraps";

    /** classpath location of the gained-supertype table ({@code I<TAB>I-min<TAB>member:since,…} rows) — doubles as the transform's site-routing data */
    public static final String GAINED_RESOURCE = "META-INF/jvmdg/gained";

    /** classpath location of hand-authored gained-supertype rows — same format, unioned over the derived table */
    public static final String GAINED_MANUAL_RESOURCE = "META-INF/jvmdg/gained-manual";

    /** classpath location of the resolved {@code (I,member,T) → impl site} table the gained dispatchers tail-call */
    public static final String GAINED_CALLS_RESOURCE = "META-INF/jvmdg/gained-calls";

    /** classpath location of hand-authored gained-call rows — same format, unioned over the derived table */
    public static final String GAINED_CALLS_MANUAL_RESOURCE = "META-INF/jvmdg/gained-calls-manual";

    private RetroTypes() {
    }

    /** retro internal name → set of implementing internal names */
    private static final Map<String, Set<String>> MEMBERSHIP = new ConcurrentHashMap<>();

    /**
     * implementer internal name → retro names whose membership sets contain
     * it — the reverse of {@link #MEMBERSHIP}, populated in lockstep by
     * {@link #register}. Lets gained-member lookups translate a stand-in /
     * impl receiver name into the member name its adapter serves, since a
     * class-stubbed member's runtime classes never carry the member name in
     * their own hierarchy (e.g. {@code J_N_H_HttpClient} impls asking the
     * {@code java/lang/AutoCloseable} gained row about {@code
     * java/net/http/HttpClient}).
     */
    private static final Map<String, Set<String>> MEMBER_OF = new ConcurrentHashMap<>();

    /**
     * gained interface internal name → member internal name → class-file
     * version the binding appeared at. Unlike {@link #MEMBERSHIP} the
     * since matters: both the interface and the member predate the
     * binding, so whether an {@code instanceof} should answer true
     * depends on the calling class's authoring version — emitted sites
     * carry it as a trailing ceiling arg.
     */
    private static final Map<String, Map<String, Integer>> GAINED = new ConcurrentHashMap<>();

    /**
     * member internal name → gained interface internal name → since — the
     * inverted view of {@link #GAINED}, kept in lockstep with it so
     * {@code Class.getInterfaces} can synthesize stand-ins for interfaces an
     * unmodified (JDK) class gained after the running JVM's version without
     * scanning the whole table per call.
     */
    private static final Map<String, Map<String, Integer>> GAINED_BY_MEMBER = new ConcurrentHashMap<>();

    /**
     * gained interface internal name → {@code "name desc"} → per-member
     * resolution rows in table order. {@code T}-selection walks the value's
     * own hierarchy (superclass chain first), so a row order collision
     * can't misroute; each {@link GainedRow} keeps parallel resolution
     * slots ascending by ceiling.
     */
    private static final Map<String, Map<String, List<GainedRow>>> GAINED_CALLS = new ConcurrentHashMap<>();

    /** one member's resolution chain: parallel {@code ver}/{@code declarer}/{@code stubSite} slots ascending by {@code ver} */
    private static final class GainedRow {
        final String t;
        final int[] vers;
        final String[] declarers;
        final String[] stubs;

        GainedRow(String t, int[] vers, String[] declarers, String[] stubs) {
            this.t = t;
            this.vers = vers;
            this.declarers = declarers;
            this.stubs = stubs;
        }
    }

    /**
     * retro internal name → wrap rows in resource order. Multiple resources
     * can carry a row for the same retro type — two jars shaded under
     * different prefixes each ship one — so rows are a list;
     * {@link #wrapImpl} picks the row whose {@code stub} column matches the
     * calling context's name (same-lib guarantee) and falls back to the
     * first for externally-registered rows.
     */
    private static final Map<String, List<WrapRow>> WRAPS = new ConcurrentHashMap<>();

    /** JDK member internal name → runtime class (bootstrap-loaded, loader-safe to cache globally) */
    private static final Map<String, Class<?>> MEMBER_CLASSES = new ConcurrentHashMap<>();

    /** context loader → impl internal name → member-typed ctor (impl classes are loader-bound: the same impl name under two loaders is a different type, and a ctor from the wrong loader mints objects the caller's trailing CHECKCAST rejects) */
    private static final Map<ClassLoader, Map<String, java.lang.reflect.Constructor<?>>> IMPL_CTORS = new ConcurrentHashMap<>();

    /** context loader → dispatcher binary name → {@code X$jvmdg$retro.wrap} handle, or {@link #NO_DISPATCHER} */
    private static final Map<ClassLoader, Map<String, Object>> DISPATCHERS = new ConcurrentHashMap<>();

    /** sentinel for "no generated dispatcher in this loader" — a null can't live in a CHM */
    private static final Object NO_DISPATCHER = new Object();

    /** map-key stand-in for the bootstrap loader in {@link #DISPATCHERS} (a null can't key a CHM) */
    private static final ClassLoader BOOTSTRAP_KEY = new ClassLoader() {};

    /** one wraps-resource row: {@code stub} = adapter name for the already-adapted passthrough */
    private static final class WrapRow {
        final String stub;
        final String[] members;
        final String[] impls;

        WrapRow(String stub, String[] members, String[] impls) {
            this.stub = stub;
            this.members = members;
            this.impls = impls;
        }
    }

    /** lazily-loaded resource tables, folded into {@link #MEMBERSHIP} on first query */
    private static final Lazy<Void> RESOURCES = new Lazy<Void>() {
        @Override
        protected Void init() {
            loadResources();
            return null;
        }
    };

    /**
     * Per runtime class, its superclass/interface closure as internal names —
     * walked once, registration-independent, so {@link #register} calls after
     * a class was first queried still resolve correctly.
     */
    private static final ClassValue<Set<String>> NAMES = new ClassValue<Set<String>>() {
        @Override
        protected Set<String> computeValue(Class<?> type) {
            Set<String> names = new HashSet<>();
            Deque<Class<?>> walk = new ArrayDeque<>();
            walk.add(type);
            while (!walk.isEmpty()) {
                Class<?> c = walk.removeFirst();
                if (c == null || !names.add(c.getName().replace('.', '/'))) {
                    continue;
                }
                Collections.addAll(walk, c.getInterfaces());
                Class<?> superclass = c.getSuperclass();
                if (superclass != null) {
                    walk.add(superclass);
                }
            }
            return Collections.unmodifiableSet(names);
        }
    };

    /**
     * Registers a retro type's membership set — internal names
     * ({@code java/util/List} form) of every class the retro type is
     * implemented by, including jvmdg marker interfaces for user code. Merged
     * into any existing set. Must run before the first query that could see
     * affected classes; the resource path already satisfies that.
     */
    public static void register(String retroInternalName, Iterable<String> implementedBy) {
        Set<String> set = MEMBERSHIP.get(retroInternalName);
        if (set == null) {
            set = ConcurrentHashMap.newKeySet();
            Set<String> prev = MEMBERSHIP.putIfAbsent(retroInternalName, set);
            if (prev != null) {
                set = prev;
            }
        }
        for (String name : implementedBy) {
            set.add(name);
            Set<String> retro = MEMBER_OF.get(name);
            if (retro == null) {
                retro = ConcurrentHashMap.newKeySet();
                Set<String> prev = MEMBER_OF.putIfAbsent(name, retro);
                if (prev != null) {
                    retro = prev;
                }
            }
            retro.add(retroInternalName);
        }
    }

    /**
     * Registers a gained binding — interface {@code i} gains member type
     * {@code memberInternalName} at class-file version {@code since}.
     * Merged over any existing row (lowest since wins); generated/api code
     * and tests may call this before first use.
     */
    public static void gainedRegister(String i, String memberInternalName, int since) {
        Map<String, Integer> members = GAINED.get(i);
        if (members == null) {
            members = new ConcurrentHashMap<>();
            Map<String, Integer> raced = GAINED.putIfAbsent(i, members);
            if (raced != null) {
                members = raced;
            }
        }
        Integer cur = members.get(memberInternalName);
        if (cur == null || since < cur) {
            members.put(memberInternalName, since);
            Map<String, Integer> ifaces = GAINED_BY_MEMBER.get(memberInternalName);
            if (ifaces == null) {
                ifaces = new ConcurrentHashMap<>();
                Map<String, Integer> raced = GAINED_BY_MEMBER.putIfAbsent(memberInternalName, ifaces);
                if (raced != null) {
                    ifaces = raced;
                }
            }
            Integer icur = ifaces.get(i);
            if (icur == null || since < icur) {
                ifaces.put(i, since);
            }
        }
    }

    /**
     * Interfaces class {@code memberInternalName} gained, as
     * interface-internal-name → since — or null when it gained none. The
     * {@code Class.getInterfaces} stub uses this to synthesize stand-ins the
     * runtime class can't carry.
     */
    public static Map<String, Integer> gainedInterfacesFor(String memberInternalName) {
        return GAINED_BY_MEMBER.get(memberInternalName);
    }

    private static void loadResources() {
        ClassLoader cl = RetroTypes.class.getClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        for (String[] row : readRows(RESOURCE, cl)) {
            List<String> names = new ArrayList<>();
            for (String name : row[row.length - 1].split(",")) {
                name = name.trim();
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
            register(row[0].trim(), names);
        }
        for (String[] row : readRows(WRAP_RESOURCE, cl)) {
            if (row.length != 3) {
                continue;
            }
            List<String> members = new ArrayList<>();
            List<String> impls = new ArrayList<>();
            for (String pair : row[2].split(",")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    members.add(pair.substring(0, eq).trim());
                    impls.add(pair.substring(eq + 1).trim());
                }
            }
            String retro = row[0].trim();
            List<WrapRow> rows = WRAPS.get(retro);
            if (rows == null) {
                rows = Collections.synchronizedList(new ArrayList<WrapRow>());
                List<WrapRow> raced = WRAPS.putIfAbsent(retro, rows);
                if (raced != null) {
                    rows = raced;
                }
            }
            rows.add(new WrapRow(row[1].trim(),
                members.toArray(new String[0]), impls.toArray(new String[0])));
        }
        for (String res : new String[]{GAINED_RESOURCE, GAINED_MANUAL_RESOURCE}) {
            for (String[] row : readRows(res, cl)) {
                if (row.length != 3) {
                    continue;
                }
                // row[1] = the interface's own first-version — transform-side
                // data (the provider's existence gate); unused here
                String iface = row[0].trim();
                for (String m : row[2].split(",")) {
                    int c = m.lastIndexOf(':');
                    if (c <= 0) {
                        continue;
                    }
                    String member = m.substring(0, c).trim();
                    int since;
                    try {
                        since = Integer.parseInt(m.substring(c + 1).trim());
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    gainedRegister(iface, member, since);
                }
            }
        }
        for (String res : new String[]{GAINED_CALLS_RESOURCE, GAINED_CALLS_MANUAL_RESOURCE}) {
            for (String[] row : readRows(res, cl)) {
                if (row.length != 3) {
                    continue;
                }
                String iface = row[0].trim();
                String member = row[1].trim();
                List<GainedRow> list = GAINED_CALLS.get(iface) == null
                    ? null : GAINED_CALLS.get(iface).get(member);
                if (list == null) {
                    list = new ArrayList<>();
                    Map<String, List<GainedRow>> byMember = GAINED_CALLS.get(iface);
                    if (byMember == null) {
                        byMember = new ConcurrentHashMap<>();
                        Map<String, List<GainedRow>> raced = GAINED_CALLS.putIfAbsent(iface, byMember);
                        if (raced != null) {
                            byMember = raced;
                        }
                    }
                    List<GainedRow> raced = byMember.putIfAbsent(member, list);
                    if (raced != null) {
                        list = raced;
                        continue;
                    }
                }
                parseGainedRows(row[2].trim(), list);
            }
        }
    }

    /** {@code T=ver:D[>site]|ver:D[>site],…} → one {@link GainedRow} per member T, appended.
     * Resolutions separate on {@code |} — {@code ;} collides with the descriptor {@code ;} inside
     * {@code owner.name(desc)} sites. */
    private static void parseGainedRows(String field, List<GainedRow> out) {
        for (String tEntry : field.split(",")) {
            int eq = tEntry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String t = tEntry.substring(0, eq);
            String[] res = tEntry.substring(eq + 1).split("\\|");
            int[] vers = new int[res.length];
            String[] decls = new String[res.length];
            String[] stubs = new String[res.length];
            for (int i = 0; i < res.length; i++) {
                String r = res[i];
                int c = r.indexOf(':');
                if (c <= 0) {
                    break;
                }
                try {
                    vers[i] = Integer.parseInt(r.substring(0, c));
                } catch (NumberFormatException e) {
                    break;
                }
                String body = r.substring(c + 1);
                int gt = body.indexOf('>');
                if (gt >= 0) {
                    decls[i] = body.substring(0, gt).equals("-") ? null : body.substring(0, gt);
                    stubs[i] = body.substring(gt + 1);
                } else {
                    decls[i] = body.equals("-") ? null : body;
                }
            }
            out.add(new GainedRow(t, vers, decls, stubs));
        }
    }

    /** {@code <TAB>}-split data rows across every classpath resource at {@code path} */
    private static List<String[]> readRows(String path, ClassLoader cl) {
        List<String[]> rows = new ArrayList<>();
        try {
            Enumeration<URL> resources = cl.getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(Utils.openUncached(url), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        rows.add(line.split("\t"));
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("failed reading " + path + " from " + url, e);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed reading " + path, e);
        }
        return rows;
    }

    private static boolean isMember(Class<?> runtimeClass, String retroInternalName) {
        RESOURCES.get();
        Set<String> impls = MEMBERSHIP.get(retroInternalName);
        if (impls == null) {
            return false;
        }
        for (String name : NAMES.get(runtimeClass)) {
            if (impls.contains(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code obj instanceof <retroType>} — membership by hierarchy names.
     * {@code null} is {@code false}, matching the {@code INSTANCEOF} opcode.
     */
    public static boolean isInstance(Object obj, String retroInternalName) {
        return obj != null && isMember(obj.getClass(), retroInternalName);
    }

    /**
     * Whether {@code retroInternalName} has a membership row at all — i.e.
     * it is a stubbed (retro-capable) type on this runtime.
     */
    public static boolean isRetroType(String retroInternalName) {
        RESOURCES.get();
        return MEMBERSHIP.containsKey(retroInternalName);
    }

    /**
     * {@code o instanceof <label>} for {@code typeSwitch} labels: the
     * emitted switch keeps the label's <em>source</em> name (which may not
     * exist on the runtime), so a {@code memberName} with a row answers
     * membership — the array form for array labels — and anything else
     * resolves the label lazily with {@link Class#forName} (no init) using
     * the calling class's loader, then defers to {@link Class#isInstance}.
     * An unresolvable label answers {@code false}.
     *
     * @param memberName   retro lookup key — the label type, or its element
     *                     type for array labels
     * @param fallbackName the name {@code Class.forName} understands —
     *                     {@code java.util.X} form, or the
     *                     {@code [Ljava.util.X;} descriptor for array labels
     * @param context      the calling class — supplies the loader
     * @param array        whether the label is an array type
     */
    public static boolean isInstanceOrFallback(Object o, String memberName, String fallbackName, Class<?> context, boolean array) {
        if (isRetroType(memberName)) {
            return array ? isArrayInstance(o, memberName) : isInstance(o, memberName);
        }
        ClassLoader cl = context.getClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        try {
            return Class.forName(fallbackName, false, cl).isInstance(o);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * {@code <retroType>[dims].class.isAssignableFrom(cls)} — whether
     * {@code cls} is a {@code dims}-array of (or, when {@code dims == 0},
     * exactly) a member type, the retro type itself, or a subtype of one.
     * {@code null} throws {@link NullPointerException}, matching
     * {@link Class#isAssignableFrom}. Primitive/array-mismatched inputs are
     * {@code false}.
     */
    public static boolean isAssignableFrom(Class<?> cls, String retroInternalName, int dims) {
        if (cls == null) {
            throw new NullPointerException();
        }
        int d = 0;
        while (cls.isArray()) {
            cls = cls.getComponentType();
            d++;
        }
        if (d != dims || cls.isPrimitive()) {
            return false;
        }
        String name = cls.getName().replace('.', '/');
        return name.equals(retroInternalName) || isMember(cls, retroInternalName);
    }

    /**
     * Gained-supertype membership: is {@code o}'s type bound to interface
     * {@code i} at-or-below {@code ceiling} — the calling class's
     * class-file version, emitted as a trailing arg by the transform (same
     * convention as {@code downgradeVersion} parameters). Both the
     * interface and the member predate the binding, so {@code instanceof}
     * on a {@code Process} answers true under a V26-authored site and
     * false under a V21 one — the ceiling is what keeps them apart.
     * Name-keyed like {@link #isInstance}: package-private JDK members
     * resolve by name where a real {@code instanceof} can't be emitted.
     * The generated dispatcher runs its native {@code o instanceof I}
     * fast-path first — this answers only for the gained members.
     * {@code null} is {@code false}.
     */
    public static boolean gainedMember(String i, Object o, int ceiling) {
        if (o == null) {
            return false;
        }
        return gainedAssignable(i, o.getClass(), ceiling);
    }

    /**
     * {@link Class#isAssignableFrom} for the gained members — whether
     * {@code cls}'s name closure contains a member bound to {@code i}
     * at-or-below {@code ceiling}. {@code null} throws
     * {@link NullPointerException}, matching
     * {@link Class#isAssignableFrom}.
     */
    public static boolean gainedAssignable(String i, Class<?> cls, int ceiling) {
        if (cls == null) {
            throw new NullPointerException();
        }
        RESOURCES.get();
        Map<String, Integer> members = GAINED.get(i);
        if (members == null) {
            return false;
        }
        for (String n : NAMES.get(cls)) {
            if (gainedAt(members, n, ceiling)) {
                return true;
            }
            Set<String> retro = MEMBER_OF.get(n);
            if (retro != null) {
                for (String r : retro) {
                    if (gainedAt(members, r, ceiling)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean gainedAt(Map<String, Integer> members, String memberName, int ceiling) {
        Integer since = members.get(memberName);
        return since != null && since <= ceiling;
    }

    /**
     * Registers a resolved gained-call row — internal names throughout,
     * resolutions ascending by ceiling version ({@code declarer == null}
     * marks an unresolvable ceiling). Generated/api code and tests may
     * call this before first use.
     */
    public static void gainedCallRegister(String i, String member, String t,
                                          int[] vers, String[] declarers, String[] stubs) {
        Map<String, List<GainedRow>> byMember = GAINED_CALLS.get(i);
        if (byMember == null) {
            byMember = new ConcurrentHashMap<>();
            Map<String, List<GainedRow>> raced = GAINED_CALLS.putIfAbsent(i, byMember);
            if (raced != null) {
                byMember = raced;
            }
        }
        List<GainedRow> list = byMember.get(member);
        if (list == null) {
            list = new ArrayList<>();
            List<GainedRow> raced = byMember.putIfAbsent(member, list);
            if (raced != null) {
                list = raced;
            }
        }
        list.add(new GainedRow(t, vers, declarers, stubs));
    }

    /**
     * The impl path for an {@code invokeinterface I.m} site on a gained
     * member — the generated dispatchers call this only after the
     * {@code self instanceof I} fast-path fails. {@code member} is the
     * {@code "name desc"} site pair; {@code ceiling} is the calling
     * class's authored class-file version. Resolves the member T via
     * {@code self}'s own hierarchy (superclass chain first, interfaces
     * breadth-first), tries the declared member natively, then its
     * registered stub — an {@link AbstractMethodError} when the ceiling's
     * resolution is unresolvable or nothing matches.
     *
     * <p>{@code context} is the calling dispatcher class — its loader
     * resolves declarer/stub/param classes so a runtime-downgrading
     * classloader transforms api classes like every other referenced api
     * ref (a raw api jar on the app classpath must not serve them at
     * their authored version).</p>
     */
    public static Object gainedCall(String i, String member, Object self, Object[] args, int ceiling,
                                    Class<?> context) throws Throwable {
        RESOURCES.get();
        ClassLoader cl = context.getClassLoader();
        Map<String, List<GainedRow>> byMember = GAINED_CALLS.get(i);
        List<GainedRow> rows = byMember == null ? null : byMember.get(member);
        if (rows != null && self != null) {
            Set<String> names = new HashSet<>();
            for (GainedRow r : rows) {
                names.add(r.t);
            }
            String t = gainedMemberName(self.getClass(), names);
            if (t != null) {
                GainedRow row = null;
                for (GainedRow r : rows) {
                    if (r.t.equals(t)) {
                        row = r;
                        break;
                    }
                }
                for (int k = row.vers.length - 1; k >= 0; k--) {
                    if (row.vers[k] <= ceiling) {
                        if (row.declarers[k] != null) {
                            java.lang.reflect.Method m = resolveGained(row.declarers[k], member, cl);
                            if (m != null) {
                                // resolution failed → fall to stub; a failure *inside* the
                                // invoked member is the real answer and must propagate
                                return invokeResolved(m, self, args);
                            }
                        }
                        if (row.stubs[k] != null) {
                            return invokeStub(row.stubs[k], member, self, args, cl);
                        }
                        break;
                    }
                }
            }
        }
        AbstractMethodError ame = new AbstractMethodError(
            i.replace('/', '.') + "." + member + " unresolved for " +
                (self == null ? "null" : self.getClass().getName()) + " at ceiling " + ceiling);
        throw ame;
    }

    /**
     * {@link #gainedCall(String, String, Object, Object[], int, Class)}
     * resolving through this jar's own loader — kept for dispatchers
     * generated before the context parameter existed.
     */
    public static Object gainedCall(String i, String member, Object self, Object[] args, int ceiling) throws Throwable {
        return gainedCall(i, member, self, args, ceiling, RetroTypes.class);
    }

    /**
     * resolve {@code member} ("name desc") as declared on {@code declarer} (internal name),
     * or {@code null} when the declarer/member is absent on this runtime — the stub path
     * then applies. Only resolution plumbing fails here; the member's own invocation
     * goes through {@link #invokeResolved} and propagates what it throws.
     */
    private static java.lang.reflect.Method resolveGained(String declarer, String member, ClassLoader cl) {
        try {
            int sp = member.indexOf(' ');
            String name = member.substring(0, sp);
            String desc = member.substring(sp + 1);
            Class<?> d = Class.forName(declarer.replace('/', '.'), false, cl);
            java.lang.reflect.Method m = d.getDeclaredMethod(name, paramClasses(desc, cl));
            try {
                m.setAccessible(true);
            } catch (RuntimeException e) {
                // strong encapsulation (jdk9+ denies deep internals) — invoke may still work
            }
            return m;
        } catch (Throwable e) {
            // declarer unresolvable/absent on this runtime — stub next
            return null;
        }
    }

    /** invoke a resolved member reflectively — the member's own exceptions propagate unfiltered */
    static Object invokeResolved(java.lang.reflect.Method m, Object self, Object[] args) throws Throwable {
        try {
            return m.invoke(self, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * invoke a self-first static stub site {@code owner.name(desc)} — api
     * classes resolve in the caller's loader. A {@code D:} prefix marks the
     * site as a <em>dispatchable</em> instance stub: the receiver's own
     * member wins when declared, {@link DispatchRuntime#dispatchStub} runs
     * that resolution, the site after {@code D:} is the impl fallback.
     */
    private static Object invokeStub(String site, String member, Object self, Object[] args,
                                     ClassLoader cl) throws Throwable {
        if (site.startsWith("D:")) {
            return DispatchRuntime.dispatchStub(member, site.substring(2), self, args, cl);
        }
        return invokeStub(site, self, args, cl);
    }

    /**
     * Rewrites {@code xyz/wagyourtail/jvmdg/} object names inside {@code desc}
     * under the shade prefix {@code anchor} actually loaded under — a shaded
     * api jar relocates its own package, so descriptor names written against
     * the unshaded api (registered/site vocabulary) must be re-anchored to
     * resolve. Returns {@code desc} unchanged when {@code anchor} carries no
     * prefix.
     */
    static String apiPrefixed(String desc, Class<?> anchor) {
        String n = anchor.getName();
        int i = n.indexOf("xyz.wagyourtail.jvmdg.");
        if (i <= 0) {
            return desc;
        }
        return desc.replace("Lxyz/wagyourtail/jvmdg/",
            "L" + n.substring(0, i).replace('.', '/') + "xyz/wagyourtail/jvmdg/");
    }

    /** invoke a self-first static stub site {@code owner.name(desc)} — api classes resolve in the caller's loader */
    static Object invokeStub(String site, Object self, Object[] args, ClassLoader cl) throws Throwable {
        int p = site.indexOf('(');
        int dot = site.lastIndexOf('.', p);
        String owner = site.substring(0, dot);
        String name = site.substring(dot + 1, p);
        String desc = site.substring(p);
        Class<?> s = Class.forName(owner.replace('/', '.'), true, cl);
        // the api jar's declared signature differs per packaging: literal
        // first (unshaded jars keep api names verbatim), then shade-prefixed
        // (api types relocated under the run's prefix), then stand-in-mapped
        // (a downgraded jar rewrites types missing on this runtime)
        java.lang.reflect.Method m = null;
        try {
            m = s.getDeclaredMethod(name, paramClasses(desc, cl));
        } catch (Throwable literal) {
        }
        if (m == null) {
            String prefixed = apiPrefixed(desc, s);
            if (!prefixed.equals(desc)) {
                try {
                    m = s.getDeclaredMethod(name, paramClasses(prefixed, cl));
                } catch (Throwable shaded) {
                }
            }
        }
        if (m == null) {
            m = s.getDeclaredMethod(name,
                ReflectionTable.paramClasses(desc, s.getClassLoader()));
        }
        try {
            m.setAccessible(true);
        } catch (RuntimeException e) {
        }
        Object[] callArgs = new Object[args.length + 1];
        callArgs[0] = self;
        System.arraycopy(args, 0, callArgs, 1, args.length);
        return invokeResolved(m, null, callArgs);
    }

    /** descriptor parameter types, ASM-free — the runtime must not drag ASM into shaded output */
    static Class<?>[] paramClasses(String desc, ClassLoader cl) throws ClassNotFoundException {
        List<Class<?>> out = new ArrayList<>();
        for (int i = 1; desc.charAt(i) != ')'; i++) {
            switch (desc.charAt(i)) {
                case 'Z': out.add(boolean.class); break;
                case 'B': out.add(byte.class); break;
                case 'C': out.add(char.class); break;
                case 'S': out.add(short.class); break;
                case 'I': out.add(int.class); break;
                case 'J': out.add(long.class); break;
                case 'F': out.add(float.class); break;
                case 'D': out.add(double.class); break;
                case '[': {
                    int j = i;
                    while (desc.charAt(j) == '[') j++;
                    int end = desc.charAt(j) == 'L' ? desc.indexOf(';', j) : j;
                    out.add(Class.forName(desc.substring(i, end + 1).replace('/', '.'), false, cl));
                    i = end;
                    break;
                }
                case 'L': {
                    int end = desc.indexOf(';', i);
                    out.add(Class.forName(desc.substring(i + 1, end).replace('/', '.'), false, cl));
                    i = end;
                    break;
                }
                default: throw new IllegalArgumentException("bad descriptor " + desc);
            }
        }
        return out.toArray(new Class<?>[0]);
    }

    /** the member row {@code cls} resolves to — superclass chain first, each level's interface closure next */
    private static String gainedMemberName(Class<?> cls, Set<String> memberNames) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            String hit = memberNameHit(c.getName().replace('.', '/'), memberNames);
            if (hit != null) {
                return hit;
            }
            String i = gainedInterfaceMember(c, memberNames, new HashSet<Class<?>>());
            if (i != null) {
                return i;
            }
        }
        return null;
    }

    private static String gainedInterfaceMember(Class<?> c, Set<String> memberNames, Set<Class<?>> seen) {
        for (Class<?> i : c.getInterfaces()) {
            if (seen.add(i)) {
                String hit = memberNameHit(i.getName().replace('.', '/'), memberNames);
                if (hit != null) {
                    return hit;
                }
                String r = gainedInterfaceMember(i, memberNames, seen);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    /**
     * {@code n} as a member name, or the member name its {@link #MEMBER_OF}
     * retro names resolve to — stand-in and impl classes bridge to the
     * member their adapter serves.
     */
    private static String memberNameHit(String n, Set<String> memberNames) {
        if (memberNames.contains(n)) {
            return n;
        }
        Set<String> retro = MEMBER_OF.get(n);
        if (retro != null) {
            for (String r : retro) {
                if (memberNames.contains(r)) {
                    return r;
                }
            }
        }
        return null;
    }

    /**
     * {@code obj instanceof <retroElementType>[]} — the object must be an
     * array whose component type is itself a member (recursively, so
     * {@code T[][]} works). {@code null} is {@code false}.
     */
    public static boolean isArrayInstance(Object obj, String retroElementInternalName) {
        if (obj == null) {
            return false;
        }
        Class<?> component = obj.getClass().getComponentType();
        if (component == null) {
            return false;
        }
        while (component.isArray()) {
            component = component.getComponentType();
        }
        if (component.isPrimitive()) {
            return false;
        }
        return isMember(component, retroElementInternalName);
    }

    /**
     * {@code checkcast <retroType>} — returns {@code obj} (the transform
     * emits the remapped {@code CHECKCAST} after this for the verifier), or
     * throws {@link ClassCastException} with the JDK-style message. Passing
     * {@code null} returns {@code null}, matching the opcode.
     */
    public static Object cast(Object obj, String retroInternalName) {
        if (obj == null) {
            return null;
        }
        if (!isInstance(obj, retroInternalName)) {
            throw new ClassCastException(
                "class " + obj.getClass().getName() + " cannot be cast to class " +
                    retroInternalName.replace('/', '.'));
        }
        return obj;
    }

    /**
     * {@code checkcast} for an OWN_INTERFACE adapter — fast path for
     * {@link #wrapImpl}: resolves {@code <context>$jvmdg$retro.wrap} (the
     * per-adapter dispatcher genStubIndex generated into the api jar)
     * through {@code context}'s loader, caches the handle per loader, and
     * invokes it. The dispatcher answers for the retro type <em>its</em>
     * adapter was generated for, so the fast path only applies when the
     * named type's {@code wraps} row names {@code context} as its stub —
     * a mismatched context (e.g. an ancestor adapter passed for a child
     * retro type) falls to the table-driven {@link #wrapImpl}, which keys
     * on the name instead. {@code null} returns {@code null}.
     */
    public static Object wrap(Object obj, String retroInternalName, Class<?> context) {
        if (obj == null) {
            return null;
        }
        RESOURCES.get();
        WrapRow row = wrapRow(retroInternalName, context);
        if (row != null && row.stub.equals(context.getName().replace('.', '/'))) {
            Object disp = dispatcher(context);
            if (disp != NO_DISPATCHER) {
                try {
                    return ((MethodHandle) disp).invoke(obj);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return wrapImpl(obj, retroInternalName, context, row);
    }

    /**
     * Table-driven OWN_INTERFACE adaptation — like {@link #cast} plus
     * member <em>adaptation</em>: an object that is already adapter-typed
     * passes through; a member of a declared {@code impls} member type is
     * wrapped via that impl's single-arg ctor (declared dispatch order,
     * child adapters' impls tried before their ancestors'); any other
     * member passes through raw so the emitted trailing {@code CHECKCAST}
     * fails it; non-members throw {@link ClassCastException}. {@code null}
     * returns {@code null}. This is the fallback tier the generated
     * {@code $jvmdg$retro} dispatchers tail-call into (and what
     * {@link #wrap} uses when no dispatcher exists in the caller's
     * namespace).
     *
     * <p>{@code context} is the class the caller would cast to (the adapter
     * type) — its loader resolves impl classes so runtime-downgrading
     * loaders transform them like every other referenced api class, and its
     * name selects the {@code wraps} row belonging to the same jar
     * (multiple shaded/prefixed jars can each carry a row for the same
     * retro type).</p>
     */
    public static Object wrapImpl(Object obj, String retroInternalName, Class<?> context) {
        if (obj == null) {
            return null;
        }
        RESOURCES.get();
        return wrapImpl(obj, retroInternalName, context, wrapRow(retroInternalName, context));
    }

    private static Object wrapImpl(Object obj, String retroInternalName, Class<?> context, WrapRow row) {
        if (row != null) {
            if (NAMES.get(obj.getClass()).contains(row.stub)) {
                return obj;
            }
            for (int i = 0; i < row.members.length; i++) {
                Class<?> member = memberClass(row.members[i]);
                if (member != null && member.isInstance(obj)) {
                    return newImpl(row.impls[i], member, obj, retroInternalName, context);
                }
            }
        }
        if (isMember(obj.getClass(), retroInternalName)) {
            return obj;
        }
        throw new ClassCastException(
            "class " + obj.getClass().getName() + " cannot be cast to class " +
                retroInternalName.replace('/', '.'));
    }

    /**
     * {@code <context>$jvmdg$retro.wrap} handle for {@code context}'s
     * loader, or {@link #NO_DISPATCHER} — cached per (loader, dispatcher
     * binary name). The dispatcher name derives from the context class
     * itself, so a prefixed jar's adapter resolves its own prefixed
     * dispatcher and no resource row is needed for the lookup.
     */
    private static Object dispatcher(Class<?> context) {
        ClassLoader cl = context.getClassLoader();
        ClassLoader key = cl == null ? BOOTSTRAP_KEY : cl;
        Map<String, Object> cache = DISPATCHERS.get(key);
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
            Map<String, Object> raced = DISPATCHERS.putIfAbsent(key, cache);
            if (raced != null) {
                cache = raced;
            }
        }
        String name = context.getName() + "$jvmdg$retro";
        Object d = cache.get(name);
        if (d == null) {
            try {
                Class<?> dc = Class.forName(name, false, cl);
                d = MethodHandles.publicLookup().findStatic(dc, "wrap",
                    MethodType.methodType(Object.class, Object.class));
            } catch (Throwable e) {
                d = NO_DISPATCHER;
            }
            cache.putIfAbsent(name, d);
        }
        return d;
    }

    /**
     * The {@code wraps} row for {@code retroInternalName} whose {@code stub}
     * column names {@code context} — the same-jar guarantee when two
     * shaded/prefixed jars each carry a row for one retro type — else the
     * first row (externally-registered tables have no matching stub name).
     */
    private static WrapRow wrapRow(String retroInternalName, Class<?> context) {
        List<WrapRow> rows = WRAPS.get(retroInternalName);
        if (rows == null) {
            return null;
        }
        String stub = context.getName().replace('.', '/');
        for (WrapRow r : rows) {
            if (r.stub.equals(stub)) {
                return r;
            }
        }
        return rows.get(0);
    }

    /** JDK member class for a wrap pair — absent on this runtime → {@code null} (pair skipped) */
    private static Class<?> memberClass(String internalName) {
        Class<?> cached = MEMBER_CLASSES.get(internalName);
        if (cached == null) {
            try {
                cached = Class.forName(internalName.replace('/', '.'));
            } catch (ClassNotFoundException e) {
                return null;
            }
            MEMBER_CLASSES.put(internalName, cached);
        }
        return cached;
    }

    private static Object newImpl(String implInternalName, Class<?> member, Object obj, String retroInternalName, Class<?> context) {
        ClassLoader cl = context.getClassLoader();
        if (cl == null) {
            cl = RetroTypes.class.getClassLoader();
        }
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        Map<String, java.lang.reflect.Constructor<?>> ctors = IMPL_CTORS.get(cl);
        if (ctors == null) {
            ctors = new ConcurrentHashMap<>();
            Map<String, java.lang.reflect.Constructor<?>> raced = IMPL_CTORS.putIfAbsent(cl, ctors);
            if (raced != null) {
                ctors = raced;
            }
        }
        java.lang.reflect.Constructor<?> ctor = ctors.get(implInternalName);
        if (ctor == null) {
            try {
                ctor = Class.forName(implInternalName.replace('/', '.'), false, cl)
                    .getDeclaredConstructor(member);
                ctor.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                throw new ClassCastException(
                    "class " + obj.getClass().getName() + " cannot be cast to class " +
                        retroInternalName.replace('/', '.') + " (" + implInternalName.replace('/', '.') + " unresolvable)");
            }
            ctors.putIfAbsent(implInternalName, ctor);
        }
        try {
            return ctor.newInstance(obj);
        } catch (ReflectiveOperationException e) {
            throw new ClassCastException(
                "class " + obj.getClass().getName() + " cannot be cast to class " +
                    retroInternalName.replace('/', '.'));
        }
    }

    /**
     * {@code checkcast <retroElementType>[]} — as {@link #cast} for
     * array-typed targets.
     */
    public static Object castArray(Object obj, String retroElementInternalName) {
        if (obj == null) {
            return null;
        }
        if (!isArrayInstance(obj, retroElementInternalName)) {
            throw new ClassCastException(
                "class " + obj.getClass().getName() + " cannot be cast to class [" +
                    "L" + retroElementInternalName.replace('/', '.') + ";");
        }
        return obj;
    }

}
