package net.blufenix.teleportationrunes;

import org.bukkit.Location;

/**
 * Created by blufenix on 7/29/15.
 */
public class Waypoint {

    public static final int NOT_EXISTS = 0;
    public static final int EXISTS_MODIFIED = 1;
    public static final int EXISTS_VERIFIED = 2;
    public static final int EXISTS_CONFLICT = 3;

    public final Location loc;
    public final Signature sig;

    public int status = NOT_EXISTS;

    Waypoint(final Location loc, final Signature sig) {
        this.loc = loc;
        this.sig = sig;
    }

    Waypoint(final Location loc, final Signature sig, int status) {
        this(loc, sig);
        this.status = status;
    }

    public static Waypoint fromLocation(Location loc) {
        if (loc == null) return null;

        loc = loc.clone();

        int rotation = BlockUtil.isWaypoint(loc);
        if (rotation == -1) {
            // try again one block lower
            rotation = BlockUtil.isWaypoint(loc.add(Vectors.DOWN));
        }

        if (rotation == -1) return null;

        WaypointDB db = TeleportationRunes.getInstance().getWaypointDB();

        Signature expectedSignature = Signature.fromLocation(loc, Config.waypointBlueprint.atRotation(rotation));
        Waypoint waypoint = db.getWaypointFromLocation(loc);

        if (waypoint != null) {
            // we know the location matches, so if the signature matches as well, we are verified
            if (waypoint.sig.equals(expectedSignature)) {
                waypoint.status = EXISTS_VERIFIED;
            } else if (!db.getWaypointFromSignature(waypoint.sig).loc.equals(waypoint.loc)) {
                // another waypoint exists in the DB with the same signature
                waypoint.status = EXISTS_CONFLICT;
            } else {
                waypoint = new Waypoint(waypoint.loc, expectedSignature, EXISTS_MODIFIED);
            }
        } else {
            waypoint = new Waypoint(loc, expectedSignature, NOT_EXISTS);
        }

        return waypoint;
    }
}
