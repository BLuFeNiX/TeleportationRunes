package net.blufenix.teleportationrunes;

import de.congrace.exp4j.Calculable;
import de.congrace.exp4j.ExpressionBuilder;
import de.congrace.exp4j.UnknownFunctionException;
import de.congrace.exp4j.UnparsableExpressionException;
import net.blufenix.common.Log;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;

import static net.blufenix.teleportationrunes.Vectors.*;

/**
 * Created by blufenix on 8/5/15.
 */
public class TeleUtils {

    /** when iteratively added to a location, will encircle it orthogonal to the Y-axis */
    private static Vector[] nearbyVectors = {NONE, NORTH, EAST, SOUTH, SOUTH, WEST, WEST, NORTH, NORTH};

    public static Teleporter getTeleporterFromLocation(Location loc) {
        int rotation;
        if ((rotation = BlockUtil.isTeleporter(loc)) >= 0) {
            return new Teleporter(loc, rotation);
        }
        return null;
    }

    public static Teleporter getTeleporterNearLocation(Location loc) {
        Teleporter teleporter;
        Location tmpLoc = loc.clone();
        for (int i = 0; i < nearbyVectors.length; i++) {
            teleporter = getTeleporterFromLocation(tmpLoc.add(nearbyVectors[i]));
            if (teleporter != null) return teleporter;
        }
        return null;
    }

    public static Waypoint getWaypointForTeleporter(Teleporter teleporter) {
        if (teleporter == null) return null;
        return getWaypointForSignature(teleporter.sig);
    }

    public static Waypoint getWaypointForSignature(Signature sig) {
        if (sig == null) return null;
        WaypointDB waypointDB = TeleportationRunes.getInstance().getWaypointDB();
        return waypointDB.getWaypointFromSignature(sig);
    }

    public static boolean attemptTeleport(final Player player, Location teleporterLoc, Waypoint existingWaypoint) {

        WaypointDB waypointDB = TeleportationRunes.getInstance().getWaypointDB();

        // is there a waypoint matching this teleporter?
        if (existingWaypoint == null) {
            player.sendMessage(StringResources.WAYPOINT_NOT_FOUND);
            return false;
        }

        // make sure the waypoint hasn't been destroyed
        int waypointRotation;
        if ((waypointRotation = BlockUtil.isWaypoint(existingWaypoint.loc)) < 0) {
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

        try {

            int fee = calculateFee(existingWaypoint.loc, teleporterLoc, player);
            int currentExp = ExpUtil.getTotalExperience(player);

            if (currentExp >= fee) {
                // teleport player
                Location playerLoc = player.getLocation();
                // ... to the middle of the block, and one block up
                Location adjustedLoc = existingWaypoint.loc.clone()
                        .add(Vectors.UP)
                        .add(Vectors.UP)
                        .add(Vectors.CENTER);
                adjustedLoc.setDirection(playerLoc.getDirection());

                // check if the player has any leashed animals to teleport as well
                // TODO add to numEntities (can player be riding and leash the same/different entity?)
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
                    final Vehicle vehicle = (Vehicle) player.getVehicle();
                    Log.d("player (%s) in vehicle (%s)", player.getDisplayName(), vehicle.getClass().getSimpleName());
                    vehicle.eject();
                    if (!vehicle.teleport(adjustedLoc, PlayerTeleportEvent.TeleportCause.PLUGIN)) {
                        throw new TeleportException("failed to teleport vehicle!");
                    }
                    if (!player.teleport(adjustedLoc, PlayerTeleportEvent.TeleportCause.PLUGIN)) {
                        // try to get the vehicle back
                        if (vehicle.teleport(playerLoc, PlayerTeleportEvent.TeleportCause.PLUGIN)) {
                            vehicle.addPassenger(player);
                        }
                        throw new TeleportException("failed to teleport player with vehicle!");
                    }

                    // if we try to board a horse too quickly, it breaks the teleport for both parties
                    Bukkit.getScheduler().runTaskLater(TeleportationRunes.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            vehicle.addPassenger(player);
                        }
                    }, 4);

                } else {
                    if (!player.teleport(adjustedLoc, PlayerTeleportEvent.TeleportCause.PLUGIN)) {
                        throw new TeleportException("failed to teleport player!");
                    }
                }

                // port leashed animals as well if there are any
                for (LivingEntity le : leashedEntities) {
                    le.teleport(adjustedLoc); // todo check if successful, and what to do if not?
                    le.setLeashHolder(player);
                    player.spawnParticle(Particle.HEART, adjustedLoc, 1);
                }

                player.getWorld().strikeLightningEffect(adjustedLoc);

                // subtract EXP
                player.giveExp(-fee);

                // slightly imprecise calculation seems to cause player xp to be set to negative integer max value
                // so, fix it.
                if (ExpUtil.getTotalExperience(player) <= 0) {
                    player.setExp(0);
                }

                Log.d(player.getName() + " teleported from " + playerLoc + " to " + adjustedLoc);
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You do not have enough experience to teleport.");
                player.sendMessage(ChatColor.RED + "Your Exp: " + currentExp);
                player.sendMessage(ChatColor.RED + "Exp needed: " + fee);
                return false;
            }

        } catch (Exception e) {
            Log.e("error in attemptTeleport()", e);
            player.sendMessage(ChatColor.RED + "Something went wrong. Please inform your server administrator.");
            return false;
        }
    }

    public static int calculateFee(Location waypointLoc, Location teleporterLoc, Player player) throws UnparsableExpressionException, UnknownFunctionException {
        // calculate teleport distance
        double distance = waypointLoc.distance(teleporterLoc);
        int deltaX = Math.abs(waypointLoc.getBlockX() - teleporterLoc.getBlockX());
        int deltaY = Math.abs(waypointLoc.getBlockY() - teleporterLoc.getBlockY());
        int deltaZ = Math.abs(waypointLoc.getBlockZ() - teleporterLoc.getBlockZ());
        int numEntities = player.isInsideVehicle() ? 2 : 1;

        Calculable calc = new ExpressionBuilder(Config.costFormula)
                .withVariable("distance", distance)
                .withVariable("deltaX", deltaX)
                .withVariable("deltaY", deltaY)
                .withVariable("deltaZ", deltaZ)
                .withVariable("numEntities", numEntities)
                .build();

        return (int) Math.round(calc.calculate());
    }

}
