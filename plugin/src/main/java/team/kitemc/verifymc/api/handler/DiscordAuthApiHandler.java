package team.kitemc.verifymc.api.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import team.kitemc.verifymc.application.dto.DiscordAuthRequestDto;
import team.kitemc.verifymc.application.dto.DiscordAuthResponseDto;
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

        DiscordAuthRequestDto req = DiscordAuthRequestDto.fromJson(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String username = req.username();

        if (!webServer.getDiscordService().isEnabled()) {
            webServer.sendJson(exchange, new DiscordAuthResponseDto(false, "Discord integration is not enabled", null).toJson());
            return;
        }

        if (username.isEmpty()) {
            webServer.sendJson(exchange, new DiscordAuthResponseDto(false, "Username is required", null).toJson());
            return;
        }

        String authUrl = webServer.getDiscordService().generateAuthUrl(username);
        if (authUrl != null) {
            webServer.sendJson(exchange, new DiscordAuthResponseDto(true, null, authUrl).toJson());
        } else {
            webServer.sendJson(exchange, new DiscordAuthResponseDto(false, "Failed to generate auth URL", null).toJson());
        }
    }
}
