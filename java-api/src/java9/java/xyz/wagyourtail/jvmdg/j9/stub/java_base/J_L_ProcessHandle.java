package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.jvmdg.j9.intl.UnixProcessHandle;
import xyz.wagyourtail.jvmdg.j9.intl.WindowsProcessHandle;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Adapter("java/lang/ProcessHandle")
public interface J_L_ProcessHandle extends Comparable<J_L_ProcessHandle> {
    RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();

    static boolean isUnix() {
        return File.pathSeparatorChar == ':';
    }

    static Optional<J_L_ProcessHandle> of(long pid) {
        if (isUnix()) {
            return Optional.of(new UnixProcessHandle(pid));
        } else {
            return Optional.of(new WindowsProcessHandle(pid));
        }
    }

    static J_L_ProcessHandle current() {
        String pidName = bean.getName();
        long pid = Long.parseLong(pidName.substring(0, pidName.indexOf('@')));
        if (isUnix()) {
            return new UnixProcessHandle(pid) {
                @Override
                public CompletableFuture<J_L_ProcessHandle> onExit() {
                    throw new IllegalStateException();
                }
            };
        } else {
            return new WindowsProcessHandle(pid) {
                @Override
                public CompletableFuture<J_L_ProcessHandle> onExit() {
                    throw new IllegalStateException();
                }
            };
        }
    }

    static Stream<J_L_ProcessHandle> allProcesses() {
        if (isUnix()) {
            Path pth = Paths.get("/proc");
            try (Stream<Path> stream = Files.list(pth)) {
                List<J_L_ProcessHandle> ph = stream.map(Path::getFileName).map(Path::toString).mapToLong(Long::parseLong).mapToObj(J_L_ProcessHandle::of).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
                return ph.stream();
            } catch (IOException e) {
                return Stream.empty();
            }
        } else {
            ProcessBuilder pb = new ProcessBuilder(WindowsProcessHandle.wmicLocation, "process", "get", "ProcessID");
            try {
                Process p = pb.start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    return br.lines().skip(1).filter(e -> !e.trim().isEmpty()).map(e -> new WindowsProcessHandle(Long.parseLong(e.trim())));
                }
            } catch (IOException e) {
                return Stream.empty();
            }
        }
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
