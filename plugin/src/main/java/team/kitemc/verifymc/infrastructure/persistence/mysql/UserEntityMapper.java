package team.kitemc.verifymc.infrastructure.persistence.mysql;

import team.kitemc.verifymc.domain.user.DiscordBinding;
import team.kitemc.verifymc.domain.user.QuestionnaireAudit;
import team.kitemc.verifymc.domain.user.User;
import team.kitemc.verifymc.domain.user.UserStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class UserEntityMapper {
    private UserEntityMapper() {
    }

    public static User toDomain(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("uuid"),
                rs.getString("username"),
                rs.getString("email"),
                UserStatus.fromValue(rs.getString("status")),
                rs.getString("password"),
                rs.getLong("regTime"),
                new QuestionnaireAudit(
                        (Integer) rs.getObject("questionnaire_score"),
                        (Boolean) rs.getObject("questionnaire_passed"),
                        rs.getString("questionnaire_review_summary"),
                        (Long) rs.getObject("questionnaire_scored_at")
                ),
                new DiscordBinding(rs.getString("discord_id"))
        );
    }

    public static Map<String, Object> toEntity(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", user.uuid());
        map.put("username", user.username());
        map.put("email", user.email());
        map.put("status", user.status().value());
        map.put("password", user.password());
        map.put("regTime", user.regTime());
        map.put("questionnaire_score", user.questionnaireAudit().score());
        map.put("questionnaire_passed", user.questionnaireAudit().passed());
        map.put("questionnaire_review_summary", user.questionnaireAudit().reviewSummary());
        map.put("questionnaire_scored_at", user.questionnaireAudit().scoredAt());
        map.put("discord_id", user.discordBinding().discordId());
        return map;
    }
}
