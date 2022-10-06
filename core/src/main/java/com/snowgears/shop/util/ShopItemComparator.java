package com.snowgears.shop.util;

import com.snowgears.shop.shop.AbstractShop;

import java.util.Comparator;

public class ShopItemComparator implements Comparator<AbstractShop>{
	@Override
    public int compare(AbstractShop o1, AbstractShop o2) {
        return o1.getGuiIcon().getItemMeta().getDisplayName().compareTo(o2.getGuiIcon().getItemMeta().getDisplayName());
    }
}
