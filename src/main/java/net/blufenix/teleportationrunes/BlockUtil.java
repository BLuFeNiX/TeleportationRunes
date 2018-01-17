package net.blufenix.teleportationrunes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Created by blufenix on 8/1/15.
 */
public class BlockUtil {

    private static final boolean DEBUG = false;

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

    // can't use the new BlockInteractor here, since we want to return immediately if we get a non-matching block
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
                        if (DEBUG) TeleportationRunes.getInstance().getLogger().info("needed: " + bblock.getMaterialName()
                                + " but got: " + loc.clone().subtract(vector).add(j, -i, k).getBlock().getType().name());
                        return false;
                    }
                }
            }
        }

        return true;
    }


    /**
     * run on next tick, otherwise part of the mirage (typically the block that was clicked to activate it)
     * will be re-updated on the client side, making the mirage incomplete
     */
    public static void showMirage(final Player p, final Block block, final Blueprint.RotatedBlueprint blueprint) {
        Bukkit.getScheduler().runTaskLater(TeleportationRunes.getInstance(), new Runnable() {
            @Override
            public void run() {
                showMirage_impl(p, block, blueprint);
            }
        }, 1);
    }

    /**
     * make it look like the structure defined by the supplied blueprint actually exists in the client's world
     */
    private static void showMirage_impl(Player p, Block block, Blueprint.RotatedBlueprint blueprint) {
        doPattern(p, block, blueprint, new BlockInteractor() {
            @Override
            void onInteract(Player p, Location loc, Blueprint.Block bblock) {
                if (bblock.getMaterial() != null) {
                    p.sendBlockChange(loc, bblock.getMaterial(), (byte) 0);
                } else {
                    if (bblock.getMaterialName().startsWith("SIGNATURE_BLOCK")) {
                        p.sendBlockChange(loc, Material.BEACON, (byte) 0);
                    }
                }
            }
        });
    }

    /**
     * Iterate over the supplied blueprint, in reference to the clicked block, and run the supplied BlockInteractor on them
     */
    private static void doPattern(Player p, Block block, Blueprint.RotatedBlueprint blueprint, BlockInteractor blockInteractor) {
        Location loc = block.getLocation();
        Blueprint.Block[][][] structure = blueprint.getMaterialMatrix();
        Vector vector = blueprint.getClickableBlockVector();

        for (int i = 0; i < structure.length; i++) {
            Blueprint.Block[][] layer = structure[i];
            for (int j = 0; j < layer.length; j++) {
                Blueprint.Block[] row = structure[i][j];
                for (int k = 0; k < row.length; k++) {
                    Blueprint.Block bblock = row[k];
                    Location tempLoc = loc.clone().subtract(vector).add(j, -i, k);
                    blockInteractor.onInteract(p, tempLoc, bblock);
                }
            }
        }
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

    private static abstract class BlockInteractor {
        abstract void onInteract(Player p, Location loc, Blueprint.Block bblock);
    }

}
