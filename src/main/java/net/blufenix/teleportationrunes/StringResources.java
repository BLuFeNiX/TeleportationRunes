package net.blufenix.teleportationrunes;

import org.bukkit.ChatColor;

/**
 * Created by blufenix on 7/31/15.
 */
public class StringResources {

    public static final String LOADED = "TeleportationRunes has been loaded!";
    public static final String UNLOADED = "TeleportationRunes has been unloaded!";
    public static final String DISABLED = "TeleportationRunes is disabled!";

    public static final String WAYPOINT_ACTIVATED = ChatColor.GREEN+"Waypoint activated!";
    public static final String WAYPOINT_ALREADY_ACTIVE = ChatColor.RED+"This waypoint is already active.";
    public static final String WAYPOINT_CHANGED = ChatColor.GREEN+"Waypoint signature updated!";
    public static final String WAYPOINT_SIGNATURE_EXISTS = ChatColor.RED+"This waypoint signature has already been used. You must change the signature in order to activate this waypoint.";
    public static final String WAYPOINT_NOT_FOUND = ChatColor.RED+"There is no waypoint with this signature.";
    public static final String WAYPOINT_DAMAGED = ChatColor.RED+"The waypoint you desire has been damaged or destroyed. You must repair the waypoint or create a new one.";
    public static final String WAYPOINT_ALTERED = ChatColor.RED+"The waypoint's signature has been altered. Teleporter unlinked.";
    public static final String WAYPOINT_OBSTRUCTED = ChatColor.RED+"Teleportation failed. Destination is obstructed.";
    public static final String WAYPOINT_DIFFERENT_WORLD = ChatColor.RED+"You cannot teleport between worlds.";
}
