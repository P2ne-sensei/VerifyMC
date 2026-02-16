package team.kitemc.verifymc.api.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

public class ReviewApiHandler implements HttpHandler {
    private final HttpHandler delegate;

    public ReviewApiHandler(HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        delegate.handle(exchange);
    }
}
