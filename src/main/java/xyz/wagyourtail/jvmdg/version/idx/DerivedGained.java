package xyz.wagyourtail.jvmdg.version.idx;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.StubCatalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Reads {@code META-INF/jvmdg/gained} (+ the hand-authored
 * {@code META-INF/jvmdg/gained-manual} union) — the gained-supertype rows
 * {@link StubIndexWriter} derives from the JDK type graph — and filters
 * them into {@link VersionProvider#gained}.
 * <p>
 * Rows are {@code interface<TAB>interface-min<TAB>member:since,…} in
 * class-file versions. An interface is gained <em>for this provider</em>
 * when the interface itself is real at the <em>final</em> downgrade
 * target and at least one member's binding crosses the boundary
 * ({@code output < since ≤ input}) — the boundary-crossing provider owns
 * the site rewrite, so a class sees its gained sites rewritten exactly
 * once at the step whose output drops below the binding version. The
 * final-target gate matters: a boundary whose own output still carries
 * {@code I} can sit above a later step where an adapter/stub takes
 * {@code I} over — claimed sites embed the real {@code I} in emitted
 * bytecode (and dispatchers {@code instanceof}/{@code implements} it), so
 * routing them here would corrupt the desc on a later remap and force
 * {@code NoClassDefFoundError} at runtime. Rows outside the window are
 * ignored here; the full {@code since} map is what the runtime table
 * reads (emitted sites evaluate membership at their own authoring-version
 * ceiling).
 * <p>
 * Unlike {@link DerivedExclusions} this is provider-level data, not
 * per-entry data — both load paths read the same resource, so idx and
 * scan resolve identically by construction. A missing resource just means
 * "no gained supertypes" — an older api jar.
 */
public final class DerivedGained {

    public static final String RESOURCE = "META-INF/jvmdg/gained";

    /** hand-authored override rows — same format, unioned over the derived table */
    public static final String MANUAL_RESOURCE = "META-INF/jvmdg/gained-manual";

    /**
     * classpath location of the resolved {@code (I,member,T) → impl site}
     * table — column 2 of each row is a {@code "name desc"} member key;
     * the keys per interface are the generated dispatcher's method
     * universe and the transform-side gate for {@code invokeinterface}
     * gained routing.
     */
    public static final String CALLS_RESOURCE = "META-INF/jvmdg/gained-calls";

    /** hand-authored gained-call rows — same format, unioned */
    public static final String CALLS_MANUAL_RESOURCE = "META-INF/jvmdg/gained-calls-manual";

    /**
     * Populate {@link VersionProvider#gained} from this provider's
     * classloader resources: {@code I → member internal names} whose
     * binding crosses this provider's boundary. Also populates
     * {@link VersionProvider#gainedMembers} — the per-interface member
     * keys of {@code gained-calls} (+manual), the dispatcher's method
     * universe.
     */
    public static void apply(StubCatalog catalog) throws IOException {
        Map<String, Integer> imins = new HashMap<>();
        Map<String, Map<String, Integer>> rows = new TreeMap<>();
        read(catalog, RESOURCE, imins, rows);
        read(catalog, MANUAL_RESOURCE, imins, rows);
        int target = catalog.provider.downgrader.flags.classVersion;
        for (Map.Entry<String, Map<String, Integer>> e : rows.entrySet()) {
            Integer imin = imins.get(e.getKey());
            if (imin != null && imin > target) {
                continue; // the interface is absent at the final target — a lower boundary's adapter/stub owns it
            }
            for (Map.Entry<String, Integer> t : e.getValue().entrySet()) {
                int since = t.getValue();
                if (since > catalog.provider.outputVersion && since <= catalog.provider.inputVersion) {
                    catalog.gained.computeIfAbsent(e.getKey(), k -> new HashSet<String>()).add(t.getKey());
                }
            }
        }
        readCallMembers(catalog, CALLS_RESOURCE);
        readCallMembers(catalog, CALLS_MANUAL_RESOURCE);
    }

    /**
     * Fold one gained-calls resource into the transform-side tables:
     * {@link VersionProvider#gainedMembers} gets every {@code "name desc"}
     * a dispatcher can serve (un-windowed since the dispatcher class covers
     * all ceilings), and {@link VersionProvider#gainedStubSites} gets the
     * {@code >stubSite} impls each member's rows can resolve to — the
     * runtime reaches them reflectively, so indy emission must declare
     * them for shading's closure. A missing resource just means no gained
     * calls (older api jar).
     */
    private static void readCallMembers(StubCatalog catalog, String resource) throws IOException {
        ClassLoader cl = catalog.provider.getClass().getClassLoader();
        Enumeration<URL> resources = cl == null
            ? ClassLoader.getSystemResources(resource)
            : cl.getResources(resource);
        while (resources.hasMoreElements()) {
            try (InputStream is = Utils.openUncached(resources.nextElement());
                 BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty() || line.charAt(0) == '#') {
                        continue;
                    }
                    String[] parts = line.split("\t");
                    if (parts.length < 2) {
                        continue;
                    }
                    catalog.gainedMembers.computeIfAbsent(parts[0], k -> new HashSet<String>()).add(parts[1]);
                    if (parts.length < 3) {
                        continue;
                    }
                    // each T= entry resolves per-ceiling as
                    // ver:D|ver:D>stubSite|ver:-; collect every stub site
                    // the member can fall back to (the D: prefix marks a
                    // dispatchable impl — the ref wants the method itself)
                    for (String entry : parts[2].split(",")) {
                        int eq = entry.indexOf('=');
                        if (eq < 0) {
                            continue;
                        }
                        for (String res : entry.substring(eq + 1).split("\\|")) {
                            int gt = res.indexOf('>');
                            if (gt < 0) {
                                continue;
                            }
                            String site = res.substring(gt + 1);
                            if (site.startsWith("D:")) {
                                site = site.substring(2);
                            }
                            catalog.gainedStubSites.computeIfAbsent(parts[0], k -> new HashMap<String, Set<String>>())
                                .computeIfAbsent(parts[1], k -> new HashSet<String>()).add(site);
                        }
                    }
                }
            }
        }
    }

    /**
     * Fold one resource's rows into {@code imins}/{@code rows}, merged
     * across every copy on the provider's classloader — multi-api-jar
     * classpaths each contribute their own gained table.
     */
    private static void read(StubCatalog catalog, String resource,
                             Map<String, Integer> imins, Map<String, Map<String, Integer>> rows) throws IOException {
        ClassLoader cl = catalog.provider.getClass().getClassLoader();
        Enumeration<URL> resources = cl == null
            ? ClassLoader.getSystemResources(resource)
            : cl.getResources(resource);
        while (resources.hasMoreElements()) {
            try (InputStream is = Utils.openUncached(resources.nextElement());
                 BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty() || line.charAt(0) == '#') {
                        continue;
                    }
                    String[] parts = line.split("\t");
                    if (parts.length != 3) {
                        continue;
                    }
                    String iface = parts[0];
                    Integer prev = imins.get(iface);
                    int imin = Integer.parseInt(parts[1]);
                    if (prev == null || imin < prev) {
                        imins.put(iface, imin);
                    }
                    Map<String, Integer> members = rows.computeIfAbsent(iface, k -> new TreeMap<String, Integer>());
                    for (String m : parts[2].split(",")) {
                        int c = m.lastIndexOf(':');
                        if (c <= 0) {
                            continue;
                        }
                        String member = m.substring(0, c);
                        int since = Integer.parseInt(m.substring(c + 1));
                        Integer cur = members.get(member);
                        if (cur == null || since < cur) {
                            members.put(member, since);
                        }
                    }
                }
            }
        }
    }

    private DerivedGained() {
    }
}
