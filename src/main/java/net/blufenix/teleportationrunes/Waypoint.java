package net.blufenix.teleportationrunes;

import org.bukkit.Location;

/**
 * Created by blufenix on 7/29/15.
 */
public class Waypoint {
    
    public final String user;
    public final Location loc;
    public final Signature sig;

    public Waypoint(String user, Location loc, Signature sig) {
        this.user = user;
        this.loc = loc;
        this.sig = sig;
    }
}
