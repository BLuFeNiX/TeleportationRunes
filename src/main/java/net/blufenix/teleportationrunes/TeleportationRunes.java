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

    @Override
    public void onLoad() {
        super.onLoad();
        _instance = this;
        JarUtils.loadLibs();
        Config.init(this);
    }

	public void onEnable() {
		if (Config.enabled) {
            db = new WaypointDB();
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
        db.closeConnections();
		this.getLogger().info(StringResources.UNLOADED);
	}
	
	@EventHandler
	public void onPlayerInteractBlock(PlayerInteractEvent event) {
		
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; } // only activate on right-click
	    if (event.isBlockInHand()) { return; } // don't do anything if the player placed a block
	    
	    Player player = event.getPlayer();
	    
	    if(player.isInsideVehicle()) {
	    	if( !(player.getVehicle() instanceof Horse) ) {
	    		player.sendMessage(StringResources.BAD_VEHICLE);
	    		return;
	    	}
	    }
	    
	    Block blockClicked = event.getClickedBlock();
	    Location blockLocation = blockClicked.getLocation();

	    if (BlockUtil.isTeleporter(blockClicked)) {
	    	attemptTeleport(player, blockLocation);
	    }
	    else if (BlockUtil.isTemporaryTeleporter(blockClicked)) {
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
	    else if (BlockUtil.isWaypoint(blockClicked)) {
	    	Signature sig = Signature.fromLocation(blockLocation);
	    	// register waypoint
			Waypoint existingWaypoint = db.getWaypointFromSignature(sig);
	    	if (existingWaypoint == null) {
	    		db.addWaypoint(new Waypoint(player.getName(), blockLocation, sig));
	    		player.sendMessage(StringResources.WAYPOINT_ACTIVATED);
	    	}
	    	else if (existingWaypoint.loc.equals(blockLocation)) {
	    		player.sendMessage(StringResources.WAYPOINT_ALREADY_ACTIVE);
	    	}
	    	else if (!BlockUtil.isWaypoint(existingWaypoint.loc.getBlock()) || !sig.equals(Signature.fromLocation(existingWaypoint.loc))) {
				// TODO change remove/add to update
				db.removeWaypoint(existingWaypoint);
				db.addWaypoint(new Waypoint(existingWaypoint.user, existingWaypoint.loc, sig));
	    		player.sendMessage(StringResources.WAYPOINT_CHANGED);
	    	}
	    	else {
	    		player.sendMessage(StringResources.WAYPOINT_SIGNATURE_EXISTS);
	    	}
	    }

	}
	
	private boolean attemptTeleport(Player player, Location blockLocation) {
    	Signature sig = Signature.fromLocation(blockLocation);
        Waypoint existingWaypoint = db.getWaypointFromSignature(sig);

        // is there a waypoint matching this teleporter?
    	if (existingWaypoint == null) {
    		player.sendMessage(StringResources.WAYPOINT_NOT_FOUND);
    		return false;
    	}
    	
    	// make sure the waypoint hasn't been destroyed
    	if (!BlockUtil.isWaypoint(existingWaypoint.loc.getBlock())) {
			player.sendMessage(StringResources.WAYPOINT_DAMAGED);
			db.removeWaypoint(existingWaypoint);
			return false;
		}
    		
    	// make sure the signature hasn't changed
    	if (!existingWaypoint.sig.equals(sig)) {
    		player.sendMessage(StringResources.WAYPOINT_ALTERED);
            db.removeWaypoint(existingWaypoint);
			return false;
    	}
    	
    	// make sure teleport destination won't suffocate the player
    	if (!BlockUtil.isSafe(existingWaypoint.loc)) {
    		player.sendMessage(StringResources.WAYPOINT_OBSTRUCTED);
    		return false;
    	}
    				
    	// is the destination in our current world?
    	if (!existingWaypoint.loc.getWorld().equals(blockLocation.getWorld())) {
    		player.sendMessage(StringResources.WAYPOINT_DIFFERENT_WORLD);
    		return false;
    	}
    	
    	// calculate teleport distance			
    	double distance = existingWaypoint.loc.distance(blockLocation);

    	try {
    		
    		int deltaX = Math.abs(existingWaypoint.loc.getBlockX() - blockLocation.getBlockX());
    		int deltaY = Math.abs(existingWaypoint.loc.getBlockY() - blockLocation.getBlockY());
    		int deltaZ = Math.abs(existingWaypoint.loc.getBlockZ() - blockLocation.getBlockZ());
    		int numEntities = (player.isInsideVehicle() && player.getVehicle() instanceof Horse) ? 2 : 1;

    		Calculable calc = new ExpressionBuilder(Config.costFormula)
    		.withVariable("distance", distance)
    		.withVariable("deltaX", deltaX)
    		.withVariable("deltaY", deltaY)
    		.withVariable("deltaZ", deltaZ)
    		.withVariable("numEntities", numEntities)
    		.build();

    		int fee = (int) Math.ceil(calc.calculate());
    		int currentExp = PlayerUtil.getTotalExp(player);

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
