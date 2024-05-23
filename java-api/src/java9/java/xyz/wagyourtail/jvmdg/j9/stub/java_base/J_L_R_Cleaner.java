package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ThreadFactory;

@Adapter("java/lang/ref/Cleaner")
public class J_L_R_Cleaner {
    private static int count = 1;
    private static final ThreadFactory DEFAULT_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "Cleaner-" + count++);
        thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
        thread.setPriority(Thread.MAX_PRIORITY - 2);
        return thread;
    };

    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    private J_L_R_Cleaner(ThreadFactory factory) {
        Thread t = factory.newThread(() -> {
            while (true) {
                try {
                    PhantomCleanable<?> ref = (PhantomCleanable<?>) queue.remove();
                    if (ref != null) {
                        ref.clean();
                    }
                } catch (Throwable e) {
                    System.err.println("Error in Cleaner thread");
                    e.printStackTrace(System.err);
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public static J_L_R_Cleaner create() {
        return create(DEFAULT_FACTORY);
    }

    public static J_L_R_Cleaner create(ThreadFactory factory) {
        return new J_L_R_Cleaner(factory);
    }

    public Cleanable register(Object obj, Runnable action) {
        return new PhantomCleanable<>(obj, queue, action);
    }

    @Adapter("java/lang/ref/Cleaner$Cleanable")
    public interface Cleanable {

        void clean();

    }

    private static class PhantomCleanable<T> extends PhantomReference<T> implements Cleanable {
        private final Runnable action;

        public PhantomCleanable(T t, ReferenceQueue<? super T> referenceQueue, Runnable action) {
            super(t, referenceQueue);
            this.action = action;
        }

        @Override
        public void clean() {
            clear();
            action.run();
        }

    }

}
