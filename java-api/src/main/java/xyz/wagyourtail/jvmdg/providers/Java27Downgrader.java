package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;

public class Java27Downgrader extends AllVersionDowngrader {

    public Java27Downgrader() {
        super(Opcodes.V27, Opcodes.V26, 0);
    }

}
