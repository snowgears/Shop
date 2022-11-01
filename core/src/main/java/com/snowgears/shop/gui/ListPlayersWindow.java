package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.util.ComparatorItemstackName;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.UUID;

public class ListPlayersWindow extends ShopGuiWindow {

    public ListPlayersWindow(UUID player){
        super(player);
        this.title = Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.LIST_PLAYERS);
        this.page = Bukkit.createInventory(null, INV_SIZE, title);
        initInvContents();
    }

    @Override
    protected void initInvContents(){
        super.initInvContents();
        this.clearInvBody();

        makeMenuBarUpper();
        makeMenuBarLower();

        //TODO make a more efficient method to sort players by name
        //this current method is way too resource intensive on large servers
        //List<OfflinePlayer> owners = Shop.getPlugin().getShopHandler().getShopOwners();
        //owners.sort(new OfflinePlayerNameComparator());

        //List<UUID> owners = Shop.getPlugin().getShopHandler().getShopOwnerUUIDs();
        ArrayList<ItemStack> shopOwnerHeads = Shop.getPlugin().getGuiHandler().getShopOwnerHeads();
        shopOwnerHeads.sort(new ComparatorItemstackName());

        int startIndex = pageIndex * 36; //36 items is a full page in the inventory
        ItemStack icon;
        boolean added = true;

        for (int i=startIndex; i< shopOwnerHeads.size(); i++) {
            icon = shopOwnerHeads.get(i);

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

    private ItemStack getPlayerIcon(UUID ownerUUID){
        //System.out.println("[Shop] creating icon for "+ownerUUID);
        if(Shop.getPlugin().getShopHandler().getAdminUUID().equals(ownerUUID)) {
            return Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_PLAYER_ADMIN, ownerUUID, null);
        }
        return Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_PLAYER, ownerUUID, null);
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

