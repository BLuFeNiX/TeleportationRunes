package net.blufenix.teleportationrunes;

import org.bukkit.Location;

/**
 * Created by blufenix on 7/29/15.
 */
public class Waypoint {

    public final Location loc;
    public final Signature sig;

    Waypoint(final Location loc, final Signature sig) {
        this.loc = loc;
        this.sig = sig;
    }
}
