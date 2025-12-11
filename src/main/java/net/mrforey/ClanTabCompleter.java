package net.mrforey;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ClanTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            String[] commands = {
                    "create", "disband", "leave", "info", "invite",
                    "kick", "deposit", "withdraw", "upgrade", "claim",
                    "unclaim", "sethome", "home", "chat", "description",
                    "promote", "demote", "top", "help"
            };

            for (String command : commands) {
                if (command.startsWith(args[0].toLowerCase())) {
                    completions.add(command);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "invite":
                case "kick":
                case "promote":
                case "demote":
                    for (Player online : player.getServer().getOnlinePlayers()) {
                        if (online.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(online.getName());
                        }
                    }
                    break;

                case "upgrade":
                    for (UpgradeType type : UpgradeType.values()) {
                        if (type.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(type.name().toLowerCase());
                        }
                    }
                    break;
            }
        }

        return completions;
    }

}
