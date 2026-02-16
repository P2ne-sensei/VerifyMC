package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import team.kitemc.verifymc.application.config.ConfigProvider;

public class WebAuthHelper {
    private static final long TOKEN_EXPIRY_TIME = 3600000;
    private final ConfigProvider configProvider;
    private final ConcurrentHashMap<String, Long> validTokens = new ConcurrentHashMap<>();

    public WebAuthHelper(ConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    public boolean isAuthenticated(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        return validateToken(token);
    }

    public String generateSecureToken() {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String random = String.valueOf(Math.random());
            String secret = configProvider.current().auth().adminPassword();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String combined = timestamp + random + secret;
            byte[] hash = md.digest(combined.getBytes());
            String token = Base64.getEncoder().encodeToString(hash);
            validTokens.put(token, System.currentTimeMillis() + TOKEN_EXPIRY_TIME);
            return token;
        } catch (NoSuchAlgorithmException e) {
            String token = "admin_token_" + System.currentTimeMillis() + "_" + Math.random();
            validTokens.put(token, System.currentTimeMillis() + TOKEN_EXPIRY_TIME);
            return token;
        }
    }

    public void startTokenCleanupTask() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(300000);
                    long currentTime = System.currentTimeMillis();
                    validTokens.entrySet().removeIf(entry -> entry.getValue() < currentTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private boolean validateToken(String token) {
        Long expiryTime = validTokens.get(token);
        if (expiryTime == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiryTime) {
            validTokens.remove(token);
            return false;
        }
        return true;
    }
}
