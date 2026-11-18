package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java20Downgrader extends AllVersionDowngrader {
    public Java20Downgrader() {
        super(Opcodes.V20, Opcodes.V19, 0);
    }

}