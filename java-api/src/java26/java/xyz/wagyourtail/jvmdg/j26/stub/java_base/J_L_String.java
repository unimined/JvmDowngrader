package xyz.wagyourtail.jvmdg.j26.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.RequiresResource;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

public class J_L_String {
    private static Map<Integer, int[]> CASE_FOLDING;

    @RequiresResource("unicode/CaseFolding.txt")
    private static void readData() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(J_L_String.class.getResourceAsStream("/unicode/CaseFolding.txt"))))) {
            Map<Integer, int[]> caseFolding = new HashMap<>();
            reader.lines().forEach(e -> {
                if (e.startsWith("#")) return;
                String before_comment = e.split("#")[0];
                String[] parts = before_comment.split(";");
                if (parts.length < 3) return;
                String status = parts[1].trim();
                if (status.equals("C") || status.equals("F")) {
                    int code = Integer.parseInt(parts[0].trim(), 16);
                    String[] mappingStr = parts[2].trim().split("\\s+");
                    int[] mapping = new int[mappingStr.length];
                    for (int i = 0; i < mappingStr.length; i++) {
                        mapping[i] = Integer.parseInt(mappingStr[i], 16);
                    }
                    caseFolding.put(code, mapping);
                }
            });
            CASE_FOLDING = Map.copyOf(caseFolding);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String caseFold(String str) {
        return str.codePoints().mapMulti((codepoint, sink) -> {
            for (int i : caseFold(codepoint)) {
                sink.accept(i);
            }
        }).mapToObj(Character::toString).collect(Collectors.joining());
    }

    private static int[] caseFold(int codepoint) {
        if (CASE_FOLDING == null) readData();
        return CASE_FOLDING.getOrDefault(codepoint, new int[] { codepoint });
    }

    @Stub(ref = @Ref(value = "java/lang/String", member = "UNICODE_CASEFOLD_ORDER", desc = "Ljava/util/Comparator;"))
    public static Comparator<String> getUnicodeCasefoldOrder() {
        return Comparator.comparing(J_L_String::caseFold);
    }

    @Stub
    public static int compareToFoldCase(String self, String other) {
        return caseFold(self).compareTo(caseFold(other));
    }

    @Stub
    public static boolean equalsFoldCase(String self, String other) {
        //noinspection StringEquality
        if (self == other) return true;
        if (other == null) return false;
        return caseFold(self).equals(caseFold(other));
    }

}
