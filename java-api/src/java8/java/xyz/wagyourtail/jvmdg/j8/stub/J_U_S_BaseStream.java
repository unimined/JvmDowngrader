package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Iterator;

@Stub(ref = @Ref("Ljava/util/stream/BaseStream;"))
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
