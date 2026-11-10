package xyz.wagyourtail.downgradetest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Batch driver for {@code DowngradeTests}: runs many fixture mains inside one
 * JVM so the suite doesn't spawn a JVM per (main x flags) case. Lives in the
 * fixture jar so it gets downgraded along with everything else — it must be
 * excluded from the test-corpus scan (see {@code mainClasses()}).
 *
 * <p>Usage: {@code java -cp app.jar BatchMain <main> [<main>...] -- [appArgs...]}
 * For each main it emits a frame:
 * <pre>
 *   ===JVMDG-BEGIN &lt;binary name&gt;
 *   ...captured stdout+stderr...
 *   ===JVMDG-EXIT &lt;code&gt;
 * </pre>
 * Exit mirrors JVM semantics: a main that throws uncaught prints its
 * stacktrace and gets exit 1.
 */
public class BatchMain {

    private BatchMain() {
    }

    public static void main(String[] args) throws Exception {
        int sep = -1;
        for (int i = 0; i < args.length; i++) {
            if ("--".equals(args[i])) {
                sep = i;
                break;
            }
        }
        if (sep < 0) {
            throw new IllegalArgumentException("usage: BatchMain <main>... -- [args...]");
        }
        List<String> allMains = new ArrayList<>(Arrays.asList(args).subList(0, sep));
        // "++" splits sequential mains (driver thread, before any parallel
        // work) from parallel ones — needed for mains that observe JVM-global
        // state like the process's child list
        int seqSplit = allMains.indexOf("++");
        List<String> seqMains = seqSplit < 0 ? new ArrayList<String>() : new ArrayList<>(allMains.subList(0, seqSplit));
        List<String> mains = seqSplit < 0 ? allMains : new ArrayList<>(allMains.subList(seqSplit + 1, allMains.size()));
        String[] appArgs = Arrays.copyOfRange(args, sep + 1, args.length);

        final PrintStream realOut = System.out;
        PrintStream realErr = System.err;

        // sequential phase: runs before any worker thread exists, so a main
        // enumerating children/threads sees only its own
        for (String name : seqMains) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int exit;
            PrintStream cap = new PrintStream(buf, true, "UTF-8");
            System.setOut(cap);
            System.setErr(cap);
            try {
                invokeMain(Class.forName(name), appArgs);
                exit = 0;
            } catch (InvocationTargetException e) {
                e.getCause().printStackTrace(cap);
                exit = 1;
            } catch (Throwable t) {
                t.printStackTrace(cap);
                exit = 1;
            } finally {
                cap.flush();
                System.setOut(realOut);
                System.setErr(realErr);
            }
            emit(realOut, name, buf, exit);
        }

        int threads = Integer.getInteger("jvmdg.batch.threads",
            Math.max(1, Math.min(mains.size(), Runtime.getRuntime().availableProcessors() / 4)));
        final Router router = new Router(realOut);
        System.setOut(new PrintStream(router, true, "UTF-8"));
        System.setErr(new PrintStream(router, true, "UTF-8"));

        final Semaphore permits = new Semaphore(threads);
        final List<Thread> workers = new ArrayList<>();
        for (final String name : mains) {
            permits.acquire();
            // own ThreadGroup so threads the main spawns inherit its routing
            final ThreadGroup group = new ThreadGroup("jvmdg:" + name);
            final ByteArrayOutputStream buf = router.register(group);
            Thread worker = new Thread(group, new Runnable() {
                @Override
                public void run() {
                    int exit = 0;
                    try {
                        invokeMain(Class.forName(name), appArgs);
                    } catch (InvocationTargetException e) {
                        e.getCause().printStackTrace();
                        exit = 1;
                    } catch (Throwable t) {
                        t.printStackTrace();
                        exit = 1;
                    } finally {
                        try {
                            emit(realOut, name, buf, exit);
                        } catch (IOException e) {
                            synchronized (realOut) {
                                realOut.println("===JVMDG-BEGIN " + name);
                                realOut.println("===JVMDG-EXIT 1");
                            }
                        }
                        permits.release();
                    }
                }
            });
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
    }

    // emitted atomically so frames from parallel mains never interleave
    private static void emit(PrintStream realOut, String name, ByteArrayOutputStream buf, int exit) throws IOException {
        synchronized (realOut) {
            realOut.println("===JVMDG-BEGIN " + name);
            realOut.print(buf.toString("UTF-8"));
            realOut.println("===JVMDG-EXIT " + exit);
        }
    }

