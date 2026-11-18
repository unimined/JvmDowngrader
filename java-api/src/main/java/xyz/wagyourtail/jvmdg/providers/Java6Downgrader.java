package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.ClassDowngrader;

public class Java6Downgrader extends AllVersionDowngrader {

    public Java6Downgrader() {
        super(Opcodes.V1_6, Opcodes.V1_5, 0);
    }

    @Override
    public void ensureInit(ClassDowngrader downgrader) {
        if (!isInitialized()) {
            downgrader.logger.warn("Java 6 -> 5 Stubs are VERY incomplete!");
        }
        super.ensureInit(downgrader);
    }

}
