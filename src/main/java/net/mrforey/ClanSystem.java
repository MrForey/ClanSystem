package net.mrforey;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;


public class ClanSystem extends JavaPlugin {
    private static ClanSystem instance;
    private ClanManager clanManager;
    private ClanGUI clanGUI;
    private ClanDataManager dataManager;
    private FileConfiguration messages;
    private File messagesFile;

    private HashMap<String, Clan> clans = new HashMap<>();
    private HashMap<UUID, String> playerClans = new HashMap<>();
    private HashMap<UUID, ClanRank> playerRanks = new HashMap<>();

    private File configFile;
    private File clansFile;

    private FileConfiguration config;
    private FileConfiguration clansConfig;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        instance = this;

        createFiles();

        saveDefaultConfig();

        setupMessages();

        dataManager = new ClanDataManager(this);
        clanManager = new ClanManager(this);
        clanGUI = new ClanGUI(this);

        getCommand("clan").setExecutor(new ClanCommands(this));
        getCommand("clan").setTabCompleter(new ClanTabCompleter());

        Bukkit.getPluginManager().registerEvents(new ClanEvents(this), this);
        Bukkit.getPluginManager().registerEvents(clanGUI, this);

        loadClans();

        new BukkitRunnable() {
            @Override
            public void run() {
                saveClans();
                getLogger().info("Данные кланов автосохранены");
            }
        }.runTaskTimer(this, 6000L, 6000L); // 5 минут

        getLogger().log(Level.INFO, "§aСистема кланов успешно загружена!");
    }

    @Override
    public void onDisable() {
        saveClans();
        getLogger().log(Level.INFO, "§cСистема кланов выгружена!");
    }

    private void createFiles() {
        saveDefaultConfig();
        config = getConfig();

        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        clansFile = new File(getDataFolder(), "clans.yml");
        if (!clansFile.exists()) {
            try {
                clansFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        clansConfig = YamlConfiguration.loadConfiguration(clansFile);
    }

    private void loadClans() {
        if (clansConfig.contains("clans")) {
            for (String clanId : clansConfig.getConfigurationSection("clans").getKeys(false)) {
                Clan clan = Clan.loadFromConfig(clansConfig, "clans." + clanId);
                if (clan != null) {
                    clans.put(clanId, clan);

                    for (ClanMember member : clan.getMembers().values()) {
                        playerClans.put(member.getPlayerId(), clanId);
                        playerRanks.put(member.getPlayerId(), member.getRank());
                    }
                }
            }
        }
        getLogger().log(Level.INFO, "Загружено " + clans.size() + " кланов");
    }

    public void saveClans() {
        clansConfig.set("clans", null);

        for (Map.Entry<String, Clan> entry : clans.entrySet()) {
            entry.getValue().saveToConfig(clansConfig, "clans." + entry.getKey());
        }

        try {
            clansConfig.save(clansFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean hasEnoughDiamonds(Player player, int amount) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                total += item.getAmount();
            }
        }
        return total >= amount;
    }

    public static boolean removeDiamonds(Player player, int amount) {
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

    public static void giveDiamonds(Player player, int amount) {
        ItemStack diamonds = new ItemStack(Material.DIAMOND, amount);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(diamonds);

        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
        player.updateInventory();
    }

    public static ClanSystem getInstance() {
        return instance;
    }

    public HashMap<String, Clan> getClans() {
        return clans;
    }

    public HashMap<UUID, String> getPlayerClans() {
        return playerClans;
    }

    public HashMap<UUID, ClanRank> getPlayerRanks() {
        return playerRanks;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public ClanGUI getClanGUI() {
        return clanGUI;
    }

    public ClanDataManager getDataManager() {
        return dataManager;
    }

    public FileConfiguration getClansConfig() {
        return clansConfig;
    }

    private void setupMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
            getLogger().info("Файл messages.yml создан из ресурсов");
        }

        reloadMessages();
    }

    public void reloadMessages() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String key, String defaultValue) {
        String message = messages.getString(key, defaultValue);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
