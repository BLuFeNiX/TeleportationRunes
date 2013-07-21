package net.blufenix.TeleportationRunes;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TeleportationRunes extends JavaPlugin implements Listener {

	Map<Signature, Location> waypoints = new HashMap<Signature, Location>();
	
	public void onEnable() {
		getLogger().info("TeleportationRunes has been loaded!");
		//TODO iterate through waypoints to remove bad ones
		// register event so we can be executed when a player clicks a block
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	public void onDisable() {
		getLogger().info("TeleportationRunes has been unloaded!");
	}
	
	@EventHandler
	public void onPlayerInteractBlock(PlayerInteractEvent event) {
	    Player player = event.getPlayer();
	    // only let BLuFeNiX use the plugin
	    if (!player.getName().equals("BLuFeNiX")) { return; }
	    
	    Block blockClicked = player.getTargetBlock(null, 4); // only allow selecting blocks 4 blocks away

	    if (isTeleporter(blockClicked)) {
	    	player.sendMessage("This is a teleporter!");
	    	Signature sig = Signature.getSignatureFromLocation(blockClicked.getLocation());
	    	if (waypoints.containsKey(sig)) { // is there a waypoint matching this teleporter?
	    		Location waypointLocation = waypoints.get(sig);
	    		if (isWaypoint(waypointLocation.getBlock()) // make sure the waypoint hasn't been destroyed
	    			&& Signature.getSignatureFromLocation(waypointLocation).equals(sig)) { // make sure the signature hasn't changed
		    		player.teleport(waypointLocation.clone().add(0.5, 1, 0.5)); // teleport to the middle of the block, and one block up
		    		player.getWorld().strikeLightningEffect(waypointLocation);
		    		player.sendMessage("Teleport!");
	    		}
	    		else {
	    			player.sendMessage("The waypoint you desire has been altered or destroyed.");
	    			//waypoints.remove(sig);
	    		}
	    	}
	    	else {
	    		player.sendMessage("There is no waypoint with this signature.");
	    	}
	    }
	    else if (isWaypoint(blockClicked)) {
	    	Signature sig = Signature.getSignatureFromLocation(blockClicked.getLocation());
	    	// register waypoint
	    	if (!waypoints.containsKey(sig)) {
	    		waypoints.put(sig, blockClicked.getLocation().clone());
	    		player.sendMessage("Waypoint activated!");
	    	}
	    	else if (waypoints.get(sig).equals(blockClicked.getLocation())) {
	    		player.sendMessage("This waypoint is already active.");
	    	}
	    	else {
	    		player.sendMessage("This waypoint signature has already been used.");
	    	}
	    }

	}
	
	/*
	 * Checks blocks for pattern:
	 * 		|R|?|R|
	 * 		|?|R|?|
	 * 		|R|?|R|
	 */
	boolean isTeleporter(Block block) {
		Location loc = block.getLocation();
		if ((block.getType().getId() == Material.REDSTONE_BLOCK.getId())
		&& (loc.clone().add(-1, 0, -1)).getBlock().getType().getId() == Material.REDSTONE_BLOCK.getId()
		&& (loc.clone().add(-1, 0, 1)).getBlock().getType().getId() == Material.REDSTONE_BLOCK.getId()
		&& (loc.clone().add(1, 0, -1)).getBlock().getType().getId() == Material.REDSTONE_BLOCK.getId()
		&& (loc.clone().add(1, 0, 1)).getBlock().getType().getId() == Material.REDSTONE_BLOCK.getId()) {
			return true;
		}
		
		return false;
	}
	
	/*
	 * Checks blocks for pattern:
	 * 		|L|?|L|
	 * 		|?|L|?|
	 * 		|L|?|L|
	 */
	boolean isWaypoint(Block block) {
		Location loc = block.getLocation();
		if ((block.getType().getId() == Material.LAPIS_BLOCK.getId())
		&& (loc.clone().add(-1, 0, -1)).getBlock().getType().getId() == Material.LAPIS_BLOCK.getId()
		&& (loc.clone().add(-1, 0, 1)).getBlock().getType().getId() == Material.LAPIS_BLOCK.getId()
		&& (loc.clone().add(1, 0, -1)).getBlock().getType().getId() == Material.LAPIS_BLOCK.getId()
		&& (loc.clone().add(1, 0, 1)).getBlock().getType().getId() == Material.LAPIS_BLOCK.getId()) {
			return true;
		}
				
		return false;
	}

}
