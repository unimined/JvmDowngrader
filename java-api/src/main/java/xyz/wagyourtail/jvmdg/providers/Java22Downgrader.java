package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java22Downgrader extends VersionProvider {

    public Java22Downgrader() {
        super(Opcodes.V22, Opcodes.V21);
    }

    @Override
    public void init() {

    }

}
