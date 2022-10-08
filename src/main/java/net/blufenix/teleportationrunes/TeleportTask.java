package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TeleportTask extends BukkitRunnable {

    private static Set<Player> playersCurrentlyTeleporting = Collections.synchronizedSet(new HashSet<Player>());

    // modify these
    private int countdownTicks;
    private final int updateIntervalTicks = Config.animationDutyCycle;

    private final Callback callback;

    private int elapsedTicks = 0;

    private final Player player;
    private final boolean canLeaveArea;
    private final boolean requireSneak;
    private final ItemStack requiredItem;
    private Location sourceLoc;
    private Waypoint destWaypoint;
    private Location potentialTeleporterLoc;
    private Signature waypointSignature;

    private SwirlAnimation animation;
    private boolean isRunning;

    TeleportTask(Player player, Location potentialTeleporterLoc, Callback callback) {
        this.player = player;
        this.callback = callback;
        this.potentialTeleporterLoc = potentialTeleporterLoc;
        canLeaveArea = false;
        requireSneak = false;
        requiredItem = null;
    }

    TeleportTask(Player player, Signature waypointSignature, ItemStack requiredItem, boolean requireSneak, Callback callback) {
        this.player = player;
        this.callback = callback;
        this.waypointSignature = waypointSignature;
        this.canLeaveArea = true;
        this.requireSneak = requireSneak;
        this.requiredItem = requiredItem;
    }

    private void lateInit() {
        if (potentialTeleporterLoc != null) {
            Teleporter teleporter = TeleUtils.getTeleporterNearLocation(potentialTeleporterLoc);
            this.sourceLoc = teleporter != null ? teleporter.loc : null;
            this.destWaypoint = TeleUtils.getWaypointForTeleporter(teleporter);
        } else if (waypointSignature != null) {
            this.sourceLoc = player.getLocation();
            this.destWaypoint = TeleUtils.getWaypointForSignature(waypointSignature);
        } else {
            throw new RuntimeException("lateInit() failed. bad params?");
        }
    }

    public void execute() {
        if (playersCurrentlyTeleporting.contains(player)) {
            return; //todo this will mean our callback isn't called
            // but we need it for now in order to prevent repeated teleport attempts
            // since calling onSuccessOrFail will remove us from playersCurrentlyTeleporting
            // and right now the callbacks don't need to work in that case.
        }

        playersCurrentlyTeleporting.add(player);
        lateInit();

        if (startTeleportationTask()) {
            isRunning = true;
        } else {
            onSuccessOrFail(false);
        }
    }

    private boolean startTeleportationTask() {
        try {
            if (sourceLoc == null || destWaypoint == null) return false;

            // calculate time before teleport
            countdownTicks = TeleUtils.calculateExpr(sourceLoc, destWaypoint.loc, Config.teleportDelayFormula);

            // show the player the cost
            int fee;
            if (!Config.costXpInCreative && player.getGameMode() == GameMode.CREATIVE) {
                fee = 0;
            } else {
                fee = TeleUtils.calculateExpr(destWaypoint.loc, sourceLoc, Config.costFormula);
            }
            int currentExp = ExpUtil.getTotalExperience(player);
            String msg = String.format("%d XP / %d XP", fee, currentExp);
            if (requireSneak && !player.isSneaking()) {
                msg = msg.concat(" [sneak to confirm]");
            }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));

            if (!requireSneak || player.isSneaking()) {
                // start teleport animation and timer
                animation = Config.particleAnimation.clone()
                        .setDuration(countdownTicks);
                if (canLeaveArea) {
                    animation.setLocation(player);
                } else {
                    // make the animation occur at the player's height
                    // in case they are hiding the teleporter under a layer of blocks
                    Location animLoc = sourceLoc.clone();
                    animLoc.setY(player.getLocation().getY());
                    animation.setLocation(animLoc);
                }

                runTaskTimer(TeleportationRunes.getInstance(), 0, updateIntervalTicks);
                return true;
            }
        } catch (Exception e) {
            Log.e("error in startTeleportationTask!", e);
        }

        return false;
    }

    @Override
    public void run() {

        // we haven't actually ticked yet, but if we pass 0 into our animation ticks
        // it will animate a single frame regardless of the interval or fake tick settings
        // TODO is there a cleaner way to fix this and remove the time shift?
        elapsedTicks += updateIntervalTicks;

        if (!canLeaveArea && !playerStillAtTeleporter()) {
            player.sendMessage("You left the teleporter area. Cancelling...");
            onSuccessOrFail(false);
            return;
        }

        if (requiredItem != null && !requiredItemInHand()) {
            player.sendMessage("You're no longer holding the scroll. Cancelling...");
            onSuccessOrFail(false);
            return;
        }

        if (elapsedTicks < countdownTicks) {
            if (Config.particleAnimationEnabled) {
                animation.update(elapsedTicks);
            }
        } else {
            if (TeleUtils.attemptTeleport(player, sourceLoc, destWaypoint)) {
                onSuccessOrFail(true);
            } else {
                onSuccessOrFail(false);
            }
        }

    }

    private void onSuccessOrFail(boolean success) {
        if (isRunning) this.cancel();
        if (callback != null) {
            callback.onFinished(success);
        }
        Bukkit.getScheduler().runTaskLater(TeleportationRunes.getInstance(), new Runnable() {
            @Override
            public void run() {
                playersCurrentlyTeleporting.remove(player);
            }
        }, 20);
    }

    private boolean playerStillAtTeleporter() {
        return player.getLocation().distance(sourceLoc) < 2.5;
    }

    private boolean requiredItemInHand() {
        return player.getInventory().getItemInMainHand().equals(requiredItem) || player.getInventory().getItemInOffHand().equals(requiredItem);
    }

    public static abstract class Callback {
        abstract void onFinished(boolean success);
    }
}
