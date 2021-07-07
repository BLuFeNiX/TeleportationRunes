package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import net.blufenix.common.SimpleDatabase;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * For now, all query syntax must be compatible with both SQLite and HSQLDB in PostgreSQL mode!
 */
public class WaypointDB extends SimpleDatabase {

    private static final String DEFAULT_FILENAME = "waypoints.db";
    private static final String WAYPOINT_TABLE = "waypoints_v2";

    public WaypointDB(Backend backend) {
        this(backend, DEFAULT_FILENAME);
    }

    public WaypointDB(Backend backend, String filename) {
        super(backend, filename);
        createTables();
    }

    private void createTables() {
        try {
            execute("CREATE TABLE IF NOT EXISTS " + WAYPOINT_TABLE +
                    " (world TEXT NOT NULL, " +
                    "x INTEGER NOT NULL, " +
                    "y INTEGER NOT NULL, " +
                    "z INTEGER NOT NULL, " +
                    "north TEXT NOT NULL, " +
                    "south TEXT NOT NULL, " +
                    "east TEXT NOT NULL, " +
                    "west TEXT NOT NULL)"
            );
        } catch (SQLException e) {
            Log.e("query error", e);
        }
    }

    public boolean addWaypoint(Waypoint waypoint) {
        String sql = String.format(
                "INSERT INTO %s VALUES ('%s', %d, %d, %d, '%s', '%s', '%s', '%s')",
                WAYPOINT_TABLE,
                waypoint.loc.getWorld().getName(),
                (int)waypoint.loc.getX(), (int)waypoint.loc.getY(), (int)waypoint.loc.getZ(),
                waypoint.sig.north, waypoint.sig.south, waypoint.sig.east, waypoint.sig.west);
        boolean success = false;
        try {
            success = execute(sql);
        } catch (SQLException e) {
            Log.e("query error", e);
        }
        if (success) waypoint.status = Waypoint.EXISTS_VERIFIED;
        return success;
    }

    public boolean removeWaypointBySignature(Signature sig) {
        String sql = String.format(
                "DELETE FROM %s WHERE north = '%s' AND south = '%s' AND east = '%s' AND west = '%s'",
                WAYPOINT_TABLE,
                sig.north, sig.south, sig.east, sig.west);
        try {
            return execute(sql);
        } catch (SQLException e) {
            Log.e("query error", e);
        }
        return false;
    }

    public boolean removeWaypointByLocation(Location loc) {
        String sql = String.format(
                "DELETE FROM %s WHERE world = '%s' AND x = %d AND y = %d AND z = %d",
                WAYPOINT_TABLE,
                loc.getWorld().getName(), (int)loc.getX(), (int)loc.getY(), (int)loc.getZ());
        try {
            return execute(sql);
        } catch (SQLException e) {
            Log.e("query error", e);
        }
        return false;
    }

    public Waypoint getWaypointFromSignature(final Signature sig) {
        String sql = String.format(
                "SELECT * FROM %s WHERE north = '%s' AND south = '%s' AND east = '%s' AND west = '%s'",
                WAYPOINT_TABLE,
                sig.north, sig.south, sig.east, sig.west);

        try {
            return (Waypoint) query(sql, new QueryHandler<Waypoint>() {
                @Override
                protected Waypoint handle(ResultSet rs) throws SQLException {
                    Waypoint waypoint = null;
                    if (rs.next()) {
                        String worldName = rs.getString(1);
                        int x = rs.getInt(2);
                        int y = rs.getInt(3);
                        int z = rs.getInt(4);

                        World world = TeleportationRunes.getInstance().getServer().getWorld(worldName);
                        Location loc = new Location(world, x, y, z);

                        // reuse input signature, since all of its members are final
                        waypoint = new Waypoint(loc, sig);
                    }
                    return waypoint;
                }
            });
        } catch (SQLException e) {
            Log.e("query error", e);
        }

        return null;
    }

    public Waypoint getWaypointFromLocation(final Location loc) {
        String sql = String.format(
                "SELECT north, south, east, west FROM %s WHERE world = '%s' AND x = %d AND y = %d AND z = %d",
                WAYPOINT_TABLE,
                loc.getWorld().getName(), (int)loc.getX(), (int)loc.getY(), (int)loc.getZ());

        try {
            return (Waypoint) query(sql, new QueryHandler<Waypoint>() {
                @Override
                protected Waypoint handle(ResultSet rs) throws SQLException {
                    Waypoint waypoint = null;
                    if (rs.next()) {
                        String n = rs.getString(1);
                        String s = rs.getString(2);
                        String e = rs.getString(3);
                        String w = rs.getString(4);

                        waypoint = new Waypoint(loc.clone(), new Signature(n, s, e, w));
                    }
                    return waypoint;
                }
            });
        } catch (SQLException e) {
            Log.e("query error", e);
        }

        return null;
    }

}
