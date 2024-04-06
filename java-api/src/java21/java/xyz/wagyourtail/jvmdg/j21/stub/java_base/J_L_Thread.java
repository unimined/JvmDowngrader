package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.FakeVirtualThread;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.ThreadFactory;
import java.util.function.LongFunction;
import java.util.function.Supplier;

public class J_L_Thread {

    @Stub(ref = @Ref("java/lang/Thread"))
    public static Builder.OfPlatform ofPlatform() {
        return new PlatformClassBuilder();
    }

    @Stub(ref = @Ref("java/lang/Thread"))
    public static Builder.OfVirtual ofVirtual() {
        return new VirtualThreadBuilder();
    }

    @Stub(ref = @Ref("java/lang/Thread"))
    public static Thread startVirtualThread(Runnable runnable) {
        return new FakeVirtualThread(runnable, new Thread().getName(), true);
    }

    @Stub
    public static boolean isVirtual(Thread self) {
        return self instanceof FakeVirtualThread;
    }


    @Stub(ref = @Ref("java/lang/Thread$Builder"))
    public interface Builder {

        ThreadFactory factory();

        Builder inheritInheritableThreadLocals(boolean inherit);

        Builder name(String name);

        Builder name(String prefix, long start);

        Thread start(Runnable target);

        Builder uncaughtExceptionHandler(Thread.UncaughtExceptionHandler handler);

        Thread unstarted(Runnable task);

        @Stub(ref = @Ref("java/lang/Thread$Builder$OfPlatform"))
        interface OfPlatform extends Builder {
            default OfPlatform daemon() {
                return this.daemon(true);
            }

            OfPlatform daemon(boolean daemon);

            OfPlatform group(ThreadGroup group);

            @Override
            OfPlatform inheritInheritableThreadLocals(boolean inherit);

            @Override
            OfPlatform name(String name);

            @Override
            OfPlatform name(String prefix, long start);

            OfPlatform priority(int priority);

            OfPlatform stackSize(long stackSize);

            @Override
            OfPlatform uncaughtExceptionHandler(Thread.UncaughtExceptionHandler handler);

        }

        @Stub(ref = @Ref("java/lang/Thread$Builder$OfVirtual"))
        interface OfVirtual extends Builder {

            @Override
            OfVirtual inheritInheritableThreadLocals(boolean inherit);

            @Override
            OfVirtual name(String name);

            @Override
            OfVirtual name(String prefix, long start);

            @Override
            OfVirtual uncaughtExceptionHandler(Thread.UncaughtExceptionHandler handler);

        }
    }

    private static abstract class BuilderImpl<T extends BuilderImpl<T>> implements Builder {
        boolean inheritInheritableThreadLocals = true;
        Supplier<String> name;
        long count = 0;
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

        BuilderImpl() {
        }

        BuilderImpl(T other) {
            this.inheritInheritableThreadLocals = other.inheritInheritableThreadLocals;
            this.name = other.name;
            this.count = other.count;
            this.uncaughtExceptionHandler = other.uncaughtExceptionHandler;
        }

        abstract T copy();

        @Override
        public ThreadFactory factory() {
            var copy = copy();
            return copy::unstarted;
        }

        @Override
        public T inheritInheritableThreadLocals(boolean inherit) {
            this.inheritInheritableThreadLocals = inherit;
            return (T) this;
        }

        @Override
        public T name(String name) {
            this.name = () -> name;
            return (T) this;
        }

        @Override
        public T name(String prefix, long start) {
            this.name = () -> prefix + "-" + count++;
            count = start;
            return (T) this;
        }

        @Override
        public T uncaughtExceptionHandler(Thread.UncaughtExceptionHandler handler) {
            this.uncaughtExceptionHandler = handler;
            return (T) this;
        }
    }

    private static class PlatformClassBuilder extends BuilderImpl<PlatformClassBuilder> implements Builder.OfPlatform {
        boolean daemon = false;
        ThreadGroup group;
        int priority = Thread.NORM_PRIORITY;
        long stackSize = 0;

        PlatformClassBuilder() {
            super();
        }

        PlatformClassBuilder(PlatformClassBuilder other) {
            super(other);
            this.daemon = other.daemon;
            this.group = other.group;
            this.priority = other.priority;
            this.stackSize = other.stackSize;
        }

        @Override
        public Thread start(Runnable target) {
            Thread thread = unstarted(target);
            thread.start();
            return thread;
        }

        @Override
        public Thread unstarted(Runnable task) {
            Thread thread = new Thread(group, task, name == null ? new Thread().getName() : name.get(), stackSize, inheritInheritableThreadLocals);
            thread.setDaemon(daemon);
            thread.setPriority(priority);
            thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            return thread;
        }


        @Override
        PlatformClassBuilder copy() {
            return new PlatformClassBuilder(this);
        }

        @Override
        public OfPlatform daemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        @Override
        public OfPlatform group(ThreadGroup group) {
            this.group = group;
            return this;
        }

        @Override
        public OfPlatform priority(int priority) {
            if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
                throw new IllegalArgumentException("Priority out of range: " + priority);
            }
            this.priority = priority;
            return this;
        }

        @Override
        public OfPlatform stackSize(long stackSize) {
            if (stackSize < 0) {
                throw new IllegalArgumentException("Non-positive stack size: " + stackSize);
            }
            this.stackSize = stackSize;
            return this;
        }
    }

    private static class VirtualThreadBuilder extends BuilderImpl<VirtualThreadBuilder> implements Builder.OfVirtual {

        VirtualThreadBuilder() {
            super();
        }

        VirtualThreadBuilder(VirtualThreadBuilder other) {
            super(other);
        }

        @Override
        public Thread start(Runnable target) {
            Thread thread = unstarted(target);
            thread.start();
            return thread;
        }

        @Override
        public Thread unstarted(Runnable task) {
            FakeVirtualThread thread = new FakeVirtualThread(task, name == null ? new Thread().getName() : name.get(), inheritInheritableThreadLocals);
            thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            return thread;
        }

        @Override
        VirtualThreadBuilder copy() {
            return new VirtualThreadBuilder(this);
        }

    }

}
