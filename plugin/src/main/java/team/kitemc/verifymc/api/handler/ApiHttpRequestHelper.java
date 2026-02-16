package team.kitemc.verifymc.api.handler;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class ApiHttpRequestHelper {
    private ApiHttpRequestHelper() {
    }

    static boolean requireMethod(HttpExchange exchange, String expectedMethod) throws IOException {
        if (expectedMethod.equals(exchange.getRequestMethod())) {
            return true;
        }
        exchange.sendResponseHeaders(405, 0);
        exchange.close();
        return false;
    }

    static Map<String, String> queryParams(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) {
            return params;
        }

        for (String pair : query.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] keyValue = pair.split("=", 2);
            String key = decode(keyValue[0]);
            String value = keyValue.length == 2 ? decode(keyValue[1]) : "";
            params.put(key, value);
        }

        return params;
    }

    static boolean prefersHtml(HttpExchange exchange) {
        String accept = exchange.getRequestHeaders().getFirst("Accept");
        return accept == null || accept.contains("text/html");
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return value;
        }
    }
}
