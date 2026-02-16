package team.kitemc.verifymc.bootstrap;

import java.io.File;
import java.nio.file.Paths;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import org.bukkit.configuration.file.FileConfiguration;
import team.kitemc.verifymc.ResourceManager;
import team.kitemc.verifymc.VerifyMC;
import team.kitemc.verifymc.db.FileAuditDao;
import team.kitemc.verifymc.db.FileUserDao;
import team.kitemc.verifymc.db.MysqlAuditDao;
import team.kitemc.verifymc.db.MysqlUserDao;
import team.kitemc.verifymc.mail.MailService;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.service.CaptchaService;
import team.kitemc.verifymc.service.DiscordService;
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

        registry.whitelistMode = config.getString("whitelist_mode", "bukkit");
        registry.whitelistJsonSync = config.getBoolean("whitelist_json_sync", true);
        registry.webRegisterUrl = config.getString("web_register_url", "https://yourdomain.com/");
        registry.webServerPrefix = config.getString("web_server_prefix", "[VerifyMC]");
        registry.whitelistJsonPath = Paths.get(plugin.getServer().getWorldContainer().getAbsolutePath(), "whitelist.json");
        plugin.debug = config.getBoolean("debug", false);

        registry.resourceManager = new ResourceManager(plugin);
        registry.resourceManager.initializeResources();

        String configLang = plugin.getConfig().getString("language", "en");
        registry.messages = registry.resourceManager.loadI18nBundle(configLang);

        registry.codeService = new VerifyCodeService(plugin);
        registry.mailService = new MailService(plugin, plugin::getMessagePublic);
        registry.authmeService = new AuthmeService(plugin);
        registry.versionCheckService = new VersionCheckService(plugin);
        registry.captchaService = new CaptchaService(plugin);
        registry.questionnaireService = new QuestionnaireService(plugin);
        registry.discordService = new DiscordService(plugin);

        String storageType = plugin.getConfig().getString("storage.type", "data");
        ResourceBundle messages = loadStorageMessages();

        if ("mysql".equalsIgnoreCase(storageType)) {
            Properties mysqlConfig = new Properties();
            mysqlConfig.setProperty("host", plugin.getConfig().getString("storage.mysql.host"));
            mysqlConfig.setProperty("port", String.valueOf(plugin.getConfig().getInt("storage.mysql.port")));
            mysqlConfig.setProperty("database", plugin.getConfig().getString("storage.mysql.database"));
            mysqlConfig.setProperty("user", plugin.getConfig().getString("storage.mysql.user"));
            mysqlConfig.setProperty("password", plugin.getConfig().getString("storage.mysql.password"));
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

        int port = config.getInt("web_port", 8080);
        int wsPort = config.getInt("ws_port", port + 1);
        registry.wsServer = new ReviewWebSocketServer(wsPort, plugin);
        try {
            registry.wsServer.start();
            plugin.getLogger().info(plugin.getMessagePublic("websocket.start_success") + ": " + wsPort);
        } catch (Exception e) {
            plugin.getLogger().warning(plugin.getMessagePublic("websocket.start_failed") + ": " + e.getMessage());
        }

        String theme = config.getString("frontend.theme", "default");
        String staticDir = registry.resourceManager.getThemeStaticDir(theme);
        registry.webServer = new WebServer(port, staticDir, plugin, registry.codeService, registry.mailService,
                registry.userDao, registry.auditDao, registry.authmeService, registry.captchaService,
                registry.questionnaireService, registry.discordService, registry.wsServer, registry.messages);
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

    private ResourceBundle loadStorageMessages() {
        String lang = plugin.getConfig().getString("language", "en");
        try {
            return ResourceBundle.getBundle("i18n/messages_" + lang);
        } catch (MissingResourceException e) {
            plugin.getLogger().warning("[VerifyMC] No messages_" + lang + ".properties found, fallback to English.");
            return ResourceBundle.getBundle("i18n/messages_en");
        }
    }
}
