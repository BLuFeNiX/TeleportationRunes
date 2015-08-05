package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

/**
 * Created by blufenix on 8/1/15.
 */
public class BlockUtil {

    public static boolean isTeleporter(Block block) {
        return isBlockAtVectorOfStructure(block, Config.teleporterBlueprint);
    }

    public static boolean isWaypoint(Block block) {
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
                        if (blueprint == Config.waypointBlueprint) {
                            TeleportationRunes.getInstance().getLogger().info(bblock.getMaterialName() + " IS NOT " + loc.clone().subtract(vector).add(j, -i, k).getBlock().getType().name());
                        }
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static boolean isSafe(Location loc) {
        Material block1 = loc.clone().add(0, 1, 0).getBlock().getType();
        Material block2 = loc.clone().add(0, 2, 0).getBlock().getType();

        if ((block1.compareTo(Material.AIR) == 0 || block1.compareTo(Material.WATER) == 0)
                && (block2.compareTo(Material.AIR) == 0 || block2.compareTo(Material.WATER) == 0)) {
            return true;
        }

        return false;
    }

}
