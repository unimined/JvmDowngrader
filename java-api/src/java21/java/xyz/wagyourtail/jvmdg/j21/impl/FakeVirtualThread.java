package xyz.wagyourtail.jvmdg.j21.impl;

public class FakeVirtualThread extends Thread {
    private static final ThreadGroup VIRTUAL_THREAD_GROUP;
    static {
        ThreadGroup parent = Thread.currentThread().getThreadGroup();
        while (parent.getParent() != null) {
            parent = parent.getParent();
        }
        VIRTUAL_THREAD_GROUP = new ThreadGroup(parent, "VirtualThreads");
        VIRTUAL_THREAD_GROUP.setMaxPriority(Thread.MAX_PRIORITY);
    }

    public FakeVirtualThread(Runnable target, String name, boolean inheritThreadLocals) {
        super(VIRTUAL_THREAD_GROUP, target, name, 0, inheritThreadLocals);
        setDaemon(true);
    }

    @Override
    public void start() {
        super.start(); // TODO: maybe some kind of pool?
    }

}
