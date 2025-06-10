package com.snowgears.shop.util;

import com.snowgears.shop.Shop;

import org.bukkit.Material;
import org.bukkit.block.Block;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.TextComponent;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;

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
            Shop.getPlugin().getShopLogger().warning("Error getting material! " + e.getMessage());
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

    public static void setLightBlockData(Block block, int level){
        // Requires 1.17+ for Light block
        if (!MCVersion.atLeast("1.17")) return;

        // For some reason versions that don't have the BlockData class freak out
        // if you even reference org.bukkit.block.data.type.Light anywhere in your code
        // even if you do not use it! It throws an error on startup when the class is first loaded.
        // So we have to use reflection to set the light level.
        try {
            block.setType(Material.LIGHT);
            // Set light level into BlockData
            Class<?> lightClass = Class.forName("org.bukkit.block.data.type.Light");
            Object light = lightClass.getDeclaredConstructor().newInstance();
            Method setLevel = lightClass.getDeclaredMethod("setLevel", int.class);
            setLevel.invoke(light, level);
            // Set Light BlockData into the Block
            MaterialUtil.setBlockData(block, light);
        } catch (Error | Exception e) {}
    }

    public static void setBlockData(Block block, Object blockData){
        try {
            Method setBlockData = Block.class.getDeclaredMethod("setBlockData", Class.forName("org.bukkit.block.BlockData"));
            setBlockData.invoke(block, blockData);
        } catch (Error | Exception e) {}
    }
}
