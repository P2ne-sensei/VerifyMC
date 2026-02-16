package team.kitemc.verifymc.api.router;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import team.kitemc.verifymc.api.handler.ReviewApiHandler;
import team.kitemc.verifymc.web.ReviewHandler;

public class AdminRouter {
    private final HttpHandler reviewHandler;

    public AdminRouter(HttpHandler reviewHandler) {
        this.reviewHandler = reviewHandler;
    }

    public void register(HttpServer server) {
        server.createContext("/api/review", new ReviewApiHandler(new ReviewHandler(reviewHandler)));
    }
}
