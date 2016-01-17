package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Created by blufenix on 8/1/15.
 */
public class BlockUtil {

    private static final boolean DEBUG = true;

    public static int isTeleporter(Block block) {
        if (DEBUG) TeleportationRunes.getInstance().getLogger().info("isTeleporter()?");
        for (int rotation : new int[]{0,90,180,270}) {
            if (isBlockAtVectorOfStructure(block, Config.teleporterBlueprint.atRotation(rotation))) {
                return rotation;
            }
        }

        return -1;
    }

    public static int isWaypoint(Block block) {
        if (DEBUG) TeleportationRunes.getInstance().getLogger().info("isWaypoint()?");
        for (int rotation : new int[]{0,90,180,270}) {
            if (isBlockAtVectorOfStructure(block, Config.waypointBlueprint.atRotation(rotation))) {
                return rotation;
            }
        }

        return -1;
    }

    private static boolean isBlockAtVectorOfStructure(Block block, Blueprint.RotatedBlueprint blueprint) {
        Location loc = block.getLocation();
        Blueprint.Block[][][] structure = blueprint.getMaterialMatrix();
        Vector vector = blueprint.getClickableBlockVector();

        for (int i = 0; i < structure.length; i++) {
            Blueprint.Block[][] layer = structure[i];
            for (int j = 0; j < layer.length; j++) {
                Blueprint.Block[] row = structure[i][j];
                for (int k = 0; k < row.length; k++) {
                    Blueprint.Block bblock = row[k];
                    if (bblock.getMaterial() != null && loc.clone().subtract(vector).add(j, -i, k).getBlock().getType() != bblock.getMaterial()) {
                        TeleportationRunes.getInstance().getLogger().info("needed: " + bblock.getMaterialName()
                                + " but got: " + loc.clone().subtract(vector).add(j, -i, k).getBlock().getType().name());
                        // TODO REMOVE
//                        for (Player player : loc.getWorld().getPlayers()) {
//                            player.sendBlockChange(loc.clone().subtract(vector).add(j, -i, k), Material.LAVA.getId(), block.getData());
//                        }
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
