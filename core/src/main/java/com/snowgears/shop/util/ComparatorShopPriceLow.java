package com.snowgears.shop.util;

import com.snowgears.shop.shop.AbstractShop;

import java.util.Comparator;

public class ComparatorShopPriceLow implements Comparator<AbstractShop>{
	@Override
    public int compare(AbstractShop o1, AbstractShop o2) {
        return Double.compare(o1.getPrice(), o2.getPrice());
    }
}
