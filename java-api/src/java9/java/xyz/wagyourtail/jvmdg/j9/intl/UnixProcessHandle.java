package xyz.wagyourtail.jvmdg.j9.intl;

import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_ProcessHandle;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class UnixProcessHandle implements J_L_ProcessHandle {
    private final long pid;
    private String[] pidInfo;
    private final String[] cmdline;

    public UnixProcessHandle(long pid) {
        this.pid = pid;
        pidInfo = readPidInfo();
        cmdline = readCmdLine();
    }

    public static boolean isUnix() {
        return File.pathSeparatorChar == ':';
    }

    /**
     * <a href="https://man7.org/linux/man-pages/man5/proc_pid_stat.5.html">/proc/&lt;pid&gt;/stat</a>
     */
    private String[] readPidInfo() {
        Path pth = Paths.get("/proc/" + pid + "/stat");
        if (Files.isReadable(pth)) {
            try {
                pidInfo = new String(Files.readAllBytes(pth)).split(" ");
            } catch (IOException e) {
                pidInfo = null;
            }
        }
        return pidInfo;
    }

    /**
     * <a href="https://man7.org/linux/man-pages/man5/proc_pid_cmdline.5.html">/proc/&lt;pid&gt;/cmdline</a>
     */
    private String[] readCmdLine() {
        Path pth = Paths.get("/proc/" + pid + "/cmdline");
        if (Files.isReadable(pth)) {
            try {
                return new String(Files.readAllBytes(pth)).split("\0");
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public long pid() {
        return pid;
    }

    @Override
    public Optional<J_L_ProcessHandle> parent() {
        String[] info = readPidInfo();
        if (info != null) {
            return Optional.of(new UnixProcessHandle(Long.parseLong(info[3])));
        }
        return Optional.empty();
    }

    @Override
    public Stream<J_L_ProcessHandle> children() {
        Path pth = Paths.get("/proc/" + pid + "/task");
        try(Stream<Path> stream = Files.list(pth)) {
            return Stream.of(stream.toArray(Path[]::new)).flatMap(e -> {
                try {
                    String s = new String(Files.readAllBytes(e.resolve("children")));
                    if (s.isEmpty()) {
                        return Stream.empty();
                    }
                    return Arrays.stream(s.split(" "));
                } catch (IOException ex) {
                    return Stream.empty();
                }
            }).mapToLong(Long::parseLong).mapToObj(UnixProcessHandle::new);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Stream<J_L_ProcessHandle> descendants() {
        return children().flatMap(e -> Stream.concat(Stream.of(e), e.descendants()));
    }

    @Override
    public Info info() {
        return new Info() {
            @Override
            public Optional<String> command() {
                if (cmdline != null) {
                    return Optional.ofNullable(cmdline[0]);
                }
                return Optional.empty();
            }

            @Override
            public Optional<String> commandLine() {
                if (cmdline != null) {
                    return Optional.of(String.join(" ", cmdline));
                }
                return Optional.empty();
            }

            @Override
            public Optional<String[]> arguments() {
                if (cmdline != null) {
                    String[] args = new String[cmdline.length - 1];
                    System.arraycopy(cmdline, 1, args, 0, cmdline.length - 1);
                    return Optional.of(args);
                }
                return Optional.empty();
            }

            @Override
            public Optional<Instant> startInstant() {
                String[] info = readPidInfo();
                if (info != null) {
                    return Optional.of(Instant.ofEpochMilli(Long.parseLong(info[22])));
                }
                return Optional.empty();
            }

            @Override
            public Optional<Duration> totalCpuDuration() {
                String[] info = readPidInfo();
                if (info != null) {
                    return Optional.of(Duration.ofMillis(Long.parseLong(info[14])));
                }
                return Optional.empty();
            }

            @Override
            public Optional<String> user() {
                try {
                    return Optional.of(Files.getOwner(Paths.get("/proc/" + pid + "/cmdline")).getName());
                } catch (IOException e) {
                    return Optional.empty();
                }
            }
        };
    }

    @Override
    public CompletableFuture<J_L_ProcessHandle> onExit() {
        return CompletableFuture.supplyAsync(() -> {
            try (WatchService ws = FileSystems.getDefault().newWatchService()) {
                Path pth = Paths.get("/proc/" + pid + "/status");
                pth.register(ws, StandardWatchEventKinds.ENTRY_DELETE);
                ws.take();
                return this;
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean supportsNormalTermination() {
        return true;
    }

    @Override
    public boolean destroy() {
        ProcessBuilder pb = new ProcessBuilder("kill", Long.toString(pid));
        try {
            Process p = pb.start();
            p.waitFor();
            return p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Override
    public boolean destroyForcibly() {
        ProcessBuilder pb = new ProcessBuilder("kill", "-9", Long.toString(pid));
        try {
            Process p = pb.start();
            p.waitFor();
            return p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Override
    public boolean isAlive() {
        return Files.exists(Paths.get("/proc/" + pid + "/status"));
    }

    @Override
    public int compareTo(@NotNull J_L_ProcessHandle other) {
        return Long.compare(pid, other.pid());
    }
}
