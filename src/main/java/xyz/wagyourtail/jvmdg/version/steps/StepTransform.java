package xyz.wagyourtail.jvmdg.version.steps;

import xyz.wagyourtail.jvmdg.pipeline.FusibleTransform;
import xyz.wagyourtail.jvmdg.pipeline.FusedTransform;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.util.Arrays;

/**
 * Base for the decomposed provider sub-transforms — each is a self-contained
 * step in the {@link VersionProvider#downgrade} sequence, so adjacent steps
 * compose by straight sequence and fusion is a
 * {@link FusedTransform} wrapping.
 */
public abstract class StepTransform extends FusibleTransform {

    protected final VersionProvider provider;

    protected StepTransform(VersionProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean canFuseWith(FusibleTransform next) {
        return true;
    }

    @Override
    public FusibleTransform fuse(FusibleTransform next) {
        return new FusedTransform(Arrays.asList(this, next));
    }

}
