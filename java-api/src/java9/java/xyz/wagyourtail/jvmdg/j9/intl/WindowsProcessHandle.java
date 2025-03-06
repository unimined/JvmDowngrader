package xyz.wagyourtail.jvmdg.j9.intl;

import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_ProcessHandle;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WindowsProcessHandle implements J_L_ProcessHandle {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle waitForProcessExit;
    private static final MethodHandle terminateProcess;
    private static final MethodHandle isProcessAlive;

    public static final String wmicLocation = System.getenv("windir") + "\\System32\\wbem\\WMIC.exe";

    static {
        MethodHandle waitForProcessExit1;
        try {
            Class<?> winProcess = Class.forName("java.lang.ProcessImpl");
            waitForProcessExit1 = IMPL_LOOKUP.findStatic(winProcess, "waitForInterruptibly", MethodType.methodType(void.class, long.class));
            terminateProcess = IMPL_LOOKUP.findStatic(winProcess, "terminateProcess", MethodType.methodType(void.class, long.class));
            isProcessAlive = IMPL_LOOKUP.findStatic(winProcess, "isProcessAlive", MethodType.methodType(boolean.class, long.class));

        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        waitForProcessExit = waitForProcessExit1;
    }

    private final long pid;
    private final Map<String, String> info;
    private final String[] commandLine;

    public WindowsProcessHandle(long pid) {
        this.pid = pid;
        this.info = readInfo();
        this.commandLine = parseCommandLine();
    }

    public Map<String, String> readInfo() {
        try {
            Map<String, String> info = new HashMap<>();
            ProcessBuilder pb = new ProcessBuilder(wmicLocation, "process", "where", "ProcessID=" + pid, "get", "/format:list");
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                br.lines().forEach(e -> {
                    if (e.isEmpty() || !e.contains("=")) return;
                    String[] line = e.split("=", 2);
                    info.put(line[0], line[1]);
                });
            }
            return info;
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    public String[] parseCommandLine() {
        List<String> processed = new ArrayList<>();
        String cmd = info.getOrDefault("CommandLine", "").trim();
        int i = 0;
        while (i < cmd.length()) {
            char c = cmd.charAt(i);
            if (c == ' ') {
                i++;
                continue;
            }
            int next;
            if (c == '"' || c == '\'') {
                next = cmd.indexOf(c, i);
                processed.add(cmd.substring(i, next).replace("\\" + c, String.valueOf(c)));
            } else {
                next = cmd.indexOf(' ', i);
                if (next == -1) next = cmd.length();
                processed.add(cmd.substring(i, next));
            }
            i = next + 1;
        }
        return processed.toArray(new String[0]);
    }


    @Override
    public long pid() {
        return pid;
    }

    @Override
    public Optional<J_L_ProcessHandle> parent() {
        String parent = info.get("ParentProcessId");
        if (parent == null || parent.isEmpty()) return Optional.empty();
        return Optional.of(new WindowsProcessHandle(Long.parseLong(parent.trim())));
    }

    @Override
    public Stream<J_L_ProcessHandle> children() {
        try {
            ProcessBuilder pb = new ProcessBuilder(wmicLocation, "process", "where", "ParentProcessID=" + pid, "get", "ProcessId");
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                return Stream.of(br.lines().skip(1).filter(e -> !e.isEmpty()).map(e -> new WindowsProcessHandle(Long.parseLong(e.trim()))).toArray(J_L_ProcessHandle[]::new));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Stream<J_L_ProcessHandle> descendants() {
        return children().flatMap(e -> Stream.concat(Stream.of(e), e.descendants()));
    }

    private static final Pattern USER_NAME = Pattern.compile("User = \"(.+)\";$", Pattern.MULTILINE);

    @Override
    public Info info() {
        return new Info() {
            @Override
            public Optional<String> command() {
                String s = info.get("ExecutablePath");
                if (s == null || s.isEmpty()) return Optional.empty();
                return Optional.of(s.trim());
            }

            @Override
            public Optional<String> commandLine() {
                String s = info.get("CommandLine");
                if (s == null || s.isEmpty()) return Optional.empty();
                return Optional.of(s.trim());
            }

            @Override
            public Optional<String[]> arguments() {
                if (commandLine != null) {
                    if (commandLine.length < 1) {
                        return Optional.of(new String[0]);
                    }
                    String[] args = new String[commandLine.length - 1];
                    System.arraycopy(commandLine, 1, args, 0, commandLine.length - 1);
                    return Optional.of(args);
                }
                return Optional.empty();
            }

            @Override
            public Optional<Instant> startInstant() {
                String s = info.get("CreationDate");
                if (s == null || s.isEmpty()) return Optional.empty();
                return Optional.of(Instant.parse(s.trim()));
            }

            @Override
            public Optional<Duration> totalCpuDuration() {
                String a = info.getOrDefault("KernelModeTime", "0").trim();
                String b = info.getOrDefault("UserModeTime", "0").trim();

                if (a.isEmpty()) a = "0";
                if (b.isEmpty()) b = "0";

                return Optional.of(Duration.ofMillis(Long.parseLong(a) + Long.parseLong(b)));
            }

            @Override
            public Optional<String> user() {
                try {
                    ProcessBuilder pb = new ProcessBuilder(wmicLocation, "process", "where", "ProcessID=" + pid, "call", "GetOwner");
                    Process p = pb.start();
                    try (InputStream is = p.getInputStream()) {
                        String s = new String(Utils.readAllBytes(is));
                        Matcher m = USER_NAME.matcher(s);
                        if (!m.find()) return Optional.empty();
                        return Optional.of(m.group(1).replace("\\\"", "\""));
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
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
                if (info.isEmpty()) {
                    waitForProcessExit.invokeExact(pid);
                    return this;
                }
                long pid = Long.parseLong(info.get("Handle").trim());
                waitForProcessExit.invokeExact(pid);
                if (Thread.interrupted()) throw new InterruptedException();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return this;
        });
    }

    @Override
    public boolean supportsNormalTermination() {
        return true;
    }

    @Override
    public boolean destroy() {
        try {
            terminateProcess.invokeExact((long) Long.parseLong(info.get("Handle").trim()));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean destroyForcibly() {
        return destroy();
    }

    @Override
    public boolean isAlive() {
        try {
            return (boolean) isProcessAlive.invokeExact((long) Long.parseLong(info.get("Handle").trim()));
        } catch (Throwable e) {
            throw new RuntimeException();
        }
    }

    @Override
    public int compareTo(@NotNull J_L_ProcessHandle other) {
        return Long.compare(pid, other.pid());
    }

}
