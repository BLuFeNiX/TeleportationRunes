package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import net.blufenix.common.SimpleDatabase;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

import static net.blufenix.common.SimpleDatabase.Backend.HSQLDB;
import static net.blufenix.common.SimpleDatabase.Backend.SQLITE;

/**
 * Created by blufenix on 7/31/15.
 */
public class Config {

    public static boolean enabled;
    public static boolean debug;
    public static String costFormula;
    public static boolean costXpInCreative;
    public static boolean enableRotation;
    public static SimpleDatabase.Backend databaseBackend;

    public static Blueprint teleporterBlueprint;
    public static Blueprint waypointBlueprint;
    public static boolean consumeBook;
    public static int numScrollsCrafted;
    public static boolean allowReattune;
    public static List<String> bookOfEnderRecipeList;
    public static List<String> scrollOfWarpRecipeList;
    public static boolean enableLightningEffect;
    public static boolean enableEnderTeleportEffect;
    public static String teleportDelayFormula;
    public static SwirlAnimation particleAnimation;
    public static boolean particleAnimationEnabled;
    public static int animationDutyCycle;
    public static boolean allowTeleportBetweenWorlds;

    private static JavaPlugin plugin;

    public static void init(JavaPlugin plugin) {
        Config.plugin = plugin;
        plugin.saveDefaultConfig();
        load();
    }

    private static void load() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("TeleportationRunes.enabled");
        debug = config.getBoolean("TeleportationRunes.debug");
        costFormula = config.getString("TeleportationRunes.costFormula");
        costXpInCreative = config.getBoolean("TeleportationRunes.costXpInCreative", false);
        allowTeleportBetweenWorlds = config.getBoolean("TeleportationRunes.allowTeleportBetweenWorlds", true);
        enableRotation = config.getBoolean("TeleportationRunes.enableRotation");
        enableLightningEffect = config.getBoolean("TeleportationRunes.enableLightningEffect", false);
        enableEnderTeleportEffect = config.getBoolean("TeleportationRunes.enableEnderTeleportEffect", true);
        databaseBackend = detectDatabaseBackend(config);
        if (databaseBackend == null) {
            Log.e("bad value for databaseBackend in config.yml, expected one of: %s",
                    Arrays.toString(SimpleDatabase.Backend.values()));
            Log.e("plugin disabled!");
            enabled = false;
        }
        consumeBook = config.getBoolean("TeleportationRunes.consumeBook", false);
        numScrollsCrafted = config.getInt("TeleportationRunes.numScrollsCrafted", 4);
        allowReattune = config.getBoolean("TeleportationRunes.allowReattune", true);
        teleportDelayFormula = config.getString("TeleportationRunes.teleportDelayFormula", "60");
        particleAnimation = createAnimation(config);
        particleAnimationEnabled = config.getBoolean("TeleportationRunes.particleAnimation.enabled", true);
        if (particleAnimationEnabled) {
            animationDutyCycle = config.getInt("TeleportationRunes.particleAnimation.dutyCycle", 1);
        } else {
            // no sense in wasting CPU cycles if we are not animating, so just make this 10 ticks
            animationDutyCycle = 10;
        }

        // create blueprints
        ConfigurationSection blueprintMaterialsConfig = config.getConfigurationSection("TeleportationRunes.blueprint.materials");
        ConfigurationSection teleporterBlueprintConfig = config.getConfigurationSection("TeleportationRunes.blueprint.teleporter");
        ConfigurationSection waypointBlueprintConfig = config.getConfigurationSection("TeleportationRunes.blueprint.waypoint");

        teleporterBlueprint = new Blueprint(teleporterBlueprintConfig, blueprintMaterialsConfig);
        waypointBlueprint = new Blueprint(waypointBlueprintConfig, blueprintMaterialsConfig);

        bookOfEnderRecipeList = config.getStringList("TeleportationRunes.bookOfEnder.recipe");
        if (bookOfEnderRecipeList.size() == 0) {
            bookOfEnderRecipeList.add(Material.BOOK.name());
            bookOfEnderRecipeList.add(Material.ENDER_PEARL.name());
        }

        scrollOfWarpRecipeList = config.getStringList("TeleportationRunes.scrollOfWarp.recipe");
        if (scrollOfWarpRecipeList.size() == 0) {
            scrollOfWarpRecipeList.add(Material.PAPER.name());
            scrollOfWarpRecipeList.add(Material.ENDER_PEARL.name());
        }
    }

    private static SwirlAnimation createAnimation(FileConfiguration config) {
        ConfigurationSection animConf = config.getConfigurationSection("TeleportationRunes.particleAnimation");
        if (animConf !=  null) {
            SwirlAnimation anim = new SwirlAnimation()
                    .setParticle(Particle.valueOf(animConf.getString("particleType", "SPELL_WITCH")))
                    .setRadius(animConf.getDouble("radius", 5))
                    .setSegments(animConf.getInt("segments", 3))
                    .setNumParticles(animConf.getInt("particles", 10))
                    .enableFakeTicks(animConf.getBoolean("animateAllFrames", false))
                    .enableRepeat(true)
                    .setRotations(animConf.getDouble("rotations", 2))
                    .setCycles(animConf.getInt("cycles", 1))
                    .compile();
            return anim;
        } else {
            return SwirlAnimation.getDefault();
        }
    }

    public static void reload() {
        plugin.reloadConfig();
        load();
        if (!enabled) {
            TeleportationRunes.getInstance().getPluginLoader().disablePlugin(TeleportationRunes.getInstance());
        }
    }

    private static SimpleDatabase.Backend detectDatabaseBackend(FileConfiguration config) {
        String backendString = config.getString("TeleportationRunes.databaseBackend");
        SimpleDatabase.Backend backend;
        // use the configured backend
        // else use HSQLDB for FreeBSD and SQLite for everything else
        if (backendString != null) {
            try {
                backend = SimpleDatabase.Backend.valueOf(backendString);
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else if ("FreeBSD".equals(System.getProperty("os.name"))) {
            backend = HSQLDB;
        } else {
            backend = SQLITE;
        }
        Log.d("Using %s backend for database.", backend);
        return backend;
    }

}
