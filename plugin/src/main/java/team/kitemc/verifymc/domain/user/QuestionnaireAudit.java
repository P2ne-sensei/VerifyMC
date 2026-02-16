package team.kitemc.verifymc.domain.user;

public record QuestionnaireAudit(
        Integer score,
        Boolean passed,
        String reviewSummary,
        Long scoredAt
) {
    public static QuestionnaireAudit empty() {
        return new QuestionnaireAudit(null, null, null, null);
    }
}
