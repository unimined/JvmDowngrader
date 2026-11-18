package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java13Downgrader extends AllVersionDowngrader {
    public Java13Downgrader() {
        super(Opcodes.V13, Opcodes.V12, 0);
    }

}