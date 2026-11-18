package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;

public class Java17Downgrader extends AllVersionDowngrader {
    public Java17Downgrader() {
        super(Opcodes.V17, Opcodes.V16, 0);
    }

}