package com.snowgears.shop.gui;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public abstract class ShopGuiWindow {
    public enum GUIArea {
        TOP_BAR, BOTTOM_BAR, BODY
    }

    protected int INV_SIZE = 54;
    protected Inventory page;
    protected ShopGuiWindow prevWindow;
    protected UUID player;
    private int currentSlot;
    protected int pageIndex;

    //TODO FOR SORTING, HAVE AN ARRAY LIST OF SORT_OPTIONS HERE THAT YOU USE ALONG WITH PAGE INDEX TO GO TO NEXT PAGE
    //For example, sort by name_ascending, name_descending, keyword, number of stock, shop type, etc...

    public ShopGuiWindow(UUID player) {
        this.player = player;
        page = null;
        prevWindow = null;
        currentSlot = 9;
    }

    public boolean scrollPageNext() {
        ItemStack nextPageIcon = page.getItem(53);

        if (nextPageIcon != null && nextPageIcon.getType() == Material.RED_STAINED_GLASS_PANE) {
            //set the previous scroll page
            page.setItem(45, this.getPrevPageIcon());

            this.pageIndex++;

            initInvContents();

            return true;
        }
        return false;
    }

    public boolean scrollPagePrev() {
        ItemStack nextPageIcon = page.getItem(45);

        if (nextPageIcon != null && nextPageIcon.getType() == Material.RED_STAINED_GLASS_PANE) {
            //set the next scroll page
            page.setItem(53, this.getNextPageIcon());

            this.pageIndex--;

            if (pageIndex == 0) {
                page.setItem(45, null);
            }

            initInvContents();

            return true;
        }
        return false;
    }

    //TODO make paging (scrolling) and previous window different
    //previous window will be in top left always (BARRIER?), going back to another window
    //previous page will simply set the inventory to the next group of items to display within the current window

//    public void setPrevWindow(ShopGuiWindow prevWindow){
//        this.prevWindow = this;
//        page.setItem(45, new ItemStack(Material.STAINED_GLASS_PANE));
//    }

    public void setPrevWindow(ShopGuiWindow prevWindow) {
        this.prevWindow = prevWindow;
        page.setItem(0, this.getBackIcon());
    }

    public boolean hasPrevWindow() {
        return prevWindow != null;
    }

    //this method will add to the GUI with taking into account top and bottom menu bars
    protected boolean addIcon(ItemStack icon) {

        //this page has been filled with icons
        if (currentSlot == 45)
            return false;

        page.setItem(currentSlot, icon);
        currentSlot++;

        return true;
    }

    public boolean open() {
        Player p = this.getPlayer();
        if (p != null) {
            p.openInventory(this.page);
            return true;
        }
        return false;
    }

    public boolean close() {
        Player p = this.getPlayer();
        if (p != null) {
            p.closeInventory();
            return true;
        }
        return false;
    }


    //override in subclasses
    protected void initInvContents() {
        currentSlot = 9;
    }

    protected void clearInvBody() {
        for (int i = 9; i < INV_SIZE - 9; i++) {
            page.setItem(i, null);
        }
    }

    //TODO search will always be in top right and will search anything all items in page or in list
    protected void makeMenuBarUpper() {
        //override in subclasses
    }

    protected void makeMenuBarLower() {
        //override in subclasses
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(player);
    }

    public Inventory getInventory() {
        return this.page;
    }

    protected ItemStack getPrevPageIcon() {
        ItemStack icon = new ItemStack(Material.RED_STAINED_GLASS_PANE);

        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName("Previous Page");
        icon.setItemMeta(meta);

        return icon;
    }

    protected ItemStack getNextPageIcon() {
        ItemStack icon = new ItemStack(Material.RED_STAINED_GLASS_PANE);

        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName("Next Page");
        icon.setItemMeta(meta);

        return icon;
    }

    protected ItemStack getBackIcon() {
        ItemStack icon = new ItemStack(Material.BARRIER);

        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName("Back");
        icon.setItemMeta(meta);

        return icon;
    }
}
