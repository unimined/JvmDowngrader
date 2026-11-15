package xyz.wagyourtail.jvmdg.version.gen;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Inputs + emit sinks shared by the api-jar {@link ApiGenerator}s: the
 * post-{@code ensureInit} provider registries, the build classpath (for
 * hand-declared-wins checks — an authored class always beats a generated
 * one), the jar-root output dir for {@code .class} bytes and the
 * {@code .java} inspection-dump dir (not jar'd), plus the derivations more
 * than one generator shares — currently {@link #flattenedWraps()}, the
 * pull-up ordering both the {@code wraps} table and the {@code $jvmdg$retro}
 * dispatchers consume so the table and the bytecode can't drift.
 */
public final class GenContext {

    /** post-{@code ensureInit} providers — classStubs/member registries fully populated */
    public final List<VersionProvider> providers;

    private final ClassLoader classpath;
    private final Path classOut;
    private final Path sourceOut;
    private Map<String, FlatWrap> flatWraps;

    /**
     * gained-supertype interfaces → their callable member keys
     * ({@code "name desc"}) at the newest ct.sym window — set by
     * {@code StubIndexWriter} before {@link GainedDispatchGenerator} runs;
     * empty when no gained table was derived.
     */
    public Map<String, Set<String>> gainedMethods = Collections.emptyMap();

    /**
     * gained interface → member key → every stub site the member's
     * gained-call rows resolve to — declared on the generated dispatcher via
     * {@code @ReflectionReferences} because {@code RetroTypes.gainedCall}
     * reaches them reflectively, leaving no bytecode edge for the shading
     * closure to follow. Empty when no {@code gained-calls} table was
     * derived.
     */
    public Map<String, Map<String, Set<MethodRef>>> gainedSites = Collections.emptyMap();

    /**
     * @param classpath resolves generated-name collisions ({@link #declared})
     * @param classOut  jar-root output dir for {@code .class} bytes (the
     *                  {@code genStubIndex} dir — {@code $Marker}s already
     *                  land here); may be null only if every generator is skipped
     * @param sourceOut inspection-dump dir for {@code .java} mirrors, or null to skip dumps
     */
    public GenContext(List<VersionProvider> providers, ClassLoader classpath, Path classOut, Path sourceOut) {
        this.providers = providers;
        this.classpath = classpath;
        this.classOut = classOut;
        this.sourceOut = sourceOut;
    }

    /**
     * An authored class with this internal name exists on the build
     * classpath — hand-declared always wins, so generators skip emitting.
     */
    public boolean declared(String internalName) {
        return classpath.getResource(internalName + ".class") != null;
    }

    /**
     * Writes {@code internalName.class} into the jar-root output dir and
     * its {@code javaDump} mirror into the dump dir (skipped when either
     * the dump text or the dump dir is null). {@code .java} output is
     * inspection-only — the classfile is authoritative.
     */
    public void emit(String internalName, ClassNode node, String javaDump) throws IOException {
        Path p = classOut.resolve(internalName + ".class");
        Files.createDirectories(p.getParent());
        ClassWriter cw = new ClassWriter(0);
        node.accept(cw);
        Files.write(p, cw.toByteArray());
        if (sourceOut != null && javaDump != null) {
            Path sp = sourceOut.resolve(internalName + ".java");
            Files.createDirectories(sp.getParent());
            Files.write(sp, javaDump.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * OWN_INTERFACE adaptation rows flattened over the adapter interface
     * graph — one {@link FlatWrap} per retro source that has contributors:
     * impls declared on a child adapter also serve its ancestors (the child
     * impl is assignable to them), so each adapter declares only its new
     * wrappers and a descendant declaration flows upward. Dispatch order
     * puts the most specific adapter's impls first (deepest interface path
     * to the queried adapter, name tiebreak), the queried adapter's own
     * {@code impls} last; first declaration wins on duplicate member types.
     * This is the single ordering both the {@code META-INF/jvmdg/wraps}
     * table and the generated dispatchers consume.
     */
    public Map<String, FlatWrap> flattenedWraps() {
        if (flatWraps == null) {
            flatWraps = flatten(providers);
        }
        return flatWraps;
    }

    /** one flattened row: the adapter (stub column) + ordered {@code [member, impl]} pairs */
    public static final class FlatWrap {
        public final String stub;
        public final List<String[]> pairs;

        FlatWrap(String stub, List<String[]> pairs) {
            this.stub = stub;
            this.pairs = pairs;
        }
    }

    private static Map<String, FlatWrap> flatten(List<VersionProvider> providers) {
        Map<String, Class<?>> adapter = new TreeMap<>();
        Map<String, String> stub = new HashMap<>();
        Map<String, Adapter.Mode> mode = new HashMap<>();
        Map<String, List<String[]>> own = new HashMap<>();
        for (VersionProvider vp : providers) {
            for (Map.Entry<Type, ClassStubEntry> e : vp.classStubs.entrySet()) {
                String src = e.getKey().getInternalName();
                adapter.put(src, e.getValue().clazz());
                stub.put(src, e.getValue().stubType.getInternalName());
                mode.put(src, e.getValue().mode);
                List<String[]> impls = vp.adapterImpls.get(src);
                if (impls != null) {
                    own.put(src, impls);
                }
            }
        }
        Map<String, FlatWrap> out = new LinkedHashMap<>();
        if (own.isEmpty()) {
            return out;
        }
        for (Map.Entry<String, Class<?>> a : adapter.entrySet()) {
            if (mode.get(a.getKey()) != Adapter.Mode.OWN_INTERFACE) {
                continue;
            }
            List<Map.Entry<String, Integer>> contributors = new ArrayList<>();
            for (Map.Entry<String, Class<?>> d : adapter.entrySet()) {
                if (!own.containsKey(d.getKey())) {
                    continue;
                }
                int depth = adapterDepth(d.getValue(), a.getValue());
                if (depth >= 0) {
                    contributors.add(new AbstractMap.SimpleEntry<>(d.getKey(), depth));
                }
            }
            if (contributors.isEmpty()) {
                continue;
            }
            Collections.sort(contributors, new Comparator<Map.Entry<String, Integer>>() {
                @Override
                public int compare(Map.Entry<String, Integer> x, Map.Entry<String, Integer> y) {
                    int c = y.getValue() - x.getValue();
                    return c != 0 ? c : x.getKey().compareTo(y.getKey());
                }
            });
            Set<String> seen = new HashSet<>();
            List<String[]> pairs = new ArrayList<>();
            for (Map.Entry<String, Integer> c : contributors) {
                for (String[] pair : own.get(c.getKey())) {
                    if (seen.add(pair[0])) {
                        pairs.add(pair);
                    }
                }
            }
            out.put(a.getKey(), new FlatWrap(stub.get(a.getKey()), pairs));
        }
        return new TreeMap<>(out);
    }

    /**
     * Interface-path distance {@code from} → {@code to} over the adapter
     * hierarchy ({@code to.isAssignableFrom(from)} as a BFS depth), or
     * {@code -1} when {@code from} doesn't extend {@code to}.
     */
    private static int adapterDepth(Class<?> from, Class<?> to) {
        if (from.equals(to)) {
            return 0;
        }
        Deque<Class<?>> walk = new ArrayDeque<>();
        Set<Class<?>> seen = new HashSet<>();
        walk.add(from);
        int depth = 0;
        while (!walk.isEmpty()) {
            depth++;
            for (int size = walk.size(); size > 0; size--) {
                Class<?> c = walk.removeFirst();
                for (Class<?> i : c.getInterfaces()) {
                    if (seen.add(i)) {
                        if (i.equals(to)) {
                            return depth;
                        }
                        walk.add(i);
                    }
                }
                Class<?> sup = c.getSuperclass();
                if (sup != null && seen.add(sup)) {
                    if (sup.equals(to)) {
                        return depth;
                    }
                    walk.add(sup);
                }
            }
        }
        return -1;
    }

}
