package net.blufenix.teleportationrunes;

import org.bukkit.entity.Player;

/*
    Derived from: https://gist.github.com/RichardB122/8958201b54d90afbc6f0
*/

public class ExpUtil {

    public static int getTotalExperience(Player player) {
        int experience;
        int level = player.getLevel();
        float expTowardsNextLevel = player.getExp();
        int requiredExperience;

        if (level >= 0 && level <= 15) {
            experience = (int) Math.ceil(Math.pow(level, 2) + (6 * level));
            requiredExperience = 2 * level + 7;
        } else if (level > 15 && level <= 30) {
            experience = (int) Math.ceil((2.5 * Math.pow(level, 2) - (40.5 * level) + 360));
            requiredExperience = 5 * level - 38;
        } else {
            experience = (int) Math.ceil(((4.5 * Math.pow(level, 2) - (162.5 * level) + 2220)));
            requiredExperience = 9 * level - 158;
        }

        experience += Math.ceil(expTowardsNextLevel * requiredExperience);
        return experience;
    }

}