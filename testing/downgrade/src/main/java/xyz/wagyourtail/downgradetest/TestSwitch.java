package xyz.wagyourtail.downgradetest;

public class TestSwitch {

    private static <T> T rt(T v) {
        return v;
    }

    static void main(String[] args) {

        switch (rt(TestEnum.C)) {
            case TestEnum.A -> System.out.println("A");
            case TestEnum.C -> System.out.println("C");
            case TestEnum b -> System.out.println("B");
        }

        switch (rt(TestEnum.B)) {
            case A -> System.out.println("A");
            case C -> System.out.println("C");
            default -> System.out.println("B");
        }

        switch (rt("test")) {
            case "a" -> System.out.println("A");
            case "test" -> System.out.println("B");
            case "c" -> System.out.println("C");
            case "d" -> System.out.println("D");
            case String s -> System.out.println(s);
        }

        switch (rt("test")) {
            case "a" -> System.out.println("A");
            case "test" -> System.out.println("B");
            case "c" -> System.out.println("C");
            case "d" -> System.out.println("D");
        }

        switch ((TestInterface) rt(TestEnum.B)) {
            case TestEnum.A -> System.out.println("A");
            case TestEnum ignored -> System.out.println("B");
            case TestClass tc -> System.out.println("TestClass");
            default -> System.out.println("default");
        }

        Number n = rt((Number) Integer.valueOf(1000));
        switch ((Integer) n) {
            case 1000 -> System.out.println("n1000");
            case 999 -> System.out.println("n999");
            default -> System.out.println("nDefault");
        }

        Object strCollide = rt((Object) "BB");
        switch ((String) strCollide) {
            case "Aa" -> System.out.println("Aa");
            case "BB" -> System.out.println("BB");
            case "C" -> System.out.println("C");
            default -> System.out.println("otherStr");
        }

        Object enumCollide = rt((Object) CollideEnum.BB);
        switch (enumCollide) {
            case CollideEnum.Aa -> System.out.println("EAa");
            case CollideEnum.BB -> System.out.println("EBB");
            case CollideEnum.C -> System.out.println("EC");
            default -> System.out.println("Eother");
        }

        String largeStr = rt("val3");
        switch (largeStr) {
            case "val1" -> System.out.println("LargeStr-1");
            case "val2" -> System.out.println("LargeStr-2");
            case "val3" -> System.out.println("LargeStr-3");
            case "val4" -> System.out.println("LargeStr-4");
            case "val5" -> System.out.println("LargeStr-5");
            default -> System.out.println("LargeStr-Def");
        }

        String largeCol = rt("BB");
        switch (largeCol) {
            case "Aa" -> System.out.println("LargeCol-Aa");
            case "BB" -> System.out.println("LargeCol-BB");
            case "C" -> System.out.println("LargeCol-C");
            case "D" -> System.out.println("LargeCol-D");
            case "E" -> System.out.println("LargeCol-E");
            default -> System.out.println("LargeCol-Def");
        }

        Object bigEnum = rt((Object) BigEnum.BE3);
        switch (bigEnum) {
            case BigEnum.BE1 -> System.out.println("BigEnum-1");
            case BigEnum.BE2 -> System.out.println("BigEnum-2");
            case BigEnum.BE3 -> System.out.println("BigEnum-3");
            case BigEnum.BE4 -> System.out.println("BigEnum-4");
            case BigEnum.BE5 -> System.out.println("BigEnum-5");
            default -> System.out.println("BigEnum-Def");
        }

        Object guarded = rt((Object) "zzz");
        switch (guarded) {
            case String s when s.length() == 0 -> System.out.println("len0");
            case String s when s.length() == 1 -> System.out.println("len1");
            default -> System.out.println("strDefault2");
        }

        Object singleEnum = rt((Object) 123);
        switch (singleEnum) {
            case TestEnum.A -> System.out.println("enumA");
            default -> System.out.println("notEnum");
        }

        switch (rt('\u0167')) {
            case 1000 -> System.out.println("10");
            case 'c' -> System.out.println("c");
            default -> System.out.println("defaultChar");
        }

        Object nul = rt(null);
        switch (nul) {
            case null -> System.out.println("nullCase");
            case String s -> System.out.println("nonnullStr");
            default -> System.out.println("nonnullOther");
        }

        Object nul2 = rt((Object) "x");
        switch (nul2) {
            case null -> System.out.println("nullCase2");
            case String s -> System.out.println("nonnullStr2");
            default -> System.out.println("nonnullOther2");
        }

        Object nul3 = rt(null);
        switch (nul3) {
            case null -> System.out.println("nullCase3");
            default -> System.out.println("nonnullOther3");
        }

        Object repInt = rt((Object) Integer.valueOf(2));
        switch (repInt) {
            case Integer i when i == 1 -> System.out.println("g1");
            case Integer i when i == 2 -> System.out.println("g2");
            case Integer i -> System.out.println("gIntAny");
            default -> System.out.println("gObjAny");
        }

        Object guarded2 = rt((Object) "ab");
        switch (guarded2) {
            case String s when s.length() == 0 -> System.out.println("len0b");
            case String s when s.length() == 1 -> System.out.println("len1b");
            case String s -> System.out.println("lenAny");
            default -> System.out.println("strDefault3");
        }

        Object unnamed = rt((Object) 42);
        switch (unnamed) {
            case Integer _ -> System.out.println("unnamedInt");
            default -> System.out.println("unnamedDefault");
        }

        Object arr = rt((Object) new String[]{"x"});
        switch (arr) {
            case String[] a -> System.out.println("strArray");
            case Object _ -> System.out.println("objAnyArray");
        }

        Object p = rt((Object) new Point(3, 4));
        switch (p) {
            case Point(int x, int y) when x == 3 && y == 4 -> System.out.println("point34");
            case Point(int x, int y) -> System.out.println("pointAny");
            default -> System.out.println("notPoint");
        }

        Object r = rt((Object) new Rect(new Point(0, 0), new Point(1, 1)));
        switch (r) {
            case Rect(Point(_, _), Point(_, _)) -> System.out.println("rect2p");
            default -> System.out.println("notRect");
        }

        Object pair = rt((Object) new Pair(new Point(1, 2), "ok"));
        switch (pair) {
            case Pair(Point(int x, int y), String s) -> System.out.println("pairPointStr");
            case Pair(var a, var b) -> System.out.println("pairAny");
            default -> System.out.println("notPair");
        }

        Shape sh = rt((Shape) new Point(9, 9));
        String shRes = switch (sh) {
            case Point(int x, int y) -> "shPoint";
            case Rect(Point p1, Point p2) -> "shRect";
        };
        System.out.println(shRes);

        String multi = rt("b");
        int multiRes = switch (multi) {
            case "a", "b" -> 1;
            case "c" -> 2;
            default -> 3;
        };
        System.out.println("multi" + multiRes);

        Integer i = rt(3);
        switch (i) {
            case 1 -> System.out.println("Int-1");
            case 2 -> System.out.println("Int-2");
            case 3 -> System.out.println("Int-3");
            case 4 -> System.out.println("Int-4");
            default -> System.out.println("Int-def");
        }

        Integer i2 = rt(6);
        switch (i2) {
            case 1 -> System.out.println("Int2-1");
            case 2 -> System.out.println("Int2-2");
            case 3 -> System.out.println("Int2-3");
            case 4 -> System.out.println("Int2-4");
            case 5 -> System.out.println("Int2-5");
            case 6 -> System.out.println("Int2-6");
            default -> System.out.println("Int2-def");
        }

        Short sh1 = rt((short) 2);
        switch (sh1) {
            case (short) 1 -> System.out.println("Short-1");
            case (short) 2 -> System.out.println("Short-2");
            case (short) 3 -> System.out.println("Short-3");
            case (short) 4 -> System.out.println("Short-4");
            default -> System.out.println("Short-def");
        }

        Short sh2 = rt((short) 5);
        switch (sh2) {
            case (short) 1 -> System.out.println("Short2-1");
            case (short) 2 -> System.out.println("Short2-2");
            case (short) 3 -> System.out.println("Short2-3");
            case (short) 4 -> System.out.println("Short2-4");
            case (short) 5 -> System.out.println("Short2-5");
            default -> System.out.println("Short2-def");
        }

        Byte by = rt((byte) 4);
        switch (by) {
            case (byte) 1 -> System.out.println("Byte-1");
            case (byte) 2 -> System.out.println("Byte-2");
            case (byte) 3 -> System.out.println("Byte-3");
            case (byte) 4 -> System.out.println("Byte-4");
            default -> System.out.println("Byte-def");
        }

        Byte by2 = rt((byte) 6);
        switch (by2) {
            case (byte) 1 -> System.out.println("Byte2-1");
            case (byte) 2 -> System.out.println("Byte2-2");
            case (byte) 3 -> System.out.println("Byte2-3");
            case (byte) 4 -> System.out.println("Byte2-4");
            case (byte) 5 -> System.out.println("Byte2-5");
            case (byte) 6 -> System.out.println("Byte2-6");
            default -> System.out.println("Byte2-def");
        }

        Character ch = rt('b');
        switch (ch) {
            case 'a' -> System.out.println("Char-a");
            case 'b' -> System.out.println("Char-b");
            case 'c' -> System.out.println("Char-c");
            default -> System.out.println("Char-def");
        }

        Character ch2 = rt('e');
        switch (ch2) {
            case 'a' -> System.out.println("Char2-a");
            case 'b' -> System.out.println("Char2-b");
            case 'c' -> System.out.println("Char2-c");
            case 'd' -> System.out.println("Char2-d");
            case 'e' -> System.out.println("Char2-e");
            default -> System.out.println("Char2-def");
        }

        bigNoColSwitch(rt("val8"));
        bigNoColSwitch(rt(null));
        bigNoColSwitch(rt("val10"));
    }

