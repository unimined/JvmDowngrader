package xyz.wagyourtail.jvmdg.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class ASMUtils {

    public static ClassNode bytesToClassNode(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    public static ClassNode bytesToClassNode(byte[] bytes, int flags) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, flags);
        return node;
    }

}
