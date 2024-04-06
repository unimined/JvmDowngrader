package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class J_U_ServiceLoader {


    @Stub
    public static <S> Stream<Provider<S>> stream(ServiceLoader<S> loader) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(loader.iterator(), 0), false)
            .map(ProviderImpl::new);
    }

//    @Stub(javaVersion = Opcodes.V9)
//    public static <S> ServiceLoader<S> load(ModuleLayer layer, Class<S> service) {
//        throw new UnsupportedOperationException("JVMDowngrade does not support this method yet.");
//    }

    @Stub
    public static <S> Optional<S> findFirst(ServiceLoader<S> loader) {
        Iterator<S> iterator = loader.iterator();
        return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }


    @Adapter("Ljava/util/ServiceLoader$Provider;")
    public interface Provider<S> extends Supplier<S> {

        Class<? extends S> type();

        @Override
        S get();

    }

    public static class ProviderImpl<S> implements Provider<S> {
        private final S service;

        public ProviderImpl(S service) {
            this.service = service;
        }

        @Override
        public Class<? extends S> type() {
            return (Class<? extends S>) service.getClass();
        }

        @Override
        public S get() {
            return service;
        }

    }

}
