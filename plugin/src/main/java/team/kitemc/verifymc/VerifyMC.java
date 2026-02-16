package team.kitemc.verifymc;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import team.kitemc.verifymc.bootstrap.PluginBootstrap;
import team.kitemc.verifymc.bootstrap.ServiceRegistry;
import team.kitemc.verifymc.application.config.ConfigProvider;
import team.kitemc.verifymc.web.WebServer;
import java.io.File;
import team.kitemc.verifymc.web.ReviewWebSocketServer;
import team.kitemc.verifymc.db.FileUserDao;
import team.kitemc.verifymc.db.FileAuditDao;
import team.kitemc.verifymc.mail.MailService;
import team.kitemc.verifymc.service.VerifyCodeService;
import java.util.Properties;
import java.util.MissingResourceException;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.db.AuditDao;
import team.kitemc.verifymc.db.MysqlAuditDao;
import team.kitemc.verifymc.db.MysqlUserDao;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.service.VersionCheckService;
import team.kitemc.verifymc.service.CaptchaService;
import team.kitemc.verifymc.service.QuestionnaireService;
import team.kitemc.verifymc.service.DiscordService;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.Path;
import java.nio.file.Paths;
import team.kitemc.verifymc.application.lifecycle.PluginLifecycleService;
import team.kitemc.verifymc.command.AddCommandHandler;
import team.kitemc.verifymc.command.CommandDispatcher;
import team.kitemc.verifymc.command.HelpCommandHandler;
import team.kitemc.verifymc.command.PortCommandHandler;
import team.kitemc.verifymc.command.ReloadCommandHandler;
import team.kitemc.verifymc.command.RemoveCommandHandler;
import team.kitemc.verifymc.listener.PlayerWhitelistListener;
import team.kitemc.verifymc.service.whitelist.WhitelistSyncService;

public class VerifyMC extends JavaPlugin {
    private ResourceBundle messages;
    private WebServer webServer;
    private ReviewWebSocketServer wsServer;
    // User data access object interface
    private UserDao userDao;
    // Audit data access object interface
    private AuditDao auditDao;
    private VerifyCodeService codeService;
    private MailService mailService;
    private AuthmeService authmeService;
    private VersionCheckService versionCheckService;
    private CaptchaService captchaService;
    private QuestionnaireService questionnaireService;
    private DiscordService discordService;
    private ResourceManager resourceManager;
    private String whitelistMode;
    private boolean whitelistJsonSync;
    private String webRegisterUrl;
    private String webServerPrefix;
    private Path whitelistJsonPath;
    public boolean debug = false;
    private Boolean isFoliaServer = null;
    private PluginBootstrap pluginBootstrap;
    private ServiceRegistry serviceRegistry;
    private ConfigProvider configProvider;
    private WhitelistSyncService whitelistSyncService;
    private PluginLifecycleService pluginLifecycleService;

    public void debugLog(String msg) {
        if (debug) getLogger().info("[DEBUG] " + msg);
    }
    
