package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java19Downgrader extends AllVersionDowngrader {
    public Java19Downgrader() {
        super(Opcodes.V19, Opcodes.V18, 0);
    }

}