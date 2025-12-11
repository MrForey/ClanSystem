package net.mrforey;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class ClanHome {

    private String worldName;
    private double x, y, z;
    private float yaw, pitch;
    private String setBy;
    private long setTime;

    public ClanHome(Location location, String setBy) {
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.setBy = setBy;
        this.setTime = System.currentTimeMillis();
    }

    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getSetBy() { return setBy; }
    public long getSetTime() { return setTime; }

}
