package com.snowgears.shop.util;

import org.bukkit.Material;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.TextComponent;
import java.util.List;
import java.util.ArrayList;

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

    public static List<Material> signTypes = getSignTypes();

    public static List<Material> getSignTypes() {
        List<Material> signTypes = new ArrayList<>();
        Material[] materials = Material.values();
        for (Material material : materials) {
            if (material.name().contains("SIGN")) {
                signTypes.add(material);
            }
        }
        return signTypes;
    }

    public static List<Material> getWallSignTypes() {
        List<Material> wallSignTypes = new ArrayList<>();

        Material[] materials = Material.values();
        for (Material material : materials) {
            if (material.name().contains("WALL_SIGN")) {
                wallSignTypes.add(material);
            }
        }
        return wallSignTypes;
    }

    public static TextComponent getTranslatableComponent(Material material) {
        if (UtilMethods.isTranslationSupported()) {
            return new TextComponent(new TranslatableComponent(material.getTranslationKey()));
        } else {
            return new TextComponent(material.name());
        }
    }
}
