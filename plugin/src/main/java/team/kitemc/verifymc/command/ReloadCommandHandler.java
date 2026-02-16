package team.kitemc.verifymc.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import team.kitemc.verifymc.VerifyMC;

public class ReloadCommandHandler implements SubCommandHandler {
    private final VerifyMC plugin;

    public ReloadCommandHandler(VerifyMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String language = plugin.getConfigLanguagePublic();
        if (sender instanceof Player && !sender.hasPermission("verifymc.admin")) {
            sender.sendMessage("§c" + plugin.getMessagePublic("command.no_permission", language));
            return true;
        }
        plugin.reloadPlugin(sender, language);
        return true;
    }
}
