package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import net.blufenix.common.SimpleDatabase;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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

import static net.blufenix.common.SimpleDatabase.Backend.HSQLDB;
import static net.blufenix.common.SimpleDatabase.Backend.SQLITE;

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
			MigrationCompat.maybeRelocateDB(); // TODO remove me later
            getServer().addRecipe(BookOfEnder.getRecipe());
            getServer().addRecipe(ScrollOfWarp.getRecipe());
			waypointDB = new WaypointDB(Config.databaseBackend);
			waypointDB.createTables();
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
        if (waypointDB != null) {
			waypointDB.closeConnections();
		}
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
		if (blockClicked != null && blockClicked.getType().isInteractable()) {
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
					// the player has modified a waypoint, so, let's remove the existing record from the DB
					waypointDB.removeWaypointByLocation(waypoint.loc);
					// the rest of this case is now the same as NOT_EXISTS_CONFLICT,
					// so fall through without a break
				case Waypoint.NOT_EXISTS_CONFLICT:
					Waypoint conflictFromDB = waypointDB.getWaypointFromSignature(waypoint.sig);
					Waypoint conflictInWorld = Waypoint.fromLocation(conflictFromDB.loc);
					if (conflictInWorld == null || conflictInWorld.status != Waypoint.EXISTS_VERIFIED) {
						Log.d("conflicting waypoint was altered or removed, so removing that one and registering this one");
						waypointDB.removeWaypointBySignature(conflictFromDB.sig);
						waypointDB.addWaypoint(waypoint);
						player.sendMessage(StringResources.WAYPOINT_ACTIVATED);
						if (Config.consumeBook) {
							player.getInventory().getItemInMainHand().setAmount(0);
						}
					} else {
						// conflicting waypoint still exists and has not been modified
						Log.d("waypoint with this signature already exists, not registering this one");
						player.sendMessage(StringResources.WAYPOINT_SIGNATURE_EXISTS);
					}
					break;
				case Waypoint.EXISTS_MODIFIED:
					// no conflict with new signature
					Log.d("waypoint exists in DB, but signature was altered. updating...");
					// TODO change remove/add to update
					getWaypointDB().removeWaypointByLocation(waypoint.loc);
					getWaypointDB().addWaypoint(waypoint);
					player.sendMessage(StringResources.WAYPOINT_CHANGED);
					if (Config.consumeBook) {
						player.getInventory().getItemInMainHand().setAmount(0);
					}
					break;
				case Waypoint.NOT_EXISTS:
					Log.d("clicked waypoint does not already exist in DB; adding now.");
					waypointDB.addWaypoint(waypoint);
					player.sendMessage(StringResources.WAYPOINT_ACTIVATED);
					if (Config.consumeBook) {
						player.getInventory().getItemInMainHand().setAmount(0);
					}
					break;
			}
		} else if (!DebugMirage.handleMirage(player, blockLocation)) {
			Log.d("neither teleporter nor waypoint clicked");
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("You must click the center of a waypoint..."));
		}
	}

	private void handleScrollOfWarpAction(Player player, Location blockLocation) {
		final ItemStack scrollStack = player.getInventory().getItemInMainHand();
		Signature sig = Signature.fromLore(scrollStack.getItemMeta().getLore());
		Waypoint waypoint = Waypoint.fromLocation(blockLocation);

		if (waypoint != null) {
			if (!Config.allowReattune && sig != null) {
				player.sendMessage(ChatColor.RED+"Cannot re-attune scroll!");
				return;
			}
			String msg;
			if (waypoint.status == Waypoint.EXISTS_VERIFIED) {
				Log.d("waypoint valid! trying to attune scroll");
				ItemMeta meta = scrollStack.getItemMeta();
				meta.setLore(waypoint.sig.asLore());
				scrollStack.setItemMeta(meta);
				int num = scrollStack.getAmount();
				if (num == 1) {
					msg = "1 scroll attuned...";
				} else {
					msg = num+" scrolls attuned...";
				}
			} else {
				msg = "This waypoint has not been activated...";
			}
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
		} else { // use scroll
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
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Scroll not attuned..."));
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
				} else if ("convertdb".startsWith(args[0])) {
                	SimpleDatabase.Backend backend = waypointDB.getBackend();
					SimpleDatabase.Backend targetBackend = backend == SQLITE ? HSQLDB : SQLITE;
					sender.sendMessage(String.format("%sConverting database from %s to %s.\n" +
							"This will NOT delete any existing data, and will NOT activate the new database.\n" +
							"Once conversion is complete, you must set\n    %sdatabaseBackend: %s%s\nin config.yml. " +
							"The old database will remain in plugins/TeleportationRunes/%s/ unless you delete it manually.",
							ChatColor.RED, backend, targetBackend, ChatColor.GOLD, targetBackend, ChatColor.RED, backend));
					if (waypointDB.attemptDatabaseConversion()) {
						sender.sendMessage(ChatColor.RED+"Conversion complete!");
					} else {
						sender.sendMessage(String.format("%sConversion FAILED!\nA database already exists " +
								"in plugins/TeleportationRunes/%s/, and must be moved or deleted manually.",
								ChatColor.RED, targetBackend));
					}
				} else if ("mirage".startsWith(args[0])) {
					if (args.length == 2) {
						DebugMirage.queueMirage(sender, args[1]);
					} else {
						sender.sendMessage(ChatColor.RED+"usage: /tr mirage <teleporter|waypoint>");
					}
				} else {
					sender.sendMessage(ChatColor.RED+"invalid command");
				}

				return true;
		}

		return false;
	}

}
