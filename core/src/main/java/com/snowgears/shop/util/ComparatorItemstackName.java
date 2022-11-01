package com.snowgears.shop.util;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;

public class ComparatorItemstackName implements Comparator<ItemStack>{
	@Override
    public int compare(ItemStack o1, ItemStack o2) {
        return ChatColor.stripColor(o1.getItemMeta().getDisplayName()).compareTo(ChatColor.stripColor(o2.getItemMeta().getDisplayName()));
    }
}
