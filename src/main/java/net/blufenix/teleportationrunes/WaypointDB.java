package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import net.blufenix.common.SimpleDatabase;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by blufenix on 7/27/15.
 */
public class WaypointDB extends SimpleDatabase {

    private static final String FILENAME = "waypoints.db";

    private static final String WAYPOINT_TABLE = "waypoints_v2";
    private static final String WAYPOINT_TABLE_LEGACY = "waypoints";

    WaypointDB() {
        super(FILENAME);
        createTables();
    }

    private void createTables() {
        execute("CREATE TABLE IF NOT EXISTS waypoints_v2 " +
                "(world TEXT NOT NULL, " +
                "x INTEGER NOT NULL, " +
                "y INTEGER NOT NULL, " +
                "z INTEGER NOT NULL, " +
                "north STRING NOT NULL, " +
                "south STRING NOT NULL, " +
                "east STRING NOT NULL, " +
                "west STRING NOT NULL)"
        );
    }

    public boolean addWaypoint(Waypoint waypoint) {
        String sql = String.format(
                "INSERT INTO %s VALUES ('%s', %d, %d, %d, '%s', '%s', '%s', '%s')",
                WAYPOINT_TABLE,
                waypoint.loc.getWorld().getName(),
                (int)waypoint.loc.getX(), (int)waypoint.loc.getY(), (int)waypoint.loc.getZ(),
                waypoint.sig.north, waypoint.sig.south, waypoint.sig.east, waypoint.sig.west);
        boolean success = execute(sql);
        if (success) waypoint.status = Waypoint.EXISTS_VERIFIED;
        return success;
    }

    public boolean removeWaypointBySignature(Signature sig) {
        String sql = String.format(
                "DELETE FROM %s WHERE north = '%s' AND south = '%s' AND east = '%s' AND west = '%s'",
                WAYPOINT_TABLE,
                sig.north, sig.south, sig.east, sig.west);
        return execute(sql);
    }

    public boolean removeWaypointByLocation(Location loc) {
        String sql = String.format(
                "DELETE FROM %s WHERE world = '%s' AND x = %d AND y = %d AND z = %d",
                WAYPOINT_TABLE,
                loc.getWorld().getName(), (int)loc.getX(), (int)loc.getY(), (int)loc.getZ());
        return execute(sql);
    }

    public Waypoint getWaypointFromSignature(Signature sig) {
        String sql = String.format(
                "SELECT * FROM %s WHERE north = '%s' AND south = '%s' AND east = '%s' AND west = '%s'",
                WAYPOINT_TABLE,
                sig.north, sig.south, sig.east, sig.west);
        // make sure we close the Statement as well
        try (ResultSet rs = query(sql); Statement stmt = rs.getStatement()){
            if (rs.next()) {
                String worldName = rs.getString(1);
                int x = rs.getInt(2);
                int y = rs.getInt(3);
                int z = rs.getInt(4);

                World world = TeleportationRunes.getInstance().getServer().getWorld(worldName);
                Location loc = new Location(world, x, y, z);

                // reuse input signature, since all of its members are final
                return new Waypoint(loc, sig);
            }
        } catch (SQLException e) {
            Log.e("query error", e);
        }

        return null;
    }

    public Waypoint getWaypointFromLocation(Location loc) {
        String sql = String.format(
                "SELECT north, south, east, west FROM %s WHERE world = '%s' AND x = %d AND y = %d AND z = %d",
                WAYPOINT_TABLE,
                loc.getWorld().getName(), (int)loc.getX(), (int)loc.getY(), (int)loc.getZ());

        // make sure we close the Statement as well
        try (ResultSet rs = query(sql); Statement stmt = rs.getStatement()){
            if (rs.next()) {
                String n = rs.getString(1);
                String s = rs.getString(2);
                String e = rs.getString(3);
                String w = rs.getString(4);

                return new Waypoint(loc.clone(), new Signature(n, s, e, w));
            }
        } catch (SQLException e) {
            Log.e("query error", e);
        }

        return null;
    }

    // TODO remove after a while
    public List<Location> getLegacyWaypointLocations() {
        List<Location> legacyLocations = new ArrayList<>();

        // make sure we close the Statement as well
        try (ResultSet rs = query(String.format("SELECT world, x, y, z FROM %s", WAYPOINT_TABLE_LEGACY)); Statement stmt = rs.getStatement()) {
            while (rs.next()) {
                String worldName = rs.getString(1);
                int x = rs.getInt(2);
                int y = rs.getInt(3);
                int z = rs.getInt(4);

                World world = TeleportationRunes.getInstance().getServer().getWorld(worldName);
                Location loc = new Location(world, x, y, z);

                legacyLocations.add(loc);
            }
        } catch (SQLException e) {
            Log.e("query error", e);
        }

        return legacyLocations;
    }

}