    private static void bigNoColSwitch(String bigNoCol) {
        switch (bigNoCol) {
            case null -> System.out.println("BigNoCol-null");
            case "val1" -> System.out.println("BigNoCol-1");
            case "val2" -> System.out.println("BigNoCol-2");
            case "val3" -> System.out.println("BigNoCol-3");
            case "val4" -> System.out.println("BigNoCol-4");
            case "val5" -> System.out.println("BigNoCol-5");
            case "val6" -> System.out.println("BigNoCol-6");
            case "val7" -> System.out.println("BigNoCol-7");
            case "val8" -> System.out.println("BigNoCol-8");
            case "val9" -> System.out.println("BigNoCol-9");
            case String s -> System.out.println("BigNoCol-other:" + s);
        }
    }

    enum TestEnum implements TestInterface {
        A, B, C
    }

    enum CollideEnum {
        Aa, BB, C
    }

    enum BigEnum {
        BE1, BE2, BE3, BE4, BE5
    }

    sealed interface TestInterface permits TestClass, TestEnum {
    }

    sealed interface Shape permits Point, Rect {
    }

    final static class TestClass implements TestInterface {
    }

    record Point(int x, int y) implements Shape {
    }

    record Rect(Point p1, Point p2) implements Shape {
    }

    record Pair(Object a, Object b) {
    }

}
