package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j19.stub.java_base.*;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java19Downgrader extends VersionProvider {
    public Java19Downgrader() {
        super(Opcodes.V19, Opcodes.V18, 0);
    }

    public void init() {
        // -- java.base --
        stub(J_I_InvalidClassException.class);
        stub(J_I_InvalidObjectException.class);
        stub(J_I_ObjectStreamException.class);
        stub(J_L_Integer.class);
        stub(J_L_Long.class);
        stub(J_L_Thread.class);
        stub(J_L_WrongThreadException.class);
        stub(J_M_BigInteger.class);
        stub(J_N_S_SSLHandshakeException.class);
        stub(J_N_S_SSLKeyException.class);
        stub(J_N_S_SSLPeerUnverifiedException.class);
        stub(J_N_S_SSLProtocolException.class);
        stub(J_N_SocketException.class);
        stub(J_T_C_Chronology.class);
        stub(J_T_DecimalFormatSymbols.class);
        // DateTimeFormatter
        // DateTimeFormatterBuilder
        stub(J_U_C_ExecutorService.class);
        stub(J_U_C_ForkJoinPool.class);
        stub(J_U_C_ForkJoinTask.class);
        // ForkJoinWorkerThread
        stub(J_U_C_Future.class);
        stub(J_U_HashMap.class);
        stub(J_U_HashSet.class);
        stub(J_U_LinkedHashMap.class);
        stub(J_U_LinkedHashSet.class);
        stub(J_U_Locale.class);
        stub(J_U_Objects.class);
        stub(J_U_Random.class);
        stub(J_U_WeakHashMap.class);
        // ToolProvider
        // SSLParameters

        // -- java.compiler --
    }
}