package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java10Downgrader extends AllVersionDowngrader {
    public Java10Downgrader() {
        super(Opcodes.V10, Opcodes.V9, 0);
    }

}
