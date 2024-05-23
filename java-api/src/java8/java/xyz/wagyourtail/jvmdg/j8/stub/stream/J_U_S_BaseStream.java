package xyz.wagyourtail.jvmdg.j8.stub.stream;

import xyz.wagyourtail.jvmdg.j8.stub.J_U_Spliterator;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.Iterator;

@Adapter("Ljava/util/stream/BaseStream;")
public interface J_U_S_BaseStream<T, S extends J_U_S_BaseStream<T, S>> extends AutoCloseable {

    Iterator<T> iterator();

    J_U_Spliterator<T> spliterator();

    boolean isParallel();

    S sequential();

    S parallel();

    S unordered();

    S onClose(Runnable closeHandler);

    @Override
    void close();

}
