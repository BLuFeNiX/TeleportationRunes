package net.blufenix.teleportationrunes;

import net.blufenix.common.JarUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class TeleportationRunes extends JavaPlugin implements Listener {

	private static final boolean DEBUG = false;

	private static TeleportationRunes _instance;
	private WaypointDB waypointDB;

	// normally these would need to be thread safe, but minecraft only runs on one thread.
    // ahahahahah hahah hah :(
	private Set<Player> playersPendingWaypointMirage = new HashSet<>();
	private Set<Player> playersPendingTeleporterMirage = new HashSet<>();

	public static TeleportationRunes getInstance() {
		return _instance;
	}

	public WaypointDB getWaypointDB() {
		return waypointDB;
	}

    @Override
    public void onLoad() {
        super.onLoad();
        _instance = this;
        JarUtils.loadLibs();
        Config.init(this);
    }

	public void onEnable() {
		if (Config.enabled) {
            waypointDB = new WaypointDB();
			// register event so we can be executed when a player clicks a block
			this.getServer().getPluginManager().registerEvents(this, this);
			this.getLogger().info(StringResources.LOADED);
		}
		else {
			this.getLogger().info(StringResources.DISABLED);
			this.getServer().getPluginManager().disablePlugin(this);
		}
	}
	
	public void onDisable() {
        waypointDB.closeConnections();
		this.getLogger().info(StringResources.UNLOADED);
	}

	@EventHandler
	public void onPlayerInteractBlock(PlayerInteractEvent event) {

		if (DEBUG) this.getLogger().info("in onPlayerInteractBlock()");

		// only activate on right-click
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			if (DEBUG) this.getLogger().info("player did not right click; returning.");
			return;
		}

		// ignore off-hand click (two events per click now :P)
		if (event.getHand() == EquipmentSlot.OFF_HAND) {
			if (DEBUG) this.getLogger().info("ignoring off-hand click");
			return;
		}

		// don't do anything if the player placed a block
	    if (event.isBlockInHand()) {
			if (DEBUG) this.getLogger().info("player placed a block; returning.");
			return;
		}
	    
	    Player player = event.getPlayer();
	    
	    Block blockClicked = event.getClickedBlock();
	    Location blockLocation = blockClicked.getLocation();

        // TODO cancel event if teleporter/waypoint was clicked? (as in, cancel other plugins' events?)
		int rotation;
	    if ( (rotation = BlockUtil.isTeleporter(blockClicked)) >= 0) {
			if (DEBUG) this.getLogger().info("teleporter clicked!");
            TeleUtils.attemptTeleport(player, blockLocation, rotation);
	    }
	    else if ( (rotation = BlockUtil.isWaypoint(blockClicked)) >= 0) {
			if (DEBUG) this.getLogger().info("waypoint clicked!");
			Signature sig = Signature.fromLocation(blockLocation, Config.waypointBlueprint.atRotation(rotation));
	    	// register waypoint
			Waypoint existingWaypoint = waypointDB.getWaypointFromSignature(sig);
	    	if (existingWaypoint == null) {
				if (DEBUG) this.getLogger().info("clicked waypoint does not already exist in DB; adding now.");
	    		waypointDB.addWaypoint(new Waypoint(player.getName(), blockLocation, sig));
	    		player.sendMessage(StringResources.WAYPOINT_ACTIVATED);
	    	}
	    	else if (existingWaypoint.loc.equals(blockLocation)) {
				if (DEBUG) this.getLogger().info("clicked waypoint exists in DB, and location matches.");
	    		player.sendMessage(StringResources.WAYPOINT_ALREADY_ACTIVE);
	    	}
	    	else if (!sig.equals(Signature.fromLocation(existingWaypoint.loc, Config.waypointBlueprint.atRotation(rotation)))) {
                if (DEBUG) this.getLogger().info("waypoint exists in DB, but blocks were altered. removing old waypoint and adding this one.");
                // TODO change remove/add to update
                waypointDB.removeWaypoint(existingWaypoint);
                waypointDB.addWaypoint(new Waypoint(existingWaypoint.user, existingWaypoint.loc, sig));
                player.sendMessage(StringResources.WAYPOINT_CHANGED);
            }
            else {
                if (DEBUG) this.getLogger().info("waypoint with this signature already exists, not registering this one");
                player.sendMessage(StringResources.WAYPOINT_SIGNATURE_EXISTS);
            }
	    }
		else if (DEBUG) {
	        this.getLogger().info("neither teleporter nor waypoint clicked");
            if (playersPendingWaypointMirage.remove(player)) {
				this.getLogger().info("showing waypoint mirage!");
				BlockUtil.showMirage(player, blockClicked, Config.waypointBlueprint.atRotation(0));
			} else if (playersPendingTeleporterMirage.remove(player)) {
				this.getLogger().info("showing teleporter mirage!");
				BlockUtil.showMirage(player, blockClicked, Config.teleporterBlueprint.atRotation(0));
			}
		}

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		switch (cmd.getName()) {
            case "tr":
                if (args.length == 0) {
                    return false;
                }
                else if (!sender.isOp()) {
                    sender.sendMessage(ChatColor.GOLD+"You are not an OP!");
                    return true;
				} else if ("reload".startsWith(args[0])) {
                    Config.reload();
					sender.sendMessage(ChatColor.GOLD+"Teleportation Runes config reloaded!");
					return true;
				} else if (DEBUG) {
				    if ("mirage".startsWith(args[0])) {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.RED + "Must be a player to show mirage!");
                            return true;
                        }
                        if ("teleporter".startsWith(args[1])) {
                            playersPendingTeleporterMirage.add((Player) sender);
                            sender.sendMessage(ChatColor.GOLD + "Ready to show teleporter!");
                            return true;
                        } else if ("waypoint".startsWith(args[1])) {
                            playersPendingWaypointMirage.add((Player) sender);
                            sender.sendMessage(ChatColor.GOLD + "Ready to show waypoint!");
                            return true;
                        }
                    }
				}
		}

		return false;
	}

}
