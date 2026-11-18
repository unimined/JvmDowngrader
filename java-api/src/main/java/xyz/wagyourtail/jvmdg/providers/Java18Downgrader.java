package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java18Downgrader extends AllVersionDowngrader {
    public Java18Downgrader() {
        super(Opcodes.V18, Opcodes.V17, 0);
    }

}