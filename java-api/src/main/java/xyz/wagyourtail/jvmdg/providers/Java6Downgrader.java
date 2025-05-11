package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.util.Iterator;

public class Java6Downgrader extends VersionProvider {

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

    @Override
    public void init() {

    }

    @Override
    public ClassNode otherTransforms(ClassNode clazz) throws IOException {
        for (MethodNode method : clazz.methods) {
            if (method.instructions != null) {
                Iterator<AbstractInsnNode> it = method.instructions.iterator();
                while (it.hasNext()) {
                    AbstractInsnNode insn = it.next();
                    if (insn.getType() == AbstractInsnNode.FRAME) {
                        it.remove();
                    }
                }
            }
        }
        return super.otherTransforms(clazz);
    }

}
