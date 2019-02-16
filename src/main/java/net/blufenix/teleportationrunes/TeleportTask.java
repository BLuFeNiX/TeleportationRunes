package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportTask extends BukkitRunnable {

    // modify these
    private static final int COUNTDOWN_SECONDS = 3;
    private static final int UPDATE_INTERVAL_TICKS = 2;

    // auto-calculated
    // todo de-dupe this calc
    private static final int COUNTDOWN_TICKS = COUNTDOWN_SECONDS * 20; // assumes 20 ticks per second standard server
    private final Callback callback;

    private int elapsedTicks = 0;

    private final Player player;
    private final Location sourceLoc;
    private final Waypoint destWaypoint;
    private final boolean canLeaveArea;

    private SwirlAnimation animation;
    private boolean isRunning;

    TeleportTask(Player player, Location potentialTeleporterLoc, Callback callback) {
        this.player = player;
        this.callback = callback;
        Teleporter teleporter = TeleUtils.getTeleporterNearLocation(potentialTeleporterLoc);
        this.sourceLoc = teleporter != null ? teleporter.loc : null;
        this.destWaypoint = TeleUtils.getWaypointForTeleporter(teleporter);
        canLeaveArea = false;
    }

    TeleportTask(Player player, Signature waypointSignature, Callback callback) {
        this.player = player;
        this.callback = callback;
        this.sourceLoc = player.getLocation();
        this.destWaypoint = TeleUtils.getWaypointForSignature(waypointSignature);
        this.canLeaveArea = true;
    }

    public void execute() {
        if (startTeleportationTask()) {
            isRunning = true;
        } else {
            onSuccessOrFail(false);
        }
    }

    private boolean startTeleportationTask() {
        try {
            if (sourceLoc == null || destWaypoint == null) return false;

            // show the player the cost
            int fee = TeleUtils.calculateFee(destWaypoint.loc, sourceLoc, player);
            int currentExp = ExpUtil.getTotalExperience(player);
            String msg = String.format("%d XP / %d XP", fee, currentExp);
            //player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
            player.sendTitle("", msg);

            // start teleport animation and timer
            animation = SwirlAnimation.getDefault();
            if (canLeaveArea) {
                animation.setLocation(player);
            } else {
                animation.setLocation(sourceLoc.clone().add(Vectors.UP));
            }

            runTaskTimer(TeleportationRunes.getInstance(), 0, UPDATE_INTERVAL_TICKS);
            return true;
        } catch (Exception e) {
            Log.e("error in startTeleportationTask!");
            return false;
        }
    }

    @Override
    public void run() {

        // we haven't actually ticked yet, but if we pass 0 into our animation ticks
        // it will animate a single frame regardless of the interval or fake tick settings
        // TODO is there a cleaner way to fix this and remove the time shift?
        elapsedTicks += UPDATE_INTERVAL_TICKS;

        if (!canLeaveArea && !playerStillAtTeleporter()) {
            player.sendMessage("You left the teleporter area. Cancelling...");
            onSuccessOrFail(false);
            return;
        }

        if (elapsedTicks < COUNTDOWN_TICKS) {
            animation.update(elapsedTicks);
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
    }

    private boolean playerStillAtTeleporter() {
        return player.getLocation().distance(sourceLoc) < 2;
    }

    public static abstract class Callback {
        abstract void onFinished(boolean success);
    }
}