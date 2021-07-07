package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import net.blufenix.common.SimpleDatabase;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;

public class TeleportationRunes extends JavaPlugin implements Listener {

	private static TeleportationRunes _instance;
	private WaypointDB waypointDB;

	private BukkitTask teleportCheckerTask;

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
        Config.init(this);
    }

    @Override
	public void onEnable() {
		super.onEnable();
		if (Config.enabled) {
            getServer().addRecipe(BookOfEnder.getRecipe());
            getServer().addRecipe(ScrollOfWarp.getRecipe());
            SimpleDatabase.Backend backend;
            // use the configured backend
			// else use HSQLDB for FreeBSD and SQLite for everything else
            if (Config.databaseBackend != null) {
            	try {
					backend = SimpleDatabase.Backend.valueOf(Config.databaseBackend);
				} catch (IllegalArgumentException e) {
            		throw new RuntimeException("bad value for databaseBackend in config.yml: "+Config.databaseBackend
							+" (expected one of: "+ Arrays.toString(SimpleDatabase.Backend.values()));
				}
			} else if ("FreeBSD".equals(System.getProperty("os.name"))) {
            	backend = SimpleDatabase.Backend.HSQLDB;
			} else {
				backend = SimpleDatabase.Backend.SQLITE;
			}
			Log.d("Using %s backend for database.", backend);
			waypointDB = new WaypointDB(backend);
			// register event so we can be executed when a player clicks a block
			this.getServer().getPluginManager().registerEvents(this, this);
            teleportCheckerTask = TeleportChecker.start();
			Log.d(StringResources.LOADED);
		}
		else {
			Log.d(StringResources.DISABLED);
			this.getServer().getPluginManager().disablePlugin(this);
		}
	}

	@Override
	public void onDisable() {
		super.onDisable();
        if (teleportCheckerTask != null) teleportCheckerTask.cancel();
        waypointDB.closeConnections();
		this.getLogger().info(StringResources.UNLOADED);
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {

		// only activate on right-click
		Action action = event.getAction();
	    if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
			return;
		}

		Player player = event.getPlayer();
	    boolean holdingBOE = false;
	    boolean holdingSOW = false;

		PlayerInventory inv = player.getInventory();
		ItemStack mainItem = inv.getItemInMainHand();
	    if (mainItem != null) {
            ItemMeta meta = mainItem.getItemMeta();
            if (meta != null) {
                if (BookOfEnder.getMeta().equals(meta)) {
                    holdingBOE = true;
                } else if (ScrollOfWarp.getMeta().getDisplayName().equals(meta.getDisplayName())) {
                    holdingSOW = true;
                }
            }
		}

	    // do nothing if the player isn't holding a book or scroll in their main hand
		if (!holdingBOE && !holdingSOW) {
			return;
		}

		// even if they are holding a key item, do nothing if they are interacting with certain blocks
		// ex: crafting table, hopper, chest, etc.
		Block blockClicked = event.getClickedBlock();
		if (BlockUtil.isPlayerInteractableWithoutSpecialItem(blockClicked)) {
			Log.d("NOT handling right-click event (player interacting with block overrides us)");
			return;
		}

		Log.d("handling right-click event!");

		// we are handling the event, so don't allow block placement (with either hand)
		event.setCancelled(true);

		// ignore the off-hand event, as this listener will run once for each hand
		if (event.getHand() == EquipmentSlot.OFF_HAND) {
			Log.d("ignoring off-hand");
			return;
		}

		// TODO allow clicking on the block directly above as well?
		// pass the player and block location onto a more specific handler
		Location blockLocation = blockClicked != null ? blockClicked.getLocation() : null;
		if (holdingBOE) {
			handleBookOfEnderAction(player, blockLocation);
		} else {
			handleScrollOfWarpAction(player, blockLocation);
		}
	}

	private void handleBookOfEnderAction(Player player, Location blockLocation) {
		Waypoint waypoint = Waypoint.fromLocation(blockLocation);

		if (waypoint != null) {
			switch (waypoint.status) {
				case Waypoint.EXISTS_VERIFIED:
					Log.d("clicked waypoint exists in DB, and signature matches.");
					player.sendMessage(StringResources.WAYPOINT_ALREADY_ACTIVE);
					break;
				case Waypoint.EXISTS_MODIFIED_CONFLICT:
					Log.d("waypoint with this signature already exists, not registering this one");
					player.sendMessage(StringResources.WAYPOINT_SIGNATURE_EXISTS);
					break;
				case Waypoint.EXISTS_MODIFIED:
					// no conflict with new signature
					Log.d("waypoint exists in DB, but signature was altered. updating...");
					// TODO change remove/add to update
					getWaypointDB().removeWaypointByLocation(waypoint.loc);
					getWaypointDB().addWaypoint(waypoint);
					player.sendMessage(StringResources.WAYPOINT_CHANGED);
					break;
				case Waypoint.NOT_EXISTS:
					Log.d("clicked waypoint does not already exist in DB; adding now.");
					waypointDB.addWaypoint(waypoint);
					player.sendMessage(StringResources.WAYPOINT_ACTIVATED);
					break;
			}
		} else if (!DebugMirage.handleMirage(player, blockLocation)) {
			Log.d("neither teleporter nor waypoint clicked");
			player.sendTitle("", "You must click the center of a waypoint...");
		}
	}

	private void handleScrollOfWarpAction(Player player, Location blockLocation) {
		final ItemStack scrollStack = player.getInventory().getItemInMainHand();
		Waypoint waypoint = Waypoint.fromLocation(blockLocation);

		if (waypoint != null) {
			if (waypoint.status == Waypoint.EXISTS_VERIFIED) {
				Log.d("waypoint valid! trying to attune scroll");
				ItemMeta meta = scrollStack.getItemMeta();
				meta.setLore(waypoint.sig.asLore());
				scrollStack.setItemMeta(meta);
				int num = scrollStack.getAmount();
				if (num == 1) {
					player.sendTitle("", "1 scroll attuned...");
				} else {
					player.sendTitle("", num+" scrolls attuned...");
				}
			} else {
				player.sendTitle("", "This waypoint has not been activated...");
			}
		} else { // use scroll
			Signature sig = Signature.fromLore(scrollStack.getItemMeta().getLore());
			if (sig != null) {
				Log.d("starting teleport task...");
				new TeleportTask(player, sig, true, new TeleportTask.Callback() {
					@Override
					void onFinished(boolean success) {
						if (success) {
							// TODO prevent player from throwing scrolls on the ground
							// ex: remove from inventory before teleport, and add back if failure
							scrollStack.setAmount(scrollStack.getAmount() - 1);
						}
					}
				}).execute();
			} else {
				player.sendTitle("", "Scroll not attuned...");
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
				} else if (Config.debug) {
				    if ("mirage".startsWith(args[0])) {
				    	if (args.length == 2) {
							DebugMirage.queueMirage(sender, args[1]);
						} else {
				    		sender.sendMessage(ChatColor.RED+"usage: /mirage <teleporter|waypoint>");
						}
                    }
				}

				return true;
		}

		return false;
	}

}
