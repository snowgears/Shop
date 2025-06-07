package com.snowgears.shop.util;

import org.bukkit.Material;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class MaterialUtil {
    public static Material of(String type) { return getMaterial(type); }
    
    public static Material getMaterial(String type) {
        try {
            // Check if we should be using legacy materials
            // Pre-flattening
            if (!MCVersion.atLeast("1.13")) {
                if (type.equals("PLAYER_HEAD")) return Material.valueOf("SKULL_ITEM");
                if (type.contains("WALL_SIGN")) return Material.valueOf("WALL_SIGN");
            }

            return Material.valueOf(type.toUpperCase());
        } catch (Error | Exception e) {
            return null;
        }
    }


    public static TextComponent getTranslatableComponent(Material material) {
        if (MCVersion.atLeast("1.16")) {
            return new TextComponent(new TranslatableComponent(material.getTranslationKey()));
        } else {
            return new TextComponent(material.name());
        }
    }
}
