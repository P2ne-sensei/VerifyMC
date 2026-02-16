package team.kitemc.verifymc.domain.user;

public record User(
        String uuid,
        String username,
        String email,
        UserStatus status,
        String password,
        long regTime,
        QuestionnaireAudit questionnaireAudit,
        DiscordBinding discordBinding
) {
    public User {
        questionnaireAudit = questionnaireAudit == null ? QuestionnaireAudit.empty() : questionnaireAudit;
        discordBinding = discordBinding == null ? new DiscordBinding(null) : discordBinding;
        status = status == null ? UserStatus.UNKNOWN : status;
    }
}
