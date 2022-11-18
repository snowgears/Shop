package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.ComparatorShopItemNameLow;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ListPlayerShopsWindow extends ShopGuiWindow {

    private UUID playerToList;

    public ListPlayerShopsWindow(UUID player, UUID playerToList){
        super(player);

        if(Shop.getPlugin().getShopHandler().getAdminUUID().equals(playerToList)) {
            ItemStack is = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_PLAYER_ADMIN, Shop.getPlugin().getShopHandler().getAdminUUID(), null);
            this.title = is.getItemMeta().getDisplayName();
        }
        else {
            this.title = Bukkit.getOfflinePlayer(playerToList).getName();

            //TODO in the future, might add total shops / allowed shops in title of own player
//            if(Shop.getPlugin().usePerms() && player.equals(playerToList)){
//                Player playerObj = Bukkit.getPlayer(player);
//                if(playerObj != null) {
//                    int totalShops = Shop.getPlugin().getShopHandler().getNumberOfShops(player);
//                    String numShops = " ("+totalShops+" / ";
//                    if (playerObj.isOp() || playerObj.hasPermission("shop.operator")) {
//                        numShops += "*)";
//                    }
//                    else{
//                        numShops += Shop.getPlugin().getShopListener().getBuildLimit(playerObj)+")";
//                    }
//                    this.title += numShops;
//                }
//            }
        }

        this.page = Bukkit.createInventory(null, INV_SIZE, this.title);
        this.playerToList = playerToList;
        initInvContents();
    }

    @Override
    protected void initInvContents() {
        super.initInvContents();
        this.clearInvBody();

        makeMenuBarUpper();
        makeMenuBarLower();

        List<AbstractShop> shops = Shop.getPlugin().getShopHandler().getShops(playerToList);
        Collections.sort(shops, new ComparatorShopItemNameLow());
        //Collections.sort(shops, new ShopTypeComparator());

        //System.out.println(player.toString()+" number of shops "+shops.size());

        //TODO break up inventory into sections by type (by default. More sorting options to come)

        int startIndex = pageIndex * 36; //36 items is a full page in the inventory
        ItemStack icon;
        boolean added = true;

        for (int i=startIndex; i< shops.size(); i++) {
            AbstractShop shop = shops.get(i);
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

//        ItemStack searchIcon = new ItemStack(Material.COMPASS);
//        ItemMeta meta = searchIcon.getItemMeta();
//        meta.setDisplayName("Search");
//        searchIcon.setItemMeta(meta);
//
//        page.setItem(8, searchIcon);
    }

    @Override
    protected void makeMenuBarLower(){
        super.makeMenuBarLower();
    }
}

