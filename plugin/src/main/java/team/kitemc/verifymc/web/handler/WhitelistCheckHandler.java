package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;
import org.json.JSONObject;
import team.kitemc.verifymc.db.UserDao;

public class WhitelistCheckHandler implements HttpHandler {
    private final UserDao userDao;
    private final Consumer<String> debugLog;
    private final JsonResponseWriter responseWriter;

    public WhitelistCheckHandler(UserDao userDao, Consumer<String> debugLog, JsonResponseWriter responseWriter) {
        this.userDao = userDao;
        this.debugLog = debugLog;
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        debugLog.accept("/api/check-whitelist called");
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

        JSONObject resp = new JSONObject();
        if (username == null || username.trim().isEmpty()) {
            resp.put("success", false);
            resp.put("msg", "Username parameter is required");
            responseWriter.write(exchange, resp);
            return;
        }

        Map<String, Object> user = userDao.getUserByUsername(username);
        if (user != null) {
            resp.put("success", true);
            resp.put("found", true);
            resp.put("username", user.get("username"));
            resp.put("status", user.get("status"));
            resp.put("email", user.get("email"));
            debugLog.accept("Whitelist check for " + username + ": found, status=" + user.get("status"));
        } else {
            resp.put("success", true);
            resp.put("found", false);
            resp.put("status", "not_registered");
            debugLog.accept("Whitelist check for " + username + ": not found");
        }
        responseWriter.write(exchange, resp);
    }
}
