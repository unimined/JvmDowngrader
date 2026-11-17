package xyz.wagyourtail.jvmdg.version.idx;

import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * Reads {@code META-INF/jvmdg/member-since} — the {@code memberSince} data
 * {@link StubIndexWriter} computes from the JDK surface index at api-jar
 * build time — into a reflectively-scanned registry. Rows are
 * {@code provider-class-name<TAB>site-member<TAB>classVersion}; only this
 * provider's rows apply.
 * <p>
 * The idx path never reads this: its {@code stubs.idx} records carry
 * {@code memberSince} inline. The scan path builds annotations-only
 * entries, so loading the resource here is what keeps
 * {@code debugNoStubIdx} member classification (BEHAVIOR vs MISSING_API)
 * identical to the indexed load. A missing resource just means "no data" —
 * an older api jar.
 */
public final class DerivedMemberSince {

    public static final String RESOURCE = "META-INF/jvmdg/member-since";

    private DerivedMemberSince() {
    }

    /**
     * Populate {@code vp.stubRegistry.memberSince} from every
     * {@code member-since} resource visible to the provider's classloader —
     * multi-api-jar classpaths each contribute their own rows (a later row
     * for the same member wins on the lower window, matching
     * {@link StubIndexWriter#populateMemberSince}).
     */
    public static void apply(VersionProvider vp) throws IOException {
        String provider = vp.getClass().getName();
        ClassLoader cl = vp.getClass().getClassLoader();
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
                    String[] parts = line.split("\t", -1);
                    if (parts.length < 3 || !parts[0].equals(provider)) {
                        continue;
                    }
                    FullyQualifiedMemberNameAndDesc member = FullyQualifiedMemberNameAndDesc.of(parts[1]);
                    int since;
                    try {
                        since = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException nfe) {
                        continue;
                    }
                    Integer cur = vp.stubRegistry.memberSince.get(member);
                    if (cur == null || since < cur) {
                        vp.stubRegistry.memberSince.put(member, since);
                    }
                }
            }
        }
    }
}
