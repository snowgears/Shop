package com.snowgears.shop.gui;

import org.bukkit.Bukkit;

import java.util.UUID;

public class OptionsWindow extends ShopGuiWindow {

    public OptionsWindow(UUID player){
        super(player);
        this.title = "Options";
        this.page = Bukkit.createInventory(null, INV_SIZE, this.title);
        initInvContents();
    }

    @Override
    protected void initInvContents(){

    }
}

