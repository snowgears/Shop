package com.snowgears.shop.util;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemNameUtil {
    public String getName(ItemStack item) {
        if (item == null) {
            return "";
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
            if (meta.hasLocalizedName()) {
                return meta.getLocalizedName();
            }
        }
         
            return getBackupName(item.getType());
        
    }

    public String getName(Material material) {
        
            return getBackupName(material);
        
    }

    private String getBackupName(Material material) {
        return WordUtils.capitalizeFully(material.name().replace("_", " ").toLowerCase());
    }
}
