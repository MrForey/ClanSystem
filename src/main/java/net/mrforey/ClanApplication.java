package net.mrforey;

import java.util.Date;
import java.util.UUID;

public class ClanApplication {

    private UUID playerId;
    private String message;
    private Date applyDate;

    public ClanApplication(UUID playerId, String message, Date applyDate) {
        this.playerId = playerId;
        this.message = message;
        this.applyDate = applyDate;
    }

    public UUID getPlayerId() { return playerId; }
    public String getMessage() { return message; }
    public Date getApplyDate() { return applyDate; }

}
