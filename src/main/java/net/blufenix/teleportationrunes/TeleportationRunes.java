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
import org.bukkit.plugin.java.JavaPlugin;

public class TeleportationRunes extends JavaPlugin implements Listener {

	private static TeleportationRunes _instance;
	private WaypointDB waypointDB;

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
		
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; } // only activate on right-click
	    if (event.isBlockInHand()) { return; } // don't do anything if the player placed a block
	    
	    Player player = event.getPlayer();
	    
	    Block blockClicked = event.getClickedBlock();
	    Location blockLocation = blockClicked.getLocation();

        // TODO cancel event if teleporter/waypoint was clicked?
	    if (BlockUtil.isTeleporter(blockClicked)) {
            TeleUtils.attemptTeleport(player, blockLocation);
	    }
	    else if (BlockUtil.isWaypoint(blockClicked)) {
	    	Signature sig = Signature.fromLocation(blockLocation, Config.waypointBlueprint);
	    	// register waypoint
			Waypoint existingWaypoint = waypointDB.getWaypointFromSignature(sig);
	    	if (existingWaypoint == null) {
	    		waypointDB.addWaypoint(new Waypoint(player.getName(), blockLocation, sig));
	    		player.sendMessage(StringResources.WAYPOINT_ACTIVATED);
	    	}
	    	else if (existingWaypoint.loc.equals(blockLocation)) {
	    		player.sendMessage(StringResources.WAYPOINT_ALREADY_ACTIVE);
	    	}
	    	else if (!sig.equals(Signature.fromLocation(existingWaypoint.loc, Config.waypointBlueprint))) {
				// TODO change remove/add to update
				waypointDB.removeWaypoint(existingWaypoint);
				waypointDB.addWaypoint(new Waypoint(existingWaypoint.user, existingWaypoint.loc, sig));
	    		player.sendMessage(StringResources.WAYPOINT_CHANGED);
	    	}
	    	else {
	    		player.sendMessage(StringResources.WAYPOINT_SIGNATURE_EXISTS);
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
                } else if (args[0].equalsIgnoreCase("reload")) {
                    Config.reload();
                    sender.sendMessage(ChatColor.GOLD+"Teleportation Runes config reloaded!");
                    return true;
                } else {
                    return false;
                }
		}

		return false;
	}

}
