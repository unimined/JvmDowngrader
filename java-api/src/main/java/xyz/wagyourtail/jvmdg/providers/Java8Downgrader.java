package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.ClassDowngrader;

public class Java8Downgrader extends AllVersionDowngrader {


    public Java8Downgrader() {
        super(Opcodes.V1_8, Opcodes.V1_7, 0);
    }

    @Override
    public void ensureInit(ClassDowngrader downgrader) {
        if (!isInitialized()) {
            downgrader.logger.warn("Java 8 -> 7 Stubs are VERY incomplete!");
        }
        super.ensureInit(downgrader);
    }

}
