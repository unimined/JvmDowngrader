package xyz.wagyourtail.jvmdg.internal.mods.stub._17;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

public class C_S_N_H_Filter {

    @Stub(value = JavaVersion.VERSION_17, desc = "Lcom/sun/net/httpserver/Filter;", include = BeforeHandler.class)
    public static Filter beforeHandler(String description, Consumer<HttpExchange> operation) {
        Objects.requireNonNull(description);
        Objects.requireNonNull(operation);
        return new BeforeHandler(description, operation);
    }

    @Stub(value = JavaVersion.VERSION_17, desc = "Lcom/sun/net/httpserver/Filter;", include = AfterHandler.class)
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
