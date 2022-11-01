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

            ShopGuiHandler.GuiIcon guiIcon = Shop.getPlugin().getGuiHandler().getIconFromOption(p, PlayerSettings.Option.NOTIFICATION_SALE_OWNER);
            ItemStack ownerNotifyIcon = Shop.getPlugin().getGuiHandler().getIcon(guiIcon, p.getUniqueId(), null);
            page.setItem(10, ownerNotifyIcon);

            guiIcon = Shop.getPlugin().getGuiHandler().getIconFromOption(p, PlayerSettings.Option.NOTIFICATION_SALE_USER);
            ItemStack userNotifyIcon = Shop.getPlugin().getGuiHandler().getIcon(guiIcon, p.getUniqueId(), null);
            page.setItem(11, userNotifyIcon);

            guiIcon = Shop.getPlugin().getGuiHandler().getIconFromOption(p, PlayerSettings.Option.NOTIFICATION_STOCK);
            ItemStack stockNotifyIcon = Shop.getPlugin().getGuiHandler().getIcon(guiIcon, p.getUniqueId(), null);
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

