package net.blufenix.teleportationrunes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.blufenix.common.JarUtils;
import net.blufenix.common.Serializer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import de.congrace.exp4j.Calculable;
import de.congrace.exp4j.ExpressionBuilder;

public class TeleportationRunes extends JavaPlugin implements Listener {

	private static TeleportationRunes _instance;
	private Map<Signature, LocationWrapper> waypoints;
	
	public void onEnable() {
		_instance = this;
		this.saveDefaultConfig();
        loadLibs();

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
	
    private void loadLibs() {
    	try {
            
        	final File[] libs = new File[] {
                    new File(getDataFolder(), "exp4j-0.3.10.jar")
            };
            
            for (final File lib : libs) {
                if (!lib.exists()) {
                    JarUtils.extractFromJar(lib.getName(), lib.getAbsolutePath());
                }
            }
            
            for (final File lib : libs) {
                if (!lib.exists()) {
                    getLogger().warning("There was a critical error loading TeleportationRunes!" +
                    		" Could not find lib: " + lib.getName());
                    Bukkit.getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                addClassPath(JarUtils.getJarUrl(lib));
            }
            
        } catch (final Exception e) {
            e.printStackTrace();
        }
	}

	private void addClassPath(final URL url) throws IOException {
        final URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        final Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            final Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { url });
        } catch (final Throwable t) {
            t.printStackTrace();
            throw new IOException("Error adding " + url + " to system classloader");
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
	    Location blockLocation = blockClicked.getLocation();

	    if (isTeleporter(blockClicked)) {

	    	// is there a waypoint matching this teleporter?
	    	Signature sig = Signature.getSignatureFromLocation(blockLocation);
	    	if (!waypoints.containsKey(sig)) { 
	    		player.sendMessage(ChatColor.RED+"There is no waypoint with this signature.");
	    		return;
	    	}
	    	
	    	// make sure the waypoint hasn't been destroyed
	    	LocationWrapper waypointLocation = waypoints.get(sig);
	    	if (!isWaypoint(waypointLocation.getLoc().getBlock())) { 
    			player.sendMessage(ChatColor.RED+"The waypoint you desire has been damaged or destroyed. You must repair the waypoint or create a new one.");
    			waypoints.remove(sig);
    			return;
    		}
	    		
	    	// make sure the signature hasn't changed
	    	if (!Signature.getSignatureFromLocation(waypointLocation.getLoc()).equals(sig)) {
	    		player.sendMessage(ChatColor.RED+"The waypoint's signature has been altered. Teleporter unlinked.");
				waypoints.remove(sig);
				return;
	    	}
	    	
	    	// make sure teleport destination won't suffocate the player
	    	if (!isSafe(waypointLocation.getLoc())) {
	    		player.sendMessage(ChatColor.RED+"Teleportation failed. Destination is obstructed.");
	    		return;
	    	}
	    				
	    	// is the destination in our current world?
	    	if (!waypointLocation.getWorldName().equals(blockLocation.getWorld().getName())) {
	    		player.sendMessage(ChatColor.RED+"You cannot teleport between worlds.");
	    		return;
	    	}
	    	
	    	// calculate teleport distance			
	    	double distance = waypointLocation.getLoc().distance(blockLocation);

	    	try {
	    		
	    		int deltaX = Math.abs(waypointLocation.getLoc().getBlockX() - blockLocation.getBlockX());
	    		int deltaY = Math.abs(waypointLocation.getLoc().getBlockY() - blockLocation.getBlockY());
	    		int deltaZ = Math.abs(waypointLocation.getLoc().getBlockZ() - blockLocation.getBlockZ());

	    		String costFormula = this.getConfig().getString("TeleportationRunes.costFormula");

	    		Calculable calc = new ExpressionBuilder(costFormula)
	    		.withVariable("distance", distance)
	    		.withVariable("deltaX", deltaX)
	    		.withVariable("deltaY", deltaY)
	    		.withVariable("deltaZ", deltaZ)
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
	    			Location adjustedLoc = waypointLocation.getLoc().add(0.5, 1, 0.5); // teleport to the middle of the block, and one block up
	    			player.getWorld().playEffect(playerLoc, Effect.MOBSPAWNER_FLAMES, 0);
	    			player.teleport(adjustedLoc); 
	    			player.getWorld().strikeLightningEffect(adjustedLoc);

	    			this.getLogger().info(player.getName() + " teleported from " + playerLoc +" to " + adjustedLoc);
	    			player.sendMessage(ChatColor.GREEN+"Teleportation successful!");
	    			player.sendMessage(ChatColor.GREEN+"You traveled "+((int)distance)+" blocks at the cost of "+fee+" experience points.");
	    		}
	    		else {
	    			player.sendMessage(ChatColor.RED+"You do not have enough experience to use this teleporter.");
	    			player.sendMessage(ChatColor.RED+"Your Exp: "+currentExp);
	    			player.sendMessage(ChatColor.RED+"Exp needed: "+fee);
	    			player.sendMessage(ChatColor.RED+"Distance: "+((int)distance)+" blocks");
	    		}
	    		
	    	} catch (Exception e) {
	    		player.sendMessage(ChatColor.RED+"TeleportationRunes cost formula is invalid. Please inform your server administrator.");
	    	}
	    	
	    }
	    else if (isWaypoint(blockClicked)) {
	    	Signature sig = Signature.getSignatureFromLocation(blockLocation);
	    	// register waypoint
	    	if (!waypoints.containsKey(sig)) {
	    		waypoints.put(sig, new LocationWrapper(blockLocation));
	    		Serializer.serializeObject(waypoints, "plugins/TeleportationRunes/waypoints.dat");
	    		player.sendMessage(ChatColor.GREEN+"Waypoint activated!");
	    	}
	    	else if (waypoints.get(sig).getLoc().equals(blockLocation)) {
	    		player.sendMessage(ChatColor.RED+"This waypoint is already active.");
	    	}
	    	else if (!isWaypoint(waypoints.get(sig).getLoc().getBlock()) || !sig.equals(Signature.getSignatureFromLocation(waypoints.get(sig).getLoc()))){
	    		waypoints.put(sig, new LocationWrapper(blockLocation));
	    		Serializer.serializeObject(waypoints, "plugins/TeleportationRunes/waypoints.dat");
	    		player.sendMessage(ChatColor.GREEN+"Old waypoint was altered or damaged. New waypoint activated!");
	    	}
	    	else {
	    		player.sendMessage(ChatColor.RED+"This waypoint signature has already been used. You must change the signature in order to activate this waypoint.");
	    	}
	    }

	}
	
