package team.kitemc.verifymc.api.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;
import team.kitemc.verifymc.application.dto.DiscordCallbackResponseDto;
import team.kitemc.verifymc.service.DiscordService;
import team.kitemc.verifymc.web.WebServer;

public class DiscordCallbackApiHandler implements HttpHandler {
    private final WebServer webServer;

    public DiscordCallbackApiHandler(WebServer webServer) {
        this.webServer = webServer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        webServer.debugLog("/api/discord/callback called");

        Map<String, String> queryParams = ApiHttpRequestHelper.queryParams(exchange);
        String code = queryParams.get("code");
        String state = queryParams.get("state");
        boolean wantsHtml = ApiHttpRequestHelper.prefersHtml(exchange);

        if (code == null || state == null) {
            if (wantsHtml) {
                webServer.sendDiscordCallbackHtml(exchange, false, "Missing code or state parameter", null);
            } else {
                webServer.sendJson(exchange, new DiscordCallbackResponseDto(false, "Missing code or state parameter").toJson());
            }
            return;
        }

        DiscordService.DiscordCallbackResult result = webServer.getDiscordService().handleCallback(code, state);
        if (wantsHtml) {
            String discordUsername = result.user != null
                    ? (result.user.globalName != null ? result.user.globalName : result.user.username)
                    : null;
            webServer.sendDiscordCallbackHtml(exchange, result.success, result.message, discordUsername);
        } else {
            webServer.sendJson(exchange, result.toJson());
        }
    }
}
