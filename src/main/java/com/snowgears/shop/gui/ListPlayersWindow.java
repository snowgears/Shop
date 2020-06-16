package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.util.OfflinePlayerNameComparator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class ListPlayersWindow extends ShopGuiWindow {
    public ListPlayersWindow(UUID player) {
        super(player);
        String title = Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.LIST_PLAYERS);
        this.page = Bukkit.createInventory(null, INV_SIZE, title);
        initInvContents();
    }

    @Override
    protected void initInvContents() {
        super.initInvContents();
        this.clearInvBody();

        makeMenuBarUpper();
        makeMenuBarLower();

        Bukkit.getScheduler().runTaskAsynchronously(Shop.getPlugin(), ()->{
        List<OfflinePlayer> owners = Shop.getPlugin().getShopHandler().getShopOwners();
        owners.sort(new OfflinePlayerNameComparator());

        int startIndex = pageIndex * 36; //36 items is a full page in the inventory
        ItemStack icon;
        boolean added = true;

        for (int i = startIndex; i < owners.size(); i++) {
            icon = createIcon(owners.get(i));

            if (!this.addIcon(icon)) {
                added = false;
                break;
            }
        }

        if (added) {
            page.setItem(53, null);
        } else {
            page.setItem(53, this.getNextPageIcon());
        }
        });
    }

    private ItemStack createIcon(OfflinePlayer owner) {
        if (Shop.getPlugin().getShopHandler().getAdminUUID().equals(owner.getUniqueId())) {
            return Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_PLAYER_ADMIN, owner, null);
        }
        return Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_PLAYER, owner, null);
    }

    @Override
    protected void makeMenuBarUpper() {
        super.makeMenuBarUpper();

//        ItemStack searchIcon = new ItemStack(Material.COMPASS);
//        ItemMeta meta = searchIcon.getItemMeta();
//        meta.setDisplayName("Search");
//        searchIcon.setItemMeta(meta);
//
//        page.setItem(8, searchIcon);
    }

    @Override
    protected void makeMenuBarLower() {
        super.makeMenuBarLower();

    }
}

