package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.module.ModuleDescriptor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class J_L_M_ModuleDescriptor$Exports {

    @Stub
    public static Set<J_L_R_AccessFlag> accessFlags(ModuleDescriptor.Exports exports) {
        Set<J_L_R_AccessFlag> flags = new HashSet<>();
        for (ModuleDescriptor.Exports.Modifier modifier : exports.modifiers()) {
            switch (modifier) {
                case SYNTHETIC -> flags.add(J_L_R_AccessFlag.SYNTHETIC);
                case MANDATED -> flags.add(J_L_R_AccessFlag.MANDATED);
                default -> throw new AssertionError();
            }
        }
        return Collections.unmodifiableSet(flags);
    }

}
