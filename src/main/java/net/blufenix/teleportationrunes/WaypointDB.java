package net.blufenix.teleportationrunes;

import net.blufenix.common.SimpleDatabase;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by blufenix on 7/27/15.
 */
public class WaypointDB extends SimpleDatabase {

    private static final String FILENAME = "waypoints.db";

    WaypointDB() {
        super(FILENAME);
        if (!new File(getDatabaseFilePath()).exists()) {
            createDatabase();
        }
    }

    private void createDatabase() {
        execute("CREATE TABLE waypoints " +
                "(user TEXT NOT NULL, " +
                "world TEXT NOT NULL, " +
                "x INTEGER NOT NULL, " +
                "y INTEGER NOT NULL, " +
                "z INTEGER NOT NULL, " +
                "north STRING NOT NULL, " +
                "north_data INTEGER NOT NULL, " +
                "south STRING NOT NULL, " +
                "south_data INTEGER NOT NULL, " +
                "east STRING NOT NULL, " +
                "east_data INTEGER NOT NULL, " +
                "west STRING NOT NULL," +
                "west_data INTEGER NOT NULL)");
    }

    public boolean addWaypoint(Waypoint waypoint) {
        String sql = String.format("INSERT INTO waypoints VALUES ('%s', '%s', %d, %d, %d, '%s', %d, '%s', %d, '%s', %d, '%s', %d)",
                waypoint.user, waypoint.loc.getWorld().getName(),
                (int)waypoint.loc.getX(), (int)waypoint.loc.getY(), (int)waypoint.loc.getZ(),
                waypoint.sig.north.getType().name(), waypoint.sig.north.getData().getData(),
                waypoint.sig.south.getType().name(), waypoint.sig.south.getData().getData(),
                waypoint.sig.east.getType().name(), waypoint.sig.east.getData().getData(),
                waypoint.sig.west.getType().name(), waypoint.sig.west.getData().getData());
        return execute(sql);
    }

    public boolean removeWaypoint(Waypoint waypoint) {
        String sql = String.format("DELETE FROM waypoints WHERE north = '%s' AND north_data = %d AND south = '%s' AND south_data = %d AND east = '%s' AND east_data = %d AND west = '%s' AND west_data = %d",
                waypoint.sig.north.getType().name(), waypoint.sig.north.getData().getData(),
                waypoint.sig.south.getType().name(), waypoint.sig.south.getData().getData(),
                waypoint.sig.east.getType().name(), waypoint.sig.east.getData().getData(),
                waypoint.sig.west.getType().name(), waypoint.sig.west.getData().getData());
        return execute(sql);
    }

    public Waypoint getWaypointFromSignature(Signature sig) {
        String sql = String.format("SELECT * FROM waypoints WHERE north = '%s' AND north_data = %d AND south = '%s' AND south_data = %d AND east = '%s' AND east_data = %d AND west = '%s' AND west_data = %d",
                sig.north.getType().name(), sig.north.getData().getData(),
                sig.south.getType().name(), sig.south.getData().getData(),
                sig.east.getType().name(), sig.east.getData().getData(),
                sig.west.getType().name(), sig.west.getData().getData());
        ResultSet rs = query(sql);
        try {
            if (rs.next()) {
                String user = rs.getString(1);
                String worldName = rs.getString(2);
                int x = rs.getInt(3);
                int y = rs.getInt(4);
                int z = rs.getInt(5);
                // reuse input signature, since all of its members are final
                //Signature sig = new Signature(Material.valueOf(rs.getString(6)), Material.valueOf(rs.getString(7)), Material.valueOf(rs.getString(8)), Material.valueOf(rs.getString(9)));

                World world = TeleportationRunes.getInstance().getServer().getWorld(worldName);
                Location loc = new Location(world, x, y, z);
                Waypoint wp = new Waypoint(user, loc, sig);

                return wp;
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return null;
    }

}
