package jvmdg.stubtests.input;

/**
 * Plain-{@code makeConcat} invokedynamic fixtures. This source set compiles
 * with {@code -XDstringConcat=indy}, so every {@code +} concat here is a
 * bootstrap call to {@code StringConcatFactory.makeConcat(Lookup, String,
 * MethodType)} — a shape javac never emits by default (it always uses
 * {@code makeConcatWithConstants}). On targets below Java 9 the j9
 * {@code @Modify} rewrites these indies to inline concat bytecode.
 *
 * <p>Called by {@code jvmdg.stubtests.manual.BootstrapTests}; results are
 * printed through {@code Support} there.
 */
public final class Concats {

    private Concats() {
    }

    /** {@code invokedynamic makeConcat (String,String)String} */
    public static String cat(String a, String b) {
        return a + b;
    }

    /** {@code invokedynamic makeConcat (String,int,String)String} */
    public static String catInt(String a, int i) {
        return a + i + "!";
    }

    /** {@code invokedynamic makeConcat (String,Object)String} — null arg probe */
    public static String catNull(String a, Object o) {
        return a + o;
    }

    /** {@code invokedynamic makeConcat (long,char,boolean)String} — primitives */
    public static String catPrimitives(long l, char c, boolean b) {
        return l + "-" + c + "-" + b;
    }

}
