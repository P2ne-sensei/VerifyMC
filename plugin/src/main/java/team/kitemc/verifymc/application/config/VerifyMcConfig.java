package team.kitemc.verifymc.application.config;

public record VerifyMcConfig(
        WebConfig web,
        AuthConfig auth,
        StorageConfig storage,
        QuestionnaireConfig questionnaire,
        DiscordConfig discord,
        String language,
        boolean debug,
        String frontendTheme,
        boolean registerAutoApprove,
        int maxAccountsPerEmail
) {
    public record WebConfig(int webPort, int wsPort, String webServerPrefix, String webRegisterUrl) {}

    public record AuthConfig(String adminPassword, boolean emailAliasLimitEnabled, boolean emailDomainWhitelistEnabled) {}

    public record StorageConfig(String type, String mysqlHost, int mysqlPort, String mysqlDatabase, String mysqlUser, String mysqlPassword) {}

    public record QuestionnaireConfig(boolean enabled, int passScore) {}

    public record DiscordConfig(boolean enabled, String clientId, String clientSecret, String redirectUri, String guildId, boolean required) {}
}
