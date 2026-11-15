package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * {@code Class.getInterfaces}/{@code getGenericInterfaces} filter out the
 * transform's injected artifacts so the reflective view matches the
 * authored source:
 * <ul>
 *   <li>gained interfaces injected for supertype membership — {@link Exec}
 *       declares nothing, but {@code ThreadPoolExecutor} gained
 *       {@code AutoCloseable} at 63, so the 63-boundary injects it and
 *       records it in {@code @GainedInterfaces};</li>
 *   <li>generated marker interfaces — {@link Co} declares {@code Constable},
 *       which remaps away below 56 and leaves a {@code $Marker} interface in
 *       its place.</li>
 * </ul>
 * Probes assert only what stays identical across shapes: interface counts
 * where the source view is empty, absence of {@code $} names, and
 * passthrough of real declared interfaces.
 */
public class ReflInterfaces {

    public static void main(String[] args) {
        Support.run(ReflInterfaces.class);
    }

    /** gained {@code AutoCloseable} injected on Exec must be filtered → 0 on every shape */
    @FeatureTest
    static void gainedFiltered(String id) {
        Support.print(id, Exec.class.getInterfaces().length);
    }

    /** generic view falls back to the raw list for signature-less classes — same filter */
    @FeatureTest
    static void gainedFilteredGeneric(String id) {
        Support.print(id, Exec.class.getGenericInterfaces().length);
    }

    /** no generated $Marker/$Ov$ names leak through either view */
    @FeatureTest
    static void markersFiltered(String id) {
        boolean leaked = false;
        for (Class<?> i : Co.class.getInterfaces()) {
            leaked |= i.getName().contains("$");
        }
        for (java.lang.reflect.Type t : Co.class.getGenericInterfaces()) {
            leaked |= t.getTypeName().contains("$");
        }
        Support.print(id, leaked);
    }

    /** a real declared interface passes straight through */
    @FeatureTest
    static void plainInterfaces(String id) {
        Support.print(id, Runner.class.getInterfaces().length == 1
            && Runner.class.getInterfaces()[0] == Runnable.class);
    }

    /**
     * gained interfaces on an unmodifiable JDK class surface as stand-ins:
     * {@code String} gained {@code Constable}/{@code ConstantDesc} at 56 —
     * on the downgraded shape the literal remaps to the stand-in, which is
     * also what {@code getInterfaces} reports, so both shapes answer true.
     */
    @FeatureTest
    static void gainedStandIns(String id) {
        java.util.List<Class<?>> ifs = java.util.Arrays.asList(String.class.getInterfaces());
        Support.print(id, ifs.contains(Constable.class) && ifs.contains(ConstantDesc.class));
    }

    /** same view through the generic entry point */
    @FeatureTest
    static void gainedStandInsGeneric(String id) {
        java.util.List<java.lang.reflect.Type> ifs =
            java.util.Arrays.asList(String.class.getGenericInterfaces());
        Support.print(id, ifs.contains(Constable.class) && ifs.contains(ConstantDesc.class));
    }

    /**
     * a gained interface that exists natively at the target resolves to the
     * real class: {@code StringBuffer} gained {@code Comparable} at 54 —
     * older than any runner — so the appended entry is the real
     * {@code Comparable}, not a stand-in. (Chronological interfaces are
     * ordered by the JDK, so only membership is asserted.)
     */
    @FeatureTest
    static void gainedNativeInterface(String id) {
        Support.print(id,
            java.util.Arrays.asList(StringBuffer.class.getInterfaces())
                .contains(Comparable.class));
    }

    /** declares nothing — the 63-boundary injects gained {@code AutoCloseable} */
    static class Exec extends ThreadPoolExecutor {
        Exec() {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        }
    }

    /** {@code Constable} remaps away below 56 → {@code $Marker} lands in the interfaces list */
    static class Co implements Constable {
        @Override
        public Optional<? extends ConstantDesc> describeConstable() {
            return Optional.empty();
        }
    }

    static class Runner implements Runnable {
        @Override
        public void run() {
        }
    }
}
