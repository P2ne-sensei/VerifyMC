package team.kitemc.verifymc.command;

import org.bukkit.command.CommandSender;
import team.kitemc.verifymc.VerifyMC;

public class HelpCommandHandler implements SubCommandHandler {
    private final VerifyMC plugin;

    public HelpCommandHandler(VerifyMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.showHelp(sender, plugin.getConfigLanguagePublic());
        return true;
    }
}
