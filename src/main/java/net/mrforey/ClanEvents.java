package net.mrforey;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.UUID;

public class ClanEvents implements Listener {

    private ClanSystem plugin;
    private HashMap<UUID, Boolean> clanChatMode = new HashMap<>();

    public ClanEvents(ClanSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (clan != null) {
            ClanMember member = clan.getMember(player.getUniqueId());
            if (member != null) {
                member.updateLastSeen();
            }

            if (plugin.getConfig().getBoolean("clan.notify-join-leave", true)) {
                clan.broadcastMessage("&a▶ &7" + player.getName() + " зашел на сервер");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (clan != null && plugin.getConfig().getBoolean("clan.notify-join-leave", true)) {
            clan.broadcastMessage("&c◀ &7" + player.getName() + " вышел с сервера");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();

        Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        Clan killerClan = killer != null ? plugin.getClanManager().getPlayerClan(killer.getUniqueId()) : null;

        if (playerClan != null) {
            playerClan.addExperience(10);
        }

        if (killer != null && killerClan != null && playerClan != null) {
            if (!playerClan.getId().equals(killerClan.getId())) {
                killerClan.addExperience(50);
                killerClan.broadcastMessage("&a" + killer.getName() + " убил " + player.getName() + " из клана " + playerClan.getName());
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) ||
                !(event.getDamager() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        Clan victimClan = plugin.getClanManager().getPlayerClan(victim.getUniqueId());
        Clan attackerClan = plugin.getClanManager().getPlayerClan(attacker.getUniqueId());

        if (victimClan != null && attackerClan != null) {
            if (victimClan.getId().equals(attackerClan.getId())) {
                if (!plugin.getConfig().getBoolean("clan.allow-friendly-fire", false)) {
                    event.setCancelled(true);
                    attacker.sendMessage(plugin.getMessage("error.friendly-fire", "Дружественный огонь Включен"));
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Clan blockClan = plugin.getClanManager().getClanAtLocation(block.getLocation());
        Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (blockClan != null) {
            if (playerClan == null || !playerClan.getId().equals(blockClan.getId())) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMessage("error.cant-break-claim", "Нельзя ломать")
                        .replace("{clan}", blockClan.getName()));
            }
        }

        if (playerClan != null) {
            int exp = getExpForBlock(block.getType());
            if (exp > 0) {
                playerClan.addExperience(exp);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Clan blockClan = plugin.getClanManager().getClanAtLocation(block.getLocation());
        Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (blockClan != null) {
            if (playerClan == null || !playerClan.getId().equals(blockClan.getId())) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMessage("error.cant-place-claim", "Нераспознано")
                        .replace("{clan}", blockClan.getName()));
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (clanChatMode.getOrDefault(player.getUniqueId(), false)) {
            event.setCancelled(true);

            Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan != null) {
                String message = event.getMessage();
                String format = plugin.getMessage("chat.format", "&8[&6Клан&8] {rank} &7{player}: &f{message}")
                        .replace("{player}", player.getName())
                        .replace("{rank}", clan.getMemberRank(player.getUniqueId()).getDisplayName())
                        .replace("{message}", message);

                clan.broadcastMessage(format);
            }
        } else {
            Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan != null && plugin.getConfig().getBoolean("clan.show-tag-in-chat", true)) {
                String format = event.getFormat();
                format = ChatColor.GRAY + "[" + clan.getTag() + ChatColor.GRAY + "] " + format;
                event.setFormat(format);
            }
        }
    }

    private int getExpForBlock(Material material) {
        switch (material) {
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return 10;
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return 8;
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
                return 5;
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return 3;
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return 2;
            default:
                return 1;
        }
    }

    public void toggleClanChat(Player player) {
        UUID uuid = player.getUniqueId();
        boolean current = clanChatMode.getOrDefault(uuid, false);
        clanChatMode.put(uuid, !current);

        if (!current) {
            player.sendMessage(plugin.getMessage("chat.enabled", "&aРежим кланового чата включен!"));
        } else {
            player.sendMessage(plugin.getMessage("chat.disabled", "&cРежим кланового чата выключен!"));
        }
    }

}
