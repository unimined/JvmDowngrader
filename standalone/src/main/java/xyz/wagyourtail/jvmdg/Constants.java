package xyz.wagyourtail.jvmdg;

import org.objectweb.asm.Opcodes;

import java.io.File;

public class Constants {

    public static final boolean DEBUG = true;

    public static final File DEBUG_DIR = new File(".jvmdg");





    public static int synthetic(int access) {
        if (DEBUG) return access;
        return access | Opcodes.ACC_SYNTHETIC;
    }
}
