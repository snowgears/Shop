package com.snowgears.shop.gui;


import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public abstract class ShopGuiWindow {

    public enum GUIArea{
        TOP_BAR, BOTTOM_BAR, BODY;
    }

    protected int INV_SIZE = 54;
    protected String title;
    protected Inventory page;
    protected ShopGuiWindow prevWindow;
    protected UUID player;
    private int currentSlot;
    protected int pageIndex;

    //TODO FOR SORTING, HAVE AN ARRAY LIST OF SORT_OPTIONS HERE THAT YOU USE ALONG WITH PAGE INDEX TO GO TO NEXT PAGE
    //For example, sort by name_ascending, name_descending, keyword, number of stock, shop type, etc...

    public ShopGuiWindow(UUID player){
        this.player = player;
        page = null;
        prevWindow = null;
        currentSlot = 9;
    }

    public boolean scrollPageNext(){
        ItemStack nextPageIcon = page.getItem(53);

        if(nextPageIcon != null){
            //set the previous scroll page
            page.setItem(45, this.getPrevPageIcon());

            this.pageIndex++;

            initInvContents();

            return true;
        }
        return false;
    }

    public boolean scrollPagePrev(){
        ItemStack nextPageIcon = page.getItem(45);

        if(nextPageIcon != null){
            //set the next scroll page
            page.setItem(53, this.getNextPageIcon());

            this.pageIndex--;

            if(pageIndex == 0){
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

    public void setPrevWindow(ShopGuiWindow prevWindow){
        this.prevWindow = prevWindow;
        page.setItem(0, this.getBackIcon());
    }

    public boolean hasPrevWindow(){
        if(prevWindow == null)
            return false;
        return true;
    }

    //this method will add to the GUI with taking into account top and bottom menu bars
    protected boolean addIcon(ItemStack icon){

        //this page has been filled with icons
        if(currentSlot == 45)
            return false;

        page.setItem(currentSlot, icon);
        currentSlot++;

        return true;
    }

    public boolean open(){
        Player p = this.getPlayer();
        if(p != null){
            p.openInventory(this.page);
            return true;
        }
        return false;
    }

    //use plugin.getGuiHandler().closeWindow(player);
//    public boolean close(){
//        Player p = this.getPlayer();
//        if(p != null){
//            p.closeInventory();
//            return true;
//        }
//        return false;
//    }


    //override in subclasses
    protected void initInvContents(){
        currentSlot = 9;
    }

    protected void clearInvBody(){
        for(int i=9; i<INV_SIZE-9; i++){
            page.setItem(i, null);
        }
    }

    //TODO search will always be in top right and will search anything all items in page or in list
    protected void makeMenuBarUpper(){
        //override in subclasses
    }

    protected void makeMenuBarLower(){
        //override in subclasses
    }

    public Player getPlayer(){
        return Bukkit.getPlayer(player);
    }

    public Inventory getInventory(){
        return this.page;
    }

    public String getTitle(){
        return this.title;
    }

    protected ItemStack getPrevPageIcon(){
        return Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_LAST_PAGE, null, null);
    }

    protected ItemStack getNextPageIcon(){
        return Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_NEXT_PAGE, null, null);
    }

    protected ItemStack getBackIcon(){
        return Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_BACK, null, null);
    }
}
