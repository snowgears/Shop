package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.util.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerSettingsWindow extends ShopGuiWindow {

    //TODO this window will call the player settings handler and set different variables in the associated player settings class
    public PlayerSettingsWindow(UUID player){
        super(player);
        this.title = Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.SETTINGS);
        this.page = Bukkit.createInventory(null, INV_SIZE, title);
        initInvContents();
    }

    @Override
    protected void initInvContents(){

        Player p = this.getPlayer();
        if(p != null) {

            ItemStack ownerNotifyIcon;
            if (Shop.getPlugin().getGuiHandler().getSettingsOption(p, PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS)) {
                ownerNotifyIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_OWNER_ON, p.getUniqueId(), null);
            }
            else{
                ownerNotifyIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_OWNER_OFF, p.getUniqueId(), null);
            }
            page.setItem(10, ownerNotifyIcon);


            ItemStack userNotifyIcon;
            if (Shop.getPlugin().getGuiHandler().getSettingsOption(p, PlayerSettings.Option.SALE_USER_NOTIFICATIONS)) {
                userNotifyIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_USER_ON, p.getUniqueId(), null);
            }
            else{
                userNotifyIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_USER_OFF, p.getUniqueId(), null);
            }
            page.setItem(11, userNotifyIcon);


            ItemStack stockNotifyIcon;
            if (Shop.getPlugin().getGuiHandler().getSettingsOption(p, PlayerSettings.Option.STOCK_NOTIFICATIONS)) {
                stockNotifyIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_STOCK_ON, p.getUniqueId(), null);
            }
            else{
                stockNotifyIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_STOCK_OFF, p.getUniqueId(), null);
            }
            page.setItem(12, stockNotifyIcon);
        }
    }

//    @Override
//    protected void makeMenuBarUpper(){
//
//    }
//
//    @Override
//    protected void makeMenuBarLower(){
//
//    }
}

