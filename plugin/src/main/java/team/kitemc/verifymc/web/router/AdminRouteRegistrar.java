package team.kitemc.verifymc.web.router;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class AdminRouteRegistrar {
    private final HttpHandler versionCheckHandler;

    public AdminRouteRegistrar(HttpHandler versionCheckHandler) {
        this.versionCheckHandler = versionCheckHandler;
    }

    public void register(HttpServer server) {
        server.createContext("/api/version-check", versionCheckHandler);
    }
}
