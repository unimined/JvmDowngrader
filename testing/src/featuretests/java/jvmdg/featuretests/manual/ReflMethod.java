package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Table-driven {@code Class.getMethod}/{@code getDeclaredMethod}: rewritten
 * sites carry the authoring version; a stubbed member inside the
 * {@code (running, authored]} window resolves to the impl's {@code Method},
 * and the {@code java/lang/reflect/Method} stubs present the source-API
 * view through {@code StubMethodView} — member name, receiver-shifted
 * invoke, declaring class, params, modifiers.
 *
 * <p>Probes here assert only what's stable across all three run shapes
 * (native, downgrade, shade): real members pass straight through the facade
 * and misses stay honest {@code NoSuchMethodException}s. The stubbed-member
 * path — {@code ByteArrayOutputStream.getMethod("writeBytes", byte[].class)}
 * resolving to the j11 impl and invoking it — is verified on unshaded
 * downgrade (9 probes identical to native) and by {@code TestReflectionTable};
 * a pruned shade table can't answer it since a dynamic site's impl reach
 * isn't a bytecode edge (CG-3e's {@code usedOwners} tier is where that
 * lands).</p>
 */
public class ReflMethod {

    public static void main(String[] args) {
        Support.run(ReflMethod.class);
    }

    /** no row → native member, invoke straight through — deterministic hash */
    @FeatureTest
    static void realMemberPassthrough(String id) throws Exception {
        Method m = String.class.getMethod("hashCode");
        Support.print(id, m.invoke("hello"));
    }

    /** a member below the window is native on both sides */
    @FeatureTest
    static void realStaticMember(String id) throws Exception {
        Method m = String.class.getMethod("valueOf", int.class);
        Support.print(id, m.invoke(null, 42));
    }

    /** facade metadata on a real method passes through identically */
    @FeatureTest
    static void realMemberView(String id) throws Exception {
        Method m = String.class.getMethod("toUpperCase");
        Support.print(id, m.getName() + ":" + m.getDeclaringClass().getName()
            + ":" + m.getParameterCount() + ":" + Arrays.toString(m.getParameterTypes())
            + ":" + Modifier.toString(m.getModifiers()));
    }

    /** getDeclaredMethod stays on the class — a supertype member misses both ways */
    @FeatureTest
    static void declaredMiss(String id) {
        try {
            StringBuilder.class.getDeclaredMethod("hashCode");
            Support.print(id, false);
        } catch (NoSuchMethodException e) {
            Support.print(id, true);
        }
    }

    /** unknown member → honest NoSuchMethodException everywhere */
    @FeatureTest
    static void unknownMember(String id) {
        try {
            String.class.getMethod("noSuchMember");
            Support.print(id, false);
        } catch (NoSuchMethodException e) {
            Support.print(id, true);
        }
    }
}
