package xyz.wagyourtail.jvmdg.util;

import java.io.IOException;

public interface IOConsumer<T> {
    void accept(T t) throws IOException;

}
