package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Objects;

/**
 * Base handler that delegates to another {@link HttpHandler}.
 */
public abstract class DelegatingHttpHandler implements HttpHandler {
    private final HttpHandler delegate;

    protected DelegatingHttpHandler(HttpHandler delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        delegate.handle(exchange);
    }
}
