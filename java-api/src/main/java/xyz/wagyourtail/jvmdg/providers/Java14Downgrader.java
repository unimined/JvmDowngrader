package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java14Downgrader extends AllVersionDowngrader {
    public Java14Downgrader() {
        super(Opcodes.V14, Opcodes.V13, 0);
    }

}