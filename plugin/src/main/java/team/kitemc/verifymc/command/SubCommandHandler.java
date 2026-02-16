package team.kitemc.verifymc.command;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;

public interface SubCommandHandler {
    String getName();

    boolean execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
