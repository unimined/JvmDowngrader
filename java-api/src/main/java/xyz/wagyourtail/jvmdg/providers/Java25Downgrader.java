package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;

public class Java25Downgrader extends AllVersionDowngrader {

    public Java25Downgrader() {
        super(Opcodes.V25, Opcodes.V24, 0);
    }

}
