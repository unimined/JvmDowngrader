package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.ref.SoftReference;
import java.util.*;

public class J_L_Character {

    private static SoftReference<Map<Integer, int[]>> charNames = new SoftReference<>(null);

    private static Map<Integer, int[]> loadChars() {
        Map<Integer, int[]> map = charNames.get();
        if (map != null) return map;
        synchronized (J_L_Character.class) {
            map = new HashMap<>();
            for (int i = Character.MIN_CODE_POINT; i < Character.MAX_CODE_POINT; i++) {
                String name = Character.getName(i);
                if (name == null) continue;
                int finalI = i;
                map.compute(name.hashCode(), (k, v) -> {
                    int[] nV;
                    if (v != null) {
                        nV = new int[v.length + 1];
                        System.arraycopy(v, 0, nV, 0, v.length);
                    } else {
                        nV = new int[1];
                    }
                    nV[nV.length - 1] = finalI;
                    return nV;
                });
            }
            charNames = new SoftReference<>(map);
        }
        return map;
    }

    @Stub(ref = @Ref("java/lang/Character"))
    public static int codePointOf(String name) {
        name = name.trim().toUpperCase(Locale.ROOT);
        int[] chars = loadChars().getOrDefault(name.hashCode(), new int[0]);
        for (Integer c : chars) {
            if (Character.getName(c).equals(name)) {
                return c;
            }
        }
        int offset = name.lastIndexOf(' ');
        if (offset != -1) {
            try {
                int c = Integer.parseInt(name.substring(offset + 1), 16);
                if (Character.isValidCodePoint(c) && name.equals(Character.getName(c))) {
                    return c;
                }
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("Unrecognized character name: " + name);
    }

}
