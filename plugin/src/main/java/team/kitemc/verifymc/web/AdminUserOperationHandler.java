package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.function.BiFunction;
import team.kitemc.verifymc.application.config.ConfigProvider;
import org.json.JSONObject;

public class AdminUserOperationHandler {
    private final ConfigProvider configProvider;
    private final WebAuthHelper authHelper;
    private final BiFunction<String, String, String> messageResolver;

    public AdminUserOperationHandler(ConfigProvider configProvider, WebAuthHelper authHelper, BiFunction<String, String, String> messageResolver) {
        this.configProvider = configProvider;
        this.authHelper = authHelper;
        this.messageResolver = messageResolver;
    }

    public HttpHandler adminLoginHandler() {
        return exchange -> {
            if (!WebResponseHelper.requireMethod(exchange, "POST")) {
                return;
            }
            JSONObject req = WebResponseHelper.readJson(exchange);
            String password = req.optString("password");
            String language = req.optString("language", "en");

            String adminPassword = configProvider.current().auth().adminPassword();
            JSONObject resp = new JSONObject();
            if (password.equals(adminPassword)) {
                resp.put("success", true);
                resp.put("token", authHelper.generateSecureToken());
                resp.put("message", messageResolver.apply("admin.login_success", language));
            } else {
                resp.put("success", false);
                resp.put("message", messageResolver.apply("admin.login_failed", language));
            }
            WebResponseHelper.sendJson(exchange, resp);
        };
    }

    public HttpHandler adminVerifyHandler() {
        return exchange -> {
            if (!WebResponseHelper.requireMethod(exchange, "POST")) {
                return;
            }
            JSONObject resp = new JSONObject();
            if (authHelper.isAuthenticated(exchange)) {
                resp.put("success", true);
                resp.put("message", "Token is valid");
            } else {
                resp.put("success", false);
                resp.put("message", "Invalid or expired token");
            }
            WebResponseHelper.sendJson(exchange, resp);
        };
    }
}
