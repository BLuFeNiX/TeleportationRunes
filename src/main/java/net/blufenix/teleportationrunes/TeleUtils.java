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
import org.bukkit.scheduler.BukkitRunnable;
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
        loc = loc.clone();
        int rotation;
        if ((rotation = BlockUtil.isTeleporter(loc)) >= 0) {
            return new Teleporter(loc, rotation);
        // check one more block down
        } else if ((rotation = BlockUtil.isTeleporter(loc.add(DOWN))) >= 0) {
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
            waypointDB.removeWaypointByLocation(existingWaypoint.loc);
            return false;
        }

        // make sure the signature hasn't changed
        if (!existingWaypoint.sig.equals(Signature.fromLocation(existingWaypoint.loc, Config.waypointBlueprint.atRotation(waypointRotation)))) {
            player.sendMessage(StringResources.WAYPOINT_ALTERED);
            waypointDB.removeWaypointByLocation(existingWaypoint.loc);
            return false;
        }

        if (!Config.allowTeleportBetweenWorlds) {
            // is the destination in our current world?
            if (!existingWaypoint.loc.getWorld().equals(teleporterLoc.getWorld())) {
                player.sendMessage(StringResources.WAYPOINT_DIFFERENT_WORLD);
                return false;
            }
        }

        try {

            int fee;
            if (!Config.costXpInCreative && player.getGameMode() == GameMode.CREATIVE) {
                fee = 0;
            } else {
                fee = calculateExpr(existingWaypoint.loc, teleporterLoc, Config.costFormula);
            }
            int currentExp = ExpUtil.getTotalExperience(player);

            if (currentExp >= fee) {
                // teleport player
                Location playerLoc = player.getLocation();
                final Location adjustedLoc = getSafeDestination(existingWaypoint.loc.clone());

                if (adjustedLoc == null) {
                    player.sendMessage(StringResources.WAYPOINT_OBSTRUCTED);
                    return false;
                }

                adjustedLoc.setDirection(playerLoc.getDirection());

                // check if the player has any leashed animals to teleport as well
                // TODO add to numEntities (can player be riding and leash the same/different entity?)
                final ArrayList<LivingEntity> leashedEntities = new ArrayList<>();
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

                // teleport leashed entities 1 tick later, to avoid behavior where leash stretches to infinity
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        // port leashed animals as well if there are any
                        for (LivingEntity le : leashedEntities) {
                            le.teleport(adjustedLoc); // todo check if successful, and what to do if not?
                            le.setLeashHolder(player);
                            player.spawnParticle(Particle.HEART, adjustedLoc, 1);
                        }
                    }
                }.runTaskLater(TeleportationRunes.getInstance(), 1);


                if (Config.enableLightningEffect) {
                    player.getWorld().strikeLightningEffect(adjustedLoc);
                }

                if (Config.enableEnderTeleportEffect) {
                    player.playEffect(EntityEffect.TELEPORT_ENDER);
                    player.playSound(adjustedLoc, "entity.enderman.teleport", 1f /*volume*/, 1.0f /*pitch*/);
                }

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

    // make sure teleport destination won't suffocate the player
    private static Location getSafeDestination(Location loc) {
        // if this one's not safe, check one block up
        if (!BlockUtil.isSafe(loc)) {
            loc.add(UP);
        }

        // if one block up isn't safe, bail
        if (!BlockUtil.isSafe(loc)) {
            return null;
        }

        loc.add(Vectors.UP)
           .add(Vectors.UP_A_LITTLE)
           .add(Vectors.CENTER);

        return loc;
    }

    public static int calculateExpr(Location waypointLoc, Location teleporterLoc, String formula) throws UnparsableExpressionException, UnknownFunctionException {
        boolean anotherWorld = !waypointLoc.getWorld().equals(teleporterLoc.getWorld());
        if (anotherWorld) {
            // fake the world for the destination, so we can measure a somewhat nonsensical distance between them
            waypointLoc = waypointLoc.clone();
            waypointLoc.setWorld(teleporterLoc.getWorld());
        }

        // calculate teleport distance
        double distance = waypointLoc.distance(teleporterLoc);

        Calculable calc = new ExpressionBuilder(formula)
                .withVariable("distance", distance)
                .withVariable("anotherWorld", anotherWorld ? 1 : 0)
                .build();

        return (int) Math.round(calc.calculate());
    }

    /*
    public static int calculateFee(Location waypointLoc, Location teleporterLoc) throws UnparsableExpressionException, UnknownFunctionException {
        Calculable calc;
        if (waypointLoc.getWorld().equals(teleporterLoc.getWorld())) {
            // calculate teleport distance
            double distance = waypointLoc.distance(teleporterLoc);
            calc = new ExpressionBuilder(Config.costFormula)
                    .withVariable("distance", distance)
                    .build();
        } else {
            // no such thing as distance between worlds
            double distance = waypointLoc.distance(teleporterLoc);
            calc = new ExpressionBuilder(Config.costFormulaBetweenWorlds)
                    .withVariable("distance", distance)
                    .build();
        }

        return (int) Math.round(calc.calculate());
    }
     */

}
