package xyz.wagyourtail.jvmdg.version.idx;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.version.StubCatalog;
import xyz.wagyourtail.jvmdg.version.map.FieldStubEntry;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Reads {@code META-INF/jvmdg/excludes} — the derived {@code excludeChild}
 * data {@link StubIndexWriter} computes from the JDK type graph at api-jar
 * build time — and unions it into a reflectively-scanned registry. Rows are
 * {@code provider-class-name<TAB>site-member<TAB>child,child,…}; only this
 * provider's rows apply.
 * <p>
 * The idx path never reads this: its {@link StubEntry}s already carry the
 * union baked in by the writer. The scan path builds annotations-only
 * entries, so applying the resource here is what keeps
 * {@code debugNoStubIdx} resolution identical to the indexed load. A
 * missing resource just means "no derivations" — an older api jar.
 */
public final class DerivedExclusions {

    public static final String RESOURCE = "META-INF/jvmdg/excludes";

    /**
     * Union the provider's resource rows into every matching registered
     * {@link StubEntry}. Site keys are the canonical
     * {@link FullyQualifiedMemberNameAndDesc} form the writer emits.
     */
    public static void apply(StubCatalog catalog) throws IOException {
        Map<String, List<String>> rows = read(catalog);
        if (rows.isEmpty()) {
            return;
        }
        for (Map.Entry<String, xyz.wagyourtail.jvmdg.version.map.StubRegistry.Owner> o : catalog.stubRegistry.owners().entrySet()) {
            apply(o.getValue().methodStub, rows, Type.getObjectType(o.getKey()));
            for (Map.Entry<MemberNameAndDesc, FieldStubEntry> m : o.getValue().fieldStub.entrySet()) {
                List<String> children = match(rows, m.getKey(), Type.getObjectType(o.getKey()));
                if (children == null) {
                    continue;
                }
                FieldStubEntry f = m.getValue();
                if (f.getter != null) {
                    f = f.withGetter(f.getter.withExcludeChild(union(f.getter.excludeChild, children)));
                }
                if (f.setter != null) {
                    f = f.withSetter(f.setter.withExcludeChild(union(f.setter.excludeChild, children)));
                }
                m.setValue(f);
            }
        }
    }

    private static void apply(Map<MemberNameAndDesc, StubEntry> methodStub, Map<String, List<String>> rows, Type owner) {
        for (Map.Entry<MemberNameAndDesc, StubEntry> m : methodStub.entrySet()) {
            List<String> children = match(rows, m.getKey(), owner);
            if (children != null) {
                m.setValue(m.getValue().withExcludeChild(union(m.getValue().excludeChild, children)));
            }
        }
    }

    private static List<String> match(Map<String, List<String>> rows, MemberNameAndDesc member, Type owner) {
        return rows.get(member.toFullyQualified(owner).toString());
    }

    private static String[] union(String[] manual, List<String> derived) {
        TreeSet<String> out = new TreeSet<>(Arrays.asList(manual));
        out.addAll(derived);
        return out.toArray(new String[0]);
    }

    /**
     * This provider's rows across every {@code excludes} resource on its
     * classloader, merged — multi-api-jar classpaths each contribute their
     * own section.
     */
    private static Map<String, List<String>> read(StubCatalog catalog) throws IOException {
        Map<String, List<String>> rows = new HashMap<>();
        String provider = catalog.provider.providerKey();
        ClassLoader cl = catalog.provider.getClass().getClassLoader();
        Enumeration<URL> resources = cl == null
            ? ClassLoader.getSystemResources(RESOURCE)
            : cl.getResources(RESOURCE);
        while (resources.hasMoreElements()) {
            try (InputStream is = Utils.openUncached(resources.nextElement());
                 BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty() || line.charAt(0) == '#') {
                        continue;
                    }
                    String[] parts = line.split("\t");
                    if (parts.length != 3 || !parts[0].equals(provider)) {
                        continue;
                    }
                    List<String> children = rows.computeIfAbsent(parts[1], k -> new ArrayList<String>());
                    for (String child : parts[2].split(",")) {
                        if (!child.isEmpty() && !children.contains(child)) {
                            children.add(child);
                        }
                    }
                }
            }
        }
        return rows;
    }

    private DerivedExclusions() {
    }
}
