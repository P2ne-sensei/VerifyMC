package team.kitemc.verifymc.bootstrap;

import java.io.File;
import java.nio.file.Paths;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import org.bukkit.configuration.file.FileConfiguration;
import team.kitemc.verifymc.ResourceManager;
import team.kitemc.verifymc.VerifyMC;
import team.kitemc.verifymc.application.config.BukkitConfigProvider;
import team.kitemc.verifymc.application.config.ConfigProvider;
import team.kitemc.verifymc.application.config.ConfigValidationException;
import team.kitemc.verifymc.application.config.VerifyMcConfig;
import team.kitemc.verifymc.db.FileAuditDao;
import team.kitemc.verifymc.db.FileUserDao;
import team.kitemc.verifymc.db.MysqlAuditDao;
import team.kitemc.verifymc.db.MysqlUserDao;
import team.kitemc.verifymc.mail.MailService;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.service.CaptchaService;
import team.kitemc.verifymc.service.DiscordService;
import team.kitemc.verifymc.service.FeatureFlagService;
import team.kitemc.verifymc.service.QuestionnaireService;
import team.kitemc.verifymc.service.VerifyCodeService;
import team.kitemc.verifymc.service.VersionCheckService;
import team.kitemc.verifymc.web.ReviewWebSocketServer;
import team.kitemc.verifymc.web.WebServer;

public class PluginBootstrap {
    private final VerifyMC plugin;

    public PluginBootstrap(VerifyMC plugin) {
        this.plugin = plugin;
    }

    public ServiceRegistry enable() {
        ServiceRegistry registry = new ServiceRegistry();
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        ConfigProvider configProvider = new BukkitConfigProvider();
        VerifyMcConfig verifiedConfig;
        try {
            verifiedConfig = configProvider.reloadAndReplace(config).config();
        } catch (ConfigValidationException e) {
            plugin.getLogger().severe("[VerifyMC] " + e.getMessage());
            throw e;
        }

        registry.configProvider = configProvider;
        registry.featureFlagService = new FeatureFlagService(configProvider);
        registry.whitelistMode = config.getString("whitelist_mode", "bukkit");
        registry.whitelistJsonSync = config.getBoolean("whitelist_json_sync", true);
        registry.webRegisterUrl = verifiedConfig.web().webRegisterUrl();
        registry.webServerPrefix = verifiedConfig.web().webServerPrefix();
        registry.whitelistJsonPath = Paths.get(plugin.getServer().getWorldContainer().getAbsolutePath(), "whitelist.json");
        plugin.debug = verifiedConfig.debug();

        registry.resourceManager = new ResourceManager(plugin);
        registry.resourceManager.initializeResources();

        registry.messages = registry.resourceManager.loadI18nBundle(verifiedConfig.language());

        registry.codeService = new VerifyCodeService(plugin);
        registry.mailService = new MailService(plugin, plugin::getMessagePublic);
        registry.authmeService = new AuthmeService(plugin);
        registry.versionCheckService = new VersionCheckService(plugin);
        registry.captchaService = new CaptchaService(plugin);
        registry.questionnaireService = new QuestionnaireService(plugin, configProvider, registry.featureFlagService);
        registry.discordService = new DiscordService(plugin, configProvider);

        ResourceBundle messages = loadStorageMessages(verifiedConfig.language());
        VerifyMcConfig.StorageConfig storageConfig = verifiedConfig.storage();

        if ("mysql".equalsIgnoreCase(storageConfig.type())) {
            Properties mysqlConfig = new Properties();
            mysqlConfig.setProperty("host", storageConfig.mysqlHost());
            mysqlConfig.setProperty("port", String.valueOf(storageConfig.mysqlPort()));
            mysqlConfig.setProperty("database", storageConfig.mysqlDatabase());
            mysqlConfig.setProperty("user", storageConfig.mysqlUser());
            mysqlConfig.setProperty("password", storageConfig.mysqlPassword());
            try {
                registry.userDao = new MysqlUserDao(mysqlConfig, messages, plugin);
                registry.auditDao = new MysqlAuditDao(mysqlConfig);
                plugin.getLogger().info(messages.getString("storage.mysql.enabled"));
            } catch (Exception e) {
                plugin.getLogger().severe(messages.getString("storage.migrate.fail").replace("{0}", e.getMessage()));
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                return registry;
            }
        } else {
            File userFile = new File(plugin.getDataFolder(), "data/users.json");
            File auditFile = new File(plugin.getDataFolder(), "data/audits.json");
            userFile.getParentFile().mkdirs();
            auditFile.getParentFile().mkdirs();
            registry.userDao = new FileUserDao(userFile, plugin);
            registry.auditDao = new FileAuditDao(auditFile);
            plugin.getLogger().info(messages.getString("storage.file.enabled"));
        }

        registry.authmeService.setUserDao(registry.userDao);
        registry.discordService.setUserDao(registry.userDao);

        int port = verifiedConfig.web().webPort();
        int wsPort = verifiedConfig.web().wsPort();
        registry.wsServer = new ReviewWebSocketServer(wsPort, plugin);
        try {
            registry.wsServer.start();
            plugin.getLogger().info(plugin.getMessagePublic("websocket.start_success") + ": " + wsPort);
        } catch (Exception e) {
            plugin.getLogger().warning(plugin.getMessagePublic("websocket.start_failed") + ": " + e.getMessage());
        }

        String theme = verifiedConfig.frontendTheme();
        String staticDir = registry.resourceManager.getThemeStaticDir(theme);
        registry.webServer = new WebServer(port, staticDir, plugin, registry.codeService, registry.mailService,
                registry.userDao, registry.auditDao, registry.authmeService, registry.captchaService,
                registry.questionnaireService, registry.discordService, registry.wsServer, registry.messages,
                registry.configProvider, registry.featureFlagService);
        try {
            registry.webServer.start();
            plugin.getLogger().info(plugin.getMessagePublic("web.start_success") + ": " + port);
        } catch (Exception e) {
            plugin.getLogger().warning(plugin.getMessagePublic("web.start_failed") + ": " + e.getMessage());
        }

        return registry;
    }

    public void disable(ServiceRegistry registry) {
        if (registry == null) {
            return;
        }
        if (registry.webServer != null) {
            registry.webServer.stop();
        }
        if (registry.wsServer != null) {
            try {
                registry.wsServer.stop();
            } catch (InterruptedException e) {
                plugin.getLogger().warning(plugin.getMessagePublic("websocket.stop_interrupted") + ": " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        if (registry.userDao != null) {
            registry.userDao.save();
        }
        if (registry.auditDao != null) {
            registry.auditDao.save();
        }
    }

    private ResourceBundle loadStorageMessages(String lang) {
        try {
            return ResourceBundle.getBundle("i18n/messages_" + lang);
        } catch (MissingResourceException e) {
            plugin.getLogger().warning("[VerifyMC] No messages_" + lang + ".properties found, fallback to English.");
            return ResourceBundle.getBundle("i18n/messages_en");
        }
    }
}
