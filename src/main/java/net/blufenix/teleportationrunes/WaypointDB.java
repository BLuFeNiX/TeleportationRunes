package net.blufenix.teleportationrunes;

import net.blufenix.common.SimpleDatabase;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Created by blufenix on 7/27/15.
 */
public class WaypointDB extends SimpleDatabase {

    private static final String FILENAME = "waypoints.db";

    public WaypointDB() {
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
                "south STRING NOT NULL, " +
                "east STRING NOT NULL, " +
                "west STRING NOT NULL)");
    }

    public boolean addWaypoint(Waypoint waypoint) {
        String sql = String.format("INSERT INTO waypoints VALUES ('%s', '%s', %d, %d, %d, '%s', '%s', '%s', '%s')",
                waypoint.user, waypoint.loc.getWorld().getName(),
                (int)waypoint.loc.getX(), (int)waypoint.loc.getY(), (int)waypoint.loc.getZ(),
                waypoint.sig.north.name(), waypoint.sig.south.name(), waypoint.sig.east.name(), waypoint.sig.west.name());
        return execute(sql);
    }

    public boolean removeWaypoint(Waypoint waypoint) {
        String sql = String.format("DELETE FROM waypoints WHERE north = '%s' AND south = '%s' AND east = '%s' AND west = '%s'",
                waypoint.sig.north.name(), waypoint.sig.south.name(), waypoint.sig.east.name(), waypoint.sig.west.name());
        return execute(sql);
    }

    public Waypoint getWaypointFromSignature(Signature sig) {
        String sql = String.format("SELECT * FROM waypoints WHERE north = '%s' AND south = '%s' AND east = '%s' AND west = '%s'",
                sig.north.name(), sig.south.name(), sig.east.name(), sig.west.name());
        ResultSet rs = query(sql);
        try {

            ResultSetMetaData metaData = rs.getMetaData();

            TeleportationRunes.getInstance().getLogger().info("TYPE 1: "+metaData.getColumnTypeName(1));
            TeleportationRunes.getInstance().getLogger().info("TYPE 2: "+metaData.getColumnTypeName(2));
            TeleportationRunes.getInstance().getLogger().info("TYPE 3: "+metaData.getColumnTypeName(3));
            TeleportationRunes.getInstance().getLogger().info("TYPE 4: "+metaData.getColumnTypeName(4));
            TeleportationRunes.getInstance().getLogger().info("TYPE 5: "+metaData.getColumnTypeName(5));
            TeleportationRunes.getInstance().getLogger().info("TYPE 6: "+metaData.getColumnTypeName(6));
            TeleportationRunes.getInstance().getLogger().info("TYPE 7: "+metaData.getColumnTypeName(7));
            TeleportationRunes.getInstance().getLogger().info("TYPE 8: "+metaData.getColumnTypeName(8));
            TeleportationRunes.getInstance().getLogger().info("TYPE 9: "+metaData.getColumnTypeName(9));

            if (rs.next()) {


                String user = rs.getString(1);
                TeleportationRunes.getInstance().getLogger().info("USER: "+user);
                String worldName = rs.getString(2);
                TeleportationRunes.getInstance().getLogger().info("WORLD: "+worldName);
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
