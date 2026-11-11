package xyz.wagyourtail.jvmdg.pipeline;

import org.objectweb.asm.tree.ClassNode;

/**
 * Per-class guard evaluated once when a class enters a {@link Stage}, before
 * any of the stage's transforms run. Throwing fails the class's run — this is
 * where assertions like "the class file version matches this stage's input"
 * live; the engine doesn't know what a precondition checks.
 */
public interface ClassPrecondition {

    void check(ClassNode node);

}
