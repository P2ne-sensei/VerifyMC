package team.kitemc.verifymc.command;

import org.bukkit.command.CommandSender;
import team.kitemc.verifymc.VerifyMC;

public class PortCommandHandler implements SubCommandHandler {
    private final VerifyMC plugin;

    public PortCommandHandler(VerifyMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "port";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.showPort(sender, plugin.getConfigLanguagePublic());
        return true;
    }
}
