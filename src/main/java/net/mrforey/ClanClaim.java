package net.mrforey;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClanClaim {

    private String clanId;
    private Chunk chunk;
    private Set<UUID> trustedPlayers;

    public ClanClaim(String clanId, Chunk chunk) {
        this.clanId = clanId;
        this.chunk = chunk;
        this.trustedPlayers = new HashSet<>();
    }

    public boolean contains(Location location) {
        return location.getChunk().equals(chunk);
    }

    public void addTrustedPlayer(UUID playerId) {
        trustedPlayers.add(playerId);
    }

    public void removeTrustedPlayer(UUID playerId) {
        trustedPlayers.remove(playerId);
    }

    public boolean isTrusted(UUID playerId) {
        return trustedPlayers.contains(playerId);
    }

    public String getClanId() { return clanId; }
    public Chunk getChunk() { return chunk; }
    public Set<UUID> getTrustedPlayers() { return new HashSet<>(trustedPlayers); }

}
