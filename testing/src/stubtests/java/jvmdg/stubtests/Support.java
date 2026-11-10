package jvmdg.stubtests;

/**
 * Shared result protocol for the stub-parity corpus. Generated per-owner
 * classes and hand-written {@code manual.*} tests both report through here so
 * native and downgraded/shaded runs diff line-for-line in StubParityTests.
 *
 * <p>Lines are {@code <id> RESULT:<norm>} or {@code <id> EXC:<exception-class-name>}.
 * {@code norm} masks identity hashes, implementation class names, thread ids and
 * other environment-dependent detail so verdicts compare across JVMs. Escapes
 * in replacement strings matter: {@code $} in a regex replacement is a group
 * reference on every JDK — {@code $$} is not reliably supported on JDK 8 — so
 * literal {@code $} replacements must be written as {@code \\$}.</p>
 */
public final class Support {

    private Support() {
    }

    public static void print(String id, Object r) {
        System.out.println(id + " RESULT:" + norm(r));
    }

    public static void exc(String id, Throwable t) {
        System.out.println(id + " EXC:" + t.getClass().getName());
    }

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
        // Set and Map iteration order is contract-unspecified and JDK impls
        // salt it per JVM — render sorted so ordering can't flake the diff
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
            // CompletableFuture$MinimalStage is a JDK impl alias for CompletableFuture
            .replace("CompletableFuture$MinimalStage", "CompletableFuture")
            // implementation-detail inner/synthetic class names differ across
            // JDKs and stub impls ($1, $Lambda/0x..., X$NamedImpl all fold here);
            // keep it ahead of the @/# hash rules so 'X$1@ab12' -> 'IMPL@#'
            .replaceAll("[\\w.$/]+\\$[\\w.$/]*", "IMPL")
            // jvmdg's own named impl classes stand in for JDK-internal impls
            // (e.g. j24 CharSequenceReader for Reader.of's anonymous class);
            // tolerate the api-shader relocation prefix
            .replaceAll("[\\w.$/]*xyz\\.wagyourtail\\.jvmdg\\.[\\w.$/]+", "IMPL")
            // real JDK-internal impl class names (jdk.internal.net.http.* and
            // friends) are equally unrepresentable
            .replaceAll("jdk\\.internal\\.[\\w.$/]+", "IMPL")
            .replaceAll("@[0-9a-fA-F]+", "@#")
            .replaceAll("#[0-9]+", "#")
            // Collectors$CollectorImpl has a custom toString; jvmdg's impl doesn't
            .replaceAll("CollectorImpl\\[.*\\]", "IMPL@#")
            // JDK19+ FJP.externalSubmit/lazySubmit wrap the task; the wrapper
            // is a JDK-internal class jvmdg cannot reproduce
            .replaceAll("\\[Wrapped task = [^\\]]*\\]", "")
            // Thread toString gained '#tid' in 19; group name presence varies
            .replaceAll("VirtualThread\\[[^\\]]*\\]/\\w+(@\\S+)?", "VirtualThread[#]")
            .replaceAll("Thread\\[[^\\]]*\\]", "Thread[#]")
            // CompletableFuture toString internals vary: dependents count and
            // exception detail differ by version
            .replaceAll(", [0-9]+ dependents", "")
            // ForkJoinPool toString gained ', delayed = n' in later versions
            .replaceAll(", delayed = [0-9]+", "")
            .replaceAll("\\[(Completed|Not completed)[^\\]]*\\]", "[$1]")
            .replaceAll("\\[Completed exceptionally[^\\]]*\\]", "[Completed exceptionally]")
            // ForkJoinPool steal counts race with worker scheduling
            .replaceAll("steals = [0-9]+", "steals = #")
            // random UUID payloads legitimately differ per call
            .replaceAll("\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b", "UUID#")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}
