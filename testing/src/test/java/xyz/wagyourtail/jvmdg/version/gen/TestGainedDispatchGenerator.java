package xyz.wagyourtail.jvmdg.version.gen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link GainedDispatchGenerator}'s {@code @ReflectionReferences} rows —
 * the {@code gainedCall} fallback resolves its stub sites reflectively, so
 * the dispatcher declares them for shading's closure. The declared desc
 * must carry the provider-chain remap: the api index keys members in
 * post-remap (stand-in) vocabulary and a JDK-vocabulary ref silently
 * prunes the impl (the j8 corpus {@code Http.close} regression).
 */
public class TestGainedDispatchGenerator {

    @TempDir
    Path out;

    @Test
    public void reflectionRefDescsCarryStandInRemap() throws Exception {
        try (ClassDowngrader cd = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            List<VersionProvider> providers = cd.versionProviders(cd.maxVersion());
            for (VersionProvider p : providers) {
                p.ensureInit(cd);
            }
            GenContext ctx = new GenContext(providers, getClass().getClassLoader(), out, null);
            // a fictional interface: declared() consults the test classpath,
            // where the real api jar's dispatchers resolve and would be skipped
            ctx.gainedMethods = Collections.singletonMap("test/GainedIface",
                Collections.singleton("close ()V"));
            MethodRef site = new MethodRef(
                new FullyQualifiedMemberNameAndDesc(
                    Type.getObjectType("xyz/wagyourtail/jvmdg/j21/stub/java_net_http/J_N_H_HttpClient"),
                    "close", Type.getMethodType("(Ljava/net/http/HttpClient;)V")),
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, false, new int[0]);
            Map<String, Set<MethodRef>> byKey = new HashMap<>();
            byKey.put("close ()V", Collections.singleton(site));
            ctx.gainedSites = Collections.singletonMap("test/GainedIface", byKey);

            new GainedDispatchGenerator().generate(ctx);

            Path emitted = out.resolve("xyz/wagyourtail/jvmdg/gen/gained/T_GainedIface.class");
            assertTrue(Files.exists(emitted), "dispatcher emitted");
            ClassReader cr = new ClassReader(Files.readAllBytes(emitted));
            ClassNode node = new ClassNode();
            cr.accept(node, 0);

            AnnotationNode ref = null;
            for (MethodNode m : node.methods) {
                if (m.invisibleAnnotations == null) {
                    continue;
                }
                for (AnnotationNode a : m.invisibleAnnotations) {
                    if (!"Lxyz/wagyourtail/jvmdg/version/ReflectionReferences;".equals(a.desc)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    List<Object> refs = (List<Object>) a.values.get(1);
                    ref = (AnnotationNode) refs.get(0);
                }
            }
            assertNotNull(ref, "gainedCall sites declare @ReflectionReferences");
            assertEquals("Lxyz/wagyourtail/jvmdg/j21/stub/java_net_http/J_N_H_HttpClient;",
                ref.values.get(1), "impl owner stays the api class");
            assertEquals("close", ref.values.get(3));
            assertEquals("(Lxyz/wagyourtail/jvmdg/j11/stub/java_net_http/J_N_H_HttpClient;)V",
                ref.values.get(5),
                "java/net/http/HttpClient remaps to the j11 stand-in through the chain");
        }
    }
}
