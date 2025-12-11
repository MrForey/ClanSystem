package net.mrforey;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum ClanRank {

    RECRUIT("Новичок", "§7", 0, new HashSet<>(Arrays.asList(
            ClanPermission.CHAT,
            ClanPermission.HOME
    ))),

    MEMBER("Участник", "§f", 1, new HashSet<>(Arrays.asList(
            ClanPermission.CHAT,
            ClanPermission.HOME,
            ClanPermission.INVITE,
            ClanPermission.CLAIM
    ))),

    ELDER("Старейшина", "§6", 2, new HashSet<>(Arrays.asList(
            ClanPermission.CHAT,
            ClanPermission.HOME,
            ClanPermission.INVITE,
            ClanPermission.CLAIM,
            ClanPermission.KICK_RECRUIT,
            ClanPermission.SET_WARP
    ))),

    OFFICER("Офицер", "§9", 3, new HashSet<>(Arrays.asList(
            ClanPermission.CHAT,
            ClanPermission.HOME,
            ClanPermission.INVITE,
            ClanPermission.CLAIM,
            ClanPermission.KICK_RECRUIT,
            ClanPermission.KICK_MEMBER,
            ClanPermission.PROMOTE_RECRUIT,
            ClanPermission.DEMOTE_MEMBER,
            ClanPermission.SET_WARP,
            ClanPermission.MANAGE_UPGRADES
    ))),

    COLEADER("Заместитель", "§c", 4, new HashSet<>(Arrays.asList(
            ClanPermission.CHAT,
            ClanPermission.HOME,
            ClanPermission.INVITE,
            ClanPermission.CLAIM,
            ClanPermission.KICK_RECRUIT,
            ClanPermission.KICK_MEMBER,
            ClanPermission.KICK_ELDER,
            ClanPermission.PROMOTE_RECRUIT,
            ClanPermission.PROMOTE_MEMBER,
            ClanPermission.DEMOTE_MEMBER,
            ClanPermission.DEMOTE_ELDER,
            ClanPermission.SET_WARP,
            ClanPermission.MANAGE_UPGRADES,
            ClanPermission.MANAGE_BANK,
            ClanPermission.MANAGE_SETTINGS
    ))),

    LEADER("Лидер", "§4", 5, new HashSet<>(Arrays.asList(
            ClanPermission.values()
    )));

    private final String displayName;
    private final String color;
    private final int power;
    private final Set<ClanPermission> permissions;

    ClanRank(String displayName, String color, int power, Set<ClanPermission> permissions) {
        this.displayName = displayName;
        this.color = color;
        this.power = power;
        this.permissions = permissions;
    }

    public String getDisplayName() {
        return color + displayName;
    }

    public String getColor() {
        return color;
    }

    public int getPower() {
        return power;
    }

    public boolean hasPermission(ClanPermission permission) {
        return permissions.contains(permission);
    }

    public ClanRank getNextRank() {
        if (this.ordinal() < values().length - 1) {
            return values()[this.ordinal() + 1];
        }
        return null;
    }

    public ClanRank getPreviousRank() {
        if (this.ordinal() > 0) {
            return values()[this.ordinal() - 1];
        }
        return null;
    }

    public static ClanRank fromString(String str) {
        try {
            return ClanRank.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RECRUIT;
        }
    }

}
