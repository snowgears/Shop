package com.snowgears.shop.util;

import org.bukkit.OfflinePlayer;

import java.util.Comparator;

public class ComparatorOfflinePlayerName implements Comparator<OfflinePlayer>{
	@Override
    public int compare(OfflinePlayer o1, OfflinePlayer o2) {
        if(o1.getName() == null || o2.getName() == null)
            return -1;
        return o1.getName().compareTo(o2.getName());
    }
}
