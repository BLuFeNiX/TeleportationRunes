package net.blufenix.teleportationrunes;

import net.blufenix.common.JarUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Logger;

public class TeleportationRunes extends JavaPlugin implements Listener {

	private static final boolean DEBUG = true;

	private static TeleportationRunes _instance;
	private WaypointDB waypointDB;

	// normally these would need to be thread safe, but minecraft only runs on one thread.
    // ahahahahah hahah hah :(
	private static Set<Player> playersPendingWaypointMirage = new HashSet<>();
	private static Set<Player> playersPendingTeleporterMirage = new HashSet<>();
	private static Map<Player, Location> playersCurrentlyTeleporting = new HashMap<>(); // location is teleporter location

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


    private BukkitTask task;
    @Override
	public void onEnable() {
		if (Config.enabled) {
            waypointDB = new WaypointDB();
			// register event so we can be executed when a player clicks a block
			this.getServer().getPluginManager().registerEvents(this, this);
			// uncomment to auto-teleport
//            task = new TeleportCheckerTask().runTaskTimer(this, 0, 20);
			this.getLogger().info(StringResources.LOADED);
		}
		else {
			this.getLogger().info(StringResources.DISABLED);
			this.getServer().getPluginManager().disablePlugin(this);
		}
	}

	@Override
	public void onDisable() {
        task.cancel();
        waypointDB.closeConnections();
		this.getLogger().info(StringResources.UNLOADED);
	}

//	private void addRecipes() {
//        ItemStack wand = new ItemStack(Material.REDSTONE_TORCH_ON, 1);
//        ItemMeta meta = wand.getItemMeta();
//        meta.setDisplayName("Magic Wand");
//        meta.setLore(Arrays.asList("A message is here, but is it the one you need?"));
//        wand.setItemMeta(meta);
//
//        ShapedRecipe wandRecipe = new ShapedRecipe(new NamespacedKey(this, "wand"), wand);
//
//        wandRecipe.shape("*","l","l");
//
//        wandRecipe.setIngredient('*', Material.REDSTONE);
//        wandRecipe.setIngredient('l', Material.STICK);
//
//        getServer().addRecipe(wandRecipe);
//    }

	static class TeleportCheckerTask extends BukkitRunnable {

        @Override
        public void run() {
            Collection<? extends Player> players = TeleportationRunes.getInstance().getServer().getOnlinePlayers();
            for (Player p: players) {
                try {
                    if (playersCurrentlyTeleporting.containsKey(p)) continue;
                    Location normalizedPlayerLoc = p.getLocation().clone();
                    normalizedPlayerLoc.setX(normalizedPlayerLoc.getBlockX());
                    normalizedPlayerLoc.setY(normalizedPlayerLoc.getBlockY());
                    normalizedPlayerLoc.setZ(normalizedPlayerLoc.getBlockZ());
                    normalizedPlayerLoc.add(Vectors.CENTER);
                    Location potentialTeleporterLoc = normalizedPlayerLoc.clone().add(Vectors.DOWN);

                    playersCurrentlyTeleporting.put(p, potentialTeleporterLoc);

                    new TeleportTask(p, potentialTeleporterLoc).execute();
                } catch (Throwable t) {
                    TeleportationRunes.getInstance().getLogger().warning("Whoops! -- "+t.getMessage());
                }
            }
        }
    }

	static class TeleportTask extends BukkitRunnable {

		private static final int COUNTDOWN_SECONDS = 5;
		private static final int COUNTDOWN_TICKS = COUNTDOWN_SECONDS * 20; // assumes 20 ticks per second standard server
		private static final int UPDATE_INTERVAL_TICKS = 5;

		// start with negative ticks, so we can update the ticks at the start of every run
		// will be 0 on first run
		private int elapsedTicks = -UPDATE_INTERVAL_TICKS;

		private final Player player;
		private final Location teleporterLoc;
		private final World departingWorld;

