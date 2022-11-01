package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HomeWindow extends ShopGuiWindow {

    public HomeWindow(UUID player){
        super(player);
        this.title = Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.HOME);
        this.page = Bukkit.createInventory(null, INV_SIZE, title);
        initInvContents();
    }

    @Override
    protected void initInvContents(){

        int listAllShopIconSlot = 19;
        int listOwnShopIconSlot = 20;
        int listPlayersIconSlot = 21;
        int searchIconSlot = 22;
        int settingsIconSlot = 24;

        ItemStack listShopsIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_LIST_ALL_SHOPS, null, null);
        ItemStack listOwnShopsIcon = Shop.getPlugin().getGuiHandler().getPlayerHeadIcon(player);
        ItemStack listPlayersIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_LIST_PLAYERS, null, null);
        ItemStack searchIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_SEARCH, null, null);
        ItemStack settingsIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_SETTINGS, null, null);

        //if search icon is not allowed, rearrange other icons so they look nice
        if(!Shop.getPlugin().allowCreativeSelection()){
            listAllShopIconSlot = 20;
            listOwnShopIconSlot = 21;
            listPlayersIconSlot = 22;
            settingsIconSlot = 23;
        }

        page.setItem(listAllShopIconSlot, listShopsIcon);
        page.setItem(listOwnShopIconSlot, listOwnShopsIcon);
        page.setItem(listPlayersIconSlot, listPlayersIcon);
        //only put search icon on page if creative selection is allowed
        if(Shop.getPlugin().allowCreativeSelection()){
            page.setItem(searchIconSlot, searchIcon);
        }
        page.setItem(settingsIconSlot, settingsIcon);

        //list the commands if they have operator permission
        Player p = this.getPlayer();
        if(p != null) {
            if ((Shop.getPlugin().usePerms() && p.hasPermission("shop.operator")) || p.isOp()) {

                ItemStack commandsIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_COMMANDS, null, null);
                page.setItem(43, commandsIcon);
            }
        }
    }
}
