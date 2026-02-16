package team.kitemc.verifymc.api.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import team.kitemc.verifymc.web.WebServer;

public class DiscordAuthApiHandler implements HttpHandler {
    private final WebServer webServer;

    public DiscordAuthApiHandler(WebServer webServer) {
        this.webServer = webServer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        webServer.debugLog("/api/discord/auth called");
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
            return;
        }

        JSONObject req = new JSONObject(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String username = req.optString("username", "");
        JSONObject resp = new JSONObject();

        if (!webServer.getDiscordService().isEnabled()) {
            resp.put("success", false);
            resp.put("msg", "Discord integration is not enabled");
            webServer.sendJson(exchange, resp);
            return;
        }

        if (username.isEmpty()) {
            resp.put("success", false);
            resp.put("msg", "Username is required");
            webServer.sendJson(exchange, resp);
            return;
        }

        String authUrl = webServer.getDiscordService().generateAuthUrl(username);
        if (authUrl != null) {
            resp.put("success", true);
            resp.put("auth_url", authUrl);
        } else {
            resp.put("success", false);
            resp.put("msg", "Failed to generate auth URL");
        }

        webServer.sendJson(exchange, resp);
    }
}
