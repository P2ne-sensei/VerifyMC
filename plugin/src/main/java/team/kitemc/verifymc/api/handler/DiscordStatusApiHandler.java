package team.kitemc.verifymc.api.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import team.kitemc.verifymc.application.dto.DiscordStatusResponseDto;
import team.kitemc.verifymc.service.DiscordService;
import team.kitemc.verifymc.web.WebServer;

public class DiscordStatusApiHandler implements HttpHandler {
    private final WebServer webServer;

    public DiscordStatusApiHandler(WebServer webServer) {
        this.webServer = webServer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        webServer.debugLog("/api/discord/status called");
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String username = null;
        if (query != null && query.contains("username=")) {
            username = query.split("username=")[1].split("&")[0];
            try {
                username = URLDecoder.decode(username, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }

        if (username == null || username.isEmpty()) {
            webServer.sendJson(exchange, new DiscordStatusResponseDto(false, "Username is required", false, null).toJson());
            return;
        }

        boolean linked = webServer.getDiscordService().isLinked(username);
        JSONObject userJson = null;
        if (linked) {
            DiscordService.DiscordUser user = webServer.getDiscordService().getLinkedUser(username);
            if (user != null) {
                userJson = user.toJson();
            }
        }
        webServer.sendJson(exchange, new DiscordStatusResponseDto(true, null, linked, userJson).toJson());
    }
}
