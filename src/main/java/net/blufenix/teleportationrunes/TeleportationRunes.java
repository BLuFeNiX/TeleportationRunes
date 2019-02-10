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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

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

	private static EquipmentSlot getBOEHand(Player player) {
		PlayerInventory inv = player.getInventory();
		ItemStack mainItem = inv.getItemInMainHand();
		ItemStack offItem = inv.getItemInOffHand();

		if (mainItem != null && BookOfEnder.getMeta().equals(mainItem.getItemMeta())) {
			return EquipmentSlot.HAND;
		} else if (offItem != null && BookOfEnder.getMeta().equals(offItem.getItemMeta())) {
			return EquipmentSlot.OFF_HAND;
		} else {
			return null;
		}
	}

	@EventHandler
	public void onPlayerInteractBlock(PlayerInteractEvent event) {

		// only activate on right-click
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		Player player = event.getPlayer();
		EquipmentSlot boeHand = getBOEHand(player);

		if (boeHand != EquipmentSlot.HAND) {
			return;
		}

		Log.d("handling right-click event!");

		// we are handling the event, so don't allow block placement (with either hand)
		event.setCancelled(true);

		if (event.getHand() == EquipmentSlot.OFF_HAND) {
			Log.d("ignoring off-hand");
			return;
		}

		Block blockClicked = event.getClickedBlock();
	    Location blockLocation = blockClicked.getLocation();

		int rotation;
	    if ( (rotation = BlockUtil.isTeleporter(blockClicked)) >= 0) {
			Log.d("teleporter clicked!");
            TeleUtils.attemptTeleport(player, blockLocation, rotation);
	    }
	    else if ( (rotation = BlockUtil.isWaypoint(blockClicked)) >= 0) {
			Log.d("waypoint clicked!");
			Signature sig = Signature.fromLocation(blockLocation, Config.waypointBlueprint.atRotation(rotation));
	    	// register waypoint
			Waypoint existingWaypoint = waypointDB.getWaypointFromSignature(sig);
	    	if (existingWaypoint == null) {
				Log.d("clicked waypoint does not already exist in DB; adding now.");
	    		waypointDB.addWaypoint(new Waypoint(player.getName(), blockLocation, sig));
	    		player.sendMessage(StringResources.WAYPOINT_ACTIVATED);
	    	}
	    	else if (existingWaypoint.loc.equals(blockLocation)) {
				Log.d("clicked waypoint exists in DB, and location matches.");
	    		player.sendMessage(StringResources.WAYPOINT_ALREADY_ACTIVE);
	    	}
	    	else if (!sig.equals(Signature.fromLocation(existingWaypoint.loc, Config.waypointBlueprint.atRotation(rotation)))) {
				Log.d("waypoint exists in DB, but blocks were altered. removing old waypoint and adding this one.");
                // TODO change remove/add to update
                waypointDB.removeWaypoint(existingWaypoint);
                waypointDB.addWaypoint(new Waypoint(existingWaypoint.user, existingWaypoint.loc, sig));
                player.sendMessage(StringResources.WAYPOINT_CHANGED);
            }
            else {
				Log.d("waypoint with this signature already exists, not registering this one");
                player.sendMessage(StringResources.WAYPOINT_SIGNATURE_EXISTS);
            }
	    }
		else {
			if (!DebugMirage.handleMirage(player, blockLocation)) {
				Log.d("neither teleporter nor waypoint clicked");
				player.sendTitle("", "You must click the center of a waypoint...");
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