    /**
     * Check if server is running Folia
     * @return true if Folia is detected
     */
    private boolean isFoliaServer() {
        if (isFoliaServer == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFoliaServer = true;
            } catch (ClassNotFoundException e) {
                isFoliaServer = false;
            }
        }
        return isFoliaServer;
    }

    private String getConfigLanguage() {
        return getConfig().getString("language", "en");
    }

    private String getMessage(String key) {
        return getMessage(key, getConfigLanguage());
    }

    public String getConfigLanguagePublic() {
        return getConfigLanguage();
    }

    public String getMessagePublic(String key) {
        return getMessage(key);
    }

    public String getMessagePublic(String key, String language) {
        return getMessage(key, language);
    }

    private String getMessage(String key, String language) {
        if (messages != null && messages.containsKey(key)) {
            return messages.getString(key);
        }
        return key;
    }

    @Override
    public void onEnable() {
        pluginBootstrap = new PluginBootstrap(this);
        try {
            serviceRegistry = pluginBootstrap.enable();
            applyServiceRegistry(serviceRegistry);
        } catch (RuntimeException ex) {
            getLogger().severe("[VerifyMC] Startup aborted due to invalid configuration: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        whitelistSyncService = new WhitelistSyncService(this, userDao, whitelistMode, whitelistJsonSync, whitelistJsonPath);
        pluginLifecycleService = new PluginLifecycleService(this, authmeService, whitelistSyncService);
        pluginLifecycleService.orchestrateStartup();

        registerCommandDispatcher();
        getServer().getPluginManager().registerEvents(new PlayerWhitelistListener(this, userDao, webRegisterUrl), this);

        if (pluginLifecycleService.isAuthmeEnabled()) {
            long syncTicks = pluginLifecycleService.getAuthmeSyncTicks();
            new BukkitRunnable() {
                @Override
                public void run() {
                    authmeService.syncApprovedUsers();
                    whitelistSyncService.syncWhitelistToServer();
                }
            }.runTaskTimerAsynchronously(this, syncTicks, syncTicks);
        }

        if (pluginLifecycleService.shouldStartWhitelistWatcher(isFoliaServer())) {
            whitelistSyncService.startWhitelistJsonWatcher();
        } else if (whitelistSyncService.isBukkitWhitelistJsonSyncEnabled() && isFoliaServer()) {
            getLogger().info("§e[VerifyMC] Whitelist.json auto-sync disabled on Folia (use manual /vmc reload instead)");
        }

        logServerCompatibility();
        getLogger().info(getMessage("plugin.enabled"));
        startVersionCheck();
        int pluginId = 26637;
        Metrics metrics = new Metrics(this, pluginId);
    }

    private void registerCommandDispatcher() {
        CommandDispatcher dispatcher = new CommandDispatcher()
            .register(new HelpCommandHandler(this))
            .register(new ReloadCommandHandler(this))
            .register(new AddCommandHandler(this))
            .register(new RemoveCommandHandler(this))
            .register(new PortCommandHandler(this));
        if (getCommand("vmc") != null) {
            getCommand("vmc").setExecutor(dispatcher);
            getCommand("vmc").setTabCompleter(dispatcher);
        }
    }

    @Override
    public void onDisable() {
        if (pluginBootstrap != null) {
            pluginBootstrap.disable(serviceRegistry);
        }
        if (pluginLifecycleService != null) {
            pluginLifecycleService.orchestrateShutdown();
        }
        getLogger().info(getMessage("plugin.disabled"));
    }


    private void applyServiceRegistry(ServiceRegistry registry) {
        if (registry == null) {
            return;
        }
        messages = registry.messages;
        webServer = registry.webServer;
        wsServer = registry.wsServer;
        userDao = registry.userDao;
        auditDao = registry.auditDao;
        codeService = registry.codeService;
        mailService = registry.mailService;
        authmeService = registry.authmeService;
        versionCheckService = registry.versionCheckService;
        captchaService = registry.captchaService;
        questionnaireService = registry.questionnaireService;
        discordService = registry.discordService;
        resourceManager = registry.resourceManager;
        configProvider = registry.configProvider;
        whitelistMode = registry.whitelistMode;
        whitelistJsonSync = registry.whitelistJsonSync;
        webRegisterUrl = registry.webRegisterUrl;
        webServerPrefix = registry.webServerPrefix;
        whitelistJsonPath = registry.whitelistJsonPath;
    }

    private void logServerCompatibility() {
        String serverName = getServer().getName().toLowerCase();
        getLogger().info(getMessage("server.cores_supported"));
        if (isFoliaServer()) {
            getLogger().info(getMessage("server.detected.folia"));
            getLogger().info("§e[VerifyMC] Folia compatibility mode enabled:");
            getLogger().info("§e  - Player kick uses delayed scheduling");
            getLogger().info("§e  - Whitelist.json auto-sync disabled (use /vmc reload to manually sync)");
            getLogger().info("§e  - Version update reminders disabled");
        } else if (serverName.contains("purpur")) {
            getLogger().info(getMessage("server.detected.purpur"));
        } else if (serverName.contains("paper")) {
            getLogger().info(getMessage("server.detected.paper"));
        } else if (serverName.contains("spigot")) {
            getLogger().info(getMessage("server.detected.spigot"));
        } else if (serverName.contains("bukkit")) {
            getLogger().info(getMessage("server.detected.bukkit"));
        } else if (serverName.contains("velocity")) {
            getLogger().info(getMessage("server.detected.velocity"));
        } else if (serverName.contains("waterfall")) {
            getLogger().info(getMessage("server.detected.waterfall"));
        } else if (serverName.contains("canvas")) {
            getLogger().info(getMessage("server.detected.canvas"));
        } else {
            getLogger().info(getMessage("server.detected.unknown"));
        }
    }

    
    
    /**
     * Display help information for console and players
     * @param sender Command sender
     * @param language Language code
     */
    public void showHelp(CommandSender sender, String language) {
        sender.sendMessage("§6=== VerifyMC " + getMessage("command.help.title", language) + " ===\n");
        sender.sendMessage("§e/vmc help §7- " + getMessage("command.help.help", language) + "\n");
        sender.sendMessage("§e/vmc port §7- " + getMessage("command.help.port", language) + "\n");
        sender.sendMessage("§e/vmc reload §7- " + getMessage("command.help.reload", language) + "\n");
        sender.sendMessage("§e/vmc add <" + getMessage("command.help.player", language) + "> §7- " + getMessage("command.help.add", language) + "\n");
        sender.sendMessage("§e/vmc remove <" + getMessage("command.help.player", language) + "> §7- " + getMessage("command.help.remove", language) + "\n");
    }

    /**
     * Reload the plugin with theme change detection
     * @param sender Command sender
     * @param language Language code
     */
    public void reloadPlugin(CommandSender sender, String language) {
        try {
            // Check if theme has changed
            String oldTheme = getConfig().getString("frontend.theme", "default");
            File configFile = new File(getDataFolder(), "config.yml");
            String configContent = new String(java.nio.file.Files.readAllBytes(configFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            String newTheme = oldTheme;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("frontend.theme\\s*:\\s*(\\w+)").matcher(configContent);
            if (m.find()) newTheme = m.group(1);
            boolean themeChanged = !oldTheme.equals(newTheme);
            sender.sendMessage("§aRestarting plugin...");
            Bukkit.getScheduler().runTask(this, () -> {
                try {
                    Bukkit.getPluginManager().disablePlugin(this);
                    Bukkit.getPluginManager().enablePlugin(this);
                    sender.sendMessage("§aPlugin restart successful");
                    sender.sendMessage("§7Note: /vmc reload can only reload partial plugin configurations. For a complete reload, please restart the server.");
                    if (themeChanged) {
                        sender.sendMessage("§ePlease restart server to switch frontend theme");
                    }
                } catch (Exception e) {
                    sender.sendMessage("§cPlugin restart failed: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            sender.sendMessage("§cPlugin restart failed: " + e.getMessage());
        }
    }

    /**
     * Add user to whitelist with email support
     * @param sender Command sender
     * @param targetName Target username
     * @param email Email address
     * @param language Language code
     */
    public void addWhitelist(CommandSender sender, String targetName, String email, String language) {
        addWhitelist(sender, targetName, email, null, language);
    }
    
    /**
     * Add user to whitelist with password support
     * @param sender Command sender
     * @param targetName Target username
     * @param email Email address
     * @param password Password (optional)
     * @param language Language code
     */
    private void addWhitelist(CommandSender sender, String targetName, String email, String password, String language) {
        try {
            // Validate password format (if password is provided)
            if (password != null && !password.isEmpty() && authmeService.isAuthmeEnabled()) {
                if (!authmeService.isValidPassword(password)) {
                    String passwordRegex = getConfig().getString("authme.password_regex", "^[a-zA-Z0-9_]{8,26}$");
                    sender.sendMessage("§c" + getMessage("command.invalid_password", language).replace("{regex}", passwordRegex));
                    return;
                }
            }
            
            // Set player to whitelist with error handling
            try {
                Bukkit.getOfflinePlayer(targetName).setWhitelisted(true);
            } catch (Exception whitelistError) {
                debugLog("Whitelist operation failed for " + targetName + ": " + whitelistError.getMessage());
                // Continue with user registration even if whitelist fails
            }
            String uuid = Bukkit.getOfflinePlayer(targetName).getUniqueId().toString();
            Map<String, Object> user = userDao.getUserByUuid(uuid);
            boolean ok;
            
            if (user != null) {
                // User exists, update status to approved
                ok = userDao.updateUserStatus(uuid, "approved");
                // If password is provided, update password
                if (password != null && !password.isEmpty()) {
                    userDao.updateUserPassword(uuid, password);
                    // If Authme integration is enabled, register to Authme
                    if (authmeService.isAuthmeEnabled()) {
                        authmeService.registerToAuthme(targetName, password);
                        sender.sendMessage("§a" + getMessage("authme.register_success", language).replace("{player}", targetName));
                    }
                }
            } else {
                // User doesn't exist, register new user (status as approved)
                if (password != null && !password.isEmpty()) {
                    ok = userDao.registerUser(uuid, targetName, email, "approved", password);
                    // If Authme integration is enabled, register to Authme
                    if (authmeService.isAuthmeEnabled()) {
                        authmeService.registerToAuthme(targetName, password);
                        sender.sendMessage("§a" + getMessage("authme.register_success", language).replace("{player}", targetName));
                    }
                } else {
                    ok = userDao.registerUser(uuid, targetName, email, "approved");
                }
            }
            
            userDao.save();
            
            // Immediately sync to whitelist.json (if enabled)
            if (whitelistSyncService != null && whitelistSyncService.isBukkitWhitelistJsonSyncEnabled()) {
                whitelistSyncService.syncPluginToWhitelistJson();
            }

            // Sync to server whitelist
            if (whitelistSyncService != null) {
                whitelistSyncService.syncWhitelistToServer();
            }
            
            // WebSocket notification
            if (wsServer != null) {
                wsServer.broadcastMessage("{\"type\":\"user_update\"}");
            }
            
            if (ok) {
                sender.sendMessage("§a" + getMessage("command.add_success", language).replace("{player}", targetName));
            } else {
                sender.sendMessage("§c" + getMessage("command.add_failed", language));
            }
        } catch (Exception e) {
            sender.sendMessage("§c" + getMessage("command.add_failed", language) + ": " + e.getMessage());
        }
    }
    
    /**
     * Remove user from whitelist and synchronize with userDao
     * @param sender Command sender
     * @param targetName Target username
     * @param language Language code
     */
    public void removeWhitelist(CommandSender sender, String targetName, String language) {
        try {
            Bukkit.getOfflinePlayer(targetName).setWhitelisted(false);
            // Prioritize username lookup
            Map<String, Object> user = userDao.getUserByUsername(targetName);
            String uuid = null;
            if (user != null && user.get("uuid") != null) {
                uuid = user.get("uuid").toString();
            } else {
                // Compatible with old data or UUID algorithm differences
                uuid = Bukkit.getOfflinePlayer(targetName).getUniqueId().toString();
            }
            
            // If Authme integration is enabled, unregister user from Authme
            if (authmeService.isAuthmeEnabled()) {
                authmeService.unregisterFromAuthme(targetName);
            }
            
            boolean ok = userDao.deleteUser(uuid);
            userDao.save();
            if (ok) {
            sender.sendMessage("§a" + getMessage("command.remove_success", language).replace("{player}", targetName));
                if (wsServer != null) wsServer.broadcastMessage("{\"type\":\"user_update\"}");
            } else {
                sender.sendMessage("§c" + getMessage("command.remove_failed", language));
            }
        } catch (Exception e) {
            sender.sendMessage("§c" + getMessage("command.remove_failed", language) + ": " + e.getMessage());
        }
    }

    /**
     * Display web server port information
     * @param sender Command sender
     * @param language Language code
     */
    public void showPort(CommandSender sender, String language) {
        int port = getConfig().getInt("web_port", 8080);
        sender.sendMessage("§a" + getMessage("command.port_info", language).replace("{port}", String.valueOf(port)));
    }



    public ResourceBundle getMessages() {
        return messages;
    }

    public void migratePlaintextPasswordsPublic() {
        migratePlaintextPasswords();
    }

    public WhitelistSyncService getWhitelistSyncService() {
        return whitelistSyncService;
    }

    private static final String USERNAME_REGEX_KEY = "username_regex";
    private static final String USERNAME_CASE_SENSITIVE_KEY = "username_case_sensitive";
    private static final String USERNAME_INVALID_KEY = "username.invalid";
    private static final String USERNAME_CASE_CONFLICT_KEY = "username.case_conflict";

    /**
     * Validate username format
     * @param username Username to validate
     * @return true if username is valid
     */
    public boolean isValidUsername(String username) {
        if (username == null) return false;
        
        // Check if bedrock support is enabled and username has bedrock prefix
        boolean bedrockEnabled = getConfig().getBoolean("bedrock.enabled", false);
        String bedrockPrefix = getConfig().getString("bedrock.prefix", ".");
        
        if (bedrockEnabled && username.startsWith(bedrockPrefix)) {
            // Use bedrock-specific regex
            String bedrockRegex = getConfig().getString("bedrock.username_regex", "^\\.[a-zA-Z0-9_\\s]{3,16}$");
            return username.matches(bedrockRegex);
        }
        
        // Use standard regex for Java players
        String regex = getConfig().getString(USERNAME_REGEX_KEY, "^[a-zA-Z0-9_-]{3,16}$");
        return username.matches(regex);
    }
    
    /**
     * Check if a username is a bedrock player
     * @param username Username to check
     * @return true if username is a bedrock player
     */
    public boolean isBedrockPlayer(String username) {
        if (username == null) return false;
        boolean bedrockEnabled = getConfig().getBoolean("bedrock.enabled", false);
        if (!bedrockEnabled) return false;
        
        String bedrockPrefix = getConfig().getString("bedrock.prefix", ".");
        return username.startsWith(bedrockPrefix);
    }
    
    /**
     * Check for username case conflicts
     * @param username Username to check
     * @return true if case conflict exists
     */
    public boolean isUsernameCaseConflict(String username) {
        boolean caseSensitive = getConfig().getBoolean(USERNAME_CASE_SENSITIVE_KEY, false);
        if (caseSensitive) return false;
        for (Map<String, Object> user : userDao.getAllUsers()) {
            String exist = (String) user.get("username");
            if (exist != null && exist.equalsIgnoreCase(username) && !exist.equals(username)) return true;
        }
        return false;
    }


    /**
     * Auto migrate data between storage types if needed
     * @param messages Resource bundle for messages
     */
    public void autoMigrateIfNeeded(ResourceBundle messages) {
        boolean autoMigrateOnSwitch = getConfig().getBoolean("storage.auto_migrate_on_switch", false);
        String storageType = getConfig().getString("storage.type", "data");
        if (autoMigrateOnSwitch) {
            if ("mysql".equalsIgnoreCase(storageType) && userDao instanceof MysqlUserDao) {
                // data -> mysql
                List<Map<String, Object>> fileUsers = new FileUserDao(new File(getDataFolder(), "data/users.json"), this).getAllUsers();
                List<Map<String, Object>> mysqlUsers = userDao.getAllUsers();
                if (!fileUsers.equals(mysqlUsers)) {
                    for (Map<String, Object> user : fileUsers) {
                        userDao.registerUser(
                            (String) user.get("uuid"),
                            (String) user.get("username"),
                            (String) user.get("email"),
                            (String) user.get("status")
                        );
                    }
                    getLogger().info(messages.getString("storage.migrate.success"));
                }
            } else if ("data".equalsIgnoreCase(storageType) && userDao instanceof FileUserDao) {
                // mysql -> data
                try {
                    List<Map<String, Object>> mysqlUsers = new MysqlUserDao(getMysqlConfig(), messages, this).getAllUsers();
                    List<Map<String, Object>> fileUsers = userDao.getAllUsers();
                    if (!mysqlUsers.equals(fileUsers)) {
                        for (Map<String, Object> user : mysqlUsers) {
                            userDao.registerUser(
                                (String) user.get("uuid"),
                                (String) user.get("username"),
                                (String) user.get("email"),
                                (String) user.get("status")
                            );
                        }
                        getLogger().info(messages.getString("storage.migrate.success"));
                    }
                } catch (Exception e) {
                    getLogger().severe(messages.getString("storage.migrate.fail").replace("{0}", e.getMessage()));
                }
            }
        }
    }
    
    /**
     * Get MySQL configuration properties
     * @return MySQL configuration properties
     */
    private Properties getMysqlConfig() {
        Properties mysqlConfig = new Properties();
        mysqlConfig.setProperty("host", getConfig().getString("storage.mysql.host"));
        mysqlConfig.setProperty("port", String.valueOf(getConfig().getInt("storage.mysql.port")));
        mysqlConfig.setProperty("database", getConfig().getString("storage.mysql.database"));
        mysqlConfig.setProperty("user", getConfig().getString("storage.mysql.user"));
        mysqlConfig.setProperty("password", getConfig().getString("storage.mysql.password"));
        return mysqlConfig;
    }

    private void migratePlaintextPasswords() {
        int migrated = 0;
        for (Map<String, Object> user : userDao.getAllUsers()) {
            String uuid = (String) user.get("uuid");
            String password = (String) user.get("password");
            if (uuid == null || password == null || password.trim().isEmpty()) {
                continue;
            }
            String encoded = authmeService.encodePasswordForStorage(password);
            if (!password.equals(encoded) && userDao.updateUserPassword(uuid, encoded)) {
                migrated++;
            }
        }
        if (migrated > 0) {
            getLogger().info("[VerifyMC] Migrated " + migrated + " stored passwords to AuthMe-compatible hash format.");
        }
    }
    
    /**
     * Start version check process
     */
    private void startVersionCheck() {
        // Check for updates asynchronously
        versionCheckService.checkForUpdatesAsync().thenAccept(result -> {
            if (result.isSuccess() && result.isUpdateAvailable()) {
                // Log update notification
                getLogger().info("§e[VerifyMC] " + getMessage("version.update_available"));
                getLogger().info("§e[VerifyMC] " + getMessage("version.current_version") + ": " + result.getCurrentVersion());
                getLogger().info("§e[VerifyMC] " + getMessage("version.latest_version") + ": " + result.getLatestVersion());
                getLogger().info("§e[VerifyMC] " + getMessage("version.download_url") + ": " + versionCheckService.getReleasesUrl());
                
                // Schedule periodic reminders (every 30 minutes) - Folia doesn't support async repeating tasks
                if (!isFoliaServer()) {
                    new BukkitRunnable() {
                        private int reminderCount = 0;
                        private final int maxReminders = 3; // Maximum 3 reminders per session
                        
                        @Override
                        public void run() {
                            if (reminderCount >= maxReminders) {
                                this.cancel();
                                return;
                            }
                            
                            reminderCount++;
                            getLogger().info("§e[VerifyMC] " + getMessage("version.update_reminder") + " (" + reminderCount + "/" + maxReminders + ")");
                            getLogger().info("§e[VerifyMC] " + getMessage("version.download_url") + ": " + versionCheckService.getReleasesUrl());
                        }
                    }.runTaskTimerAsynchronously(this, 36000L, 36000L); // 30 minutes = 36000 ticks
                }
                
            } else if (result.isSuccess()) {
                debugLog("Version check completed. Plugin is up to date.");
            } else {
                debugLog("Version check failed: " + result.getErrorMessage());
            }
        }).exceptionally(throwable -> {
            debugLog("Version check error: " + throwable.getMessage());
            return null;
        });
    }
    
    /**
     * Get version check service instance
     * @return VersionCheckService instance
     */
    public VersionCheckService getVersionCheckService() {
        return versionCheckService;
    }
} 