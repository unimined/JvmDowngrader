package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;

public class Java16Downgrader extends AllVersionDowngrader {
    public Java16Downgrader() {
        super(Opcodes.V16, Opcodes.V15, 0);
    }

}