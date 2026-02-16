package team.kitemc.verifymc.service.whitelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import team.kitemc.verifymc.VerifyMC;
import team.kitemc.verifymc.db.UserDao;

public class WhitelistSyncService {
    private final VerifyMC plugin;
    private final UserDao userDao;
    private final String whitelistMode;
    private final boolean whitelistJsonSync;
    private final Path whitelistJsonPath;
    private long lastWhitelistJsonModified = 0L;

    public WhitelistSyncService(VerifyMC plugin, UserDao userDao, String whitelistMode, boolean whitelistJsonSync,
                                Path whitelistJsonPath) {
        this.plugin = plugin;
        this.userDao = userDao;
        this.whitelistMode = whitelistMode;
        this.whitelistJsonSync = whitelistJsonSync;
        this.whitelistJsonPath = whitelistJsonPath;
    }

    public void syncWhitelistToServer() {
        for (Map<String, Object> user : userDao.getAllUsers()) {
            String name = (String) user.get("username");
            String status = (String) user.get("status");
            if ("approved".equals(status)) {
                Bukkit.getOfflinePlayer(name).setWhitelisted(true);
            } else if ("banned".equals(status)) {
                Bukkit.getOfflinePlayer(name).setWhitelisted(false);
            }
        }
    }

    public void cleanupServerWhitelist() {
        for (OfflinePlayer player : Bukkit.getWhitelistedPlayers()) {
            Map<String, Object> user = userDao.getUserByUsername(player.getName());
            if (user == null || !"approved".equals(user.get("status"))) {
                player.setWhitelisted(false);
            } else {
                player.setWhitelisted(true);
            }
        }
    }

    public void startWhitelistJsonWatcher() {
        if (whitelistJsonPath == null) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (Files.exists(whitelistJsonPath)) {
                        long modified = Files.getLastModifiedTime(whitelistJsonPath).toMillis();
                        if (modified != lastWhitelistJsonModified) {
                            lastWhitelistJsonModified = modified;
                            syncWhitelistJsonToPlugin();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }.runTaskTimerAsynchronously(plugin, 40L, 100L);
    }

    public void syncPluginToWhitelistJson() {
        if (!"bukkit".equalsIgnoreCase(whitelistMode) || whitelistJsonPath == null) {
            return;
        }
        try {
            List<Map<String, Object>> users = userDao.getAllUsers();
            List<Map<String, Object>> whitelistEntries = new ArrayList<>();
            for (Map<String, Object> user : users) {
                if ("approved".equals(user.get("status"))) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("uuid", user.get("uuid"));
                    entry.put("name", user.get("username"));
                    entry.put("whitelisted", true);
                    whitelistEntries.add(entry);
                }
            }
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(whitelistEntries);
            Files.write(
                whitelistJsonPath,
                json.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception ignored) {
        }
    }

    public void syncWhitelistJsonToPlugin() {
        if (whitelistJsonPath == null) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(whitelistJsonPath);
            String json = String.join("\n", lines);
            List<Map<String, Object>> list = new Gson().fromJson(json, List.class);
            for (Map<String, Object> entry : list) {
                String uuid = (String) entry.get("uuid");
                if (uuid == null) {
                    continue;
                }
                Map<String, Object> user = userDao.getAllUsers().stream()
                    .filter(candidate -> uuid.equals(candidate.get("uuid")))
                    .findFirst()
                    .orElse(null);
                if (user == null) {
                    continue;
                }
                Object whitelisted = entry.get("whitelisted");
                String currentStatus = (String) user.get("status");
                if ("pending".equals(currentStatus) && Boolean.TRUE.equals(whitelisted)) {
                    user.put("status", "approved");
                } else if (!"approved".equals(currentStatus) && !"banned".equals(currentStatus)
                    && !Boolean.TRUE.equals(whitelisted)) {
                    user.put("status", "pending");
                }
            }
            userDao.save();
        } catch (Exception ignored) {
        }
    }

    public boolean isBukkitWhitelistJsonSyncEnabled() {
        return "bukkit".equalsIgnoreCase(whitelistMode) && whitelistJsonSync;
    }
}
