package xyz.wagyourtail.jvmdg.j9.intl;

import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_ProcessHandle;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class UnixProcessHandle implements J_L_ProcessHandle {
    private static final Unsafe unsafe = Utils.getUnsafe();
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle waitForProcessExit;

    static {
        MethodHandle waitForProcessExit1;
        try {
            Class<?> unixProcess = Class.forName("java.lang.UNIXProcess");
            waitForProcessExit1 = IMPL_LOOKUP.findVirtual(unixProcess, "waitForProcessExit", MethodType.methodType(int.class, int.class)).bindTo(unsafe.allocateInstance(unixProcess));
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            // we are probably on j9+, fallback on actual processHandle impl stuff
            try {
                waitForProcessExit1 = MethodHandles.insertArguments(IMPL_LOOKUP.findStatic(Class.forName("java.lang.ProcessHandleImpl"), "waitForProcessExit0", MethodType.methodType(int.class, long.class, boolean.class)), 1, false).asType(MethodType.methodType(int.class, int.class));
            } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }

        waitForProcessExit = waitForProcessExit1;
    }

    private final long pid;
    private final String[] cmdline;
    private String[] pidInfo;

    public UnixProcessHandle(long pid) {
        this.pid = pid;
        pidInfo = readPidInfo();
        cmdline = readCmdLine();
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
                String args = new String(Files.readAllBytes(pth));
                if (args.isEmpty()) {
                    return null;
                }
                return args.split("\0");
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
        try (Stream<Path> stream = Files.list(pth)) {
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
            if (pid > Integer.MAX_VALUE) {
                throw MissingStubError.create();
            }
            try {
                int exitCode = (int) waitForProcessExit.invokeExact((int) pid);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return new UnixProcessHandle(pid);
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
