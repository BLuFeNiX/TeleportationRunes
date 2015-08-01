package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Created by blufenix on 8/1/15.
 */
public class BlockUtil {

    /*
	 * Checks blocks for pattern:
	 * 		|R|?|R|
	 * 		|?|R|?|
	 * 		|R|?|R|
	 */
    public static boolean isTeleporter(Block block) {
        Location loc = block.getLocation();
        if ((block.getType() == Config.teleporterMaterial)
                && (loc.clone().add(-1, 0, -1)).getBlock().getType() == Config.teleporterMaterial
                && (loc.clone().add(-1, 0, 1)).getBlock().getType() == Config.teleporterMaterial
                && (loc.clone().add(1, 0, -1)).getBlock().getType() == Config.teleporterMaterial
                && (loc.clone().add(1, 0, 1)).getBlock().getType() == Config.teleporterMaterial) {
            return true;
        }

        return false;
    }

    public static boolean isTemporaryTeleporter(Block block) {
        Location loc = block.getLocation();
        if ((block.getType() == Material.REDSTONE_WIRE)
                && (loc.clone().add(-1, 0, -1)).getBlock().getType() == Material.REDSTONE_WIRE
                && (loc.clone().add(-1, 0, 1)).getBlock().getType() == Material.REDSTONE_WIRE
                && (loc.clone().add(1, 0, -1)).getBlock().getType() == Material.REDSTONE_WIRE
                && (loc.clone().add(1, 0, 1)).getBlock().getType() == Material.REDSTONE_WIRE

                && (loc.clone().add(0, 0, 1)).getBlock().getType() == Material.REDSTONE_WIRE
                && (loc.clone().add(1, 0, 0)).getBlock().getType() == Material.REDSTONE_WIRE
                && (loc.clone().add(0, 0, -1)).getBlock().getType() == Material.REDSTONE_WIRE
                && (loc.clone().add(-1, 0, 0)).getBlock().getType() == Material.REDSTONE_WIRE) {
            return true;
        }

        return false;
    }

    /*
     * Checks blocks for pattern:
     * 		|L|?|L|
     * 		|?|L|?|
     * 		|L|?|L|
     */
    public static boolean isWaypoint(Block block) {
        Location loc = block.getLocation();
        if ((block.getType() == Config.waypointMaterial)
                && (loc.clone().add(-1, 0, -1)).getBlock().getType() == Config.waypointMaterial
                && (loc.clone().add(-1, 0, 1)).getBlock().getType() == Config.waypointMaterial
                && (loc.clone().add(1, 0, -1)).getBlock().getType() == Config.waypointMaterial
                && (loc.clone().add(1, 0, 1)).getBlock().getType() == Config.waypointMaterial) {
            return true;
        }

        return false;
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
