package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class J_N_C_Selector {

    @Stub
    public static int select(Selector selector, Consumer<SelectionKey> action, long timeout) throws IOException {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout");
        }
        return SelectorInternal.doSelect(selector, Objects.requireNonNull(action), timeout);
    }

    @Stub
    public static int select(Selector selector, Consumer<SelectionKey> action) throws IOException {
        return SelectorInternal.doSelect(selector, Objects.requireNonNull(action), 0);
    }

    @Stub
    public static int selectNow(Selector selector, Consumer<SelectionKey> action) throws IOException {
        return SelectorInternal.doSelect(selector, Objects.requireNonNull(action), -1);
    }

    public static class SelectorInternal {

        public static int doSelect(Selector selector, Consumer<SelectionKey> action, long timeout) throws IOException {
            synchronized (selector) {
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                synchronized (selectedKeys) {
                    selectedKeys.clear();
                    int numKeySelected;
                    if (timeout < 0) {
                        numKeySelected = selector.selectNow();
                    } else {
                        numKeySelected = selector.select(timeout);
                    }

                    // copy selected-key set as action may remove keys
                    Set<SelectionKey> keysToConsume = Set.copyOf(selectedKeys);
                    assert keysToConsume.size() == numKeySelected;
                    selectedKeys.clear();

                    for (SelectionKey k : keysToConsume) {
                        action.accept(k);
                        if (!selector.isOpen()) {
                            throw new ClosedSelectorException();
                        }
                    }

                    return numKeySelected;
                }
            }
        }

    }

}
