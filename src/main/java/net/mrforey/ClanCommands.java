package net.mrforey;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ClanCommands implements CommandExecutor {

    private ClanSystem plugin;

    public ClanCommands(ClanSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            plugin.getClanGUI().openMainMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreateCommand(player, args);
                break;

            case "test":
                player.sendMessage(ChatColor.GREEN + "Плагин работает!");
                player.sendMessage(ChatColor.YELLOW + "Всего кланов: " + plugin.getClans().size());
                break;

            case "disband":
                plugin.getClanManager().disbandClan(player);
                break;

            case "leave":
                plugin.getClanManager().leaveClan(player);
                break;

            case "info":
                handleInfoCommand(player, args);
                break;

            case "invite":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessage("clan.invite-usage", "&eИспользуйте: &7/clan invite <игрок>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    plugin.getClanManager().invitePlayer(player, target);
                } else {
                    player.sendMessage(plugin.getMessage("error.player-offline", "&cИгрок не в сети!"));
                }
                break;

            case "join":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessage("clan.join-usage", "&eИспользуйте: &7/clan join <название/тег>"));
                    return true;
                }
                Clan clan = plugin.getClanManager().getClanByName(args[1]);
                if (clan == null) {
                    clan = plugin.getClanManager().getClanByTag(args[1]);
                }
                if (clan != null) {
                    plugin.getClanManager().acceptInvite(player, clan);
                } else {
                    player.sendMessage(plugin.getMessage("error.clan-not-found", "&cКлан не найден!"));
                }
                break;

            case "kick":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessage("clan.kick-usage", "&eИспользуйте: &7/clan kick <игрок>"));
                    return true;
                }
                Player kickTarget = Bukkit.getPlayer(args[1]);
                if (kickTarget != null) {
                    plugin.getClanManager().kickPlayer(player, kickTarget.getUniqueId());
                } else {
                    player.sendMessage(plugin.getMessage("error.player-offline", "&cИгрок не в сети!"));
                }
                break;

            case "deposit":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessage("bank.deposit-usage", "&eИспользуйте: &7/clan deposit <количество>"));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    if (amount <= 0) throw new NumberFormatException();
                    plugin.getClanManager().depositDiamonds(player, amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getMessage("error.invalid-amount", "&cНекорректная сумма!"));
                }
                break;

            case "withdraw":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessage("bank.withdraw-usage", "&eИспользуйте: &7/clan withdraw <количество>"));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    if (amount <= 0) throw new NumberFormatException();
                    plugin.getClanManager().withdrawDiamonds(player, amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getMessage("error.invalid-amount", "&cНекорректная сумма!"));
                }
                break;

            case "upgrade":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessage("upgrade.usage", "&eИспользуйте: &7/clan upgrade <тип>"));
                    return true;
                }
                try {
                    UpgradeType type = UpgradeType.valueOf(args[1].toUpperCase());
                    plugin.getClanManager().purchaseUpgrade(player, type);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(plugin.getMessage("error.invalid-upgrade", "&cНекорректное улучшение!"));
                }
                break;

            case "claim":
                Chunk chunk = player.getLocation().getChunk();
                plugin.getClanManager().createClaim(player, chunk);
                break;

            case "unclaim":
                handleUnclaimCommand(player);
                break;

            case "sethome":
                handleSetHomeCommand(player);
                break;

            case "home":
                handleHomeCommand(player);
                break;

            case "chat":
            case "c":
                handleChatCommand(player, args);
                break;

            case "description":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessage("clan.description-usage", "&eИспользуйте: &7/clan description <текст>"));
                    return true;
                }
                String description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                handleDescriptionCommand(player, description);
                break;

            case "promote":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessage("member.promote-usage", "&eИспользуйте: &7/clan promote <игрок>"));
                    return true;
                }
                handlePromoteCommand(player, args[1]);
                break;

            case "demote":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessage("member.demote-usage", "&eИспользуйте: &7/clan demote <игрок>"));
                    return true;
                }
                handleDemoteCommand(player, args[1]);
                break;

            case "top":
                showTopClans(player);
                break;

            case "help":
                showHelp(player);
                break;

            default:
                player.sendMessage(plugin.getMessage("error.unknown-command", "&cНеизвестная команда. Используйте &7/clan help"));
                break;
        }

        return true;
    }

    private void handleInfoCommand(Player player, String[] args) {
        Clan clan;
        if (args.length > 1) {
            clan = plugin.getClanManager().getClanByName(args[1]);
            if (clan == null) {
                clan = plugin.getClanManager().getClanByTag(args[1]);
            }
        } else {
            clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        }

        if (clan != null) {
            player.sendMessage(clan.getFormattedInfo());
        } else {
            player.sendMessage(plugin.getMessage("error.clan-not-found", "&cКлан не найден!"));
        }
    }

    private void handleUnclaimCommand(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return;
        }

        Location location = player.getLocation();
        ClanClaim claimToRemove = null;

        for (ClanClaim claim : clan.getClaims()) {
            if (claim.contains(location)) {
                claimToRemove = claim;
                break;
            }
        }

        if (claimToRemove != null) {
            clan.removeClaim(claimToRemove);
            player.sendMessage(plugin.getMessage("claim.removed", "&cТерритория удалена!"));
            clan.broadcastMessage("&cТерритория удалена " + player.getName());
        } else {
            player.sendMessage(plugin.getMessage("error.no-claim-here", "&cЗдесь нет вашей территории!"));
        }
    }

    private void handleSetHomeCommand(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return;
        }

        ClanRank rank = clan.getMemberRank(player.getUniqueId());
        if (!rank.hasPermission(ClanPermission.SET_WARP)) {
            player.sendMessage(plugin.getMessage("error.no-permission", "&cУ вас нет прав для этого действия!"));
            return;
        }

        clan.setHome(player.getLocation(), player.getName());
        player.sendMessage(plugin.getMessage("home.set", "&aДом клана установлен!"));
        clan.broadcastMessage("&aДом клана установлен " + player.getName());
    }

    private void handleHomeCommand(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return;
        }

        Location home = clan.getHomeLocation();
        if (home == null) {
            player.sendMessage(plugin.getMessage("error.no-home-set", "&cДом клана не установлен!"));
            return;
        }

        player.teleport(home);
        player.sendMessage(plugin.getMessage("home.teleported", "&aВы телепортированы к дому клана!"));
    }

    private void handleChatCommand(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return;
        }

        if (args.length > 1) {
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            String format = plugin.getMessage("chat.format", "&8[&2Клан&8] &e{rank}&7 {player}&f: {message}")
                    .replace("{player}", player.getName())
                    .replace("{rank}", clan.getMemberRank(player.getUniqueId()).getDisplayName())
                    .replace("{message}", message);

            clan.broadcastMessage(format);
        } else {
            player.sendMessage(plugin.getMessage("chat.toggle-usage", "&eИспользуйте: &7/clan chat <сообщение> &eили &7/c <сообщение>"));
        }
    }

    private void handleDescriptionCommand(Player player, String description) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return;
        }

        ClanRank rank = clan.getMemberRank(player.getUniqueId());
        if (!rank.hasPermission(ClanPermission.SET_DESCRIPTION)) {
            player.sendMessage(plugin.getMessage("error.no-permission", "&cУ вас нет прав для этого действия!"));
            return;
        }

        if (description.length() > 100) {
            player.sendMessage(plugin.getMessage("error.description-too-long", "&cОписание слишком длинное! (макс. 100 символов)"));
            return;
        }

        clan.setDescription(description);
        player.sendMessage(plugin.getMessage("clan.description-changed", "&aОписание клана изменено!"));
        clan.broadcastMessage("&eОписание клана изменено " + player.getName());
    }

    private void handlePromoteCommand(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getMessage("error.player-offline", "&cИгрок не в сети!"));
            return;
        }

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return;
        }

        ClanRank playerRank = clan.getMemberRank(player.getUniqueId());
        ClanRank targetRank = clan.getMemberRank(target.getUniqueId());

        if (targetRank == null) {
            player.sendMessage(plugin.getMessage("error.player-not-in-clan", "&cИгрок не состоит в вашем клане!"));
            return;
        }

        if (playerRank.getPower() <= targetRank.getPower()) {
            player.sendMessage(plugin.getMessage("error.cant-modify-higher-rank", "&cВы не можете изменять ранг игрока с таким же или более высоким рангом!"));
            return;
        }

        ClanRank nextRank = targetRank.getNextRank();
        if (nextRank == null) {
            player.sendMessage(plugin.getMessage("error.max-rank", "&cИгрок уже имеет максимальный ранг!"));
            return;
        }

        clan.setMemberRank(target.getUniqueId(), nextRank);
        player.sendMessage(plugin.getMessage("member.promoted", "&aИгрок &e{player} &aповышен до &e{rank}")
                .replace("{player}", target.getName())
                .replace("{rank}", nextRank.getDisplayName()));

        target.sendMessage(plugin.getMessage("member.you-promoted", "&aВы повышены до &e{rank}")
                .replace("{rank}", nextRank.getDisplayName()));

        clan.broadcastMessage("&e" + target.getName() + " повышен до " + nextRank.getDisplayName());
    }

    private void handleDemoteCommand(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getMessage("error.player-offline", "&cИгрок не в сети!"));
            return;
        }

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return;
        }

        ClanRank playerRank = clan.getMemberRank(player.getUniqueId());
        ClanRank targetRank = clan.getMemberRank(target.getUniqueId());

        if (targetRank == null) {
            player.sendMessage(plugin.getMessage("error.player-not-in-clan", "&cИгрок не состоит в вашем клане!"));
            return;
        }

        if (playerRank.getPower() <= targetRank.getPower()) {
            player.sendMessage(plugin.getMessage("error.cant-modify-higher-rank", "&cВы не можете изменять ранг игрока с таким же или более высоким рангом!"));
            return;
        }

        ClanRank prevRank = targetRank.getPreviousRank();
        if (prevRank == null) {
            player.sendMessage(plugin.getMessage("error.min-rank", "&cИгрок уже имеет минимальный ранг!"));
            return;
        }

        clan.setMemberRank(target.getUniqueId(), prevRank);
        player.sendMessage(plugin.getMessage("member.demoted", "&aИгрок &e{player} &aпонижен до &e{rank}")
                .replace("{player}", target.getName())
                .replace("{rank}", prevRank.getDisplayName()));

        target.sendMessage(plugin.getMessage("member.you-demoted", "&cВы понижены до &e{rank}")
                .replace("{rank}", prevRank.getDisplayName()));

        clan.broadcastMessage("&e" + target.getName() + " понижен до " + prevRank.getDisplayName());
    }

    private void showTopClans(Player player) {
        List<Clan> top = plugin.getClanManager().getTopClans(10);

        player.sendMessage(ChatColor.GOLD + "=== Топ кланов ===");

        int rank = 1;
        for (Clan clan : top) {
            String rankColor;
            switch (rank) {
                case 1: rankColor = "§6❶"; break;
                case 2: rankColor = "§7❷"; break;
                case 3: rankColor = "§c❸"; break;
                default: rankColor = "§f" + rank + "."; break;
            }

            player.sendMessage(rankColor + " " + ChatColor.GOLD + clan.getName() +
                    " [" + clan.getTag() + "] §7- Ур. " + clan.getLevel() +
                    " | Уч.: " + clan.getMemberCount());
            rank++;
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Помощь по кланам ===");
        player.sendMessage(ChatColor.YELLOW + "/clan" + ChatColor.WHITE + " - Открыть меню");
        player.sendMessage(ChatColor.YELLOW + "/clan create <название> <тег>" + ChatColor.WHITE + " - Создать клан");
        player.sendMessage(ChatColor.YELLOW + "/clan info [клан]" + ChatColor.WHITE + " - Информация о клане");
        player.sendMessage(ChatColor.YELLOW + "/clan invite <игрок>" + ChatColor.WHITE + " - Пригласить игрока");
        player.sendMessage(ChatColor.YELLOW + "/clan kick <игрок>" + ChatColor.WHITE + " - Исключить игрока");
        player.sendMessage(ChatColor.YELLOW + "/clan leave" + ChatColor.WHITE + " - Покинуть клан");
        player.sendMessage(ChatColor.YELLOW + "/clan deposit <кол-во>" + ChatColor.WHITE + " - Внести алмазы");
        player.sendMessage(ChatColor.YELLOW + "/clan withdraw <кол-во>" + ChatColor.WHITE + " - Снять алмазы");
        player.sendMessage(ChatColor.YELLOW + "/clan chat <сообщение>" + ChatColor.WHITE + " - Клановый чат");
        player.sendMessage(ChatColor.YELLOW + "/clan home" + ChatColor.WHITE + " - Телепорт к дому");
        player.sendMessage(ChatColor.YELLOW + "/clan sethome" + ChatColor.WHITE + " - Установить дом");
        player.sendMessage(ChatColor.YELLOW + "/clan top" + ChatColor.WHITE + " - Топ кланов");
        player.sendMessage(ChatColor.YELLOW + "/clan help" + ChatColor.WHITE + " - Эта помощь");
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Используйте: /clan create <название> <тег>");
            player.sendMessage(ChatColor.YELLOW + "Пример: /clan create Warriors WAR");
            return;
        }

        String clanName = args[1];
        String clanTag = args[2];

        ClanManager clanManager = plugin.getClanManager();

        if (clanManager == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: ClanManager не инициализирован!");
            return;
        }

        boolean success = clanManager.createClan(player, clanName, clanTag);

        if (success) {
            player.sendMessage(plugin.getMessage("clan.created", "&aКлан успешно создан!"));
            player.sendMessage(ChatColor.GREEN + "Название: " + clanName);
            player.sendMessage(ChatColor.GREEN + "Тег: [" + clanTag.toUpperCase() + "]");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getClanGUI().openClanMenu(player);
            }, 5L);
        }
    }
}
