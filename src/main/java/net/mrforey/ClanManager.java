package net.mrforey;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClanManager {

    private ClanSystem plugin;

    public ClanManager(ClanSystem plugin) {
        this.plugin = plugin;
    }

    // =============== Создание и удаление ===============
    public boolean createClan(Player player, String name, String tag) {
        UUID playerId = player.getUniqueId();

        if (isInClan(playerId)) {
            player.sendMessage(ChatColor.RED + "Вы уже состоите в клане!");
            return false;
        }

        int maxNameLength = plugin.getConfig().getInt("clan.max-name-length", 16);
        if (name.length() > maxNameLength) {
            player.sendMessage(ChatColor.RED + "Название слишком длинное! Макс: " + maxNameLength);
            return false;
        }

        int maxTagLength = plugin.getConfig().getInt("clan.max-tag-length", 6);
        if (tag.length() > maxTagLength) {
            player.sendMessage(ChatColor.RED + "Тег слишком длинный! Макс: " + maxTagLength);
            return false;
        }

        if (getClanByName(name) != null) {
            player.sendMessage(ChatColor.RED + "Клан с таким названием уже существует!");
            return false;
        }

        if (getClanByTag(tag.toUpperCase()) != null) {
            player.sendMessage(ChatColor.RED + "Клан с таким тегом уже существует!");
            return false;
        }

        int cost = plugin.getConfig().getInt("clan.creation-cost", 16);
        if (!hasEnoughDiamonds(player, cost)) {
            player.sendMessage(ChatColor.RED + "Недостаточно алмазов! Нужно: " + cost);
            return false;
        }

        String clanId = UUID.randomUUID().toString();

        Clan clan = new Clan(clanId, name, tag, playerId);

        removeDiamonds(player, cost);

        clan.addDiamonds(cost);

        plugin.getClans().put(clanId, clan);
        plugin.getPlayerClans().put(playerId, clanId);
        plugin.getPlayerRanks().put(playerId, ClanRank.LEADER);

        plugin.saveClans();

        player.sendMessage(ChatColor.GREEN + "═".repeat(40));
        player.sendMessage(ChatColor.GOLD + "Клан успешно создан!");
        player.sendMessage(ChatColor.YELLOW + "Название: " + ChatColor.WHITE + name);
        player.sendMessage(ChatColor.YELLOW + "Тег: " + ChatColor.WHITE + "[" + tag.toUpperCase() + "]");
        player.sendMessage(ChatColor.YELLOW + "Стоимость: " + ChatColor.AQUA + cost + " алмазов");
        player.sendMessage(ChatColor.GREEN + "═".repeat(40));

        return true;
    }

    private boolean hasEnoughDiamonds(Player player, int amount) {
        int diamonds = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                diamonds += item.getAmount();
            }
        }
        return diamonds >= amount;
    }

    private boolean removeDiamonds(Player player, int amount) {
        if (!hasEnoughDiamonds(player, amount)) {
            return false;
        }

        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.DIAMOND) {
                int itemAmount = item.getAmount();

                if (itemAmount <= remaining) {
                    player.getInventory().setItem(i, null);
                    remaining -= itemAmount;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }

                if (remaining <= 0) break;
            }
        }

        player.updateInventory();
        return true;
    }

    public boolean disbandClan(Player player) {
        Clan clan = getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return false;
        }

        if (!clan.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("error.not-leader", "&cТолько лидер может распустить клан!"));
            return false;
        }

        clan.broadcastMessage("&cКлан распущен лидером!");

        for (UUID memberId : clan.getMembers().keySet()) {
            plugin.getPlayerClans().remove(memberId);
            plugin.getPlayerRanks().remove(memberId);
        }

        plugin.getClans().remove(clan.getId());

        player.sendMessage(plugin.getMessage("clan.disbanded", "&cКлан распущен!"));
        return true;
    }

    // =============== Участники ===============
    public boolean invitePlayer(Player inviter, Player target) {
        Clan clan = getPlayerClan(inviter.getUniqueId());
        if (clan == null) {
            inviter.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return false;
        }

        if (!clan.getMemberRank(inviter.getUniqueId()).hasPermission(ClanPermission.INVITE)) {
            inviter.sendMessage(plugin.getMessage("error.no-permission", "&cУ вас нет прав для этого действия!"));
            return false;
        }

        if (isInClan(target.getUniqueId())) {
            inviter.sendMessage(plugin.getMessage("error.target-in-clan", "&cЭтот игрок уже состоит в клане!"));
            return false;
        }

        if (clan.getMemberCount() >= clan.getMaxMembers()) {
            inviter.sendMessage(plugin.getMessage("error.clan-full", "&cКлан достиг максимального количества участников!"));
            return false;
        }

        String message = plugin.getMessage("invite.received", "&aВы получили приглашение в клан {clan} от {player}!")
                .replace("{clan}", clan.getName())
                .replace("{player}", inviter.getName());
        target.sendMessage(message);

        inviter.sendMessage(plugin.getMessage("invite.sent", "&aПриглашение отправлено {player}!")
                .replace("{player}", target.getName()));

        return true;
    }

    public boolean acceptInvite(Player player, Clan clan) {
        if (isInClan(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("error.already-in-clan", "&cВы уже состоите в клане!"));
            return false;
        }

        if (clan.getMemberCount() >= clan.getMaxMembers()) {
            player.sendMessage(plugin.getMessage("error.clan-full", "&cКлан достиг максимального количества участников!"));
            return false;
        }

        clan.addMember(player.getUniqueId(), ClanRank.RECRUIT);
        plugin.getPlayerClans().put(player.getUniqueId(), clan.getId());
        plugin.getPlayerRanks().put(player.getUniqueId(), ClanRank.RECRUIT);

        player.sendMessage(plugin.getMessage("clan.joined", "&aВы вступили в клан {name}!")
                .replace("{name}", clan.getName()));

        clan.broadcastMessage("&aИгрок " + player.getName() + " вступил в клан!");

        return true;
    }

    public boolean kickPlayer(Player kicker, UUID targetId) {
        Clan clan = getPlayerClan(kicker.getUniqueId());
        if (clan == null) {
            kicker.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return false;
        }

        ClanRank kickerRank = clan.getMemberRank(kicker.getUniqueId());
        ClanRank targetRank = clan.getMemberRank(targetId);

        if (targetRank == null) {
            kicker.sendMessage(plugin.getMessage("error.player-not-in-clan", "&cИгрок не состоит в вашем клане!"));
            return false;
        }

        if (kickerRank.getPower() <= targetRank.getPower()) {
            kicker.sendMessage(plugin.getMessage("error.cant-kick-higher-rank", "&cВы не можете исключить игрока с таким же или более высоким рангом!"));
            return false;
        }

        clan.removeMember(targetId);
        plugin.getPlayerClans().remove(targetId);
        plugin.getPlayerRanks().remove(targetId);

        Player target = Bukkit.getPlayer(targetId);
        if (target != null) {
            target.sendMessage(plugin.getMessage("clan.kicked", "&cВы были исключены из клана {clan}!")
                    .replace("{clan}", clan.getName()));
        }

        clan.broadcastMessage("&cИгрок " +
                (target != null ? target.getName() : "Unknown") +
                " исключен из клана!");

        return true;
    }

    public boolean leaveClan(Player player) {
        Clan clan = getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return false;
        }

        if (clan.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("error.leader-cant-leave", "&cЛидер не может покинуть клан! Используйте /clan disband"));
            return false;
        }

        clan.removeMember(player.getUniqueId());
        plugin.getPlayerClans().remove(player.getUniqueId());
        plugin.getPlayerRanks().remove(player.getUniqueId());

        player.sendMessage(plugin.getMessage("clan.left", "&eВы покинули клан {name}!")
                .replace("{name}", clan.getName()));

        clan.broadcastMessage("&eИгрок " + player.getName() + " покинул клан!");

        return true;
    }

    // =============== Алмазы ===============
    public boolean depositDiamonds(Player player, int amount) {
        Clan clan = getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return false;
        }

        if (!ClanSystem.hasEnoughDiamonds(player, amount)) {
            player.sendMessage(plugin.getMessage("error.not-enough-diamonds", "&cУ вас недостаточно алмазов! Нужно: {amount}")
                    .replace("{amount}", String.valueOf(amount)));
            return false;
        }

        ClanSystem.removeDiamonds(player, amount);
        clan.addDiamonds(amount);

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member != null) {
            member.addContribution(amount);
        }

        player.sendMessage(plugin.getMessage("bank.deposited", "&aВы внесли {amount} алмазов в банк клана!")
                .replace("{amount}", String.valueOf(amount)));

        clan.broadcastMessage("&a" + player.getName() + " внес " + amount + " алмазов в банк!");

        return true;
    }

    public boolean withdrawDiamonds(Player player, int amount) {
        Clan clan = getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return false;
        }

        ClanRank rank = clan.getMemberRank(player.getUniqueId());
        if (!rank.hasPermission(ClanPermission.WITHDRAW_DIAMONDS)) {
            player.sendMessage(plugin.getMessage("error.no-permission", "&cУ вас нет прав для этого действия!"));
            return false;
        }

        if (clan.getDiamonds() < amount) {
            player.sendMessage(plugin.getMessage("error.not-enough-clan-diamonds", "&cВ банке клана недостаточно алмазов! Нужно: {amount}")
                    .replace("{amount}", String.valueOf(amount)));
            return false;
        }

        clan.removeDiamonds(amount);
        ClanSystem.giveDiamonds(player, amount);

        player.sendMessage(plugin.getMessage("bank.withdrawn", "&aВы сняли {amount} алмазов из банка клана!")
                .replace("{amount}", String.valueOf(amount)));

        clan.broadcastMessage("&e" + player.getName() + " снял " + amount + " алмазов из банка!");

        return true;
    }

    // =============== Улучшения ===============
    public boolean purchaseUpgrade(Player player, UpgradeType type) {
        Clan clan = getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return false;
        }

        ClanRank rank = clan.getMemberRank(player.getUniqueId());
        if (!rank.hasPermission(ClanPermission.MANAGE_UPGRADES)) {
            player.sendMessage(plugin.getMessage("error.no-permission", "&cУ вас нет прав для этого действия!"));
            return false;
        }

        int currentLevel = clan.getUpgradeLevel(type);
        if (currentLevel >= type.getMaxLevel()) {
            player.sendMessage(plugin.getMessage("error.max-upgrade-level", "&cУлучшение уже достигло максимального уровня!"));
            return false;
        }

        int cost = clan.getUpgradeCost(type);
        if (clan.getDiamonds() < cost) {
            player.sendMessage(plugin.getMessage("error.not-enough-clan-diamonds", "&cВ банке клана недостаточно алмазов! Нужно: {amount}")
                    .replace("{amount}", String.valueOf(cost)));
            return false;
        }

        clan.removeDiamonds(cost);
        clan.upgrade(type);

        player.sendMessage(plugin.getMessage("upgrade.purchased", "&aУлучшение {upgrade} повышено до уровня {level}! Стоимость: {cost} алмазов")
                .replace("{upgrade}", type.getDisplayName())
                .replace("{level}", String.valueOf(currentLevel + 1))
                .replace("{cost}", String.valueOf(cost)));

        clan.broadcastMessage("&aУлучшение " + type.getDisplayName() +
                " повышено до уровня " + (currentLevel + 1) + "!");

        return true;
    }

    // =============== Территории ===============
    public boolean createClaim(Player player, Chunk chunk) {
        Clan clan = getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getMessage("error.not-in-clan", "&cВы не состоите в клане!"));
            return false;
        }

        ClanRank rank = clan.getMemberRank(player.getUniqueId());
        if (!rank.hasPermission(ClanPermission.CLAIM)) {
            player.sendMessage(plugin.getMessage("error.no-permission", "&cУ вас нет прав для этого действия!"));
            return false;
        }

        if (!clan.canCreateClaim()) {
            player.sendMessage(plugin.getMessage("error.max-claims-reached", "&cКлан достиг максимального количества территорий!"));
            return false;
        }

        if (isChunkClaimed(chunk)) {
            player.sendMessage(plugin.getMessage("error.chunk-already-claimed", "&cЭта территория уже закреплена за другим кланом!"));
            return false;
        }

        int cost = plugin.getConfig().getInt("clan.claim-cost", 8);
        if (clan.getDiamonds() < cost) {
            player.sendMessage(plugin.getMessage("error.not-enough-clan-diamonds", "&cВ банке клана недостаточно алмазов! Нужно: {amount}")
                    .replace("{amount}", String.valueOf(cost)));
            return false;
        }

        ClanClaim claim = new ClanClaim(clan.getId(), chunk);
        clan.removeDiamonds(cost);
        clan.addClaim(claim);

        player.sendMessage(plugin.getMessage("claim.created", "&aТерритория закреплена за кланом! Стоимость: {cost} алмазов")
                .replace("{cost}", String.valueOf(cost)));

        clan.broadcastMessage("&aСоздана новая территория!");

        return true;
    }

    // =============== Поиск ===============
    public Clan getClanByName(String name) {
        for (Clan clan : plugin.getClans().values()) {
            if (clan.getName().equalsIgnoreCase(name)) {
                return clan;
            }
        }
        return null;
    }

    public Clan getClanByTag(String tag) {
        for (Clan clan : plugin.getClans().values()) {
            if (clan.getTag().equalsIgnoreCase(tag)) {
                return clan;
            }
        }
        return null;
    }

    public Clan getPlayerClan(UUID playerId) {
        String clanId = plugin.getPlayerClans().get(playerId);
        return clanId != null ? plugin.getClans().get(clanId) : null;
    }

    public boolean isInClan(UUID playerId) {
        return plugin.getPlayerClans().containsKey(playerId);
    }

    public boolean isChunkClaimed(Chunk chunk) {
        for (Clan clan : plugin.getClans().values()) {
            for (ClanClaim claim : clan.getClaims()) {
                if (claim.getChunk().equals(chunk)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Clan getClanAtLocation(Location location) {
        Chunk chunk = location.getChunk();
        for (Clan clan : plugin.getClans().values()) {
            if (clan.hasClaimAt(location)) {
                return clan;
            }
        }
        return null;
    }

    // =============== Рейтинг ===============
    public List<Clan> getTopClans(int limit) {
        return plugin.getClans().values().stream()
                .sorted((c1, c2) -> Integer.compare(c2.getLevel(), c1.getLevel()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<UUID, Integer>> getTopContributors(Clan clan, int limit) {
        HashMap<UUID, Integer> contributions = new HashMap<>();
        for (ClanMember member : clan.getMembers().values()) {
            contributions.put(member.getPlayerId(), member.getContributedDiamonds());
        }

        return contributions.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
