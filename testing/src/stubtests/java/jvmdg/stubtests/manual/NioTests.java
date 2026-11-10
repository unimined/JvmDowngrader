package jvmdg.stubtests.manual;

import jvmdg.stubtests.StubTest;
import jvmdg.stubtests.Support;
import xyz.wagyourtail.jvmdg.version.Ref;

/** Parity tests needing multi-step receiver construction the generator can't do. */
public final class NioTests {

    private NioTests() {
    }

    /** SelectionKey only exists via channel registration on an open selector. */
    @StubTest(member = @Ref(value = "java/nio/channels/SelectionKey",
        member = "interestOpsOr", desc = "(I)I"))
    public static void interestOpsOr(String id) throws Exception {
        java.nio.channels.Selector sel = java.nio.channels.Selector.open();
        java.nio.channels.Pipe.SourceChannel src = java.nio.channels.Pipe.open().source();
        try {
            src.configureBlocking(false);
            java.nio.channels.SelectionKey key =
                src.register(sel, java.nio.channels.SelectionKey.OP_READ);
            Support.print(id, key.interestOpsOr(java.nio.channels.SelectionKey.OP_WRITE));
        } finally {
            src.close();
            sel.close();
        }
    }

    /** WatchService arg is unsynthesizable; register exercises the key path. */
    @StubTest(member = @Ref(value = "java/nio/file/Path", member = "register",
        desc = "(Ljava/nio/file/WatchService;[Ljava/nio/file/WatchEvent$Kind;)"
            + "Ljava/nio/file/WatchKey;"))
    public static void pathRegister(String id) throws Exception {
        java.nio.file.WatchService ws = java.nio.file.FileSystems.getDefault().newWatchService();
        try {
            java.nio.file.WatchKey key = java.nio.file.Paths.get(".").register(ws,
                java.nio.file.StandardWatchEventKinds.ENTRY_CREATE);
            Support.print(id, key.pollEvents().size());
            key.cancel();
        } finally {
            ws.close();
        }
    }
}
