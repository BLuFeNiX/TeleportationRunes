package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

/**
 * Created by blufenix on 8/1/15.
 */
public class BlockUtil {

    private static final boolean DEBUG = true;

    public static boolean isTeleporter(Block block) {
        if (DEBUG) TeleportationRunes.getInstance().getLogger().info("isTeleporter()?");
        return isBlockAtVectorOfStructure(block, Config.teleporterBlueprint);
    }

    public static boolean isWaypoint(Block block) {
        if (DEBUG) TeleportationRunes.getInstance().getLogger().info("isWaypoint()?");
        return isBlockAtVectorOfStructure(block, Config.waypointBlueprint);
    }

    private static boolean isBlockAtVectorOfStructure(Block block, Blueprint blueprint) {
        Location loc = block.getLocation();
        Blueprint.Block[][][] structure = blueprint.getMaterialMatrix();
        Vector vector = blueprint.getVectors()[0];

        for (int i = 0; i < structure.length; i++) {
            Blueprint.Block[][] layer = structure[i];
            for (int j = 0; j < layer.length; j++) {
                Blueprint.Block[] row = structure[i][j];
                for (int k = 0; k < row.length; k++) {
                    Blueprint.Block bblock = row[k];
                    if (bblock.getMaterial() != null && loc.clone().subtract(vector).add(j, -i, k).getBlock().getType() != bblock.getMaterial()) {
                        TeleportationRunes.getInstance().getLogger().info("needed: " + bblock.getMaterialName()
                                + " but got: " + loc.clone().subtract(vector).add(j, -i, k).getBlock().getType().name());
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static boolean isSafe(Location loc) {
        Material block1 = loc.clone().add(Vectors.UP).getBlock().getType();
        Material block2 = loc.clone().add(Vectors.UP).add(Vectors.UP).getBlock().getType();

        if ((block1.compareTo(Material.AIR) == 0 || block1.compareTo(Material.WATER) == 0)
                && (block2.compareTo(Material.AIR) == 0 || block2.compareTo(Material.WATER) == 0)) {
            return true;
        }

        return false;
    }

}
