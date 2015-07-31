package net.blufenix.teleportationrunes;

import de.congrace.exp4j.Calculable;
import de.congrace.exp4j.ExpressionBuilder;
import net.blufenix.common.JarUtils;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TeleportationRunes extends JavaPlugin implements Listener {

	private static TeleportationRunes _instance;
	private WaypointDB db;

	public static JavaPlugin getInstance() {
		return _instance;
	}

	@SuppressWarnings("unchecked")
	public void onEnable() {
		_instance = this;
		this.saveDefaultConfig();
        JarUtils.loadLibs();

		if (this.getConfig().getBoolean("TeleportationRunes.enabled") == true) {
            db = new WaypointDB();
			
/*			// check for (and remove) broken waypoints
			Iterator<Entry<Signature2, LocationWrapper>> it = waypoints.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Signature2, LocationWrapper> entry = it.next();
				Location loc = waypoints.get(entry.getKey()).getLoc();
				// remove broken
				if (!isWaypoint(loc.getBlock())) {
					it.remove();
				} // remove changed 
				else if (!entry.getKey().equals(Signature2.getSignatureFromLocation(loc))) {
					it.remove();
				}
			}*/
			
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
		
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; } // only activate on right-click
	    if (event.isBlockInHand()) { return; } // don't do anything if the player placed a block
	    
	    Player player = event.getPlayer();
	    
	    if(player.isInsideVehicle()) {
	    	if( !(player.getVehicle() instanceof Horse) ) {
	    		player.sendMessage(ChatColor.RED+"Teleportation failed. You must be on foot or riding a horse to teleport.");
	    		return;
	    	}
	    }
	    
	    Block blockClicked = event.getClickedBlock();
	    Location blockLocation = blockClicked.getLocation();

	    if (isTeleporter(blockClicked)) {
	    	attemptTeleport(player, blockLocation);
	    }
	    else if (isTemporaryTeleporter(blockClicked)) {
	    	// go one block down for temporary teleporters
	    	// since redstone will sit on top
	    	if (attemptTeleport(player, blockLocation.clone().add(0, -1, 0))) { 
	    		// remove redstone
	    		blockClicked.setType(Material.AIR);
	    		blockClicked.getRelative(-1, 0, -1).setType(Material.AIR);
	    		blockClicked.getRelative(-1, 0, 1).setType(Material.AIR);
	    		blockClicked.getRelative(1, 0, -1).setType(Material.AIR);
	    		blockClicked.getRelative(1, 0, 1).setType(Material.AIR);
	    		
	    		blockClicked.getRelative(0, 0, 1).setType(Material.AIR);
	    		blockClicked.getRelative(0, 0, -1).setType(Material.AIR);
	    		blockClicked.getRelative(1, 0, 0).setType(Material.AIR);
	    		blockClicked.getRelative(-1, 0, 0).setType(Material.AIR);
	    	}
	    }
	    else if (isWaypoint(blockClicked)) {
	    	Signature sig = Signature.fromLocation(blockLocation);
	    	// register waypoint
			Waypoint existingWaypoint = db.getWaypointFromSignature(sig);
	    	if (existingWaypoint == null) {
	    		db.addWaypoint(new Waypoint(player.getName(), blockLocation, sig));
	    		player.sendMessage(ChatColor.GREEN+"Waypoint activated!");
	    	}
	    	else if (existingWaypoint.loc.equals(blockLocation)) {
	    		player.sendMessage(ChatColor.RED+"This waypoint is already active.");
	    	}
	    	else if (!isWaypoint(existingWaypoint.loc.getBlock()) || !sig.equals(Signature.fromLocation(existingWaypoint.loc))) {
				// TODO change remove/add to update
				db.removeWaypoint(existingWaypoint);
				db.addWaypoint(new Waypoint(existingWaypoint.user, existingWaypoint.loc, sig));
	    		player.sendMessage(ChatColor.GREEN+"Old waypoint was altered or damaged. New waypoint activated!");
	    	}
	    	else {
	    		player.sendMessage(ChatColor.RED+"This waypoint signature has already been used. You must change the signature in order to activate this waypoint.");
	    	}
	    }

	}
	
	private boolean attemptTeleport(Player player, Location blockLocation) {
    	Signature sig = Signature.fromLocation(blockLocation);
        Waypoint existingWaypoint = db.getWaypointFromSignature(sig);

        // is there a waypoint matching this teleporter?
    	if (existingWaypoint == null) {
    		player.sendMessage(ChatColor.RED+"There is no waypoint with this signature.");
    		return false;
    	}
    	
    	// make sure the waypoint hasn't been destroyed
    	if (!isWaypoint(existingWaypoint.loc.getBlock())) {
			player.sendMessage(ChatColor.RED+"The waypoint you desire has been damaged or destroyed. You must repair the waypoint or create a new one.");
			db.removeWaypoint(existingWaypoint);
			return false;
		}
    		
    	// make sure the signature hasn't changed
    	if (!existingWaypoint.sig.equals(sig)) {
    		player.sendMessage(ChatColor.RED+"The waypoint's signature has been altered. Teleporter unlinked.");
            db.removeWaypoint(existingWaypoint);
			return false;
    	}
    	
    	// make sure teleport destination won't suffocate the player
    	if (!isSafe(existingWaypoint.loc)) {
    		player.sendMessage(ChatColor.RED+"Teleportation failed. Destination is obstructed.");
    		return false;
    	}
    				
    	// is the destination in our current world?
    	if (!existingWaypoint.loc.getWorld().equals(blockLocation.getWorld())) {
    		player.sendMessage(ChatColor.RED+"You cannot teleport between worlds.");
    		return false;
    	}
    	
    	// calculate teleport distance			
    	double distance = existingWaypoint.loc.distance(blockLocation);

    	try {
    		
    		int deltaX = Math.abs(existingWaypoint.loc.getBlockX() - blockLocation.getBlockX());
    		int deltaY = Math.abs(existingWaypoint.loc.getBlockY() - blockLocation.getBlockY());
    		int deltaZ = Math.abs(existingWaypoint.loc.getBlockZ() - blockLocation.getBlockZ());
    		int numEntities = (player.isInsideVehicle() && player.getVehicle() instanceof Horse) ? 2 : 1;

    		String costFormula = this.getConfig().getString("TeleportationRunes.costFormula");

    		Calculable calc = new ExpressionBuilder(costFormula)
    		.withVariable("distance", distance)
    		.withVariable("deltaX", deltaX)
    		.withVariable("deltaY", deltaY)
    		.withVariable("deltaZ", deltaZ)
    		.withVariable("numEntities", numEntities)
    		.build();

    		int fee = (int) Math.ceil(calc.calculate());
    		int currentExp = getTotalExp(player);

    		if (currentExp >= fee) {
    			// subtract EXP
    			player.setLevel(0);
    			player.setExp(0);
    			player.giveExp(currentExp-fee);

    			// teleport player
    			Location playerLoc = player.getLocation();
    			Location adjustedLoc = existingWaypoint.loc.clone().add(0.5, 1, 0.5); // teleport to the middle of the block, and one block up
    			player.getWorld().playEffect(playerLoc, Effect.MOBSPAWNER_FLAMES, 0);
    			
    			if (player.isInsideVehicle() && player.getVehicle() instanceof Horse) {
    				Horse horse = (Horse) player.getVehicle();
    				horse.eject();
    				horse.teleport(adjustedLoc);
    				player.teleport(adjustedLoc);
    				horse.setPassenger(player);
    			}
    			else {
    				player.teleport(adjustedLoc);
    			}
    	
    			player.getWorld().strikeLightningEffect(adjustedLoc);

    			this.getLogger().info(player.getName() + " teleported from " + playerLoc +" to " + adjustedLoc);
    			player.sendMessage(ChatColor.GREEN+"Teleportation successful!");
    			player.sendMessage(ChatColor.GREEN+"You traveled "+((int)distance)+" blocks at the cost of "+fee+" experience points.");
    			return true;
    		}
    		else {
    			player.sendMessage(ChatColor.RED+"You do not have enough experience to use this teleporter.");
    			player.sendMessage(ChatColor.RED+"Your Exp: "+currentExp);
    			player.sendMessage(ChatColor.RED+"Exp needed: "+fee);
    			player.sendMessage(ChatColor.RED+"Distance: "+((int)distance)+" blocks");
    			return false;
    		}
    		
    	} catch (Exception e) {
    		player.sendMessage(ChatColor.RED+"TeleportationRunes cost formula is invalid. Please inform your server administrator.");
    		return false;
    	}
	}

	private boolean isSafe(Location loc) {
		Material block1 = loc.clone().add(0, 1, 0).getBlock().getType();
		Material block2 = loc.clone().add(0, 2, 0).getBlock().getType();
		
		if ((block1.compareTo(Material.AIR) == 0 || block1.compareTo(Material.WATER) == 0)
		 && (block2.compareTo(Material.AIR) == 0 || block2.compareTo(Material.WATER) == 0)) {
			return true;
		}
		
		return false;
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
					if (args[0].equalsIgnoreCase("reload")) {
						this.reloadConfig();
						sender.sendMessage(ChatColor.GOLD+"Teleportation Runes config reloaded!");
						return true;
					}
					else if (args[0].equalsIgnoreCase("list")) {
						sender.sendMessage(ChatColor.GOLD+"--------------------------------------------------");
                        sender.sendMessage("Not yet implemented.");
//						for (Entry<Signature2, LocationWrapper> entry : waypoints.entrySet()) {
//							Material n = entry.getKey().getNorth();
//							Material s = entry.getKey().getSouth();
//							Material e = entry.getKey().getEast();
//							Material w = entry.getKey().getWest();
//							Location loc = entry.getValue().getLoc();
//							sender.sendMessage(ChatColor.GOLD+"Waypoint:");
//							sender.sendMessage("   North: "+n);
//							sender.sendMessage("   South: "+s);
//							sender.sendMessage("   East: "+e);
//							sender.sendMessage("   West: "+w);
//							sender.sendMessage("   Location: "+loc.getX()+", "+loc.getY()+", "+loc.getZ()+"\n");
//						}
						sender.sendMessage(ChatColor.GOLD+"--------------------------------------------------");
						return true;
					}
					else {
						return false;
					}
				}
				else {
					return false;
				}
			}
			else {
				sender.sendMessage(ChatColor.GOLD+"You are not an OP!");
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
		if ((block.getType() == Material.REDSTONE_BLOCK)
		&& (loc.clone().add(-1, 0, -1)).getBlock().getType() == Material.REDSTONE_BLOCK
		&& (loc.clone().add(-1, 0, 1)).getBlock().getType() == Material.REDSTONE_BLOCK
		&& (loc.clone().add(1, 0, -1)).getBlock().getType() == Material.REDSTONE_BLOCK
		&& (loc.clone().add(1, 0, 1)).getBlock().getType() == Material.REDSTONE_BLOCK) {
			return true;
		}
		
		return false;
	}
	
	boolean isTemporaryTeleporter(Block block) {
		Location loc = block.getLocation();
		if ((block.getType() == Material.REDSTONE_WIRE)
		&& (loc.clone().add(-1, 0, -1)).getBlock().getType() == Material.REDSTONE_WIRE
		&& (loc.clone().add(-1, 0, 1)).getBlock().getType() == Material.REDSTONE_WIRE
		&& (loc.clone().add(1, 0, -1)).getBlock().getType() == Material.REDSTONE_WIRE
		&& (loc.clone().add(1, 0, 1)).getBlock().getType() == Material.REDSTONE_WIRE
		
		&& (loc.clone().add(0, 0, 1)).getBlock().getType() == Material.REDSTONE_WIRE
		&& (loc.clone().add(1, 0, 0)).getBlock().getType() == Material.REDSTONE_WIRE
		&& (loc.clone().add(0, 0, -1)).getBlock().getType() == Material.REDSTONE_WIRE
		&& (loc.clone().add(-1, 0, 0)).getBlock().getType() == Material.REDSTONE_WIRE) {
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
		if ((block.getType() == Material.LAPIS_BLOCK)
		&& (loc.clone().add(-1, 0, -1)).getBlock().getType() == Material.LAPIS_BLOCK
		&& (loc.clone().add(-1, 0, 1)).getBlock().getType() == Material.LAPIS_BLOCK
		&& (loc.clone().add(1, 0, -1)).getBlock().getType() == Material.LAPIS_BLOCK
		&& (loc.clone().add(1, 0, 1)).getBlock().getType() == Material.LAPIS_BLOCK) {
			return true;
		}
				
		return false;
	}

}
