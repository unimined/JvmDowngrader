package xyz.wagyourtail.jvmdg.benchmarks.indy;

/**
 * Plain-Java fixture types shared by the woven dispatch fixtures and the
 * benchmark. Everything here is real and loadable — the phantom "gained
 * interface" for the old-JVM scenario is deliberately NOT here (the woven
 * dispatcher references a class name nothing can resolve).
 */
public final class Fix {

    private Fix() {
    }

    /** the member's own declared interface — receivers implement this (direct-call baseline) */
    public interface Member {
        int op(int x);
    }

    /**
     * the "newer JVM" gained interface — receivers in the newer-* sets
     * implement it so {@code instanceof NewI}/{@code invokeinterface} is a
     * real fast path; receivers in the plain sets don't (instanceof miss)
     */
    public interface NewI {
        int op(int x);
    }

    /** $Marker replica — membership witness a retro SUPERTYPE injects */
    public interface Marker {
    }

    /** retro adapter type — real interface the wrap impl implements */
    public interface Adapter {
    }

    /** the generated {@code $Wrap} sibling's stand-in — adapter-typed, holds the member */
    public static final class WrapImpl implements Adapter {
        public final Object member;

        public WrapImpl(Object member) {
            this.member = member;
        }
    }

    /** erased-shape callsite every dispatch variant implements — {@code call(self,arg,ceiling) -> Object} */
    public interface Site {
        Object call(Object self, int arg, int ceiling) throws Throwable;
    }

    /** {@code boolean test(Object)} — instanceof/isInstance sites */
    public interface BoolSite {
        boolean test(Object o);
    }

    /**
     * the api-side impl for the instance-stub baseline — self-first
     * signature {@code op(Object self, int arg)} like a real
     * {@code J_*} stub impl
     */
    public static final class Impl {
        private Impl() {
        }

        public static int op(Object self, int arg) {
            return arg * 31;
        }
    }
}
