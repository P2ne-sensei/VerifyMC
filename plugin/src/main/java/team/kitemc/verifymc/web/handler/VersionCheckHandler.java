package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import team.kitemc.verifymc.VerifyMC;
import team.kitemc.verifymc.service.VersionCheckService;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebAuthHelper;

public class VersionCheckHandler implements HttpHandler {
    private final Plugin plugin;
    private final WebAuthHelper webAuthHelper;
    private final Consumer<String> debugLog;
    private final JsonResponseWriter responseWriter;

    public VersionCheckHandler(Plugin plugin, WebAuthHelper webAuthHelper, Consumer<String> debugLog, JsonResponseWriter responseWriter) {
        this.plugin = plugin;
        this.webAuthHelper = webAuthHelper;
        this.debugLog = debugLog;
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!webAuthHelper.isAuthenticated(exchange)) {
            responseWriter.write(exchange, ApiResponseFactory.failure("Authentication required"));
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
            return;
        }

        try {
            VerifyMC mainPlugin = (VerifyMC) plugin;
            VersionCheckService versionService = mainPlugin.getVersionCheckService();
            if (versionService != null) {
                versionService.checkForUpdatesAsync().thenAccept(result -> {
                    try {
                        responseWriter.write(exchange, result.toJson());
                    } catch (Exception e) {
                        debugLog.accept("Error sending version check response: " + e.getMessage());
                    }
                }).exceptionally(throwable -> {
                    try {
                        JSONObject errorResp = new JSONObject();
                        errorResp.put("success", false);
                        errorResp.put("error", "Version check failed: " + throwable.getMessage());
                        responseWriter.write(exchange, errorResp);
                    } catch (Exception e) {
                        debugLog.accept("Error sending version check error response: " + e.getMessage());
                    }
                    return null;
                });
            } else {
                JSONObject resp = new JSONObject();
                resp.put("success", false);
                resp.put("error", "Version check service not available");
                responseWriter.write(exchange, resp);
            }
        } catch (Exception e) {
            debugLog.accept("Version check API error: " + e.getMessage());
            JSONObject resp = new JSONObject();
            resp.put("success", false);
            resp.put("error", "Internal server error");
            responseWriter.write(exchange, resp);
        }
    }
}
