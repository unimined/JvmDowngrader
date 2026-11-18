package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;

public class Java15Downgrader extends AllVersionDowngrader {
    public Java15Downgrader() {
        super(Opcodes.V15, Opcodes.V14, 0);
    }

}