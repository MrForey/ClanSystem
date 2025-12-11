package net.mrforey;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class ClanGUI implements Listener {

    private ClanSystem plugin;
    private Map<UUID, GUIPage> playerPages = new HashMap<>();
    private Map<UUID, Integer> playerPageNumbers = new HashMap<>();

    public ClanGUI(ClanSystem plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                ChatColor.DARK_GRAY + "Система кланов");

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        if (clan != null) {
            inv.setItem(11, createItem(Material.SHIELD,
                    ChatColor.GOLD + "Мой клан",
                    "§7Название: §e" + clan.getName(),
                    "§7Тег: §f[" + clan.getTag() + "]",
                    "§7Участников: §a" + clan.getMemberCount(),
                    "",
                    "§eНажмите для управления"));

            inv.setItem(13, createItem(Material.DIAMOND,
                    ChatColor.AQUA + "Банк клана",
                    "§7Алмазов: §b" + clan.getDiamonds() + " ♦",
                    "§7Уровень: §e" + clan.getLevel(),
                    "",
                    "§eНажмите для управления"));

            inv.setItem(15, createItem(Material.COMPASS,
                    ChatColor.YELLOW + "Обзор кланов",
                    "§7Просмотреть другие кланы",
                    "",
                    "§eНажмите для просмотра"));
        } else {
            inv.setItem(11, createItem(Material.WRITABLE_BOOK,
                    ChatColor.GREEN + "Создать клан",
                    "§7Создать новый клан",
                    "§7Цена: §b" + plugin.getConfig().getInt("clan.creation-cost", 16) + " алмазов ♦",
                    "",
                    "§eНажмите для создания",
                    "§7Или используйте §e/clan create <название> <тег>"));

            inv.setItem(13, createItem(Material.COMPASS,
                    ChatColor.YELLOW + "Найти кланы",
                    "§7Просмотреть список кланов",
                    "",
                    "§eНажмите для просмотра"));

            inv.setItem(15, createItem(Material.PAPER,
                    ChatColor.BLUE + "Приглашения",
                    "§7Просмотреть приглашения",
                    "§cНет приглашений"));
        }

        inv.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "Закрыть"));

        player.openInventory(inv);
        playerPages.put(player.getUniqueId(), GUIPage.MAIN);
    }

    public void openClanMenu(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Вы не состоите в клане!");
            openMainMenu(player);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_GRAY + "Клан: " + clan.getName());

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }

        inv.setItem(4, createClanInfoItem(clan, player));

        inv.setItem(19, createItem(Material.PLAYER_HEAD,
                ChatColor.YELLOW + "Участники",
                "§7Участников: §e" + clan.getMemberCount() + "/" + clan.getMaxMembers(),
                "§7Онлайн: §a" + clan.getOnlineMemberCount(),
                "",
                "§eНажмите для управления"));

        inv.setItem(21, createItem(Material.ANVIL,
                ChatColor.LIGHT_PURPLE + "Улучшения",
                "§7Улучшить возможности клана",
                "",
                "§eНажмите для просмотра"));

        inv.setItem(23, createItem(Material.MAP,
                ChatColor.GREEN + "Территории",
                "§7Территорий: §a" + clan.getClaims().size(),
                "",
                "§eНажмите для управления"));

        inv.setItem(25, createItem(Material.CHEST,
                ChatColor.AQUA + "Банк клана",
                "§7Алмазов: §b" + clan.getDiamonds() + " ♦",
                "§7Ваш вклад: §a" + clan.getMember(player.getUniqueId()).getContributedDiamonds() + " алмазов",
                "",
                "§eНажмите для управления"));

        ClanRank rank = clan.getMemberRank(player.getUniqueId());
        if (rank.hasPermission(ClanPermission.MANAGE_SETTINGS)) {
            inv.setItem(39, createItem(Material.REDSTONE_TORCH,
                    ChatColor.RED + "Настройки",
                    "§7Настройки клана",
                    "",
                    "§eНажмите для изменения"));
        }

        inv.setItem(40, createItem(Material.BOOK,
                ChatColor.GOLD + "Описание",
                "§7Текущее описание:",
                "§f" + clan.getDescription(),
                "",
                "§eНажмите чтобы изменить"));

        if (!clan.getLeaderId().equals(player.getUniqueId())) {
            inv.setItem(41, createItem(Material.OAK_DOOR,
                    ChatColor.DARK_RED + "Покинуть клан",
                    "§cНажмите чтобы покинуть клан",
                    "§7Используйте §e/clan leave §7для подтверждения"));
        }

        inv.setItem(49, createItem(Material.ARROW, ChatColor.GRAY + "← Назад"));

        player.openInventory(inv);
        playerPages.put(player.getUniqueId(), GUIPage.CLAN_MENU);
    }

    public void openClanBrowser(Player player, int page) {
        List<Clan> clans = new ArrayList<>(plugin.getClans().values());
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_GRAY + "Список кланов - Страница " + page);

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }

        int startIndex = (page - 1) * 45;
        int slot = 0;

        for (int i = startIndex; i < Math.min(startIndex + 45, clans.size()); i++) {
            Clan clan = clans.get(i);
            inv.setItem(slot, createClanBrowserItem(clan));
            slot++;
        }

        if (page > 1) {
            inv.setItem(45, createItem(Material.ARROW, ChatColor.GREEN + "← Предыдущая"));
        }

        if (startIndex + 45 < clans.size()) {
            inv.setItem(53, createItem(Material.ARROW, ChatColor.GREEN + "Следующая →"));
        }

        inv.setItem(49, createItem(Material.ARROW, ChatColor.GRAY + "← Назад"));

        player.openInventory(inv);
        playerPages.put(player.getUniqueId(), GUIPage.BROWSER);
        playerPageNumbers.put(player.getUniqueId(), page);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (item == null || !item.hasItemMeta()) return;

        String title = event.getView().getTitle();

        if (!title.contains("Система кланов") &&
                !title.contains("Клан:") &&
                !title.contains("Список кланов")) {
            return;
        }

        event.setCancelled(true);

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        if (displayName.equals("Назад") || displayName.equals("← Назад")) {
            handleBackButton(player);
            return;
        }

        if (displayName.equals("Предыдущая") || displayName.equals("← Предыдущая")) {
            int currentPage = playerPageNumbers.getOrDefault(player.getUniqueId(), 1);
            if (currentPage > 1) {
                openClanBrowser(player, currentPage - 1);
            }
            return;
        }

        if (displayName.equals("Следующая") || displayName.equals("Следующая →")) {
            int currentPage = playerPageNumbers.getOrDefault(player.getUniqueId(), 1);
            openClanBrowser(player, currentPage + 1);
            return;
        }

        handleMenuClick(player, item, title);
    }

    private void handleBackButton(Player player) {
        GUIPage currentPage = playerPages.get(player.getUniqueId());

        switch (currentPage) {
            case CLAN_MENU:
            case BROWSER:
                openMainMenu(player);
                break;
            default:
                openMainMenu(player);
                break;
        }
    }

    private void handleMenuClick(Player player, ItemStack item, String title) {
        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        if (title.contains("Система кланов")) {
            switch (item.getType()) {
                case WRITABLE_BOOK:
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Используйте: /clan create <название> <тег>");
                    break;

                case SHIELD:
                    openClanMenu(player);
                    break;

                case COMPASS:
                    openClanBrowser(player, 1);
                    break;

                case BARRIER:
                    player.closeInventory();
                    break;
            }
        } else if (title.contains("Клан:")) {
            switch (item.getType()) {
                case PLAYER_HEAD:
                    player.sendMessage(ChatColor.YELLOW + "Функция участников в разработке");
                    break;

                case ANVIL:
                    player.sendMessage(ChatColor.YELLOW + "Функция улучшений в разработке");
                    break;

                case MAP:
                    player.sendMessage(ChatColor.YELLOW + "Функция территорий в разработке");
                    break;

                case CHEST:
                    player.sendMessage(ChatColor.YELLOW + "Функция банка в разработке");
                    break;

                case OAK_DOOR:
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Используйте /clan leave для выхода из клана");
                    break;
            }
        } else if (title.contains("Список кланов")) {
            if (item.getType() == Material.SHIELD) {
                Clan clan = getClanFromBrowserItem(item);
                if (clan != null) {
                    player.sendMessage(ChatColor.GOLD + "=== Информация о клане ===");
                    player.sendMessage(ChatColor.YELLOW + "Название: " + ChatColor.WHITE + clan.getName());
                    player.sendMessage(ChatColor.YELLOW + "Тег: " + ChatColor.WHITE + "[" + clan.getTag() + "]");
                    player.sendMessage(ChatColor.YELLOW + "Участников: " + ChatColor.WHITE + clan.getMemberCount());
                    player.sendMessage(ChatColor.YELLOW + "Уровень: " + ChatColor.WHITE + clan.getLevel());
                    player.sendMessage(ChatColor.YELLOW + "Описание: " + ChatColor.WHITE + clan.getDescription());
                }
            }
        }
    }

    private Clan getClanFromBrowserItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        String[] parts = displayName.split(" ");

        if (parts.length >= 3) {
            String clanName = parts[0];
            String tag = parts[2].replace("[", "").replace("]", "");
            return plugin.getClanManager().getClanByName(clanName);
        }

        return null;
    }

    private ItemStack createClanInfoItem(Clan clan, Player player) {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + clan.getName() + " [" + clan.getTag() + "]");

        List<String> lore = new ArrayList<>();
        lore.add("§7Уровень: §e" + clan.getLevel());
        lore.add("§7Опыт: §a" + clan.getExperience() + "/" + clan.getRequiredExperience());
        lore.add("§7Алмазы: §b" + clan.getDiamonds() + " ♦");
        lore.add("§7Участников: §f" + clan.getMemberCount() + "/" + clan.getMaxMembers());
        lore.add("§7Онлайн: §a" + clan.getOnlineMemberCount());
        lore.add("§7Территорий: §2" + clan.getClaims().size());
        lore.add("");

        ClanRank playerRank = clan.getMemberRank(player.getUniqueId());
        lore.add("§7Ваш ранг: " + playerRank.getDisplayName());

        String leaderName = Bukkit.getOfflinePlayer(clan.getLeaderId()).getName();
        lore.add("§7Лидер: §6" + (leaderName != null ? leaderName : "Неизвестно"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClanBrowserItem(Clan clan) {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + clan.getName() + " [" + clan.getTag() + "]");

        List<String> lore = new ArrayList<>();
        lore.add("§7Уровень: §e" + clan.getLevel());
        lore.add("§7Участников: §a" + clan.getMemberCount() + "/" + clan.getMaxMembers());
        lore.add("§7Алмазы: §b" + clan.getDiamonds() + " ♦");

        String leaderName = Bukkit.getOfflinePlayer(clan.getLeaderId()).getName();
        lore.add("§7Лидер: §6" + (leaderName != null ? leaderName : "Неизвестно"));
        lore.add("");

        if (clan.getMemberCount() < clan.getMaxMembers()) {
            lore.add("§eНажмите для информации");
        } else {
            lore.add("§cКлан заполнен");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }

        item.setItemMeta(meta);
        return item;
    }
}

enum GUIPage {
    MAIN,
    CLAN_MENU,
    MEMBERS,
    BANK,
    UPGRADES,
    BROWSER
}
