package xyz.wagyourtail.jvmdg.compile.shade;

import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jvmdg.compile.shade.ShadeClosureBarrier.ReflRow;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ShadeClosureBarrier#parseReflRow} / {@code specToFQM} row grammar —
 * the reflection-keep roots the shade closure injects. Rows use real shapes
 * from the generated {@code reflection.idx}; the F-mode ctor row has no live
 * rows in the corpus but stays part of the grammar.
 */
public class TestShadeClosureBarrier {

    private static String impl(int i, ReflRow row) {
        return row.impls.get(i).toString();
    }

    @Test
    public void typeRowRootsItsTarget() {
        ReflRow row = ShadeClosureBarrier.parseReflRow(
            "T\tjava/io/UncheckedIOException\txyz/wagyourtail/jvmdg/j8/stub/J_I_UncheckedIOException\t52");
        assertNotNull(row);
        assertEquals("java/io/UncheckedIOException", row.owner);
        assertEquals("java/io/UncheckedIOException", row.matchName);
        assertEquals(1, row.impls.size());
        assertEquals("Lxyz/wagyourtail/jvmdg/j8/stub/J_I_UncheckedIOException;", impl(0, row));
    }

    @Test
    public void memberRowRootsImpl() {
        ReflRow row = ShadeClosureBarrier.parseReflRow(
            "M\tjava/io/BufferedReader\tlines\t()Ljava/util/stream/Stream;\ti\t" +
            "xyz/wagyourtail/jvmdg/j8/stub/J_I_BufferedReader\tlines\t" +
            "(Ljava/io/BufferedReader;)Lxyz/wagyourtail/jvmdg/j8/stub/stream/J_U_S_Stream;\t52\t1");
        assertNotNull(row);
        assertEquals("java/io/BufferedReader", row.owner);
        assertEquals("java/io/BufferedReader.lines", row.matchName);
        assertEquals(1, row.impls.size());
        assertEquals("Lxyz/wagyourtail/jvmdg/j8/stub/J_I_BufferedReader;lines;" +
            "(Ljava/io/BufferedReader;)Lxyz/wagyourtail/jvmdg/j8/stub/stream/J_U_S_Stream;", impl(0, row));
    }

    @Test
    public void ctorRRowReadsSpecColumnsAndSkipsDashes() {
        // R layout: cols 6,7,8 hold the post-call member specs — '-' means none
        ReflRow row = ShadeClosureBarrier.parseReflRow(
            "C\tjava/io/FileReader\t(Ljava/io/File;Ljava/nio/charset/Charset;)V\tR\t" +
            "(Ljava/io/File;)V\t0\t-\t-\t" +
            "xyz/wagyourtail/jvmdg/j11/stub/java_base/J_I_FileReader.setCharset" +
            "(Ljava/io/FileReader;Ljava/nio/charset/Charset;)V:1\t55\t1");
        assertNotNull(row);
        assertEquals("java/io/FileReader", row.owner);
        assertEquals("java/io/FileReader.<init>", row.matchName);
        assertEquals(1, row.impls.size());
        assertEquals("Lxyz/wagyourtail/jvmdg/j11/stub/java_base/J_I_FileReader;setCharset;" +
            "(Ljava/io/FileReader;Ljava/nio/charset/Charset;)V", impl(0, row));
    }

    @Test
    public void ctorFRowReadsFactorySpecColumn() {
        // F layout: col 4 carries the factory impl spec
        ReflRow row = ShadeClosureBarrier.parseReflRow(
            "C\tjava/nio/file/Path\t()V\tF\t" +
            "xyz/wagyourtail/jvmdg/j11/stub/java_base/J_N_F_Path.of(Ljava/lang/String;)Lxyz/wagyourtail/jvmdg/j11/stub/java_base/J_N_F_Path;:0\t" +
            "-\t-\t-\t-\t55\t1");
        assertNotNull(row);
        assertEquals("java/nio/file/Path.<init>", row.matchName);
        assertEquals(1, row.impls.size());
        assertEquals("Lxyz/wagyourtail/jvmdg/j11/stub/java_base/J_N_F_Path;of;" +
            "(Ljava/lang/String;)Lxyz/wagyourtail/jvmdg/j11/stub/java_base/J_N_F_Path;", impl(0, row));
    }

    @Test
    public void ctorRowPipesMultipleSpecs() {
        ReflRow row = ShadeClosureBarrier.parseReflRow(
            "C\tjava/io/X\t()V\tR\t-\t0\t" +
            "a/b/C.m()V:1|a/b/D.n(I)I:2\t-\ta/b/E.f(Ljava/lang/String;)V:0\t55\t1");
        assertNotNull(row);
        assertEquals(3, row.impls.size());
        assertEquals("La/b/C;m;()V", impl(0, row));
        assertEquals("La/b/D;n;(I)I", impl(1, row));
        assertEquals("La/b/E;f;(Ljava/lang/String;)V", impl(2, row));
    }

    @Test
    public void malformedRowsAreSkipped() {
        assertNull(ShadeClosureBarrier.parseReflRow("# comment header"));
        assertNull(ShadeClosureBarrier.parseReflRow(""));
        // wrong column counts
        assertNull(ShadeClosureBarrier.parseReflRow("T\tjava/io/X\tabc"));
        assertNull(ShadeClosureBarrier.parseReflRow("M\to\tn\t()V\ti\to\tn\t()V\t52"));
        // unknown C mode letter
        assertNull(ShadeClosureBarrier.parseReflRow("C\to\t()V\tX\ta/b/C.m()V\t-\t-\t-\t-\t55\t1"));
        // unknown row tag
        assertNull(ShadeClosureBarrier.parseReflRow("Q\to\tn\t()V"));
    }

    @Test
    public void unparseableSpecsAreDroppedNotFatal() {
        // a spec that isn't owner.name(desc):from yields no impl — the row's
        // valid siblings still register
        ReflRow row = ShadeClosureBarrier.parseReflRow(
            "C\to\t()V\tR\t-\t0\tgarbage-spec|a/b/D.n(I)I:2\t-\ta/b/E.f(Ljava/lang/String;)V:0\t55\t1");
        assertNotNull(row);
        assertEquals(2, row.impls.size());
        assertEquals("La/b/D;n;(I)I", impl(0, row));
        assertEquals("La/b/E;f;(Ljava/lang/String;)V", impl(1, row));
    }

    @Test
    public void trailingEmptyColumnSurvives() {
        // split("\t", -1) keeps the empty tail — it hits the spec parser,
        // returns null, and the row still registers its real impls instead
        // of silently widening the column count
        ReflRow row = ShadeClosureBarrier.parseReflRow(
            "C\to\t()V\tR\t-\t0\ta/b/D.n(I)I:2\t-\t\t55\t1");
        assertNotNull(row);
        assertEquals(1, row.impls.size());
        assertEquals("La/b/D;n;(I)I", impl(0, row));
    }

}
