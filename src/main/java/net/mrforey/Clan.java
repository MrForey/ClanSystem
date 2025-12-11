package net.mrforey;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class Clan {

    private String id;
    private String name;
    private String tag;
    private String description;
    private UUID leaderId;
    private Date creationDate;

    private int diamonds;
    private int experience;
    private int level;

    private HashMap<UUID, ClanMember> members = new HashMap<>();
    private HashMap<UUID, ClanApplication> applications = new HashMap<>();

    private HashMap<UpgradeType, Integer> upgrades = new HashMap<>();

    private int kills;
    private int deaths;
    private int mobKills;
    private int blocksMined;
    private int blocksPlaced;

    private ClanHome home;
    private ArrayList<ClanClaim> claims = new ArrayList<>();
    private HashSet<String> allies = new HashSet<>();
    private HashSet<String> enemies = new HashSet<>();

    public Clan(String id, String name, String tag, UUID leaderId) {
        this.id = id;
        this.name = name;
        this.tag = tag.toUpperCase();
        this.description = "Новый клан";
        this.leaderId = leaderId;
        this.creationDate = new Date();
        this.diamonds = 0;
        this.experience = 0;
        this.level = 1;

        ClanMember leader = new ClanMember(leaderId, ClanRank.LEADER, new Date());
        members.put(leaderId, leader);

        for (UpgradeType type : UpgradeType.values()) {
            upgrades.put(type, 0);
        }
    }

    // =============== Участники ===============
    public void addMember(UUID playerId, ClanRank rank) {
        ClanMember member = new ClanMember(playerId, rank, new Date());
        members.put(playerId, member);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public ClanMember getMember(UUID playerId) {
        return members.get(playerId);
    }

    public boolean hasMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public ClanRank getMemberRank(UUID playerId) {
        ClanMember member = members.get(playerId);
        return member != null ? member.getRank() : null;
    }

    public void setMemberRank(UUID playerId, ClanRank rank) {
        ClanMember member = members.get(playerId);
        if (member != null) {
            member.setRank(rank);
        }
    }

    public int getMemberCount() {
        return members.size();
    }

    public int getOnlineMemberCount() {
        int count = 0;
        for (UUID playerId : members.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                count++;
            }
        }
        return count;
    }

    public List<ClanMember> getOnlineMembers() {
        return members.values().stream()
                .filter(m -> {
                    Player player = Bukkit.getPlayer(m.getPlayerId());
                    return player != null && player.isOnline();
                })
                .collect(Collectors.toList());
    }

    // =============== Заявки ===============
    public void addApplication(UUID playerId, String message) {
        ClanApplication application = new ClanApplication(playerId, message, new Date());
        applications.put(playerId, application);
    }

    public void removeApplication(UUID playerId) {
        applications.remove(playerId);
    }

    public boolean hasApplication(UUID playerId) {
        return applications.containsKey(playerId);
    }

    public int getApplicationCount() {
        return applications.size();
    }

    // =============== Ресурсы ===============
    public void addDiamonds(int amount) {
        diamonds += amount;
    }

    public boolean removeDiamonds(int amount) {
        if (diamonds >= amount) {
            diamonds -= amount;
            return true;
        }
        return false;
    }

    public void addExperience(int amount) {
        experience += amount;
        checkLevelUp();
    }

    private void checkLevelUp() {
        int required = getRequiredExperience();
        while (experience >= required && level < getMaxLevel()) {
            experience -= required;
            level++;
            required = getRequiredExperience();

            broadcastMessage("&aКлан повысил уровень! Теперь уровень " + level);
        }
    }

    public int getRequiredExperience() {
        return 1000 * level;
    }

    public int getMaxLevel() {
        return 50;
    }

    // =============== Улучшения ===============
    public void upgrade(UpgradeType type) {
        int currentLevel = upgrades.getOrDefault(type, 0);
        upgrades.put(type, currentLevel + 1);
    }

    public int getUpgradeLevel(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public int getUpgradeCost(UpgradeType type) {
        int currentLevel = getUpgradeLevel(type);
        return type.getBaseCost() * (int) Math.pow(2, currentLevel);
    }

    // =============== Территории ===============
    public boolean canCreateClaim() {
        int maxClaims = 3 + getUpgradeLevel(UpgradeType.CLAIM_AMOUNT);
        return claims.size() < maxClaims;
    }

    public void addClaim(ClanClaim claim) {
        claims.add(claim);
    }

    public void removeClaim(ClanClaim claim) {
        claims.remove(claim);
    }

    public boolean hasClaimAt(Location location) {
        for (ClanClaim claim : claims) {
            if (claim.contains(location)) {
                return true;
            }
        }
        return false;
    }

    // =============== Дом ===============
    public void setHome(Location location, String setBy) {
        this.home = new ClanHome(location, setBy);
    }

    public Location getHomeLocation() {
        return home != null ? home.getLocation() : null;
    }

    // =============== Сообщения ===============
    public void broadcastMessage(String message) {
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        for (ClanMember member : members.values()) {
            Player player = Bukkit.getPlayer(member.getPlayerId());
            if (player != null && player.isOnline()) {
                player.sendMessage(formatted);
            }
        }
    }

    // =============== Сериализация ===============
    public void saveToConfig(FileConfiguration config, String path) {
        config.set(path + ".name", name);
        config.set(path + ".tag", tag);
        config.set(path + ".description", description);
        config.set(path + ".leader", leaderId.toString());
        config.set(path + ".creationDate", creationDate.getTime());
        config.set(path + ".diamonds", diamonds);
        config.set(path + ".experience", experience);
        config.set(path + ".level", level);

        for (Map.Entry<UUID, ClanMember> entry : members.entrySet()) {
            String memberPath = path + ".members." + entry.getKey().toString();
            ClanMember member = entry.getValue();
            config.set(memberPath + ".rank", member.getRank().name());
            config.set(memberPath + ".joinDate", member.getJoinDate().getTime());
            config.set(memberPath + ".contributedDiamonds", member.getContributedDiamonds());
        }

        for (Map.Entry<UpgradeType, Integer> entry : upgrades.entrySet()) {
            config.set(path + ".upgrades." + entry.getKey().name(), entry.getValue());
        }

        config.set(path + ".stats.kills", kills);
        config.set(path + ".stats.deaths", deaths);
        config.set(path + ".stats.mobKills", mobKills);
        config.set(path + ".stats.blocksMined", blocksMined);
        config.set(path + ".stats.blocksPlaced", blocksPlaced);
    }

    public static Clan loadFromConfig(FileConfiguration config, String path) {
        try {
            String name = config.getString(path + ".name");
            String tag = config.getString(path + ".tag");
            String leaderIdStr = config.getString(path + ".leader");

            if (name == null || tag == null || leaderIdStr == null) {
                return null;
            }

            UUID leaderId = UUID.fromString(leaderIdStr);
            Clan clan = new Clan(path.substring(path.lastIndexOf(".") + 1), name, tag, leaderId);

            clan.description = config.getString(path + ".description", "Новый клан");
            clan.creationDate = new Date(config.getLong(path + ".creationDate", System.currentTimeMillis()));
            clan.diamonds = config.getInt(path + ".diamonds", 0);
            clan.experience = config.getInt(path + ".experience", 0);
            clan.level = config.getInt(path + ".level", 1);

            if (config.contains(path + ".members")) {
                for (String memberIdStr : config.getConfigurationSection(path + ".members").getKeys(false)) {
                    UUID memberId = UUID.fromString(memberIdStr);
                    ClanRank rank = ClanRank.valueOf(config.getString(path + ".members." + memberIdStr + ".rank"));
                    Date joinDate = new Date(config.getLong(path + ".members." + memberIdStr + ".joinDate"));
                    int contributed = config.getInt(path + ".members." + memberIdStr + ".contributedDiamonds", 0);

                    ClanMember member = new ClanMember(memberId, rank, joinDate);
                    member.setContributedDiamonds(contributed);
                    clan.members.put(memberId, member);
                }
            }

            if (config.contains(path + ".upgrades")) {
                for (String upgradeStr : config.getConfigurationSection(path + ".upgrades").getKeys(false)) {
                    UpgradeType type = UpgradeType.valueOf(upgradeStr);
                    int level = config.getInt(path + ".upgrades." + upgradeStr);
                    clan.upgrades.put(type, level);
                }
            }
            clan.kills = config.getInt(path + ".stats.kills", 0);
            clan.deaths = config.getInt(path + ".stats.deaths", 0);
            clan.mobKills = config.getInt(path + ".stats.mobKills", 0);
            clan.blocksMined = config.getInt(path + ".stats.blocksMined", 0);
            clan.blocksPlaced = config.getInt(path + ".stats.blocksPlaced", 0);

            return clan;
        } catch (Exception e) {
            ClanSystem.getInstance().getLogger().warning("Ошибка загрузки клана: " + e.getMessage());
            return null;
        }
    }

    // =============== Геттеры ===============
    public String getId() { return id; }
    public String getName() { return name; }
    public String getTag() { return tag; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getLeaderId() { return leaderId; }
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }
    public Date getCreationDate() { return creationDate; }
    public int getDiamonds() { return diamonds; }
    public int getExperience() { return experience; }
    public int getLevel() { return level; }
    public HashMap<UUID, ClanMember> getMembers() { return new HashMap<>(members); }
    public HashMap<UUID, ClanApplication> getApplications() { return new HashMap<>(applications); }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public double getKDR() { return deaths == 0 ? kills : (double) kills / deaths; }
    public ClanHome getHome() { return home; }
    public ArrayList<ClanClaim> getClaims() { return new ArrayList<>(claims); }

    public int getMaxMembers() {
        return 10 + (getUpgradeLevel(UpgradeType.MEMBER_SLOT) * 5);
    }

    public String getFormattedInfo() {
        return ChatColor.GOLD + "=== " + name + " [" + tag + "] ===\n" +
                ChatColor.YELLOW + "Лидер: " + ChatColor.WHITE + getLeaderName() + "\n" +
                ChatColor.YELLOW + "Уровень: " + ChatColor.WHITE + level + "\n" +
                ChatColor.YELLOW + "Опыт: " + ChatColor.WHITE + experience + "/" + getRequiredExperience() + "\n" +
                ChatColor.YELLOW + "Алмазы: " + ChatColor.AQUA + diamonds + " ♦\n" +
                ChatColor.YELLOW + "Участников: " + ChatColor.WHITE + members.size() + "/" + getMaxMembers() + "\n" +
                ChatColor.YELLOW + "Территорий: " + ChatColor.WHITE + claims.size() + "/" + (3 + getUpgradeLevel(UpgradeType.CLAIM_AMOUNT)) + "\n" +
                ChatColor.YELLOW + "K/D: " + ChatColor.WHITE + String.format("%.2f", getKDR()) + "\n" +
                ChatColor.YELLOW + "Описание: " + ChatColor.WHITE + description;
    }



    private String getLeaderName() {
        OfflinePlayer leader = Bukkit.getOfflinePlayer(leaderId);
        return leader.getName() != null ? leader.getName() : "Неизвестно";
    }

}
