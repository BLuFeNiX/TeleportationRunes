package net.blufenix.teleportationrunes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.blufenix.common.Serializer;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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

	static TeleportationRunes _instance;
	Map<Signature, LocationWrapper> waypoints;
	
	public void onEnable() {
		_instance = this;
		this.saveDefaultConfig();

		if (this.getConfig().getBoolean("TeleportationRunes.enabled") == true) {
			waypoints = (Map<Signature, LocationWrapper>) Serializer.getSerializedObject(HashMap.class, "plugins/TeleportationRunes/waypoints.dat");
			
			// check for (and remove) broken waypoints
			Iterator<Entry<Signature, LocationWrapper>> it = waypoints.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Signature, LocationWrapper> entry = it.next();
				Location loc = waypoints.get(entry.getKey()).getLoc();
				// remove broken
				if (!isWaypoint(loc.getBlock())) {
					it.remove();
				} // remove changed 
				else if (!entry.getKey().equals(Signature.getSignatureFromLocation(loc))) {
					it.remove();
				}
			}
			
			// register event so we can be executed when a player clicks a block
			this.getServer().getPluginManager().registerEvents(this, this);
			this.getLogger().info("TeleportationRunes has been loaded!");
		}
		else {
			this.getLogger().info("TeleportationRunes is disabled!");
			this.getServer().getPluginManager().disablePlugin(this);
		}
	}
	
	public void onDisable() {
		this.getLogger().info("TeleportationRunes has been unloaded!");
	}
	
	@EventHandler
	public void onPlayerInteractBlock(PlayerInteractEvent event) {
		// only activate on right-click
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
	    
	    Player player = event.getPlayer();
	    Block blockClicked = player.getTargetBlock(null, 4); // only allow selecting blocks 4 blocks away

	    if (isTeleporter(blockClicked)) {
	    	//player.sendMessage("This is a teleporter!");
	    	Signature sig = Signature.getSignatureFromLocation(blockClicked.getLocation());
	    	if (waypoints.containsKey(sig)) { // is there a waypoint matching this teleporter?
	    		LocationWrapper waypointLocation = waypoints.get(sig);
	    		if (isWaypoint(waypointLocation.getLoc().getBlock())) { // make sure the waypoint hasn't been destroyed
	    			if (Signature.getSignatureFromLocation(waypointLocation.getLoc()).equals(sig)) { // make sure the signature hasn't changed
	    				
	    				int currentExp = getTotalExp(player);
	    				double distance = waypointLocation.getLoc().distance(blockClicked.getLocation());
	    				int fee = (int) (Math.ceil(distance/10));
	    				if (currentExp >= fee) {
	    					player.setLevel(0);
		    				player.setExp(0);
		    				player.giveExp(currentExp-fee);
		    				
		    				Location adjustedLoc = waypointLocation.getLoc().add(0.5, 1, 0.5); // teleport to the middle of the block, and one block up
		    				player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
		    				player.teleport(adjustedLoc); 
		    				player.getWorld().strikeLightningEffect(adjustedLoc);
		    				
		    				player.sendMessage("§aTeleportation successful!");
		    				player.sendMessage("§aYou traveled "+((int)distance)+" blocks at the cost of "+fee+" experience points.");
	    				}
	    				else {
	    					player.sendMessage("§cYou do not have enough experience to use this teleporter.");
	    					player.sendMessage("§cYour Exp: "+currentExp);
	    					player.sendMessage("§cExp needed: "+fee);
	    					player.sendMessage("§cDistance: "+((int)distance)+" blocks");
	    				}
	    				
	    			}
	    			else {
	    				player.sendMessage("§cThe waypoint's signature has been altered. Teleporter unlinked.");
	    				waypoints.remove(sig);
	    			}
	    		}
	    		else {
	    			player.sendMessage("§cThe waypoint you desire has been damaged or destroyed. You must repair the waypoint or create a new one.");
	    			waypoints.remove(sig);
	    		}
	    	}
	    	else {
	    		player.sendMessage("§cThere is no waypoint with this signature.");
	    	}
	    }
	    else if (isWaypoint(blockClicked)) {
	    	Signature sig = Signature.getSignatureFromLocation(blockClicked.getLocation());
	    	// register waypoint
	    	if (!waypoints.containsKey(sig)) {
	    		waypoints.put(sig, new LocationWrapper(blockClicked.getLocation()));
	    		Serializer.serializeObject(waypoints, "plugins/TeleportationRunes/waypoints.dat");
	    		player.sendMessage("§aWaypoint activated!");
	    	}
	    	else if (waypoints.get(sig).getLoc().equals(blockClicked.getLocation())) {
	    		player.sendMessage("§cThis waypoint is already active.");
	    	}
	    	else if (!isWaypoint(waypoints.get(sig).getLoc().getBlock()) || !sig.equals(Signature.getSignatureFromLocation(waypoints.get(sig).getLoc()))){
	    		waypoints.put(sig, new LocationWrapper(blockClicked.getLocation()));
	    		Serializer.serializeObject(waypoints, "plugins/TeleportationRunes/waypoints.dat");
	    		player.sendMessage("§aOld waypoint was altered or damaged. New waypoint activated!");
	    	}
	    	else {
	    		player.sendMessage("§cThis waypoint signature has already been used. You must change the signature in order to activate this waypoint.");
	    	}
	    }

	}
	
	private int getTotalExp(Player player) {
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

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("tr")) { // If the player typed /tr then do the following...
			if (sender.isOp()) {
				if (args.length > 0) {
					if (args[0].equalsIgnoreCase("list")) {
						sender.sendMessage("§6--------------------------------------------------");
						for (Entry<Signature, LocationWrapper> entry : waypoints.entrySet()) {
							int n = entry.getKey().getNorth();
							int s = entry.getKey().getSouth();
							int e = entry.getKey().getEast();
							int w = entry.getKey().getWest();
							Location loc = entry.getValue().getLoc();
							sender.sendMessage("§6Rune:");
							sender.sendMessage("   North: "+Material.getMaterial(n)+" ("+n+")");
							sender.sendMessage("   South: "+Material.getMaterial(s)+" ("+s+")");
							sender.sendMessage("   East: "+Material.getMaterial(e)+" ("+e+")");
							sender.sendMessage("   West: "+Material.getMaterial(w)+" ("+w+")");
							sender.sendMessage("   Location: "+loc.getX()+", "+loc.getY()+", "+loc.getZ()+"\n");
						}
						sender.sendMessage("§6--------------------------------------------------");
						return true;
					}
					else if (args[0].equalsIgnoreCase("exp")) {
						sender.sendMessage("INT: "+Integer.parseInt(args[1]));
						((Player)sender).setLevel(0);
						((Player)sender).setExp(0);
						((Player)sender).giveExp(Integer.parseInt(args[1]));
//						((Player)sender).setTotalExperience(Integer.parseInt(args[1]));
						return true;
					}
					else {
						sender.sendMessage("§6Invalid command.");
						return true;
					}
				}
				else {
					sender.sendMessage("§6Specify a command.");
					return true;
				}
			}
			else {
				sender.sendMessage("§6You are not an OP!");
				return true;
			}
		} 
		return false;
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

	public static TeleportationRunes getInstance() {
		return _instance;
	}

}
