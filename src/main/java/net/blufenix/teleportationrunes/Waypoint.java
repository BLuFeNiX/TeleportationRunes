package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import org.bukkit.Location;

import java.util.Arrays;

/**
 * Created by blufenix on 7/29/15.
 */
public class Waypoint {

    public static final int NOT_EXISTS = 0;
    public static final int EXISTS_MODIFIED = 1;
    public static final int EXISTS_VERIFIED = 2;
    public static final int EXISTS_MODIFIED_CONFLICT = 3;
    public static final int NOT_EXISTS_CONFLICT = 4;

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

        Signature sigAtLoc = Signature.fromLocation(loc, Config.waypointBlueprint.atRotation(rotation));
        Waypoint waypointFromDBLoc = db.getWaypointFromLocation(loc);
        Waypoint ret = new Waypoint(loc, sigAtLoc);

        if (waypointFromDBLoc != null) {
            // we know the location matches, so if the signature matches as well, we are verified
            if (waypointFromDBLoc.sig.equals(sigAtLoc)) {
                ret.status = EXISTS_VERIFIED;
            } else if (db.getWaypointFromSignature(sigAtLoc) != null) {
                // another waypoint exists in the DB with the same signature
                ret.status = EXISTS_MODIFIED_CONFLICT;
            } else {
                ret.status = EXISTS_MODIFIED;
            }
        } else {
            // no waypoint has been registered at this location,
            // but we need to see if the desired signature is available
            if (db.getWaypointFromSignature(sigAtLoc) == null) {
                // signature not used
                ret.status = NOT_EXISTS;
            } else {
                ret.status = NOT_EXISTS_CONFLICT;
            }
        }

        return ret;
    }
}
