package xyz.wagyourtail.jvmdg.providers;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.pipeline.Stage;
import xyz.wagyourtail.jvmdg.pipeline.TransformContributor;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Appends the {@code --noIndy} sweep at {@link Anchor#AFTER_VERSION_CHAIN}
 * — after boundary desugars have run and before shade stages. Inert unless
 * {@code flags.noIndy} is set. The {@code indyDesugars} union is built here
 * (stage-list time): off-chain provider init mutates the resolver graph,
 * which is only safe before any class transforms — doing it lazily inside
 * the sweep poisons resolutions for classes the chain hasn't reached yet.
 */
public class NoIndyContributor implements TransformContributor {

    @Override
    public void contribute(Anchor anchor, ClassDowngrader downgrader, List<Stage> out) {
        if (anchor != Anchor.AFTER_VERSION_CHAIN || downgrader == null || !downgrader.flags.noIndy) {
            return;
        }
        List<String> initFailures = new ArrayList<>();
        Map<MemberNameAndDesc, List<MethodRef>> desugars =
            NoIndyTransform.unionDesugars(downgrader, initFailures);
        out.add(new Stage("downgrade:noIndy").transform(new NoIndyTransform(desugars, initFailures)));
    }
}
