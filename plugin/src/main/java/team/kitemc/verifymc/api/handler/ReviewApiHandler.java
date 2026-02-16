package team.kitemc.verifymc.api.handler;

import com.sun.net.httpserver.HttpHandler;
import team.kitemc.verifymc.web.DelegatingHttpHandler;

public class ReviewApiHandler extends DelegatingHttpHandler {
    public ReviewApiHandler(HttpHandler delegate) {
        super(delegate);
    }
}