	private boolean isSafe(Location loc) {
		int block1 = loc.clone().add(0, 1, 0).getBlock().getTypeId();
		int block2 = loc.clone().add(0, 2, 0).getBlock().getTypeId();
		
		if ((block1 == Material.AIR.getId() || block1 == Material.WATER.getId())
		 && (block2 == Material.AIR.getId() || block2 == Material.WATER.getId())) {
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
						for (Entry<Signature, LocationWrapper> entry : waypoints.entrySet()) {
							int n = entry.getKey().getNorth();
							int s = entry.getKey().getSouth();
							int e = entry.getKey().getEast();
							int w = entry.getKey().getWest();
							Location loc = entry.getValue().getLoc();
							sender.sendMessage(ChatColor.GOLD+"Waypoint:");
							sender.sendMessage("   North: "+Material.getMaterial(n)+" ("+n+")");
							sender.sendMessage("   South: "+Material.getMaterial(s)+" ("+s+")");
							sender.sendMessage("   East: "+Material.getMaterial(e)+" ("+e+")");
							sender.sendMessage("   West: "+Material.getMaterial(w)+" ("+w+")");
							sender.sendMessage("   Location: "+loc.getX()+", "+loc.getY()+", "+loc.getZ()+"\n");
						}
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
		if ((block.getTypeId() == Material.REDSTONE_BLOCK.getId())
		&& (loc.clone().add(-1, 0, -1)).getBlock().getTypeId() == Material.REDSTONE_BLOCK.getId()
		&& (loc.clone().add(-1, 0, 1)).getBlock().getTypeId() == Material.REDSTONE_BLOCK.getId()
		&& (loc.clone().add(1, 0, -1)).getBlock().getTypeId() == Material.REDSTONE_BLOCK.getId()
		&& (loc.clone().add(1, 0, 1)).getBlock().getTypeId() == Material.REDSTONE_BLOCK.getId()) {
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
		if ((block.getTypeId() == Material.LAPIS_BLOCK.getId())
		&& (loc.clone().add(-1, 0, -1)).getBlock().getTypeId() == Material.LAPIS_BLOCK.getId()
		&& (loc.clone().add(-1, 0, 1)).getBlock().getTypeId() == Material.LAPIS_BLOCK.getId()
		&& (loc.clone().add(1, 0, -1)).getBlock().getTypeId() == Material.LAPIS_BLOCK.getId()
		&& (loc.clone().add(1, 0, 1)).getBlock().getTypeId() == Material.LAPIS_BLOCK.getId()) {
			return true;
		}
				
		return false;
	}

	public static TeleportationRunes getInstance() {
		return _instance;
	}

}
