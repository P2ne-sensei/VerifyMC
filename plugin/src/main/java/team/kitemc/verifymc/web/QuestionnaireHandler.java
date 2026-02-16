package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpHandler;

public class QuestionnaireHandler extends DelegatingHttpHandler {
    public QuestionnaireHandler(HttpHandler delegate) {
        super(delegate);
    }
}
