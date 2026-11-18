package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.ClassDowngrader;

public class Java7Downgrader extends AllVersionDowngrader {

    public Java7Downgrader() {
        super(Opcodes.V1_7, Opcodes.V1_6, 0);
    }

    @Override
    public void ensureInit(ClassDowngrader downgrader) {
        if (!isInitialized()) {
            downgrader.logger.warn("Java 7 -> 6 Stubs are VERY incomplete!");
        }
        super.ensureInit(downgrader);
    }

}
