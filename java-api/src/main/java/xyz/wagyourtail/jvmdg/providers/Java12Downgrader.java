package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java12Downgrader extends AllVersionDowngrader {
    public Java12Downgrader() {
        super(Opcodes.V12, Opcodes.V11, 0);
    }

}
