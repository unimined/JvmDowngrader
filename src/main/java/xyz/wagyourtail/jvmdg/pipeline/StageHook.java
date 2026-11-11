package xyz.wagyourtail.jvmdg.pipeline;

import java.io.IOException;

/**
 * A once-per-stage lifecycle hook with context access — {@link Stage#onEnter}
 * runs after this stage's worklist exists (so hooks can emit classes that
 * join the remaining stages) and before variant captures and the class
 * loop; {@link Stage#onExit} runs after the class loop drains.
 */
public interface StageHook {

    void run(TransformContext ctx) throws IOException;

}
