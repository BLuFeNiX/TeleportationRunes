package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

public class TeleportChecker extends BukkitRunnable {

    public static BukkitTask start() {
        return new TeleportChecker().runTaskTimer(TeleportationRunes.getInstance(), 0, 20);
    }

    @Override
    public void run() {
        Collection<? extends Player> players = TeleportationRunes.getInstance().getServer().getOnlinePlayers();
        for (final Player p: players) {
            try {
                // todo we could recycle this object, since this runs all the time
                new TeleportTask(p, p.getLocation().add(Vectors.DOWN), null).execute();
            } catch (Throwable t) {
                Log.e("whoops!", t);
            }
        }
    }

}