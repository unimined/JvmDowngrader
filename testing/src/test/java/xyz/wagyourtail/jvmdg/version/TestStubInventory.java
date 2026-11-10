package xyz.wagyourtail.jvmdg.version;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enumerates every stub member registered by every {@link VersionProvider}
 * on the classpath and writes the full inventory to
 * {@code testing/unexercised.txt} (sorted, one member per line).
 *
 * <p>This file is the ratchet baseline for stub parity coverage: entries are
 * removed as behavioral tests are added that exercise each member. Lines are
 * formatted as {@code <provider> <owner>.<name><desc> [<kind>]} so they can be
 * grepped and diffed.</p>
 */
public class TestStubInventory {

    // the file lives at <repo>/testing/unexercised.txt; the Gradle test workdir
    // is the testing module dir, so also try the bare name
    private static final List<Path> OUTPUT_CANDIDATES = java.util.Arrays.asList(
        Paths.get("unexercised.txt"),
        Paths.get("testing", "unexercised.txt"));
    private static final Path OUTPUT = OUTPUT_CANDIDATES.stream()
        .filter(Files::exists).findFirst()
        .orElse(OUTPUT_CANDIDATES.get(0));

    private static void addAll(List<String> lines, String src, String owner,
                               Iterable<MemberNameAndDesc> members, String kind) {
        for (MemberNameAndDesc member : members) {
            if (member == null) continue; // placeholder registrations
            char sep = "field-stub".equals(kind) ? ':' : '.';
            lines.add(src + " " + owner + sep + member.getName() + member.getDesc() + " [" + kind + "]");
        }
    }

    @Test
    public void writeStubInventory() throws IOException {
        List<String> lines = new ArrayList<>();
        int providerCount = 0;
        int mappingCount = 0;

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
                        String owner = e.getKey().getInternalName();
                        String src = provider.getClass().getSimpleName();
                        addAll(lines, src, owner, mapping.getMethodStubMap().keySet(), "method-stub");
                        addAll(lines, src, owner, mapping.getFieldStubMap().keySet(), "field-stub");
                        addAll(lines, src, owner, mapping.getMethodModifyMap().keySet(), "modify");
                        addAll(lines, src, owner, mapping.getAbstracts().keySet(), "abstract");
                    }
                } catch (IllegalArgumentException ex) {
                    // supertype resolution reads runtime JDK classes; an ASM version
                    // too old for the launching JVM's class files can't enumerate
                    if (ex.getMessage() != null && ex.getMessage().contains("major version")) {
                        lines.add("? " + provider.getClass().getSimpleName()
                            + " skipped: " + ex.getMessage());
                    } else {
                        throw ex;
                    }
                }
            }
        }

        assertTrue(providerCount > 0, "no VersionProviders found on test classpath");
        assertTrue(mappingCount > 0, "providers registered no stub mappings");

        Collections.sort(lines);
        Path out = OUTPUT.toAbsolutePath();
        boolean degraded = lines.stream().anyMatch(l -> l.startsWith("? "));
        if (degraded) {
            // incomplete enumeration — keep the existing baseline intact
            System.out.println("stub inventory: incomplete on this JVM (ASM vs class-version), "
                + "leaving " + out + " untouched");
        } else {
            try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                w.write("# registered stub members (ratchet baseline)\n");
                w.write("# format: <provider> <owner>.<name><desc> [<kind>]\n");
                for (String line : lines) {
                    w.write(line);
                    w.write('\n');
                }
            }
        }
        assertTrue(lines.size() > 0, "stub inventory is empty");
        System.out.println("stub inventory: " + lines.size() + " members across "
            + mappingCount + " types from " + providerCount + " providers -> " + out);
    }
}
