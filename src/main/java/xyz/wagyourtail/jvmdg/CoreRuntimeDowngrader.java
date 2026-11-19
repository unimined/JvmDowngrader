package xyz.wagyourtail.jvmdg;

import xyz.wagyourtail.jvmdg.stub.runtime.RuntimeDowngrader;
import xyz.wagyourtail.jvmdg.util.Function;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Core-jar backend for {@link OptionalDependency} — registered via
 * {@code META-INF/services/xyz.wagyourtail.jvmdg.stub.runtime.RuntimeDowngrader} so
 * api-jar stubs discover it only when the core jar shares their classpath.
 */
public class CoreRuntimeDowngrader implements RuntimeDowngrader {

    @Override
    public Map<String, byte[]> downgrade(AtomicReference<String> name, byte[] bytes,
                                         boolean enableRuntime, Function<String, byte[]> getExtraRead)
        throws IllegalClassFormatException {
        return ClassDowngrader.getCurrentVersionDowngrader().downgrade(name, bytes, enableRuntime, getExtraRead);
    }

}
