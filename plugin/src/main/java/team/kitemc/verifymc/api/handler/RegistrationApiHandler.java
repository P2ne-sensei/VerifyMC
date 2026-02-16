package team.kitemc.verifymc.api.handler;

import com.sun.net.httpserver.HttpHandler;
import team.kitemc.verifymc.web.DelegatingHttpHandler;

public class RegistrationApiHandler extends DelegatingHttpHandler {
    public RegistrationApiHandler(HttpHandler delegate) {
        super(delegate);
    }
}
