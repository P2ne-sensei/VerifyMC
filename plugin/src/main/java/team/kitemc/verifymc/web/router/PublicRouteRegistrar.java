package team.kitemc.verifymc.web.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class PublicRouteRegistrar {
    private final HttpHandler configHandler;
    private final HttpHandler whitelistCheckHandler;

    public PublicRouteRegistrar(HttpHandler configHandler, HttpHandler whitelistCheckHandler) {
        this.configHandler = configHandler;
        this.whitelistCheckHandler = whitelistCheckHandler;
    }

    public void register(HttpServer server) {
        server.createContext("/api/ping", this::ping);
        server.createContext("/api/config", configHandler);
        server.createContext("/api/check-whitelist", whitelistCheckHandler);
    }

    private void ping(HttpExchange exchange) throws java.io.IOException {
        String resp = "{\"msg\":\"pong\"}";
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        byte[] data = resp.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, data.length);
        OutputStream os = exchange.getResponseBody();
        os.write(data);
        os.close();
    }
}
