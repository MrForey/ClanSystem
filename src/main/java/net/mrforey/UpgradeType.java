package net.mrforey;

import org.bukkit.Material;

public enum UpgradeType {

    MEMBER_SLOT("Дополнительные слоты", Material.EMERALD,
            "Увеличивает максимальное количество участников", 10, 0.05, 100),

    EXP_BOOST("Бонус опыта", Material.EXPERIENCE_BOTTLE,
            "Увеличивает получаемый опыт", 10, 0.1, 80),

    DAMAGE_BOOST("Усиление урона", Material.DIAMOND_SWORD,
            "Увеличивает урон участников", 5, 0.05, 120),

    DEFENSE_BOOST("Защита", Material.SHIELD,
            "Уменьшает получаемый урон", 5, 0.05, 120),

    CLAIM_AMOUNT("Дополнительные территории", Material.GRASS_BLOCK,
            "Увеличивает количество территорий", 5, 1, 200),

    CLAIM_SIZE("Размер территории", Material.MAP,
            "Увеличивает размер территории", 5, 1, 150);

    private final String displayName;
    private final Material icon;
    private final String description;
    private final int maxLevel;
    private final double bonusPerLevel;
    private final int baseCost;

    UpgradeType(String displayName, Material icon, String description,
                int maxLevel, double bonusPerLevel, int baseCost) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.maxLevel = maxLevel;
        this.bonusPerLevel = bonusPerLevel;
        this.baseCost = baseCost;
    }

    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public String getDescription() { return description; }
    public int getMaxLevel() { return maxLevel; }
    public double getBonusPerLevel() { return bonusPerLevel; }
    public int getBaseCost() { return baseCost; }

    public String getColor() {
        switch (this) {
            case MEMBER_SLOT: return "§a";
            case EXP_BOOST: return "§e";
            case DAMAGE_BOOST: return "§c";
            case DEFENSE_BOOST: return "§9";
            case CLAIM_AMOUNT: return "§2";
            case CLAIM_SIZE: return "§6";
            default: return "§f";
        }
    }

}
