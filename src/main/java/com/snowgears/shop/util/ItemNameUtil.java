package com.snowgears.shop.util;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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
         	if(item.getType()!=Material.ENCHANTED_BOOK)
            return getBackupName(item.getType());
         	else {
         		Iterator<Entry<Enchantment, Integer>> it=item.getEnchantments().entrySet().iterator();
         		if(it.hasNext())
         			return WordUtils.capitalizeFully(it.next().getKey().toString().replace("_", " ").toLowerCase());
         		else
         			 return getBackupName(item.getType());
         		//if a ordering method is found. I dont know best way to order
         		/*for(Entry<Enchantment, Integer> s:item.getEnchantments().entrySet()) {
         			s.getKey();
         		}*/
         	}
        
    }

    public String getName(Material material) {
        
            return getBackupName(material);
        
    }

    private String getBackupName(Material material) {
        return WordUtils.capitalizeFully(material.name().replace("_", " ").toLowerCase());
    }
}