    /**
     * Routes writes to the capturing buffer of the current thread's group (or
     * any ancestor group — so threads a main spawns stay captured). Threads
     * outside any registered group (ForkJoinPool.commonPool workers, JVM
     * threads) fall through to the real stream, where their output lands
     * between frames and is ignored by the frame parser — the original-side
     * batch does the same so comparisons stay symmetric.
     */
    private static final class Router extends OutputStream {
        private final PrintStream real;
        private final Map<ThreadGroup, ByteArrayOutputStream> buffers = new ConcurrentHashMap<>();

        Router(PrintStream real) {
            this.real = real;
        }

        ByteArrayOutputStream register(ThreadGroup group) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            buffers.put(group, buf);
            // intentionally never removed: threads still running after their
            // frame was emitted keep routing into the dead buffer rather than
            // leaking into another main's frame
            return buf;
        }

        private OutputStream route() {
            for (ThreadGroup g = Thread.currentThread().getThreadGroup(); g != null; g = g.getParent()) {
                ByteArrayOutputStream buf = buffers.get(g);
                if (buf != null) {
                    return buf;
                }
            }
            return real;
        }

        @Override
        public void write(int b) throws IOException {
            route().write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            route().write(b, off, len);
        }
    }

    // same main-dispatch as xyz.wagyourtail.jvmdg.cli.Main#invokeMain —
    // duplicated because this class runs inside the fixture jar where the cli
    // classes are not on the classpath. Supports the JDK25-style static and
    // instance main forms for original (non-downgraded) runs; downgraded jars
    // normalize every main to the traditional form anyway.
    // package-private: private would force a synthetic nestmate accessor on
    // downgraded jars, leaking an extra frame into stack walks
    static void invokeMain(Class<?> clazz, String[] bootstrapArgs) throws Throwable {
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodHandle traditional = null;
        try {
            traditional = publicLookup.findStatic(clazz, "main", MethodType.methodType(void.class, String[].class));
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
        }
        if (traditional != null) {
            traditional.invoke((Object) bootstrapArgs);
            return;
        }

        // compact/main-form dispatch only exists on JDK25+
        if (Double.parseDouble(System.getProperty("java.class.version")) < 69) {
            throw new IllegalArgumentException("Main method not found");
        }

        Method mWithStr = null;
        try {
            mWithStr = clazz.getDeclaredMethod("main", String[].class);
            if (mWithStr.getReturnType() != void.class) {
                mWithStr = null;
            }
        } catch (NoSuchMethodException ignored) {
        }

        if (mWithStr != null && Modifier.isStatic(mWithStr.getModifiers()) && !Modifier.isPrivate(mWithStr.getModifiers())) {
            mWithStr.setAccessible(true);
            mWithStr.invoke(null, (Object) bootstrapArgs);
            return;
        }

        Method m = null;
        try {
            m = clazz.getDeclaredMethod("main");
            if (m.getReturnType() != void.class) {
                m = null;
            }
        } catch (NoSuchMethodException ignored) {
        }

        if (m != null && Modifier.isStatic(m.getModifiers()) && !Modifier.isPrivate(m.getModifiers())) {
            m.setAccessible(true);
            m.invoke(null);
            return;
        }

        Method instanceMain = findInstanceMainMethod(clazz);
        Object obj;
        try {
            Constructor<?> c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            obj = c.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        instanceMain.setAccessible(true);
        if (instanceMain.getParameterTypes().length > 0) {
            instanceMain.invoke(obj, (Object) bootstrapArgs);
        } else {
            instanceMain.invoke(obj);
        }
    }

    private static Method findInstanceMainMethod(Class<?> clazz) throws NoSuchMethodException {
        try {
            return clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException ignored) {
        }
        Class<?> current = clazz;
        Method noArgsCandidate = null;

        while (current != null && current != Object.class) {
            try {
                Method m = current.getDeclaredMethod("main", String[].class);
                if (!Modifier.isPrivate(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && m.getReturnType() == void.class) {
                    return m;
                }
            } catch (NoSuchMethodException e) {
                // Fall through to check for a no-arg main variant
            }

            if (noArgsCandidate == null) {
                try {
                    noArgsCandidate = current.getDeclaredMethod("main");
                    if (Modifier.isPrivate(noArgsCandidate.getModifiers()) || Modifier.isStatic(noArgsCandidate.getModifiers()) || noArgsCandidate.getReturnType() != void.class) {
                        noArgsCandidate = null;
                    }
                } catch (NoSuchMethodException e) {
                    // Method doesn't exist at this specific layer
                }
            }

            current = current.getSuperclass();
        }

        if (noArgsCandidate != null) {
            return noArgsCandidate;
        }

        try {
            return clazz.getMethod("main");
        } catch (NoSuchMethodException ignored) {
        }

        throw new NoSuchMethodException("no main method found!");
    }
}
