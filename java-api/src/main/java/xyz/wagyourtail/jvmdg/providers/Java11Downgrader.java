package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;

public class Java11Downgrader extends AllVersionDowngrader {
    public Java11Downgrader() {
        super(Opcodes.V11, Opcodes.V10, 0);
    }

}
