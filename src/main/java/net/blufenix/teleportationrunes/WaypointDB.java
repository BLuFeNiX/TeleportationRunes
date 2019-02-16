package net.blufenix.teleportationrunes;

import net.blufenix.common.SimpleDatabase;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.ResultSet;
import java.sql.SQLException;
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
        return execute(sql);
    }

    public boolean removeWaypoint(Waypoint waypoint) {
        String sql = String.format(
                "DELETE FROM %s WHERE north = '%s' AND south = '%s' AND east = '%s' AND west = '%s'",
                WAYPOINT_TABLE,
                waypoint.sig.north, waypoint.sig.south, waypoint.sig.east, waypoint.sig.west);
        return execute(sql);
    }

    public Waypoint getWaypointFromSignature(Signature sig) {
        String sql = String.format(
                "SELECT * FROM %s WHERE north = '%s' AND south = '%s' AND east = '%s' AND west = '%s'",
                WAYPOINT_TABLE,
                sig.north, sig.south, sig.east, sig.west);
        ResultSet rs = query(sql);
        try {
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
        } catch (SQLException e) { e.printStackTrace(); }

        return null;
    }

    // TODO remove after a while
    public List<Location> getLegacyWaypointLocations() {
        List<Location> legacyLocations = new ArrayList<>();
        ResultSet rs = query(String.format("SELECT world, x, y, z FROM %s", WAYPOINT_TABLE_LEGACY));
        try {
            while (rs.next()) {
                String worldName = rs.getString(1);
                int x = rs.getInt(2);
                int y = rs.getInt(3);
                int z = rs.getInt(4);

                World world = TeleportationRunes.getInstance().getServer().getWorld(worldName);
                Location loc = new Location(world, x, y, z);

                legacyLocations.add(loc);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return legacyLocations;
    }

}
