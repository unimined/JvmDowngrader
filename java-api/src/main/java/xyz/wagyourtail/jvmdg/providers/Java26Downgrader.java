package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java26Downgrader extends AllVersionDowngrader {

    public Java26Downgrader() {
        super(Opcodes.V26, Opcodes.V25, 0);
    }

}
