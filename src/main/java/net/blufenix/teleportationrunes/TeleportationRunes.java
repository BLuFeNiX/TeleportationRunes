package net.blufenix.teleportationrunes;

import net.blufenix.common.JarUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class TeleportationRunes extends JavaPlugin implements Listener {

	private static final boolean DEBUG = true;

	private static TeleportationRunes _instance;
	private WaypointDB waypointDB;

	// normally these would need to be thread safe, but minecraft only runs on one thread.
    // ahahahahah hahah hah :(
	private static Set<Player> playersPendingWaypointMirage = new HashSet<>();
	private static Set<Player> playersPendingTeleporterMirage = new HashSet<>();
	private static Map<Player, Location> playersCurrentlyTeleporting = new HashMap<>(); // location is teleporter location

    ItemMeta BOE_META;

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
            addRecipes();
            waypointDB = new WaypointDB();
			// register event so we can be executed when a player clicks a block
			this.getServer().getPluginManager().registerEvents(this, this);
			// uncomment to auto-teleport
            task = new TeleportCheckerTask().runTaskTimer(this, 0, 20);
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


	private void addRecipes() {
        ItemStack bookOfEnder = new ItemStack(Material.ENCHANTED_BOOK, 1);
        BOE_META = bookOfEnder.getItemMeta();
        BOE_META.setDisplayName("Book of Ender");
        BOE_META.setLore(Arrays.asList("Bending spacetime has never been easier!"));
        bookOfEnder.setItemMeta(BOE_META);

        ShapelessRecipe boeRecipe = new ShapelessRecipe(new NamespacedKey(this, "book_of_ender"), bookOfEnder);

        boeRecipe.addIngredient(Material.BOOK);
        boeRecipe.addIngredient(Material.ENDER_PEARL);

        getServer().addRecipe(boeRecipe);
    }

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
                    TeleportationRunes.getInstance().getLogger().warning("whoops! "+t.getMessage());
					t.printStackTrace();
                }
            }
        }
    }

    private static class SwirlAnimation {

    	private int duration = 20; // ticks
    	private int segments;
    	private double radius;
    	private boolean useFakeTicks;
    	private Location location;
    	private World world;
    	private Particle[] particles;
    	private int numParticles;
		private boolean repeat;

		private double degreesPerTick;
		private int lastElapsedTicks = -1;

		private int totalRotationsDegrees = 360;

		private Vector[][] compiledVectors;

		private static Vector[][] defaultCompiledVectors;

		public static SwirlAnimation getDefault() {

			SwirlAnimation anim = new SwirlAnimation()
					.setDuration(20*3)
					.setParticle(Particle.SPELL_WITCH)
					.setRadius(5)
					.setSegments(3)
					.setNumParticles(10)
					.enableFakeTicks(true)
					.enableRepeat(true)
					.setRotations(2);

			if (defaultCompiledVectors != null) {
				anim.setCompiledVectors(defaultCompiledVectors);
			} else {
				defaultCompiledVectors = SwirlAnimation.compile(anim);
			}

			return anim;
		}

		public SwirlAnimation setDuration(int duration) {
			this.duration = duration;
			this.degreesPerTick = (double) totalRotationsDegrees/duration;
			return this;
		}

		public SwirlAnimation setSegments(int segments) {
			this.segments = segments;
			return this;
		}

		public SwirlAnimation setRadius(double radius) {
			this.radius = radius;
			return this;
		}

		public SwirlAnimation enableFakeTicks(boolean useFakeTicks) {
			this.useFakeTicks = useFakeTicks;
			return this;
		}

		public SwirlAnimation setLocation(Location location) {
			this.location = location;
			this.world  = location.getWorld();
			return this;
		}

		public SwirlAnimation setParticle(Particle... particles) {
			this.particles = particles;
			return this;
		}

		public SwirlAnimation setNumParticles(int numParticles) {
			this.numParticles = numParticles;
			return this;
		}

		public SwirlAnimation setRotations(double rotations) {
			this.totalRotationsDegrees = (int) (360 * rotations);
			this.degreesPerTick = (double) totalRotationsDegrees/duration;
			return this;
		}

		public SwirlAnimation enableRepeat(boolean repeat) {
			this.repeat = repeat;
			return this;
		}

		public void update(int elapsedTicks) {
			if (repeat && elapsedTicks > duration) {
				elapsedTicks %= duration;
				lastElapsedTicks = -1;
			} else if (elapsedTicks > duration) {
				TeleportationRunes.getInstance().getLogger().warning("animation finished. not ticking!");
				return;
			}

			if (useFakeTicks) {
				for (int fakeTicks = lastElapsedTicks + 1; fakeTicks <= elapsedTicks; fakeTicks++) {
					onTick(fakeTicks);
				}
			} else {
				onTick(elapsedTicks);
			}

			lastElapsedTicks = elapsedTicks;
		}

		private void onTick(int tick) {
			for (int segment = 0; segment < compiledVectors[tick].length; segment++) {
				for (Particle p : particles) {
					world.spawnParticle(p, location.clone().add(compiledVectors[tick][segment]), numParticles, null);
				}
			}
		}

		public static Vector[][] compile(SwirlAnimation anim) {
			Vector[][] compiledVectors = new Vector[anim.duration][];
			for (int tick = 0; tick < anim.duration; tick++) {
				compiledVectors[tick] = compileTick(anim, tick);
			}
			anim.compiledVectors = compiledVectors;
			return compiledVectors;
		}

		private static Vector[] compileTick(SwirlAnimation anim, int tick) {
			Vector[] vectors = new Vector[anim.segments];
			for (int segment = 0; segment < anim.segments; segment++) {
				double segmentOffset = ((double)360/anim.segments) * segment;
				double radians = Math.toRadians(((double) tick * anim.degreesPerTick) + segmentOffset);
				double r = anim.radius * (1 - ((double) tick / anim.duration));
				double xPos = r * Math.cos(radians);
				double zPos = r * Math.sin(radians);
				vectors[segment] = new Vector(xPos, 0, zPos);
			}
			return vectors;
		}

		public void setCompiledVectors(Vector[][] compiledVectors) {
			this.compiledVectors = compiledVectors;
		}
	}

	static class TeleportTask extends BukkitRunnable {

    	static {

		}

		// modify these
		private static final int COUNTDOWN_SECONDS = 3;
		private static final int UPDATE_INTERVAL_TICKS = 2;

		// auto-calculated
		// todo de-dupe this calc
		private static final int COUNTDOWN_TICKS = COUNTDOWN_SECONDS * 20; // assumes 20 ticks per second standard server

		private int elapsedTicks = 0;

		private final Player player;
		private final Location potentialTeleporterLoc;

		private Teleporter teleporter;
		private Waypoint waypoint;


		private SwirlAnimation animation;

		TeleportTask(Player player, Location potentialTeleporterLoc) {
			this.player = player;
			this.potentialTeleporterLoc = potentialTeleporterLoc;
		}

		public void execute() {
			this.teleporter = TeleUtils.getTeleporterNearLocation(potentialTeleporterLoc);
			if (teleporter != null) {
				this.waypoint = TeleUtils.getWaypointForTeleporter(teleporter);
				if (waypoint != null) {
                    this.animation = SwirlAnimation.getDefault();
                    animation.setLocation(teleporter.loc.clone().add(Vectors.UP));
                    this.runTaskTimer(TeleportationRunes.getInstance(), 0, UPDATE_INTERVAL_TICKS);
                    return; // success
                }
			}

			// if we made it here, the it wasn't a teleporter, or it had no waypoint
			playersCurrentlyTeleporting.remove(player);
		}

		@Override
		public void run() {

			// we haven't actually ticked yet, but if we pass 0 into our animation ticks
			// it will animate a single frame regardless of the interval or fake tick settings
			// TODO is there a cleaner way to fix this and remove the time shift?
			elapsedTicks += UPDATE_INTERVAL_TICKS;

			if (!playerStillAtTeleporter()) {
				player.sendMessage("You left the teleporter area. Cancelling...");
				onSuccessOrFail();
				return;
			}

			if (elapsedTicks < COUNTDOWN_TICKS) {
				animation.update(elapsedTicks);
				//if (elapsedTicks % 20 == 0) player.sendMessage("Teleporting in " + ((COUNTDOWN_TICKS-elapsedTicks)/20) + "...");
			} else {
				TeleUtils.attemptTeleport(player, teleporter.loc, waypoint);
				onSuccessOrFail();
			}

		}

		private void onSuccessOrFail() {
			playersCurrentlyTeleporting.remove(player);
			this.cancel();
		}

		private boolean playerStillAtTeleporter() {
			return player.getLocation().distance(potentialTeleporterLoc) < 2;
		}
	}

	@EventHandler
	public void onPlayerInteractBlock(PlayerInteractEvent event) {

		if (DEBUG) this.getLogger().info("in onPlayerInteractBlock()");

		// only activate on right-click
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			if (DEBUG) this.getLogger().info("player did not right click; returning.");
			return;
		}

		Block blockClicked = event.getClickedBlock();

		// ignore off-hand click (two events per click now :P)
		if (event.getHand() == EquipmentSlot.OFF_HAND) {
			if (DEBUG) this.getLogger().info("ignoring off-hand click");
			return;
		}

		// don't do anything unless the player is holding a book
	    if (!event.getPlayer().getInventory().getItemInMainHand().getItemMeta().equals(BOE_META)) {
			if (DEBUG) this.getLogger().info("player not holding book; returning.");
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
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		switch (cmd.getName()) {
            case "tr":
                if (args.length == 0) {
                    return false;
                }
                else if (!sender.isOp()) {
                    sender.sendMessage(ChatColor.GOLD+"You are not an OP!");
				} else if ("reload".startsWith(args[0])) {
                    Config.reload();
					sender.sendMessage(ChatColor.GOLD+"Teleportation Runes config reloaded!");
				} else if (DEBUG) {
				    if ("mirage".startsWith(args[0])) {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.RED + "Must be a player to show mirage!");
                        }
                        if ("teleporter".startsWith(args[1])) {
                            playersPendingTeleporterMirage.add((Player) sender);
                            sender.sendMessage(ChatColor.GOLD + "Ready to show teleporter!");
                        } else if ("waypoint".startsWith(args[1])) {
                            playersPendingWaypointMirage.add((Player) sender);
                            sender.sendMessage(ChatColor.GOLD + "Ready to show waypoint!");
                        }
                    }
				}

				return true;
		}

		return false;
	}

}
