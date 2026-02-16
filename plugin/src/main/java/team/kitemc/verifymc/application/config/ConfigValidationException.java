package team.kitemc.verifymc.application.config;

public class ConfigValidationException extends RuntimeException {
    public ConfigValidationException(String message) {
        super(message);
    }
}
