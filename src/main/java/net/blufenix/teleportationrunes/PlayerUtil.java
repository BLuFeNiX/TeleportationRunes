package net.blufenix.teleportationrunes;

import org.bukkit.entity.Player;

/**
 * Created by blufenix on 8/1/15.
 */
public class PlayerUtil {

    public static int getTotalExp(Player player) {
        double level = player.getLevel()+player.getExp();
        int exp = 0;
        if (level < 16) {
            exp = (int) (Math.round(level*17));
        }
        else if (level < 31) {
            exp = (int) (Math.round(1.5*(level*level)-(29.5*level)+360));
        }
        else {
            exp = (int) (Math.round(3.5*(level*level)-(151.5*level)+2220));
        }
        return exp;
    }

}
