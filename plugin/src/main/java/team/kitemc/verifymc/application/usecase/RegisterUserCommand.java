package team.kitemc.verifymc.application.usecase;

import org.json.JSONObject;

public record RegisterUserCommand(
        String email,
        String code,
        String uuid,
        String username,
        String normalizedUsername,
        String password,
        String captchaToken,
        String captchaAnswer,
        String language,
        String platform,
        JSONObject questionnaire,
        String requestId
) {
}
