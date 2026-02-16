package team.kitemc.verifymc.listener;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import team.kitemc.verifymc.VerifyMC;
import team.kitemc.verifymc.db.UserDao;

public class PlayerWhitelistListener implements Listener {
    private final VerifyMC plugin;
    private final UserDao userDao;
    private final String webRegisterUrl;

    public PlayerWhitelistListener(VerifyMC plugin, UserDao userDao, String webRegisterUrl) {
        this.plugin = plugin;
        this.userDao = userDao;
        this.webRegisterUrl = webRegisterUrl;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String ip = event.getAddress() != null ? event.getAddress().getHostAddress() : "";
        if (plugin.getConfig().getStringList("whitelist_bypass_ips").contains(ip)) {
            plugin.debugLog("Bypassed whitelist check for IP: " + ip);
            return;
        }

        Map<String, Object> user = userDao != null ? userDao.getAllUsers().stream()
            .filter(u -> player.getName().equalsIgnoreCase((String) u.get("username"))
                && "approved".equals(u.get("status")))
            .findFirst()
            .orElse(null) : null;

        if (user == null) {
            String msg = "§c[ VerifyMC ]\n§7Please visit §a" + webRegisterUrl + " §7to register";
            event.disallow(Result.KICK_WHITELIST, msg);
            plugin.debugLog("Blocked unregistered player: " + player.getName() + " from IP: " + ip);
            return;
        }

        event.setResult(Result.ALLOWED);
        plugin.debugLog("Allowed registered player: " + player.getName() + " (Status: approved)");
    }
}
