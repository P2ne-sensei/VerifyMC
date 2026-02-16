package team.kitemc.verifymc.infrastructure.persistence.file;

import team.kitemc.verifymc.domain.user.DiscordBinding;
import team.kitemc.verifymc.domain.user.QuestionnaireAudit;
import team.kitemc.verifymc.domain.user.User;
import team.kitemc.verifymc.domain.user.UserStatus;

import java.util.HashMap;
import java.util.Map;

public final class UserEntityMapper {
    private UserEntityMapper() {
    }

    public static User toDomain(Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        return new User(
                asString(source.get("uuid")),
                asString(source.get("username")),
                asString(source.get("email")),
                UserStatus.fromValue(asString(source.get("status"))),
                asString(source.get("password")),
                asLong(source.get("regTime"), 0L),
                new QuestionnaireAudit(
                        asInteger(source.get("questionnaire_score")),
                        asBoolean(source.get("questionnaire_passed")),
                        asString(source.get("questionnaire_review_summary")),
                        asLongObject(source.get("questionnaire_scored_at"))
                ),
                new DiscordBinding(asString(source.get("discord_id")))
        );
    }

    public static Map<String, Object> toEntity(User user) {
        Map<String, Object> target = new HashMap<>();
        if (user == null) {
            return target;
        }
        target.put("uuid", user.uuid());
        target.put("username", user.username());
        target.put("email", user.email());
        target.put("status", user.status().value());
        target.put("password", user.password());
        target.put("regTime", user.regTime());
        target.put("discord_id", user.discordBinding().discordId());
        target.put("questionnaire_score", user.questionnaireAudit().score());
        target.put("questionnaire_passed", user.questionnaireAudit().passed());
        target.put("questionnaire_review_summary", user.questionnaireAudit().reviewSummary());
        target.put("questionnaire_scored_at", user.questionnaireAudit().scoredAt());
        return target;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long asLongObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long asLong(Object value, long fallback) {
        Long parsed = asLongObject(value);
        return parsed == null ? fallback : parsed;
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
