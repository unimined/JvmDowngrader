package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java21Downgrader extends AllVersionDowngrader {
    public Java21Downgrader() {
        super(Opcodes.V21, Opcodes.V20, 0);
    }

}