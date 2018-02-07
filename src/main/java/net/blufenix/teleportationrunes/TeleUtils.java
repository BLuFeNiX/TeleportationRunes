package net.blufenix.teleportationrunes;

import de.congrace.exp4j.Calculable;
import de.congrace.exp4j.ExpressionBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

import java.util.ArrayList;

/**
 * Created by blufenix on 8/5/15.
 */
public class TeleUtils {

    public static Teleporter getTeleporterFromLocation(Location loc) {
        int rotation;
        if ((rotation = BlockUtil.isTeleporter(loc.getBlock())) >= 0) {
            return new Teleporter(loc, rotation);
        }
        return null;
    }

    public static Waypoint getWaypointForTeleporter(Teleporter teleporter) {
        WaypointDB waypointDB = TeleportationRunes.getInstance().getWaypointDB();
        return waypointDB.getWaypointFromSignature(teleporter.sig);
    }

    public static boolean attemptTeleport(Player player, Location blockLocation, int rotation) {
        WaypointDB waypointDB = TeleportationRunes.getInstance().getWaypointDB();

        Signature sig = Signature.fromLocation(blockLocation, Config.teleporterBlueprint.atRotation(rotation));
        Waypoint existingWaypoint = waypointDB.getWaypointFromSignature(sig);

        return attemptTeleport(player, blockLocation, existingWaypoint);
    }

    public static boolean attemptTeleport(Player player, Location teleporterLoc, Waypoint existingWaypoint) {

        WaypointDB waypointDB = TeleportationRunes.getInstance().getWaypointDB();

        // is there a waypoint matching this teleporter?
        if (existingWaypoint == null) {
            player.sendMessage(StringResources.WAYPOINT_NOT_FOUND);
            return false;
        }

        // make sure the waypoint hasn't been destroyed
        int waypointRotation;
        if ((waypointRotation = BlockUtil.isWaypoint(existingWaypoint.loc.getBlock())) < 0) {
            player.sendMessage(StringResources.WAYPOINT_DAMAGED);
            waypointDB.removeWaypoint(existingWaypoint);
            return false;
        }

        // make sure the signature hasn't changed
        if (!existingWaypoint.sig.equals(Signature.fromLocation(existingWaypoint.loc, Config.waypointBlueprint.atRotation(waypointRotation)))) {
            player.sendMessage(StringResources.WAYPOINT_ALTERED);
            waypointDB.removeWaypoint(existingWaypoint);
            return false;
        }

        // make sure teleport destination won't suffocate the player
        if (!BlockUtil.isSafe(existingWaypoint.loc)) {
            player.sendMessage(StringResources.WAYPOINT_OBSTRUCTED);
            return false;
        }

        // is the destination in our current world?
        if (!existingWaypoint.loc.getWorld().equals(teleporterLoc.getWorld())) {
            player.sendMessage(StringResources.WAYPOINT_DIFFERENT_WORLD);
            return false;
        }

        // calculate teleport distance
        double distance = existingWaypoint.loc.distance(teleporterLoc);

        try {

            int deltaX = Math.abs(existingWaypoint.loc.getBlockX() - teleporterLoc.getBlockX());
            int deltaY = Math.abs(existingWaypoint.loc.getBlockY() - teleporterLoc.getBlockY());
            int deltaZ = Math.abs(existingWaypoint.loc.getBlockZ() - teleporterLoc.getBlockZ());
            int numEntities = player.isInsideVehicle() ? 2 : 1;

            Calculable calc = new ExpressionBuilder(Config.costFormula)
                    .withVariable("distance", distance)
                    .withVariable("deltaX", deltaX)
                    .withVariable("deltaY", deltaY)
                    .withVariable("deltaZ", deltaZ)
                    .withVariable("numEntities", numEntities)
                    .build();

            int fee = (int) Math.ceil(calc.calculate());
            int currentExp = ExpUtil.getTotalExperience(player);

            if (currentExp >= fee) {
                // subtract EXP
                player.giveExp(-fee);

                // slightly imprecise calculation seems to cause player xp to be set to negative integer max value
                // so, fix it.
                if (ExpUtil.getTotalExperience(player) <= 0) {
                    player.setExp(0);
                }

                // teleport player
                Location playerLoc = player.getLocation();
                Location adjustedLoc = existingWaypoint.loc.clone().add(Vectors.UP).add(Vectors.CENTER); // teleport to the middle of the block, and one block up
                adjustedLoc.setDirection(playerLoc.getDirection());
                player.getWorld().playEffect(playerLoc, Effect.MOBSPAWNER_FLAMES, 0);

                // check if the player has any leashed animals to teleport as well
                ArrayList<LivingEntity> leashedEntities = new ArrayList<>();
                for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
                    if (entity instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity) entity;
                        if (le.isLeashed() && le.getLeashHolder() == player) {
                            leashedEntities.add(le);
                        }
                    }
                }

                if (player.isInsideVehicle()) {
                    Vehicle vehicle = (Vehicle) player.getVehicle();
                    vehicle.eject();
                    vehicle.teleport(adjustedLoc);
                    player.teleport(adjustedLoc);
                    vehicle.setPassenger(player);
                } else {
                    player.teleport(adjustedLoc);
                }

                // port them leashed animals as well if there are any
                for (LivingEntity le : leashedEntities) {
                    le.teleport(adjustedLoc);
                    le.setLeashHolder(player);
                    player.spawnParticle(Particle.HEART, adjustedLoc, 1);
                }

                player.getWorld().strikeLightningEffect(adjustedLoc);

                TeleportationRunes.getInstance().getLogger().info(player.getName() + " teleported from " + playerLoc + " to " + adjustedLoc);
                player.sendMessage(ChatColor.GREEN + "Teleportation successful!");
                player.sendMessage(ChatColor.GREEN + "You traveled " + ((int) distance) + " blocks at the cost of " + fee + " experience points.");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You do not have enough experience to use this teleporter.");
                player.sendMessage(ChatColor.RED + "Your Exp: " + currentExp);
                player.sendMessage(ChatColor.RED + "Exp needed: " + fee);
                player.sendMessage(ChatColor.RED + "Distance: " + ((int) distance) + " blocks");
                return false;
            }

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "TeleportationRunes cost formula is invalid. Please inform your server administrator.");
            return false;
        }
    }

}
