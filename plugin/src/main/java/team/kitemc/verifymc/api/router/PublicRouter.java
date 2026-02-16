package team.kitemc.verifymc.api.router;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import team.kitemc.verifymc.api.handler.RegistrationApiHandler;
import team.kitemc.verifymc.web.RegistrationHandler;

public class PublicRouter {
    private final HttpHandler registrationHandler;

    public PublicRouter(HttpHandler registrationHandler) {
        this.registrationHandler = registrationHandler;
    }

    public void register(HttpServer server) {
        server.createContext("/api/register", new RegistrationApiHandler(new RegistrationHandler(registrationHandler)));
    }
}
