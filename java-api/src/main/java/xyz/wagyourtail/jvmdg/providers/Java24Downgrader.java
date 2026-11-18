package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java24Downgrader extends AllVersionDowngrader {

    public Java24Downgrader() {
        super(Opcodes.V24, Opcodes.V23, 0);
    }

}
