package net.blufenix.teleportationrunes;

import net.blufenix.common.JarUtils;
import net.blufenix.common.Log;
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
        JarUtils.loadLibs();
        Config.init(this);
    }

    @Override
	public void onEnable() {
		super.onEnable();
		if (Config.enabled) {
            getServer().addRecipe(BookOfEnder.getRecipe());
            getServer().addRecipe(ScrollOfWarp.getRecipe());
            waypointDB = new WaypointDB();
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
        teleportCheckerTask.cancel();
        waypointDB.closeConnections();
		this.getLogger().info(StringResources.UNLOADED);
	}

	@EventHandler
	public void onPlayerInteractBlock(PlayerInteractEvent event) {

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

		if (!holdingBOE && !holdingSOW) {
			return;
		}

		Block blockClicked = event.getClickedBlock();
		if (BlockUtil.isPlayerInteractableWithoutSpecialItem(blockClicked)) {
			Log.d("NOT handling right-click event (player interacting with block overrides us)");
			return;
		}

		Log.d("handling right-click event!");

		// we are handling the event, so don't allow block placement (with either hand)
		event.setCancelled(true);

		if (event.getHand() == EquipmentSlot.OFF_HAND) {
			Log.d("ignoring off-hand");
			return;
		}

	    Location blockLocation = blockClicked.getLocation();

		int rotation;
	    if (holdingBOE) {
			if ((rotation = BlockUtil.isTeleporter(blockClicked)) >= 0) {
				Log.d("teleporter clicked!");
				TeleUtils.attemptTeleport(player, blockLocation, rotation);
			} else if ((rotation = BlockUtil.isWaypoint(blockClicked)) >= 0) {
				Log.d("waypoint clicked!");
				Signature sig = Signature.fromLocation(blockLocation, Config.waypointBlueprint.atRotation(rotation));
				// register waypoint
				Waypoint existingWaypoint = waypointDB.getWaypointFromSignature(sig);
				if (existingWaypoint == null) {
					Log.d("clicked waypoint does not already exist in DB; adding now.");
					waypointDB.addWaypoint(new Waypoint(player.getName(), blockLocation, sig));
					player.sendMessage(StringResources.WAYPOINT_ACTIVATED);
				} else if (existingWaypoint.loc.equals(blockLocation)) {
					Log.d("clicked waypoint exists in DB, and location matches.");
					player.sendMessage(StringResources.WAYPOINT_ALREADY_ACTIVE);
				} else if (!sig.equals(Signature.fromLocation(existingWaypoint.loc, Config.waypointBlueprint.atRotation(rotation)))) {
					Log.d("waypoint exists in DB, but blocks were altered. removing old waypoint and adding this one.");
					// TODO change remove/add to update
					waypointDB.removeWaypoint(existingWaypoint);
					waypointDB.addWaypoint(new Waypoint(existingWaypoint.user, existingWaypoint.loc, sig));
					player.sendMessage(StringResources.WAYPOINT_CHANGED);
				} else {
					Log.d("waypoint with this signature already exists, not registering this one");
					player.sendMessage(StringResources.WAYPOINT_SIGNATURE_EXISTS);
				}
			} else if (!DebugMirage.handleMirage(player, blockLocation)) {
				Log.d("neither teleporter nor waypoint clicked");
				player.sendTitle("", "You must click the center of a waypoint...");
			}
		} else { // we know holdingSOW is true
	    	// attune scroll
			if ((rotation = BlockUtil.isWaypoint(blockClicked)) >= 0) {
				Log.d("waypoint clicked (scroll in hand)!");
				Signature sig = Signature.fromLocation(blockLocation, Config.waypointBlueprint.atRotation(rotation));
				Waypoint existingWaypoint = waypointDB.getWaypointFromSignature(sig);
				if (existingWaypoint.loc.equals(blockLocation)) {
					Log.d("waypoint valid! trying to attune scroll");
					ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
					meta.setLore(Arrays.asList(sig.getEncoded()));
					player.getInventory().getItemInMainHand().setItemMeta(meta);
				}
			} else { // use scroll
				final ItemStack scrollStack = player.getInventory().getItemInMainHand();
				Signature sig = Signature.fromEncoded(scrollStack.getItemMeta().getLore().get(0));
				new TeleportTask(player, sig, new TeleportTask.Callback() {
					@Override
					void onFinished(boolean success) {
						if (success) {
							scrollStack.setAmount(scrollStack.getAmount()-1);
						}
					}
				}).execute();
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
