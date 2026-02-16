package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpHandler;

public class UserAdminHandler extends DelegatingHttpHandler {
    public UserAdminHandler(HttpHandler delegate) {
        super(delegate);
    }
}
