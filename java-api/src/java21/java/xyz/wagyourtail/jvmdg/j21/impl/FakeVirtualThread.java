package xyz.wagyourtail.jvmdg.j21.impl;

public class FakeVirtualThread extends Thread {
    private static final ThreadGroup VIRTUAL_THREAD_GROUP = new ThreadGroup("Virtual Threads");

    public FakeVirtualThread(Runnable target, String name, boolean inheritThreadLocals) {
        super(VIRTUAL_THREAD_GROUP, target, name, 0, inheritThreadLocals);
        setDaemon(true);
    }

    @Override
    public void start() {
        super.start(); // TODO: maybe some kind of pool?
    }

}
