package net.mrforey;

import java.util.Date;
import java.util.UUID;

public class ClanMember {

    private UUID playerId;
    private ClanRank rank;
    private Date joinDate;
    private int contributedDiamonds;
    private Date lastSeen;

    public ClanMember(UUID playerId, ClanRank rank, Date joinDate) {
        this.playerId = playerId;
        this.rank = rank;
        this.joinDate = joinDate;
        this.contributedDiamonds = 0;
        this.lastSeen = new Date();
    }

    public void promote() {
        if (rank.ordinal() < ClanRank.LEADER.ordinal()) {
            rank = ClanRank.values()[rank.ordinal() + 1];
        }
    }

    public void demote() {
        if (rank.ordinal() > ClanRank.RECRUIT.ordinal()) {
            rank = ClanRank.values()[rank.ordinal() - 1];
        }
    }

    public boolean hasPermission(ClanPermission permission) {
        return rank.hasPermission(permission);
    }

    public void addContribution(int diamonds) {
        contributedDiamonds += diamonds;
    }

    public UUID getPlayerId() { return playerId; }
    public ClanRank getRank() { return rank; }
    public void setRank(ClanRank rank) { this.rank = rank; }
    public Date getJoinDate() { return joinDate; }
    public int getContributedDiamonds() { return contributedDiamonds; }
    public void setContributedDiamonds(int diamonds) { this.contributedDiamonds = diamonds; }
    public Date getLastSeen() { return lastSeen; }
    public void updateLastSeen() { this.lastSeen = new Date(); }

}
