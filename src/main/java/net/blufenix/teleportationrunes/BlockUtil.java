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

    private static class LazyHolder {
        public static final int[] ROTATIONS = Config.enableRotation ? new int[]{0,90,180,270} : new int[]{0};
    }

    public static int isTeleporter(Location loc) {
        if (loc == null) return -1;
        for (int rotation : LazyHolder.ROTATIONS) {
            if (isLocationAtVectorOfStructure(loc, Config.teleporterBlueprint.atRotation(rotation))) {
                return rotation;
            }
        }

        return -1;
    }

    public static int isWaypoint(Location loc) {
        if (loc == null) return -1;
        for (int rotation : LazyHolder.ROTATIONS) {
            if (isLocationAtVectorOfStructure(loc, Config.waypointBlueprint.atRotation(rotation))) {
                return rotation;
            }
        }

        return -1;
    }

    // can't use the new BlockInteractor here, since we want to return immediately if we get a non-matching block
    private static boolean isLocationAtVectorOfStructure(Location loc, Blueprint.RotatedBlueprint blueprint) {
        Blueprint.Block[][][] structure = blueprint.materialMatrix;
        Vector vector = blueprint.clickableVector;

        Location tempLoc = loc.clone().subtract(vector);

        for (int i = 0; i < structure.length; i++) {
            Blueprint.Block[][] layer = structure[i];
            for (int j = 0; j < layer.length; j++) {
                Blueprint.Block[] row = structure[i][j];
                for (int k = 0; k < row.length; k++) {
                    Material mat = row[k].getMaterial();
                    if (mat != null && tempLoc.clone().add(j, -i, k).getBlock().getType() != mat) {
                        //Log.d("needed: %s but got: %s", mat.toString(), tempLoc.clone().add(j, -i, k).getBlock().getType().name());
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * make it look like the structure defined by the supplied blueprint actually exists in the client's world
     *
     * run on next tick, otherwise part of the mirage (typically the block that was clicked to activate it)
     * will be re-updated on the client side, making the mirage incomplete
     */
    public static void showMirage(final Player p, final Location loc, final Blueprint.RotatedBlueprint blueprint) {
        Bukkit.getScheduler().runTaskLater(TeleportationRunes.getInstance(), new Runnable() {
            @Override
            public void run() {
                doPattern(p, loc, blueprint, new BlockInteractor() {
                    @Override
                    void onInteract(Player p, Location loc, Blueprint.Block bblock) {
                        if (bblock.getMaterial() != null) {
                            p.sendBlockChange(loc, bblock.getMaterial().createBlockData());
                        } else {
                            if (bblock.getMaterialName().startsWith("SIGNATURE_BLOCK")) {
                                p.sendBlockChange(loc, Material.BEACON.createBlockData());
                            }
                        }
                    }
                });
            }
        }, 1);
    }

    /**
     * Iterate over the supplied blueprint, in reference to the clicked block, and run the supplied BlockInteractor on them
     */
    private static void doPattern(Player p, Location loc, Blueprint.RotatedBlueprint blueprint, BlockInteractor blockInteractor) {
        Blueprint.Block[][][] structure = blueprint.materialMatrix;
        Vector vector = blueprint.clickableVector;

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

    // todo configurable safety for water to avoid drowning
    // todo only enable extra height check when using a horse?
    public static boolean isSafe(Location loc) {
        Material mat1 = loc.clone().add(Vectors.UP).getBlock().getType();
        Material mat2 = loc.clone().add(Vectors.UP).add(Vectors.UP).getBlock().getType();
        Material mat3 = loc.clone().add(Vectors.UP).add(Vectors.UP).add(Vectors.UP).getBlock().getType();

        return (mat1 == Material.AIR || mat1 == Material.WATER || !mat1.isOccluding() || mat1.name().contains("SLAB") || mat1.name().contains("STEP"))
                && (mat2 == Material.AIR || mat2 == Material.WATER || !mat2.isOccluding())
                && (mat3 == Material.AIR || mat3 == Material.WATER || !mat3.isOccluding());
    }

    private static abstract class BlockInteractor {
        abstract void onInteract(Player p, Location loc, Blueprint.Block bblock);
    }

}
