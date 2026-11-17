package xyz.wagyourtail.jvmdg.compile.jar;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.compile.shade.ReferenceIndex;
import xyz.wagyourtail.jvmdg.compile.shade.ShadeContext;
import xyz.wagyourtail.jvmdg.compile.shade.ShadeRegistrar;
import xyz.wagyourtail.jvmdg.pipeline.Stage;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.pipeline.TransformContributor;
import xyz.wagyourtail.jvmdg.pipeline.TransformPipeline;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.DowngradeRegistrar;
import xyz.wagyourtail.jvmdg.version.PipelineRegistrar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whole-set class pipeline — the fused analogue of running
 * {@code ClassDowngrader.downgrade} per entry followed by a second
 * {@code ApiShader.shadeApis} pass. Every class entry is collected during
 * {@link #visitEntry}, then a single {@link TransformPipeline} runs at
 * {@link #endFused}: the {@link DowngradeRegistrar} union chain for the
 * observed input versions (when {@code ctx.downgrader} is present), followed
 * by the {@link ShadeRegistrar} stage list.
 * <p>
 * Slot routing reproduces the legacy two-pass flow:
 * <ul>
 *   <li>{@code META-INF/versions/<n>/**} entries that survived routing
 *       ({@code multiReleaseOriginal}) never enter the transform worklist —
 *       same-named root classes would collide with them. They seed the
 *       pipeline's variant channel instead, pinned to their jar slot so the
 *       closure barrier indexes and shades them in place.</li>
 *   <li>Everything else joins the primary class list. With a downgrader,
 *       mixed input versions share one run via {@code Stage#filter}; without
 *       one (pure shade of an already-downgraded jar) each node is pinned to
 *       its input slot since the version heuristic can't apply.</li>
 *   <li>{@code multiReleaseOriginal} input-state captures are seeded as
 *       deep-copied variants at their own class-file versions — the union
 *       chain can't distinguish an input boundary from an arrived-at
 *       version mid-chain.</li>
 * </ul>
 * Emission reproduces the legacy writer: finals at their jar-relative names,
 * variants at {@code META-INF/versions/<slot>/}, the same-name cross-version
 * byte dedup (versioned entries identical past the 8-byte header fall back
 * to the root copy), api classes from {@link ShadeContext#emitted}, and
 * resource bytes from {@link TransformContext#resources}.
 */
public class PipelineJarStep extends JarStep {

    private final String shadePrefix;
    private final ReferenceIndex apiIndex;
    private final Set<Type> apiClasses;
    private final List<Path> apiRoots;

    private final List<ClassNode> finals =
        Collections.synchronizedList(new ArrayList<ClassNode>());
    private final ConcurrentHashMap<Integer, List<ClassNode>> slotVariants =
        new ConcurrentHashMap<Integer, List<ClassNode>>();
    private final List<JarEntry> serviceEntries =
        Collections.synchronizedList(new ArrayList<JarEntry>());

    private ShadeContext shadeContext;
    private TransformContext transformContext;

    /**
     * A shading run — downgrades through {@code ctx.downgrader} when the
     * context carries one, shades always.
     */
    public PipelineJarStep(String shadePrefix, ReferenceIndex apiIndex, Set<Type> apiClasses, List<Path> apiRoots) {
        this.shadePrefix = shadePrefix;
        this.apiIndex = apiIndex;
        this.apiClasses = apiClasses;
        this.apiRoots = apiRoots;
    }

    /** The shade products of the last {@link #endFused} — null before it runs. */
    public ShadeContext shadeContext() {
        return shadeContext;
    }

    /** The pipeline context of the last {@link #endFused} — null before it runs. */
    public TransformContext transformContext() {
        return transformContext;
    }

    @Override
    public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
        if (!entry.name.endsWith(".class")) {
            if (entry.name.startsWith("META-INF/services/") && !entry.name.endsWith("/")) {
                // defer to endFused — provider lines need the shade remapper,
                // which doesn't exist until the closure barrier runs
                serviceEntries.add(entry);
                return null;
            }
            return entry;
        }
        ClassNode node = ASMUtils.bytesToClassNode(entry.bytes());
        int slot = slotOf(entry);
        if (slot != 0 && "module-info".equals(node.name)) {
            // legacy dropped versioned module-info entirely — it was never
            // scanned for api refs and only the root copy was rewritten
            return null;
        }
        if (slot != 0 && entry.name.startsWith("META-INF/versions/")) {
            // versioned entry: shaded in place at its slot, never downgraded
            List<ClassNode> list = slotVariants.computeIfAbsent(slot,
                k -> Collections.synchronizedList(new ArrayList<ClassNode>()));
            list.add(node);
        } else {
            finals.add(node);
        }
        return null;
    }

    @Override
    public void endFused(StepContext ctx) throws IOException {
        Flags flags = ctx.flags;
        TransformContext tctx = new TransformContext(
            ctx.downgrader != null ? ctx.downgrader.classpathIndex : null, flags, ctx.logger);
        this.transformContext = tctx;

        List<Stage> stages = new ArrayList<Stage>();
        if (ctx.downgrader != null) {
            // version-chain registrars — a future UpgradeRegistrar slots in
            // beside this one; TransformContributor anchors order the chains
            PipelineRegistrar downgrade = new DowngradeRegistrar(
                ctx.downgrader, false, memoized(ctx.extraRead));
            downgrade.install(tctx);
            Set<Integer> inputVersions = new LinkedHashSet<Integer>();
            List<String> preview = null;
            for (ClassNode node : finals) {
                if (Utils.isPreviewVersion(node.version)) {
                    if (preview == null) {
                        preview = new ArrayList<String>();
                    }
                    preview.add(node.name);
                }
                // the registrar routes on the major — collect it masked
                inputVersions.add(Utils.majorClassVersion(node.version));
            }
            if (preview != null && !flags.allowPreview) {
                throw new IllegalArgumentException("input contains " + preview.size() +
                    " preview-compiled classes (javac --enable-preview, minor 0xFFFF), " +
                    "e.g. " + preview.get(0) + " — rerun with --allowPreview to process them");
            }
            if (inputVersions.size() == 1) {
                stages.addAll(downgrade.stages(inputVersions.iterator().next()));
            } else {
                stages.addAll(downgrade.stages(inputVersions));
            }
        }

        ShadeRegistrar shade = new ShadeRegistrar(
            flags, ctx.logger, shadePrefix, apiIndex, apiClasses, apiRoots,
            new ReferenceIndex(ctx.logger, true, flags.shadeInlining));
        shade.install(tctx);
        this.shadeContext = shade.context();
        stages.addAll(shade.stages());

        // TransformContributor POST_EMIT — after the last assembled stage;
        // downgrader may be null on pure-shade runs
        TransformContributor.invoke(
            TransformContributor.Anchor.POST_EMIT, ctx.downgrader, stages);

        TransformPipeline pipeline = new TransformPipeline().addStages(stages);

        // seed variant channels before run: versioned entries pinned at
        // their slots; multiReleaseOriginal inputs deep-copied at their own
        // versions (the input-boundary capture the union chain can't do)
        for (Map.Entry<Integer, List<ClassNode>> e : slotVariants.entrySet()) {
            List<ClassNode> nodes = new ArrayList<ClassNode>(e.getValue());
            for (ClassNode node : nodes) {
                shade.context().pinPosition(node, e.getKey());
            }
            pipeline.variants().put(e.getKey(), nodes);
        }
        if (ctx.downgrader != null && flags.multiReleaseOriginal) {
            for (ClassNode node : finals) {
                ClassNode copy = TransformPipeline.copy(node);
                // mask minor bits: preview minors (0xFFFF) would otherwise pin
                // a garbage slot and mis-route the original
                putVariant(pipeline, Utils.classVersionToMajorVersion(copy.version), copy);
            }
        }

        // pure shade keeps input slots — finals pinned at 0 (their jar
        // position), since the version heuristic would misroute classes
        // above the flags target
        if (ctx.downgrader == null) {
            for (ClassNode node : finals) {
                shade.context().pinPosition(node, 0);
            }
        }

        List<ClassNode> out = pipeline.run(new ArrayList<ClassNode>(finals), tctx);

        // gather emitted input classes: name → (slot → bytes)
        Map<String, Map<Integer, byte[]>> byName = new TreeMap<String, Map<Integer, byte[]>>();
        for (ClassNode node : out) {
            putEmitted(byName, node, positionOf(shade.context(), node, flags));
        }
        for (Map.Entry<Object, List<ClassNode>> e : pipeline.variants().entrySet()) {
            for (ClassNode node : e.getValue()) {
                putEmitted(byName, node, positionOf(shade.context(), node, flags));
            }
        }
        flushClasses(byName, ctx.sink);

        for (Map.Entry<String, ClassNode> e : shade.context().emitted.entrySet()) {
            ctx.sink.putIfAbsent(e.getKey(),
                ASMUtils.classNodeToBytes(e.getValue(), ClassWriter.COMPUTE_MAXS));
        }
        // deferred input service files: remap provider/interface names through
        // the slot-0 prefix map, merging into any api-side copy of the same
        // service (one output file per interface — input providers lead)
        SimpleRemapper servicesRemapper = shade.context().remappers.get(0);
        for (JarEntry e : serviceEntries) {
            Map.Entry<String, byte[]> mapped = ApiShader.mapServiceEntry(
                e.name, e.bytes(), servicesRemapper, shade.context().apiClasses);
            if (mapped == null) {
                continue;
            }
            byte[] existing = tctx.resources().get(mapped.getKey());
            tctx.resources().put(mapped.getKey(), existing == null ? mapped.getValue()
                : ApiShader.mergeServices(mapped.getValue(), existing));
        }
        for (Map.Entry<String, byte[]> e : tctx.resources().entrySet()) {
            ctx.sink.put(e.getKey(), e.getValue());
        }
    }

    private void putVariant(TransformPipeline pipeline, int slot, ClassNode node) {
        List<ClassNode> list = pipeline.variants().get(slot);
        if (list == null) {
            list = new ArrayList<ClassNode>();
            pipeline.variants().put(slot, list);
        }
        list.add(node);
    }

    private void putEmitted(Map<String, Map<Integer, byte[]>> byName, ClassNode node, int slot) {
        Map<Integer, byte[]> versions = byName.get(node.name);
        if (versions == null) {
            versions = new TreeMap<Integer, byte[]>();
            byName.put(node.name, versions);
        }
        versions.put(slot, ASMUtils.classNodeToBytes(node, ClassWriter.COMPUTE_MAXS));
    }

    /**
     * Same-name cross-version dedup, ported from the legacy
     * {@code ClassDowngrader.downgrade} outputs filter: the slot-0 copy always
     * emits; each higher slot emits only when its bytes differ from the last
     * emitted slot past the 8-byte class header (magic + version).
     */
    private void flushClasses(Map<String, Map<Integer, byte[]>> byName, JarEntrySink sink) {
        for (Map.Entry<String, Map<Integer, byte[]>> entry : byName.entrySet()) {
            String name = entry.getKey();
            Map<Integer, byte[]> versions = entry.getValue();
            // versions/-only entries are legal (a class added in a later
            // JDK has no root copy) — emit the lowest present slot, then
            // each higher slot only when it differs past the header
            byte[] current = null;
            for (Map.Entry<Integer, byte[]> vs : versions.entrySet()) {
                int slot = vs.getKey();
                byte[] bytes = vs.getValue();
                if (current == null) {
                    current = bytes;
                    sink.put(slot == 0 ? name + ".class"
                        : "META-INF/versions/" + slot + "/" + name + ".class", bytes);
                } else if (!Utils.equals(current, 8, current.length, bytes, 8, bytes.length)) {
                    current = bytes;
                    sink.put("META-INF/versions/" + slot + "/" + name + ".class", bytes);
                }
            }
        }
    }

    private static int positionOf(ShadeContext shade, ClassNode node, Flags flags) {
        return shade.positionOf(node, flags);
    }

    /** The jar slot of an input entry: the routed slot when set, else parsed from {@code META-INF/versions/<n>/}. */
    private static int slotOf(JarEntry entry) {
        if (entry.slot != 0) {
            return entry.slot;
        }
        if (entry.name.startsWith("META-INF/versions/")) {
            int end = entry.name.indexOf('/', "META-INF/versions/".length());
            if (end < 0) {
                return 0;
            }
            try {
                return Integer.parseInt(entry.name.substring("META-INF/versions/".length(), end));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static Function<String, ClassNode> memoized(final Function<String, byte[]> extraRead) {
        final Map<String, Object> cache = new HashMap<String, Object>();
        final Object miss = new Object();
        return s -> {
            Object cached = cache.get(s);
            if (cached == null) {
                try {
                    byte[] out = extraRead.apply(s);
                    cached = out == null ? miss : (Object) ASMUtils.bytesToClassNode(out);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                cache.put(s, cached);
            }
            return cached == miss ? null : (ClassNode) cached;
        };
    }

}
