package net.blufenix.teleportationrunes;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
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
        }
        if ("teleporter".startsWith(mirageType)) {
            playersPendingTeleporterMirage.add((Player) sender);
            sender.sendMessage(ChatColor.GOLD + "Ready to show teleporter!");
        } else if ("waypoint".startsWith(mirageType)) {
            playersPendingWaypointMirage.add((Player) sender);
            sender.sendMessage(ChatColor.GOLD + "Ready to show waypoint!");
        }
    }

    public static void handleMirage(Player player, Block blockClicked) {
        if (playersPendingWaypointMirage.remove(player)) {
            TeleportationRunes.getInstance().getLogger().info("showing waypoint mirage!");
            BlockUtil.showMirage(player, blockClicked, Config.waypointBlueprint.atRotation(0));
        } else if (playersPendingTeleporterMirage.remove(player)) {
            TeleportationRunes.getInstance().getLogger().info("showing teleporter mirage!");
            BlockUtil.showMirage(player, blockClicked, Config.teleporterBlueprint.atRotation(0));
        }
    }
}
