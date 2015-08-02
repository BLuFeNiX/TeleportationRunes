package net.blufenix.teleportationrunes;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by blufenix on 7/31/15.
 */
public class Config {

    public static boolean enabled;
    public static String costFormula;
    public static Material teleporterMaterial;
    public static Material tempTeleporterMaterial;
    public static Material waypointMaterial;

    private static JavaPlugin plugin;

    public static void init(JavaPlugin plugin) {
        Config.plugin = plugin;
        plugin.saveDefaultConfig();
        load();
    }

    private static void load() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("TeleportationRunes.enabled");
        costFormula = config.getString("TeleportationRunes.costFormula");
        teleporterMaterial = Material.matchMaterial(config.getString("TeleportationRunes.teleporterMaterial"));
        tempTeleporterMaterial = Material.matchMaterial(config.getString("TeleportationRunes.tempTeleporterMaterial"));
        waypointMaterial = Material.matchMaterial(config.getString("TeleportationRunes.waypointMaterial"));
    }

    public static void reload() {
        plugin.reloadConfig();
        load();
    }
}
