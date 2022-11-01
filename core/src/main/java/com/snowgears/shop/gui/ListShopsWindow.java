package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ListShopsWindow extends ShopGuiWindow {

    private List<AbstractShop> allShops;

    public ListShopsWindow(UUID player){

        super(player);

        //TODO save a list of all shops to a collection when opening this window for you to modify with filters

        this.title = Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.LIST_SHOPS);

        this.page = Bukkit.createInventory(null, INV_SIZE, this.title);

        allShops = Shop.getPlugin().getShopHandler().getAllShops();
        //Collections.sort(allShops, new ShopItemComparator()); //this will be taken care of in inv contents

        initInvContents();

    }

    @Override
    protected void initInvContents() {
        super.initInvContents();
        this.clearInvBody();

        makeMenuBarUpper();
        makeMenuBarLower();

        //first do shop type filtering
        ShopGuiHandler.GuiIcon guiFilterTypeIcon = Shop.getPlugin().getGuiHandler().getIconFromOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_TYPE);
        List<AbstractShop> filteredShops;
        switch(guiFilterTypeIcon){
            case MENUBAR_FILTER_TYPE_ALL:
                filteredShops = allShops;
                break;
            case MENUBAR_FILTER_TYPE_SELL:
                filteredShops = allShops.stream()
                        .filter(shop -> (shop.getType() == ShopType.SELL || shop.getType() == ShopType.COMBO)).collect(Collectors.toList());
                break;
            case MENUBAR_FILTER_TYPE_BUY:
                filteredShops = allShops.stream()
                        .filter(shop -> (shop.getType() == ShopType.BUY || shop.getType() == ShopType.COMBO)).collect(Collectors.toList());
                break;
            case MENUBAR_FILTER_TYPE_BARTER:
                filteredShops = allShops.stream()
                        .filter(shop -> shop.getType() == ShopType.BARTER).collect(Collectors.toList());
                break;
            case MENUBAR_FILTER_TYPE_GAMBLE:
                filteredShops = allShops.stream()
                        .filter(shop -> shop.getType() == ShopType.GAMBLE).collect(Collectors.toList());
                break;
            default:
                filteredShops = new ArrayList<>();
                break;
        }

        //first do shop type filtering
        ShopGuiHandler.GuiIcon guiFilterStockIcon = Shop.getPlugin().getGuiHandler().getIconFromOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_STOCK);
        switch(guiFilterStockIcon){
            case MENUBAR_FILTER_STOCK_IN:
                filteredShops = filteredShops.stream()
                        .filter(shop -> (shop.getStock() > 0)).collect(Collectors.toList());
                break;
            case MENUBAR_FILTER_STOCK_OUT:
                filteredShops = filteredShops.stream()
                        .filter(shop -> (shop.getStock() <= 0)).collect(Collectors.toList());
                break;
            default:
                break;
        }

        //now do sorting
        ShopGuiHandler.GuiIcon guiSortIcon = Shop.getPlugin().getGuiHandler().getIconFromOption(player, PlayerSettings.Option.GUI_SORT);
        switch (guiSortIcon){
            case MENUBAR_SORT_NAME_HIGH:
                Collections.sort(filteredShops, new ComparatorShopItemNameHigh());
                break;
            case MENUBAR_SORT_PRICE_LOW:
                Collections.sort(filteredShops, new ComparatorShopPriceLow());
                break;
            case MENUBAR_SORT_PRICE_HIGH:
                Collections.sort(filteredShops, new ComparatorShopPriceHigh());
                break;
            default:
                Collections.sort(filteredShops, new ComparatorShopItemNameLow());
                break;
        }

        int startIndex = pageIndex * 36; //36 items is a full page in the inventory
        ItemStack icon;
        boolean added = true;

        for (int i=startIndex; i< filteredShops.size(); i++) {
            AbstractShop shop = filteredShops.get(i);
            icon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_SHOP, null, shop);

            if(!this.addIcon(icon)){
                added = false;
                break;
            }
        }

        if(added){
            page.setItem(53, null);
        }
        else{
            page.setItem(53, this.getNextPageIcon());
        }
    }

    @Override
    protected void makeMenuBarUpper(){
        super.makeMenuBarUpper();

        //init the menu bar with the saved sort settings
        ShopGuiHandler.GuiIcon guiIcon = Shop.getPlugin().getGuiHandler().getIconFromOption(player, PlayerSettings.Option.GUI_SORT);
        ItemStack sortIcon = Shop.getPlugin().getGuiHandler().getIcon(guiIcon, player, null);
        page.setItem(3, sortIcon);

        //filter shop type - all, sell, buy, barter, gamble
        guiIcon = Shop.getPlugin().getGuiHandler().getIconFromOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_TYPE);
        ItemStack filterTypeIcon = Shop.getPlugin().getGuiHandler().getIcon(guiIcon, player, null);
        page.setItem(5, filterTypeIcon);

        //filter stock - in stock, out of stock, all
        guiIcon = Shop.getPlugin().getGuiHandler().getIconFromOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_STOCK);
        ItemStack filterStockIcon = Shop.getPlugin().getGuiHandler().getIcon(guiIcon, player, null);
        page.setItem(6, filterStockIcon);

        //search icon
        ItemStack searchIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_SEARCH, null, null);
        page.setItem(8, searchIcon);
    }

    @Override
    protected void makeMenuBarLower(){
        super.makeMenuBarLower();
    }
}

