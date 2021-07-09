package net.blufenix.teleportationrunes;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ScrollOfWarp extends ItemStack {

    private static class LazyHolder {
        public static final ScrollOfWarp _instance = new ScrollOfWarp();
    }

    public static ScrollOfWarp getInstance() {
        return LazyHolder._instance;
    }

    public static ItemMeta getMeta() {
        return LazyHolder._instance.getItemMeta();
    }

    public static Recipe getRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(
                new NamespacedKey(TeleportationRunes.getInstance(), "scroll_of_warp"),
                ScrollOfWarp.getInstance());

        for (String matName: Config.scrollOfWarpRecipeList) {
            recipe.addIngredient(Material.matchMaterial(matName));
        }

        return recipe;
    }

    private ScrollOfWarp() {
        super(Material.PAPER, Config.numScrollsCrafted);
        ItemMeta meta = getItemMeta();
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName("Scroll of Warp");
        meta.setLore(Arrays.asList("Unattuned"));
        setItemMeta(meta);
    }

}
