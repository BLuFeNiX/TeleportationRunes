package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TeleportChecker extends BukkitRunnable {

    // normally this would need to be thread safe, but minecraft only runs on one thread
    private static Map<Player, Location> playersCurrentlyTeleporting = new HashMap<>(); // location is teleporter location

    public static BukkitTask start() {
        return new TeleportChecker().runTaskTimer(TeleportationRunes.getInstance(), 0, 20);
    }

    @Override
    public void run() {
        Collection<? extends Player> players = TeleportationRunes.getInstance().getServer().getOnlinePlayers();
        for (final Player p: players) {
            try {
                if (playersCurrentlyTeleporting.containsKey(p)) continue;
                final Location potentialTeleporterLoc = p.getLocation().add(Vectors.DOWN);

                playersCurrentlyTeleporting.put(p, potentialTeleporterLoc);

                new TeleportTask(p, potentialTeleporterLoc, new TeleportTask.Callback() {
                    @Override
                    void onFinished() {
                        playersCurrentlyTeleporting.remove(p);
                    }
                }).execute();

            } catch (Throwable t) {
                TeleportationRunes.getInstance().getLogger().warning("whoops! "+t.getMessage());
                t.printStackTrace();
            }
        }
    }

}