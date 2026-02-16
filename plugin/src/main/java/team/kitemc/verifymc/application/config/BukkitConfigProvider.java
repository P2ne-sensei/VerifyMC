package team.kitemc.verifymc.application.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.configuration.file.FileConfiguration;

public class BukkitConfigProvider implements ConfigProvider {
    private final AtomicReference<VerifyMcConfig> configRef = new AtomicReference<>();
    private final AtomicLong versionRef = new AtomicLong(0L);
    private final AtomicReference<FileConfiguration> rawRef = new AtomicReference<>();

    @Override
    public VerifyMcConfig current() {
        VerifyMcConfig config = configRef.get();
        if (config == null) {
            throw new IllegalStateException("Config has not been initialized yet.");
        }
        return config;
    }

    @Override
    public long version() {
        return versionRef.get();
    }

    @Override
    public FileConfiguration raw() {
        FileConfiguration raw = rawRef.get();
        if (raw == null) {
            throw new IllegalStateException("Raw config has not been initialized yet.");
        }
        return raw;
    }

    @Override
    public ConfigVersionSnapshot reloadAndReplace(FileConfiguration rawConfig) {
        VerifyMcConfig parsed = parseAndValidate(rawConfig);
        long newVersion = versionRef.incrementAndGet();
        configRef.set(parsed);
        rawRef.set(rawConfig);
        return new ConfigVersionSnapshot(newVersion, parsed);
    }

    private VerifyMcConfig parseAndValidate(FileConfiguration config) {
        List<String> errors = new ArrayList<>();

        String language = defaultIfBlank(config.getString("language"), "en");
        int webPort = intInRange(config, "web_port", 8080, 1, 65535, errors);
        int wsPort = intInRange(config, "ws_port", webPort + 1, 1, 65535, errors);
        String webServerPrefix = defaultIfBlank(config.getString("web_server_prefix"), "[VerifyMC]");
        String webRegisterUrl = defaultIfBlank(config.getString("web_register_url"), "https://yourdomain.com/");

        String adminPassword = defaultIfBlank(config.getString("admin.password"), "");
        if (adminPassword.isBlank()) {
            errors.add("admin.password is required and cannot be empty");
        }

        String storageType = defaultIfBlank(config.getString("storage.type"), "data").toLowerCase(Locale.ROOT);
        if (!"mysql".equals(storageType) && !"data".equals(storageType)) {
            errors.add("storage.type must be one of [mysql, data]");
        }
        String mysqlHost = defaultIfBlank(config.getString("storage.mysql.host"), "localhost");
        int mysqlPort = intInRange(config, "storage.mysql.port", 3306, 1, 65535, errors);
        String mysqlDatabase = defaultIfBlank(config.getString("storage.mysql.database"), "");
        String mysqlUser = defaultIfBlank(config.getString("storage.mysql.user"), "");
        String mysqlPassword = defaultIfBlank(config.getString("storage.mysql.password"), "");
        if ("mysql".equals(storageType)) {
            if (mysqlDatabase.isBlank()) {
                errors.add("storage.mysql.database is required when storage.type=mysql");
            }
            if (mysqlUser.isBlank()) {
                errors.add("storage.mysql.user is required when storage.type=mysql");
            }
        }

        boolean questionnaireEnabled = config.getBoolean("questionnaire.enabled", false);
        int questionnairePassScore = intInRange(config, "questionnaire.pass_score", 60, 0, 100, errors);

        boolean discordEnabled = config.getBoolean("discord.enabled", false);
        String discordClientId = defaultIfBlank(config.getString("discord.client_id"), "");
        String discordClientSecret = defaultIfBlank(config.getString("discord.client_secret"), "");
        String discordRedirectUri = defaultIfBlank(config.getString("discord.redirect_uri"), "");
        String discordGuildId = defaultIfBlank(config.getString("discord.guild_id"), "");
        boolean discordRequired = config.getBoolean("discord.required", false);
        if (discordEnabled) {
            if (discordClientId.isBlank()) {
                errors.add("discord.client_id is required when discord.enabled=true");
            }
            if (discordClientSecret.isBlank()) {
                errors.add("discord.client_secret is required when discord.enabled=true");
            }
            if (discordRedirectUri.isBlank()) {
                errors.add("discord.redirect_uri is required when discord.enabled=true");
            }
        }

        String frontendTheme = defaultIfBlank(config.getString("frontend.theme"), "default");
        boolean registerAutoApprove = config.getBoolean("register.auto_approve", false);
        int maxAccountsPerEmail = intInRange(config, "max_accounts_per_email", 2, 1, 100, errors);

        boolean emailAliasLimitEnabled = config.getBoolean("enable_email_alias_limit", false)
                || config.getBoolean("email_alias_limit_enabled", false);
        boolean emailDomainWhitelistEnabled = config.getBoolean("enable_email_domain_whitelist", false)
                || config.getBoolean("email_domain_whitelist_enabled", false);

        if (!errors.isEmpty()) {
            throw new ConfigValidationException("Config validation failed:\n - " + String.join("\n - ", errors));
        }

        return new VerifyMcConfig(
                new VerifyMcConfig.WebConfig(webPort, wsPort, webServerPrefix, webRegisterUrl),
                new VerifyMcConfig.AuthConfig(adminPassword, emailAliasLimitEnabled, emailDomainWhitelistEnabled),
                new VerifyMcConfig.StorageConfig(storageType, mysqlHost, mysqlPort, mysqlDatabase, mysqlUser, mysqlPassword),
                new VerifyMcConfig.QuestionnaireConfig(questionnaireEnabled, questionnairePassScore),
                new VerifyMcConfig.DiscordConfig(discordEnabled, discordClientId, discordClientSecret, discordRedirectUri, discordGuildId, discordRequired),
                language,
                config.getBoolean("debug", false),
                frontendTheme,
                registerAutoApprove,
                maxAccountsPerEmail
        );
    }

    private static int intInRange(FileConfiguration config, String path, int def, int min, int max, List<String> errors) {
        int value = config.getInt(path, def);
        if (value < min || value > max) {
            errors.add(path + " must be in range [" + min + ", " + max + "], but was " + value);
        }
        return value;
    }

    private static String defaultIfBlank(String value, String def) {
        return (value == null || value.isBlank()) ? def : value;
    }
}
