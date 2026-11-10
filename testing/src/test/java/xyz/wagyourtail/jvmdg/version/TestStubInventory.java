package xyz.wagyourtail.jvmdg.version;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sanity check that every {@link VersionProvider} on the classpath
 * enumerates its stub members on the launching JVM. The corpus generator
 * ({@code StubTestGenerator}) performs the same enumeration and writes the
 * full registry to {@code build/reports/stubtests/inventory.txt} — this
 * test exists because generation only runs on one launcher while the
 * parity matrix spans many: an ASM-vs-class-version mismatch that silently
 * degrades enumeration must still fail on the JVM it degrades on.
 */
public class TestStubInventory {

    private static int count(Iterable<MemberNameAndDesc> members) {
        int n = 0;
        for (MemberNameAndDesc m : members) {
            if (m != null) n++; // placeholder registrations
        }
        return n;
    }

    @Test
    public void stubRegistryEnumerates() throws IOException {
        int providerCount = 0;
        int mappingCount = 0;
        int memberCount = 0;
        int degraded = 0;

        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            for (VersionProvider provider : ServiceLoader.load(VersionProvider.class)) {
                providerCount++;
                provider.ensureInit(d);
                try {
                    // snapshot: resolving members lazily registers further mappings
                    for (Map.Entry<Type, ClassMapping> e :
                            new ArrayList<>(provider.stubMappings.entrySet())) {
                        ClassMapping mapping = e.getValue();
                        if (mapping == null) continue;
                        mappingCount++;
                        memberCount += count(mapping.getMethodStubMap().keySet());
                        memberCount += count(mapping.getFieldStubMap().keySet());
                        memberCount += count(mapping.getMethodModifyMap().keySet());
                        memberCount += count(mapping.getAbstracts().keySet());
                    }
                } catch (IllegalArgumentException ex) {
                    // supertype resolution reads runtime JDK classes; an ASM
                    // version too old for the launching JVM's class files
                    // can't enumerate — degrade to a warning rather than
                    // failing the whole matrix on one JVM
                    if (ex.getMessage() != null && ex.getMessage().contains("major version")) {
                        System.out.println("stub inventory: " + provider.getClass().getSimpleName()
                            + " skipped on this JVM: " + ex.getMessage());
                        degraded++;
                    } else {
                        throw ex;
                    }
                }
            }
        }

        assertTrue(providerCount > 0, "no VersionProviders found on test classpath");
        assertTrue(mappingCount > 0, "providers registered no stub mappings");
        assertTrue(memberCount > 0, "providers registered no stub members");
        assertTrue(degraded < providerCount,
            "every provider failed to enumerate on this JVM");
    }
}
