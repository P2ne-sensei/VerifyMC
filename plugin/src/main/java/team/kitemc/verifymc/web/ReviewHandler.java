package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpHandler;

public class ReviewHandler extends DelegatingHttpHandler {
    public ReviewHandler(HttpHandler delegate) {
        super(delegate);
    }
}
