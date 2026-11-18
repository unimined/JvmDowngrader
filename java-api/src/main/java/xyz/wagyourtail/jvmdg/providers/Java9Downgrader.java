package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;

public class Java9Downgrader extends AllVersionDowngrader {

    public Java9Downgrader() {
        super(Opcodes.V9, Opcodes.V1_8, 0);
    }

}
