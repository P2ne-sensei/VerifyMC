package team.kitemc.verifymc.web.router;

import com.sun.net.httpserver.HttpServer;

public class QuestionnaireRouteRegistrar {
    private final Runnable registration;

    public QuestionnaireRouteRegistrar(Runnable registration) {
        this.registration = registration;
    }

    public void register(HttpServer server) {
        registration.run();
    }
}
