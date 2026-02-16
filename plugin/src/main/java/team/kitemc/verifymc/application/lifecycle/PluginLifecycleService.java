package team.kitemc.verifymc.application.lifecycle;

import org.bukkit.configuration.file.FileConfiguration;
import team.kitemc.verifymc.VerifyMC;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.service.whitelist.WhitelistSyncService;

public class PluginLifecycleService {
    private final VerifyMC plugin;
    private final AuthmeService authmeService;
    private final WhitelistSyncService whitelistSyncService;

    public PluginLifecycleService(VerifyMC plugin, AuthmeService authmeService, WhitelistSyncService whitelistSyncService) {
        this.plugin = plugin;
        this.authmeService = authmeService;
        this.whitelistSyncService = whitelistSyncService;
    }

    public void orchestrateStartup() {
        plugin.autoMigrateIfNeeded(plugin.getMessages());
        plugin.migratePlaintextPasswordsPublic();

        if (isAuthmeEnabled()) {
            authmeService.syncApprovedUsers();
        }

        FileConfiguration config = plugin.getConfig();
        if (config.getBoolean("auto_sync_whitelist", true)) {
            whitelistSyncService.syncWhitelistToServer();
        }
        if (config.getBoolean("auto_cleanup_whitelist", true)) {
            whitelistSyncService.cleanupServerWhitelist();
        }
    }

    public void orchestrateShutdown() {
        if (whitelistSyncService.isBukkitWhitelistJsonSyncEnabled()) {
            whitelistSyncService.syncPluginToWhitelistJson();
        }
    }

    public boolean isAuthmeEnabled() {
        return authmeService != null && authmeService.isAuthmeEnabled();
    }

    public long getAuthmeSyncTicks() {
        return Math.max(20L, plugin.getConfig().getLong("authme.database.sync_interval_seconds", 30L) * 20L);
    }

    public boolean shouldStartWhitelistWatcher(boolean isFoliaServer) {
        return whitelistSyncService.isBukkitWhitelistJsonSyncEnabled() && !isFoliaServer;
    }
}
