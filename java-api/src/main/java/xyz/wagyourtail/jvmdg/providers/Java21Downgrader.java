package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j21.stub.java_base.*;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java21Downgrader extends VersionProvider {
    public Java21Downgrader() {
        super(Opcodes.V21, Opcodes.V20);
    }

    public void init() {
        // -- java.base --
        // DHKEM
        // PBEKeyFactory
        // PBES2Core
        // PBES2Parameters
        // PBKDF2Core
        // PBMAC1Core
        stub(J_L_Character.class);
        stub(J_L_MatchException.class);
        stub(J_L_Math.class);
        stub(J_L_StrictMath.class);
        stub(J_L_String.class);
        stub(J_L_StringBuffer.class);
        stub(J_L_StringBuilder.class);
        stub(J_L_Thread.class);
        // ClassDesc
        // MethodHandleDesc
        // MethodTypeDesc
        // ModuleDesc
        // PackageDesc
        stub(J_L_R_SwitchBootstraps.class);
        stub(J_U_Collections.class);
        stub(J_U_Deque.class);
        // LinkedHashMap
        stub(J_U_List.class);
        // Locale
        // NavigableMap
        stub(J_U_SequencedCollection.class);
        stub(J_U_SequencedMap.class);
        stub(J_U_SequencedSet.class);
        // SortedMap
        // TreeMap
        stub(J_U_R_Pattern.class);
        // ConcurrentSkipListMap
        // DelayQueue
        // Executors
        // DecapsulateException
        // KEM
        // KEMSpi

        // -- java.desktop --
        // SwingUtilities3
        // AccessibleHTML$HTMLAccessibleContext

    }
}