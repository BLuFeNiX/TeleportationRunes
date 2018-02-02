package net.blufenix.teleportationrunes;

import org.bukkit.Location;

public class Teleporter {
    public final Location loc;
    public final int rotation;
    public final Signature sig;

    Teleporter(Location loc, int rotation) {
        this.loc = loc;
        this.rotation = rotation;
        this.sig = Signature.fromLocation(loc, Config.teleporterBlueprint.atRotation(rotation));
    }
}
