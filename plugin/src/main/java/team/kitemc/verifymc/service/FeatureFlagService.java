package team.kitemc.verifymc.service;

import team.kitemc.verifymc.application.config.ConfigProvider;

public class FeatureFlagService {
    private final ConfigProvider configProvider;

    public FeatureFlagService(ConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    public boolean isQuestionnaireEnabled() {
        return configProvider.current().questionnaire().enabled();
    }

    public boolean isDiscordRequired() {
        return configProvider.current().discord().enabled() && configProvider.current().discord().required();
    }
}
