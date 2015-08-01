package net.blufenix.teleportationrunes;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by blufenix on 7/31/15.
 */
public class Config {

    public static boolean enabled;
    public static String costFormula;

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
    }

    public static void reload() {
        plugin.reloadConfig();
        load();
    }
}
