package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.*;
import java.util.*;

public class J_L_Character {
    private static int[][] EMOJI_RANGES;
    private static int[][] EMOJI_COMPONENT_RANGES;
    private static int[][] EMOJI_MODIFIER_RANGES;
    private static int[][] EMOJI_MODIFIER_BASE_RANGES;
    private static int[][] EMOJI_PRESENTATION_RANGES;
    private static int[][] EXTENDED_PICTOGRAPHIC_RANGES;


    private static void readData() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(J_L_Character.class.getResourceAsStream("/unicode/emoji-data.txt"))))) {
            Map<String, List<int[]>> data = new HashMap<>();
            // read data
            reader.lines().forEach(e -> {
                if (e.startsWith("#")) return;
                String before_comment = e.split("#")[0];
                String[] parts = before_comment.split(";");
                if (parts.length < 2) return;
                String[] ranges = parts[0].trim().split("\\.\\.");
                if (ranges.length != 1 && ranges.length != 2) return;
                int[] range = new int[2];
                range[0] = Integer.parseInt(ranges[0].trim(), 16);
                range[1] = ranges.length == 2 ? Integer.parseInt(ranges[1].trim(), 16) : range[0];
                data.computeIfAbsent(parts[1].trim(), k -> new ArrayList<>()).add(range);
            });
            // convert to arrays
            EMOJI_RANGES = data.get("Emoji").toArray(new int[0][]);
            EMOJI_COMPONENT_RANGES = data.get("Emoji_Component").toArray(new int[0][]);
            EMOJI_MODIFIER_RANGES = data.get("Emoji_Modifier").toArray(new int[0][]);
            EMOJI_MODIFIER_BASE_RANGES = data.get("Emoji_Modifier_Base").toArray(new int[0][]);
            EMOJI_PRESENTATION_RANGES = data.get("Emoji_Presentation").toArray(new int[0][]);
            EXTENDED_PICTOGRAPHIC_RANGES = data.get("Extended_Pictographic").toArray(new int[0][]);
        }
    }

    @Stub(ref = @Ref("java/lang/Character"))
    public static boolean isEmoji(int codepoint) throws IOException {
        if (EMOJI_RANGES == null) readData();
        // TODO: maybe binary search
        for (int[] range : EMOJI_RANGES) {
            if (range[0] <= codepoint && codepoint <= range[1]) return true;
        }
        return false;
    }

    @Stub(ref = @Ref("java/lang/Character"))
    public static boolean isEmojiComponent(int codepoint) throws IOException {
        if (EMOJI_COMPONENT_RANGES == null) readData();
        for (int[] range : EMOJI_COMPONENT_RANGES) {
            if (range[0] <= codepoint && codepoint <= range[1]) return true;
        }
        return false;
    }

    @Stub(ref = @Ref("java/lang/Character"))
    public static boolean isEmojiModifier(int codepoint) throws IOException {
        if (EMOJI_MODIFIER_RANGES == null) readData();
        for (int[] range : EMOJI_MODIFIER_RANGES) {
            if (range[0] <= codepoint && codepoint <= range[1]) return true;
        }
        return false;
    }

    @Stub(ref = @Ref("java/lang/Character"))
    public static boolean isEmojiModifierBase(int codepoint) throws IOException {
        if (EMOJI_MODIFIER_BASE_RANGES == null) readData();
        for (int[] range : EMOJI_MODIFIER_BASE_RANGES) {
            if (range[0] <= codepoint && codepoint <= range[1]) return true;
        }
        return false;

    }

    @Stub(ref = @Ref("java/lang/Character"))
    public static boolean isEmojiPresentation(int codepoint) throws IOException {
        if (EMOJI_PRESENTATION_RANGES == null) readData();
        for (int[] range : EMOJI_PRESENTATION_RANGES) {
            if (range[0] <= codepoint && codepoint <= range[1]) return true;
        }
        return false;

    }

    @Stub(ref = @Ref("java/lang/Character"))
    public static boolean isExtendedPictographic(int codepoint) throws IOException {
        if (EXTENDED_PICTOGRAPHIC_RANGES == null) readData();
        for (int[] range : EXTENDED_PICTOGRAPHIC_RANGES) {
            if (range[0] <= codepoint && codepoint <= range[1]) return true;
        }
        return false;
    }

}
