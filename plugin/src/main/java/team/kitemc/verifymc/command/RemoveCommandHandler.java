package team.kitemc.verifymc.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import team.kitemc.verifymc.VerifyMC;

public class RemoveCommandHandler implements SubCommandHandler {
    private final VerifyMC plugin;

    public RemoveCommandHandler(VerifyMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String language = plugin.getConfigLanguagePublic();
        if (args.length < 1) {
            sender.sendMessage("§e" + plugin.getMessagePublic("command.remove_usage", language));
            return true;
        }
        if (sender instanceof Player && !sender.hasPermission("verifymc.admin")) {
            sender.sendMessage("§c" + plugin.getMessagePublic("command.no_permission", language));
            return true;
        }
        plugin.removeWhitelist(sender, args[0], language);
        return true;
    }
}
