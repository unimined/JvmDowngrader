package xyz.wagyourtail.jvmdg.j17.stub;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

public class C_S_N_H_Filter {

    @Stub(opcVers = Opcodes.V17, ref = @Ref("Lcom/sun/net/httpserver/Filter;"))
    public static Filter beforeHandler(String description, Consumer<HttpExchange> operation) {
        Objects.requireNonNull(description);
        Objects.requireNonNull(operation);
        return new BeforeHandler(description, operation);
    }

    @Stub(opcVers = Opcodes.V17, ref = @Ref("Lcom/sun/net/httpserver/Filter;"))
    public static Filter afterHandler(String description, Consumer<HttpExchange> operation) {
        Objects.requireNonNull(description);
        Objects.requireNonNull(operation);
        return new AfterHandler(description, operation);
    }

    public static class BeforeHandler extends Filter {
        private final Consumer<HttpExchange> operation;
        private final String description;

        public BeforeHandler(String description, Consumer<HttpExchange> operation) {
            super();
            this.operation = operation;
            this.description = description;
        }

        @Override
        public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
            operation.accept(exchange);
            chain.doFilter(exchange);
        }

        @Override
        public String description() {
            return description;
        }

    }

    public static class AfterHandler extends Filter {
        private final Consumer<HttpExchange> operation;
        private final String description;

        public AfterHandler(String description, Consumer<HttpExchange> operation) {
            super();
            this.operation = operation;
            this.description = description;
        }

        @Override
        public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
            chain.doFilter(exchange);
            operation.accept(exchange);
        }

        @Override
        public String description() {
            return description;
        }

    }

}
