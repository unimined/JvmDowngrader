package jvmdg.featuretests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shared result protocol + probe dispatcher for the feature corpus. Mirrors
 * {@code jvmdg.stubtests.Support}: lines are {@code <id> RESULT:<norm>} or
 * {@code <id> EXC:<exception-class-name>} so native and downgraded/shaded runs
 * diff line-for-line in DowngradeTests.
 *
 * <p>{@link #run(Class)} is called from each manual corpus class's
 * {@code main}: it invokes every {@link FeatureTest}-annotated method in name
 * order with the id {@code <SimpleName>.<method>}. Probes report via
 * {@link #print}/{@link #exc} or plain stdout; uncaught throwables become an
 * {@code EXC} line instead of crashing the frame.</p>
 */
public final class Support {

    private Support() {
    }

    /**
     * Invokes every {@link FeatureTest} method on {@code corpus}, sorted by
     * method name for deterministic output order. A probe throwing any
     * throwable is reported as {@code <id> EXC:<class>} and the run continues.
     */
    public static void run(Class<?> corpus) {
        List<Method> probes = new ArrayList<>();
        for (Method m : corpus.getDeclaredMethods()) {
            if (m.getAnnotation(FeatureTest.class) != null) {
                probes.add(m);
            }
        }
        probes.sort(Comparator.comparing(Method::getName));
        for (Method m : probes) {
            String id = corpus.getSimpleName() + "." + m.getName();
            try {
                Object recv = null;
                if ((m.getModifiers() & Modifier.STATIC) == 0) {
                    java.lang.reflect.Constructor<?> ctor = corpus.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    recv = ctor.newInstance();
                }
                m.setAccessible(true);
                if (m.getParameterCount() == 0) {
                    m.invoke(recv);
                } else {
                    m.invoke(recv, id);
                }
            } catch (InvocationTargetException e) {
                exc(id, e.getCause() == null ? e : e.getCause());
            } catch (Throwable t) {
                exc(id, t);
            }
        }
    }

    public static void print(String id, Object r) {
        System.out.println(id + " RESULT:" + norm(r));
    }

    public static void exc(String id, Throwable t) {
        System.out.println(id + " EXC:" + t.getClass().getName());
    }

    /**
     * Same normalization as the stub-parity corpus: masks identity hashes,
     * implementation class names, thread ids and other environment-dependent
     * detail so verdicts compare across JVMs and transform modes. Escapes in
     * replacement strings matter: literal {@code $} replacements must be
     * written as {@code \\$}.
     */
    public static String norm(Object r) {
        if (r == null) return "null";
        Class<?> c = r.getClass();
        if (c.isArray()) {
            StringBuilder sb = new StringBuilder("[");
            int n = java.lang.reflect.Array.getLength(r);
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(", ");
                sb.append(norm(java.lang.reflect.Array.get(r, i)));
            }
            return sb.append(']').toString();
        }
        if (r instanceof java.util.Set) {
            java.util.List<String> items = new java.util.ArrayList<>();
            for (Object e : (java.util.Set<?>) r) items.add(norm(e));
            java.util.Collections.sort(items);
            return "{" + String.join(", ", items) + "}";
        }
        if (r instanceof java.util.Map) {
            java.util.List<String> items = new java.util.ArrayList<>();
            for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) r).entrySet()) {
                items.add(norm(e.getKey()) + "=" + norm(e.getValue()));
            }
            java.util.Collections.sort(items);
            return "{" + String.join(", ", items) + "}";
        }
        return String.valueOf(r)
            .replace("CompletableFuture$MinimalStage", "CompletableFuture")
            .replaceAll("[\\w.$/]+\\$[\\w.$/]*", "IMPL")
            .replaceAll("[\\w.$/]*xyz\\.wagyourtail\\.jvmdg\\.[\\w.$/]+", "IMPL")
            .replaceAll("jdk\\.internal\\.[\\w.$/]+", "IMPL")
            .replaceAll("@[0-9a-fA-F]+", "@#")
            .replaceAll("#[0-9]+", "#")
            .replaceAll("CollectorImpl\\[.*\\]", "IMPL@#")
            .replaceAll("\\[Wrapped task = [^\\]]*\\]", "")
            .replaceAll("VirtualThread\\[[^\\]]*\\]/\\w+(@\\S+)?", "VirtualThread[#]")
            .replaceAll("Thread\\[[^\\]]*\\]", "Thread[#]")
            .replaceAll(", [0-9]+ dependents", "")
            .replaceAll(", delayed = [0-9]+", "")
            .replaceAll("\\[(Completed|Not completed)[^\\]]*\\]", "[$1]")
            .replaceAll("\\[Completed exceptionally[^\\]]*\\]", "[Completed exceptionally]")
            .replaceAll("steals = [0-9]+", "steals = #")
            .replaceAll("\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b", "UUID#")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}
