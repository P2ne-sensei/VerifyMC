package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpHandler;

public class RegistrationHandler extends DelegatingHttpHandler {
    public RegistrationHandler(HttpHandler delegate) {
        super(delegate);
    }
}
