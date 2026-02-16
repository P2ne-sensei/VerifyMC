package team.kitemc.verifymc.application.usecase;

import org.json.JSONArray;
import org.json.JSONObject;

public record QuestionnaireSubmissionRecord(
        boolean passed,
        int score,
        int passScore,
        JSONArray details,
        boolean manualReviewRequired,
        boolean scoringServiceUnavailable,
        JSONObject answers,
        long submittedAt,
        long expiresAt
) {
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
