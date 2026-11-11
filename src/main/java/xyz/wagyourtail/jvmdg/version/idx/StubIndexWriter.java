package xyz.wagyourtail.jvmdg.version.idx;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

/**
 * Build-time entry point that writes {@code META-INF/jvmdg/stubs.idx}: runs
 * every {@link VersionProvider} found by {@link ServiceLoader} through
 * {@link VersionProvider#ensureInit} — the same reflective scan the runtime
 * uses, so the index is parity-by-construction — then serializes the
 * resulting registries via {@link StubIndex}.
 *
 * <p>Usage: {@code StubIndexWriter <output-file> <jvmdg-version>}. The output
 * file should be packaged into the api jar at {@link StubIndex#RESOURCE}.
 */
public class StubIndexWriter {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: StubIndexWriter <output-file> <jvmdg-version>");
        }
        Path out = Paths.get(args[0]);
        List<VersionProvider> providers = new ArrayList<>();
        for (VersionProvider vp : ServiceLoader.load(VersionProvider.class)) {
            providers.add(vp);
        }
        ClassDowngrader downgrader = ClassDowngrader.downgradeTo(new Flags());
        for (VersionProvider vp : providers) {
            vp.ensureInit(downgrader);
        }
        StubIndex.write(out, args[1], providers);
    }

    private StubIndexWriter() {
    }
}
