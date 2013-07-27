package net.blufenix.teleportationrunes;

import java.io.Serializable;

import org.bukkit.Location;
import org.bukkit.Server;

public class LocationWrapper implements Serializable {

	private static final long serialVersionUID = 3605072683155274382L;
	
	String world;
	double x;
	double y;
	double z;
	
	LocationWrapper(String world, double x, double y, double z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public LocationWrapper(Location location) {
		this.world = location.getWorld().getName();
		this.x = location.getX();
		this.y = location.getY();
		this.z = location.getZ();
	}

	Location getLoc() {
		Server server = TeleportationRunes.getInstance().getServer();
		return new Location(server.getWorld("world"), x, y, z);
	}
	
	String getWorldName() {
		return world;
	}
	
}
