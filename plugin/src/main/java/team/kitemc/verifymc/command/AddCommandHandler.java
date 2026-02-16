package team.kitemc.verifymc.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import team.kitemc.verifymc.VerifyMC;

public class AddCommandHandler implements SubCommandHandler {
    private final VerifyMC plugin;

    public AddCommandHandler(VerifyMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String language = plugin.getConfigLanguagePublic();
        if (args.length < 2) {
            sender.sendMessage("§e" + plugin.getMessagePublic("command.add_usage", language));
            return true;
        }
        if (sender instanceof Player && !sender.hasPermission("verifymc.admin")) {
            sender.sendMessage("§c" + plugin.getMessagePublic("command.no_permission", language));
            return true;
        }
        plugin.addWhitelist(sender, args[0], args[1], language);
        return true;
    }
}
