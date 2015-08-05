package net.blufenix.teleportationrunes;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by blufenix on 7/31/15.
 */
public class Config {

    public static boolean enabled;
    public static String costFormula;
    public static Material tempTeleporterMaterial;

    public static Blueprint teleporterBlueprint;
    public static Blueprint waypointBlueprint;

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
        tempTeleporterMaterial = Material.matchMaterial(config.getString("TeleportationRunes.tempTeleporterMaterial"));

        // create blueprints
        ConfigurationSection blueprintMaterialsConfig = config.getConfigurationSection("TeleportationRunes.blueprint.materials");
        ConfigurationSection teleporterBlueprintConfig = config.getConfigurationSection("TeleportationRunes.blueprint.teleporter");
        ConfigurationSection waypointBlueprintConfig = config.getConfigurationSection("TeleportationRunes.blueprint.waypoint");

        teleporterBlueprint = new Blueprint(teleporterBlueprintConfig, blueprintMaterialsConfig);
        waypointBlueprint = new Blueprint(waypointBlueprintConfig, blueprintMaterialsConfig);
    }

    public static void reload() {
        plugin.reloadConfig();
        load();
    }
}
