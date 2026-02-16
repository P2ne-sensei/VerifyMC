package team.kitemc.verifymc.domain.user;

import java.util.Locale;

public enum UserStatus {
    PENDING,
    APPROVED,
    BANNED,
    REJECTED,
    UNKNOWN;

    public static UserStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return UserStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