		private Teleporter teleporter;
		private Waypoint waypoint;

		private Location particleEffectLoc;

		TeleportTask(Player player, Location teleporterLoc) {
			this.player = player;
			this.teleporterLoc = teleporterLoc;
			this.departingWorld = teleporterLoc.getWorld();
			this.particleEffectLoc = teleporterLoc.clone().add(Vectors.UP); // one block above teleporter
		}

		public void execute() {
            this.teleporter = TeleUtils.getTeleporterFromLocation(teleporterLoc);
            if (teleporter != null) {
                this.waypoint = TeleUtils.getWaypointForTeleporter(teleporter);
                this.runTaskTimer(TeleportationRunes.getInstance(), 0, UPDATE_INTERVAL_TICKS);
            } else {
                // player not standing on a teleporter
                playersCurrentlyTeleporting.remove(player);
            }
		}

		@Override
		public void run() {
			elapsedTicks += UPDATE_INTERVAL_TICKS;

			if (!playerStillAtTeleporter()) {
                player.sendMessage("You left the teleporter area. Cancelling...");
                onSuccessOrFail();
                return;
            }

			if (elapsedTicks < COUNTDOWN_TICKS) {
				departingWorld.spawnParticle(Particle.PORTAL,
						particleEffectLoc.clone().add(0, Math.random()*2, 0), /* location of particles */
						(int) (Math.random() * 1000), /* number of particles */
						null);
				if (elapsedTicks % 20 == 0) player.sendMessage("Teleporting in " + ((COUNTDOWN_TICKS-elapsedTicks)/20) + "...");
			} else {
				TeleUtils.attemptTeleport(player, teleporterLoc, waypoint);
				onSuccessOrFail();
			}
		}

		private void onSuccessOrFail() {
            playersCurrentlyTeleporting.remove(player);
            this.cancel();
        }

        private boolean playerStillAtTeleporter() {
            return player.getLocation().distance(teleporterLoc) < 2;
        }
	}

	/*

	TODO
	* player must have spell book in off-hand and light fire on block to activate?
	* attempt teleport when movement onto correct area detected? (radius size?)
	* notify player via message? animation/effect? how long should it take?
	* must have iron, gold, diamond, emerald, something from nether, or bedrock as CORE?
	*
	*

	 */

	@EventHandler
	public void onPlayerInteractBlock(PlayerInteractEvent event) {

		long time = System.nanoTime();

		if (DEBUG) this.getLogger().info("in onPlayerInteractBlock()");

		// only activate on right-click
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			if (DEBUG) this.getLogger().info("player did not right click; returning.");
            long time2 = System.nanoTime();
//            event.getPlayer().sendMessage("time: "+((time2-time)/1000000f)+" ms");
			return;
		}

		Block blockClicked = event.getClickedBlock();

		// ignore off-hand click (two events per click now :P)
		if (event.getHand() == EquipmentSlot.OFF_HAND) {
			if (DEBUG) this.getLogger().info("ignoring off-hand click");
			// don't place off-hand blocks on structures
			if (BlockUtil.isWaypoint(blockClicked) >= 0 || BlockUtil.isTeleporter(blockClicked) >= 0) {
				event.setCancelled(true);
			}
//            long time2 = System.nanoTime();
//            event.getPlayer().sendMessage("time: "+((time2-time)/1000000f)+" ms");
			return;
		}

		// don't do anything if the player placed a block
	    if (event.isBlockInHand()) {
			if (DEBUG) this.getLogger().info("player placed a block; returning.");
            long time2 = System.nanoTime();
//            event.getPlayer().sendMessage("time: "+((time2-time)/1000000f)+" ms");
			return;
		}
	    
	    Player player = event.getPlayer();
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

//		long time2 = System.nanoTime();
//	    player.sendMessage("time: "+((time2-time)/1000000f)+" ms");

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
