package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Reflection surface: @Deprecated since/forRemoval attributes (j9),
 * declared-vs-inherited annotations, synthetic/bridge method flags, generic
 * superclass signatures.
 */
@Deprecated(forRemoval = true, since = "1.0")
public class Reflections {

    public static void main(String[] args) {
        Support.run(Reflections.class);
    }

    @Deprecated(since = "1.1")
    public void test() {
    }

    @FeatureTest
    static void deprecatedAttrs() throws NoSuchMethodException {
        // @Deprecated since/forRemoval (j9 elementValues) on class + method
        Deprecated dep = Reflections.class.getAnnotation(Deprecated.class);
        System.out.println(dep.since());
        System.out.println(dep.forRemoval());

        Deprecated dep2 = Reflections.class.getMethod("test").getAnnotation(Deprecated.class);
        System.out.println(dep2.since());
        System.out.println(dep2.forRemoval());
    }

    @FeatureTest
    static void inheritedAnnotations() {
        // @Inherited propagates to subclasses
        System.out.println(Child.class.getAnnotation(Marked.class) != null);
        System.out.println(Child.class.getDeclaredAnnotation(Marked.class) != null);
    }

    @FeatureTest
    static void methodFlags() throws NoSuchMethodException {
        // isPublic/isSynthetic/isBridge/isStatic — modifier flag reflection
        // must survive member rewrites
        Method m = Reflections.class.getMethod("test");
        System.out.println(Modifier.isPublic(m.getModifiers()));
        System.out.println(m.isSynthetic());
        System.out.println(m.isBridge());
        // main is static+public
        Method main = Reflections.class.getMethod("main", String[].class);
        System.out.println(Modifier.isStatic(main.getModifiers()));
    }

    @FeatureTest
    static void elementValues() throws NoSuchMethodException {
        // annotation elementValues: enum constant, Class literals (incl.
        // primitive int.class -> 'I' field descriptor), arrays, default
        Params p = Reflections.class.getDeclaredMethod("annotated").getAnnotation(Params.class);
        System.out.println(p.tag());
        System.out.println(p.kind());
        System.out.println(p.types().length);
        System.out.println(p.types()[0].getSimpleName());
        System.out.println(p.types()[1] == int.class);
        Params d = Reflections.class.getDeclaredMethod("annotatedDefault").getAnnotation(Params.class);
        System.out.println(d.tag() + ":" + d.kind() + ":" + d.types().length);
    }

    enum Kind {
        ALPHA, BETA
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface Params {
        String tag() default "def";
        Kind kind() default Kind.ALPHA;
        Class<?>[] types() default {};
    }

    @Params(types = {String.class, int.class}, kind = Kind.BETA, tag = "t")
    static void annotated() {
    }

    @Params
    static void annotatedDefault() {
    }

    @java.lang.annotation.Inherited
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface Marked {
    }

    @Marked
    static class Parent {
    }

    static class Child extends Parent {
    }
}
