package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportRequest {

    Player player;
    Location destination;

    public TeleportRequest(Player player, Location destination) {
        this.player = player;
        this.destination = destination.clone();
    }

    public void execute() {

    }

}
