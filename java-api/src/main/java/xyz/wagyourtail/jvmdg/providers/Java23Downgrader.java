package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;

public class Java23Downgrader extends AllVersionDowngrader {

    public Java23Downgrader() {
        super(Opcodes.V23, Opcodes.V22, 0);
    }

}
