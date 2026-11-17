package xyz.wagyourtail.jvmdg.compile.jar;

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Downgrades each class entry through {@code ctx.downgrader} and emits every
 * produced artifact — finals, multi-release variants (already routed to
 * {@code META-INF/versions/<n>/} by the downgrader), and generated helper
 * classes. Classes already at or below the target version pass through as
 * lazy copies (the downgrader returns {@code null}), matching the legacy
 * {@code PathDowngrader} behavior.
 */
public class ClassDowngradeStep extends JarStep {

    @Override
    public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
        if (!entry.name.endsWith(".class")) {
            return entry;
        }
        String internalName = entry.name.substring(0, entry.name.length() - ".class".length());
        try {
            Map<String, byte[]> outputs = ctx.downgrader.downgrade(
                new AtomicReference<>(internalName), entry.bytes(), false, ctx.extraRead);
            if (outputs == null) {
                // already at or below target — pass through from (possibly
                // multi-release-swapped) source
                return entry;
            }
            for (Map.Entry<String, byte[]> out : outputs.entrySet()) {
                ctx.sink.put(out.getKey() + ".class", out.getValue());
            }
            return null;
        } catch (IllegalClassFormatException e) {
            throw new IOException("Failed to downgrade " + entry.name, e);
        }
    }

}
