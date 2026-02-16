package team.kitemc.verifymc.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class CommandDispatcher implements CommandExecutor, TabCompleter {
    private final Map<String, SubCommandHandler> handlers = new LinkedHashMap<>();

    public CommandDispatcher register(SubCommandHandler handler) {
        handlers.put(handler.getName().toLowerCase(Locale.ROOT), handler);
        return this;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("vmc")) {
            return false;
        }
        String commandName = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        SubCommandHandler handler = handlers.get(commandName);
        if (handler == null) {
            handler = handlers.get("help");
        }
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        return handler != null && handler.execute(sender, subArgs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("vmc")) {
            return null;
        }
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String name : handlers.keySet()) {
                if (name.startsWith(prefix)) {
                    result.add(name);
                }
            }
            return result;
        }

        SubCommandHandler handler = handlers.get(args[0].toLowerCase(Locale.ROOT));
        if (handler == null) {
            return List.of();
        }
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return handler.tabComplete(sender, subArgs);
    }
}
