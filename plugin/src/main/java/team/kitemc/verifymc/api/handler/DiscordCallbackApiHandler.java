package team.kitemc.verifymc.api.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
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

        String query = exchange.getRequestURI().getQuery();
        String code = null;
        String state = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    if ("code".equals(keyValue[0])) {
                        code = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    } else if ("state".equals(keyValue[0])) {
                        state = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    }
                }
            }
        }

        String accept = exchange.getRequestHeaders().getFirst("Accept");
        boolean wantsHtml = accept == null || accept.contains("text/html");

        if (code == null || state == null) {
            if (wantsHtml) {
                webServer.sendDiscordCallbackHtml(exchange, false, "Missing code or state parameter", null);
            } else {
                JSONObject resp = new JSONObject();
                resp.put("success", false);
                resp.put("msg", "Missing code or state parameter");
                webServer.sendJson(exchange, resp);
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
