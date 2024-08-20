package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.jetbrains.annotations.NotNull;
import sun.management.ManagementFactoryHelper;
import sun.management.VMManagement;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j9.intl.UnixProcessHandle;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Implementation of ProcessHandle is very incomplete, as can only get this kind of info on the
 * current process without access to natives.
 * <p>
 * If you know some funny sun classes or something to get other processes, lmk
 */
@Adapter("java/lang/ProcessHandle")
public interface J_L_ProcessHandle extends Comparable<J_L_ProcessHandle> {
    RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();

    static Optional<J_L_ProcessHandle> of(long pid) {
        if (UnixProcessHandle.isUnix()) {
            return Optional.of(new UnixProcessHandle(pid));
        }
        throw MissingStubError.create();
    }

    static J_L_ProcessHandle current() {
        String pidName = bean.getName();
        long pid = Long.parseLong(pidName.substring(0, pidName.indexOf('@')));
        if (UnixProcessHandle.isUnix()) {
            return new UnixProcessHandle(pid) {
                @Override
                public CompletableFuture<J_L_ProcessHandle> onExit() {
                    throw new IllegalStateException();
                }
            };
        }
        throw MissingStubError.create();
    }

    static Stream<J_L_ProcessHandle> allProcesses() {
        if (UnixProcessHandle.isUnix()) {
            return of(0).get().descendants();
        }
        throw MissingStubError.create();
    }

    long pid();

    Optional<J_L_ProcessHandle> parent();

    Stream<J_L_ProcessHandle> children();

    Stream<J_L_ProcessHandle> descendants();

    Info info();

    CompletableFuture<J_L_ProcessHandle> onExit();

    boolean supportsNormalTermination();

    boolean destroy();

    boolean destroyForcibly();

    boolean isAlive();

    int hashCode();

    boolean equals(Object obj);

    @Override
    int compareTo(@NotNull J_L_ProcessHandle other);

    @Adapter("java/lang/ProcessHandle$Info")
    interface Info {
        Optional<String> command();

        Optional<String> commandLine();

        Optional<String[]> arguments();

        Optional<Instant> startInstant();

        Optional<Duration> totalCpuDuration();

        Optional<String> user();
    }
}
