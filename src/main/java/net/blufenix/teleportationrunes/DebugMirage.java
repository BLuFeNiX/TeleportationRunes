package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class DebugMirage {

    // normally these would need to be thread safe, but minecraft only runs on one thread
    private static Set<Player> playersPendingWaypointMirage = new HashSet<>();
    private static Set<Player> playersPendingTeleporterMirage = new HashSet<>();

    public static void queueMirage(CommandSender sender, String mirageType) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Must be a player to show mirage!");
        } else if ("teleporter".startsWith(mirageType)) {
            playersPendingTeleporterMirage.add((Player) sender);
            sender.sendMessage(ChatColor.GOLD + "Ready to show teleporter!");
        } else if ("waypoint".startsWith(mirageType)) {
            playersPendingWaypointMirage.add((Player) sender);
            sender.sendMessage(ChatColor.GOLD + "Ready to show waypoint!");
        }
    }

    public static boolean handleMirage(Player player, Location loc) {
        if (playersPendingWaypointMirage.remove(player)) {
            Log.d("showing waypoint mirage!");
            BlockUtil.showMirage(player, loc, Config.waypointBlueprint.atRotation(0));
            return true;
        } else if (playersPendingTeleporterMirage.remove(player)) {
            Log.d("showing teleporter mirage!");
            BlockUtil.showMirage(player, loc, Config.teleporterBlueprint.atRotation(0));
            return true;
        }

        return false;
    }
}
