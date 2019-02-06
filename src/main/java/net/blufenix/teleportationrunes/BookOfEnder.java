package net.blufenix.teleportationrunes;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class BookOfEnder extends ItemStack {

    private static class LazyHolder {
        public static final BookOfEnder _instance = new BookOfEnder();
    }

    public static BookOfEnder getInstance() {
        return LazyHolder._instance;
    }

    public static ItemMeta getMeta() {
        return LazyHolder._instance.getItemMeta();
    }

    public static Recipe getRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(
                new NamespacedKey(TeleportationRunes.getInstance(), "book_of_ender"),
                BookOfEnder.getInstance());

        recipe.addIngredient(Material.BOOK);
        recipe.addIngredient(Material.ENDER_PEARL);

        return recipe;
    }

    private BookOfEnder() {
        super(Material.ENCHANTED_BOOK, 1);
        ItemMeta meta = getItemMeta();
        meta.setDisplayName("Book of Ender");
        meta.setLore(Arrays.asList("Bending spacetime has never been easier!"));
        setItemMeta(meta);
    }

}
