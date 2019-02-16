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
                            p.sendBlockChange(loc, bblock.getMaterial(), (byte) 0);
                        } else {
                            if (bblock.getMaterialName().startsWith("SIGNATURE_BLOCK")) {
                                p.sendBlockChange(loc, Material.BEACON, (byte) 0);
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

        return (mat1 == Material.AIR || mat1 == Material.WATER || mat1.isTransparent() || mat1.name().contains("SLAB") || mat1.name().contains("STEP"))
                && (mat2 == Material.AIR || mat2 == Material.WATER || mat2.isTransparent())
                && (mat3 == Material.AIR || mat3 == Material.WATER || mat3.isTransparent());
    }

    private static abstract class BlockInteractor {
        abstract void onInteract(Player p, Location loc, Blueprint.Block bblock);
    }

    public static boolean isPlayerInteractableWithoutSpecialItem(Block block) {
        if (block == null) return false;
        switch(block.getType()) {
            case ACACIA_BOAT:
            case ACACIA_BUTTON:
            case ACACIA_DOOR:
            case ACACIA_FENCE_GATE:
            case ACACIA_TRAPDOOR:
            case ANVIL:
            case BEACON:
            case BEDROCK:
            case BIRCH_BOAT:
            case BIRCH_BUTTON:
            case BIRCH_DOOR:
            case BIRCH_FENCE_GATE:
            case BIRCH_TRAPDOOR:
            case BLACK_BED:
            case BLUE_BED:
            case BREWING_STAND:
            case BROWN_BED:
            case CAKE:
            case CHAINMAIL_CHESTPLATE:
            case CHAIN_COMMAND_BLOCK:
            case CHEST:
            case CHEST_MINECART:
            case CHIPPED_ANVIL:
            case COMMAND_BLOCK:
            case COMMAND_BLOCK_MINECART:
            case COMPARATOR:
            case CRAFTING_TABLE:
            case CYAN_BED:
            case DAMAGED_ANVIL:
            case DARK_OAK_BOAT:
            case DARK_OAK_BUTTON:
            case DARK_OAK_DOOR:
            case DARK_OAK_FENCE_GATE:
            case DARK_OAK_TRAPDOOR:
            case DIAMOND_CHESTPLATE:
            case DISPENSER:
            case ENCHANTING_TABLE:
            case ENDER_CHEST:
            case END_GATEWAY:
            case END_PORTAL_FRAME:
            case FURNACE:
            case FURNACE_MINECART:
            case GOLDEN_CHESTPLATE:
            case GRAY_BED:
            case GREEN_BED:
            case HOPPER:
            case HOPPER_MINECART:
            case IRON_CHESTPLATE:
            case IRON_DOOR:
            case IRON_TRAPDOOR:
            case ITEM_FRAME:
            case JUNGLE_BOAT:
            case JUNGLE_BUTTON:
            case JUNGLE_DOOR:
            case JUNGLE_FENCE_GATE:
            case JUNGLE_TRAPDOOR:
            case LEATHER_CHESTPLATE:
            case LEVER:
            case LIGHT_BLUE_BED:
            case LIGHT_GRAY_BED:
            case LIME_BED:
            case MAGENTA_BED:
            case OAK_BOAT:
            case OAK_BUTTON:
            case OAK_DOOR:
            case OAK_FENCE_GATE:
            case OAK_TRAPDOOR:
            case ORANGE_BED:
            case PINK_BED:
            case PURPLE_BED:
            case RED_BED:
            case REPEATING_COMMAND_BLOCK:
            case SPRUCE_BOAT:
            case SPRUCE_BUTTON:
            case SPRUCE_DOOR:
            case SPRUCE_FENCE_GATE:
            case SPRUCE_TRAPDOOR:
            case STONE_BUTTON:
            case TRAPPED_CHEST:
            case WHITE_BED:
            case YELLOW_BED:
                return true;
            default:
                return false;
        }
    }

}
