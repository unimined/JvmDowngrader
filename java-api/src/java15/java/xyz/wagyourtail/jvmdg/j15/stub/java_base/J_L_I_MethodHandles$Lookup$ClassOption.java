package xyz.wagyourtail.jvmdg.j15.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.Set;

@Adapter("Ljava/lang/invoke/MethodHandles$Lookup$ClassOption;")
public enum J_L_I_MethodHandles$Lookup$ClassOption {
    NESTMATE(0x00000001),
    STRONG(0x00000004);

    private final int flag;

    J_L_I_MethodHandles$Lookup$ClassOption(int flag) {
        this.flag = flag;
    }

    public static int optionsToFlag(Set<J_L_I_MethodHandles$Lookup$ClassOption> options) {
        int flags = 0;
        for (J_L_I_MethodHandles$Lookup$ClassOption cp : options) {
            flags |= cp.flag;
        }
        return flags;
    }
}
