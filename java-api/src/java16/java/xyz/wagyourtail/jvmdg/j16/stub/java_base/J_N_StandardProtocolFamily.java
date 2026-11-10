package xyz.wagyourtail.jvmdg.j16.stub.java_base;

import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.exc.PartialStubError;
import xyz.wagyourtail.jvmdg.stub.EnumExtender;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.ExtendEnum;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;

/**
 * Fake enum and extension hook for the missing
 * {@link StandardProtocolFamily#UNIX} constant (JEP 380).
 * <p>
 * {@code @ExtendEnum} wires everything: a field stub for {@code GETSTATIC
 * StandardProtocolFamily.UNIX} dispatched to {@link #unix()} (the convention
 * getter — lowercase constant name, real return type), plus
 * inject-then-delegate shadows of the enum-universe readers
 * ({@code values}/{@code valueOf}/{@code Enum.valueOf}/{@code getEnumConstants}/
 * {@code EnumSet}/{@code EnumMap}) so the constant is injected before any
 * consumer-reachable reader can see the stale universe on the non-agent path.
 * <p>
 * {@link #unix()} injects a <em>real</em> constant into
 * {@code StandardProtocolFamily}'s universe via {@link EnumExtender}, so
 * consumer code observes the genuine type. If injection fails (SecurityManager
 * / exotic JVM), this fake's {@link #UNIX} constant is returned through an
 * Unsafe-array store that bypasses the declared-type check — it is still a
 * {@link ProtocolFamily}, so pass-through uses like
 * {@code SocketChannel.open(UNIX)} keep working, while anything demanding the
 * concrete enum type breaks loudly.
 */
@JEP(380)
@ExtendEnum(@Ref(value = "Ljava/net/StandardProtocolFamily;", member = "UNIX", desc = "Ljava/net/StandardProtocolFamily;"))
public enum J_N_StandardProtocolFamily implements ProtocolFamily {

    UNIX;

    private static volatile boolean injected = false;

    private static void ensureInjected() {
        if (!injected) {
            synchronized (J_N_StandardProtocolFamily.class) {
                if (!injected) {
                    EnumExtender.extend(StandardProtocolFamily.class, "UNIX");
                    injected = true;
                }
            }
        }
    }

    /**
     * Field-stub getter for {@code StandardProtocolFamily.UNIX} — registered
     * by {@link ExtendEnum} via the {@code unix()} naming convention. The
     * {@code GETSTATIC} rewrite is mandatory on every path (injection can
     * never create the missing static field), so this is deliberately
     * <em>not</em> {@code excludeWithRuntime}.
     */
    public static StandardProtocolFamily unix() {
        ensureInjected();
        try {
            return StandardProtocolFamily.valueOf("UNIX");
        } catch (IllegalArgumentException e) {
            // injection failed — degrade to the fake constant via an Unsafe
            // array store, bypassing javac's mandatory checkcast so the fake
            // flows as "StandardProtocolFamily" to pass-through uses
            try {
                StandardProtocolFamily[] box = new StandardProtocolFamily[1];
                Utils.getUnsafe().putObject(box, Unsafe.ARRAY_OBJECT_BASE_OFFSET, UNIX);
                return box[0];
            } catch (Throwable t) {
                PartialStubError error = new PartialStubError("unable to inject StandardProtocolFamily.UNIX into the enum universe");
                error.initCause(e);
                throw error;
            }
        }
    }

}
