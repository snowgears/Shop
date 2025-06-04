package com.snowgears.shop.gui;


import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import com.snowgears.shop.gui.ListPlayersWindow;
import com.snowgears.shop.gui.ListPlayerShopsWindow;
import com.snowgears.shop.gui.ListSearchResultsWindow;
import com.snowgears.shop.gui.ListShopsWindow;
import com.snowgears.shop.gui.PlayerSettingsWindow;
import com.snowgears.shop.gui.ShopGuiWindow;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.CompatibilityUtil;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.PlayerSettings;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;

public class ShopGUIListener implements Listener {

    private Shop plugin;

    public ShopGUIListener(Shop instance) {
        plugin = instance;
    }

    public String getInventoryViewTitle(InventoryClickEvent event) {
        try {
            Object view = event.getView();
            Method getTitle = view.getClass().getMethod("getTitle");
            getTitle.setAccessible(true);
            return (String) getTitle.invoke(view);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private ShopGuiHandler.GuiIcon getNextOptionIcon(ItemStack[] iconItems, ShopGuiHandler.GuiIcon[] icons, ShopGuiHandler.GuiIcon current) {
        int currentIndex = -1;
        // Find the current index of the icon
        for (int i = 0; i < icons.length; i++) {
            if (icons[i] == current) {
                currentIndex = i;
                break;
            }
        }

        // If current icon is not found, return itself (sanity check)
        if (currentIndex == -1) {
            return current;
        }

        // Iterate through the list starting from the next element
        for (int i = 1; i < icons.length; i++) {
            int nextIndex = (currentIndex + i) % icons.length;
            if (iconItems[nextIndex] != null) { // Check if the next icon is available
                return icons[nextIndex];
            }
        }

        // If no other option is available, return the current icon
        return current;
    }

    @EventHandler (ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent event){
        if(event.getWhoClicked() instanceof Player){
            Player player = (Player)event.getWhoClicked();

            ShopGuiWindow window = plugin.getGuiHandler().getWindow(player);

            if(getInventoryViewTitle(event).equals(window.getTitle())){

                if(event.getClick() == ClickType.NUMBER_KEY) {
                    event.setCancelled(true);
                    return;
                }

                ItemStack clicked = event.getCurrentItem();
                if(clicked != null && clicked.getType() != Material.AIR){

                    event.setCancelled(true);

                    //this is the case in all windows
                    if(event.getRawSlot() == 0) {
                        if (window.hasPrevWindow()) {
                            plugin.getGuiHandler().setWindow(player, window.prevWindow);
                            return;
                        }
                    }

                    //TODO search window
                    //this is the case in all windows
//                    if(event.getRawSlot() == 8 && clicked.getType() == Material.COMPASS) {
//                        SearchWindow searchWindow = new SearchWindow(player.getUniqueId());
//                        searchWindow.setPrevWindow(window);
//                        plugin.getGuiHandler().setWindow(player, searchWindow);
//                        return;
//                    }

                    //this is the case in all windows
                    if(event.getRawSlot() == 45){
                        window.scrollPagePrev();
                        return;
                    }

                    //this is the case in all windows
                    if(event.getRawSlot() == 53){
                        window.scrollPageNext();
                        return;
                    }

                    if(window instanceof HomeWindow){
                        ItemStack listOwnShopsIcon = plugin.getGuiHandler().getPlayerHeadIcon(player.getUniqueId());
                        ItemStack listAllShopsIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_LIST_ALL_SHOPS, null, null);
                        ItemStack listPlayersIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_LIST_PLAYERS, null, null);
                        ItemStack searchIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_SEARCH, null, null);
                        ItemStack settingsIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_SETTINGS, null, null);
                        ItemStack commandsIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_COMMANDS, null, null);

                        if(clicked.getType() == listOwnShopsIcon.getType() && clicked.getItemMeta().getDisplayName().equals(listOwnShopsIcon.getItemMeta().getDisplayName())){
                            ListPlayerShopsWindow ownShopsWindow = new ListPlayerShopsWindow(player.getUniqueId(), player.getUniqueId());
                            ownShopsWindow.setPrevWindow(window);
                            plugin.getGuiHandler().setWindow(player, ownShopsWindow);
                            return;
                        }
                        else if(clicked.getType() == listAllShopsIcon.getType()){
                            ListShopsWindow shopsWindow = new ListShopsWindow(player.getUniqueId());
                            shopsWindow.setPrevWindow(window);
                            plugin.getGuiHandler().setWindow(player, shopsWindow);
                            return;
                        }
                        else if(clicked.getType() == listPlayersIcon.getType()){
                            ListPlayersWindow playersWindow = new ListPlayersWindow(player.getUniqueId());
                            playersWindow.setPrevWindow(window);
                            plugin.getGuiHandler().setWindow(player, playersWindow);
                            return;
                        }
                        else if(searchIcon != null && clicked.getType() == searchIcon.getType()){
                            plugin.getGuiHandler().closeWindow(player);
                            plugin.getCreativeSelectionListener().putPlayerInCreativeSelection(player, player.getLocation(), true);

                            for(String message : ShopMessage.getUnformattedMessageList("guiSearchSelection", "prompt")){
                                if(message != null && !message.isEmpty())
                                    ShopMessage.sendMessage(message, player);
                            }
                            return;
                        }
                        else if(clicked.getType() == settingsIcon.getType()){
                            PlayerSettingsWindow settingsWindow = new PlayerSettingsWindow(player.getUniqueId());
                            settingsWindow.setPrevWindow(window);
                            plugin.getGuiHandler().setWindow(player, settingsWindow);
                            return;
                        }
                        else if(clicked.getType() == commandsIcon.getType()){
                            CommandsWindow commandsWindow = new CommandsWindow(player.getUniqueId());
                            commandsWindow.setPrevWindow(window);
                            plugin.getGuiHandler().setWindow(player, commandsWindow);
                            return;
                        }
                    }
                    else if(window instanceof ListPlayersWindow){
                        //ItemStack playerIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_PLAYER, null, null); //for some reason this is returning null
                        //ItemStack adminPlayerIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_PLAYER_ADMIN, null, null);

                        String playerUUIDString = CompatibilityUtil.getItemData(clicked, "playerUUID", "");
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(playerUUIDString);
                        }catch(IllegalArgumentException e){
                            return;
                        }

                        ListPlayerShopsWindow shopsWindow = new ListPlayerShopsWindow(player.getUniqueId(), uuid);
                        shopsWindow.setPrevWindow(window);
                        plugin.getGuiHandler().setWindow(player, shopsWindow);
                        return;
                    }
                    else if(window instanceof ListPlayerShopsWindow || window instanceof ListSearchResultsWindow || window instanceof ListShopsWindow){

                        if(clicked.getItemMeta().hasLore() && clicked.getItemMeta().getLore().size() > 0)
                        {
                            //gets the location from the itemstack lore (this is how the teleportation works)
                            String signLocation = CompatibilityUtil.getItemData(clicked, "signLocation", "");
                            if(!signLocation.isEmpty())
                            {
                                Location loc = UtilMethods.getLocation(signLocation);
                                AbstractShop shop = plugin.getShopHandler().getShop(loc);

                                if(shop != null){
                                    if(Shop.getPlugin().usePerms()){
                                        if(player.hasPermission("shop.operator") || player.hasPermission("shop.gui.teleport")){
                                            if(!player.isOp()){
                                                if(plugin.getTeleportCost() > 0) {
                                                    if (EconomyUtils.hasSufficientFunds(player, player.getInventory(), plugin.getTeleportCost())) {
                                                        EconomyUtils.removeFunds(player, player.getInventory(), plugin.getTeleportCost());
                                                    } else {
                                                        ShopMessage.sendMessage("interactionIssue", "teleportInsufficientFunds", player, shop);
                                                        plugin.getGuiHandler().closeWindow(player);
                                                        return;
                                                    }
                                                }
                                                if(plugin.getTeleportCooldown() > 0){
                                                    int secondsRemaining = plugin.getShopListener().getTeleportCooldownRemaining(player);
                                                    if(secondsRemaining > 0){
                                                        ShopMessage.sendMessage("interactionIssue", "teleportInsufficientCooldown", player, shop);
                                                        plugin.getGuiHandler().closeWindow(player);
                                                        return;
                                                    }
                                                }
                                            }
                                            shop.teleportPlayer(player);
                                            plugin.getGuiHandler().closeWindow(player);
                                        }
                                    }
                                    else{
                                        if(player.isOp()){
                                            shop.teleportPlayer(player);
                                            plugin.getGuiHandler().closeWindow(player);
                                        }
                                    }
                                    return;
                                }
                            }
                        }

                        if(window instanceof ListShopsWindow){

                            //SORTING

                            ItemStack sortNameLow = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_SORT_NAME_LOW, null, null);
                            ItemStack sortNameHigh = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_SORT_NAME_HIGH, null, null);
                            ItemStack sortPriceLow = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_SORT_PRICE_LOW, null, null);
                            ItemStack sortPriceHigh = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_SORT_PRICE_HIGH, null, null);

                            ShopGuiHandler.GuiIcon[] sortOptions = new ShopGuiHandler.GuiIcon[]{
                                ShopGuiHandler.GuiIcon.MENUBAR_SORT_NAME_LOW,
                                ShopGuiHandler.GuiIcon.MENUBAR_SORT_NAME_HIGH,
                                ShopGuiHandler.GuiIcon.MENUBAR_SORT_PRICE_LOW,
                                ShopGuiHandler.GuiIcon.MENUBAR_SORT_PRICE_HIGH,
                            };
                            ItemStack[] sortItems = new ItemStack[]{
                                    sortNameLow,
                                    sortNameHigh,
                                    sortPriceLow,
                                    sortPriceHigh
                            };
                            boolean reloadPage = false;
                            if(clicked.isSimilar(sortNameLow)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_SORT, getNextOptionIcon(sortItems, sortOptions, ShopGuiHandler.GuiIcon.MENUBAR_SORT_NAME_LOW));
                                reloadPage = true;
                            }
                            else if(clicked.isSimilar(sortNameHigh)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_SORT, getNextOptionIcon(sortItems, sortOptions, ShopGuiHandler.GuiIcon.MENUBAR_SORT_NAME_HIGH));
                                reloadPage = true;
                            }
                            else if(clicked.isSimilar(sortPriceLow)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_SORT, getNextOptionIcon(sortItems, sortOptions, ShopGuiHandler.GuiIcon.MENUBAR_SORT_PRICE_LOW));
                                reloadPage = true;
                            }
                            else if(clicked.isSimilar(sortPriceHigh)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_SORT, getNextOptionIcon(sortItems, sortOptions, ShopGuiHandler.GuiIcon.MENUBAR_SORT_PRICE_HIGH));
                                reloadPage = true;
                            }

                            //FILTERING SHOP TYPES

                            ItemStack filterTypeAll = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_FILTER_TYPE_ALL, null, null);
                            ItemStack filterTypeSell = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_FILTER_TYPE_SELL, null, null);
                            ItemStack filterTypeBuy = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_FILTER_TYPE_BUY, null, null);
                            ItemStack filterTypeBarter = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_FILTER_TYPE_BARTER, null, null);
                            ItemStack filterTypeGamble = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_FILTER_TYPE_GAMBLE, null, null);

                            if(clicked.isSimilar(filterTypeAll)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_TYPE, ShopGuiHandler.GuiIcon.MENUBAR_FILTER_TYPE_SELL);
                                reloadPage = true;
                            }
                            else if(clicked.isSimilar(filterTypeSell)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_TYPE, ShopGuiHandler.GuiIcon.MENUBAR_FILTER_TYPE_BUY);
                                reloadPage = true;
                            }
                            else if(clicked.isSimilar(filterTypeBuy)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_TYPE, ShopGuiHandler.GuiIcon.MENUBAR_FILTER_TYPE_BARTER);
                                reloadPage = true;
                            }
                            else if(clicked.isSimilar(filterTypeBarter)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_TYPE, ShopGuiHandler.GuiIcon.MENUBAR_FILTER_TYPE_GAMBLE);
                                reloadPage = true;
                            }
                            else if(clicked.isSimilar(filterTypeGamble)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_TYPE, ShopGuiHandler.GuiIcon.MENUBAR_FILTER_TYPE_ALL);
                                reloadPage = true;
                            }

                            //FILTERING SHOP STOCK

                            ItemStack filterStockAll = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_FILTER_STOCK_ALL, null, null);
                            ItemStack filterStockIn = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_FILTER_STOCK_IN, null, null);
                            ItemStack filterStockOut = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_FILTER_STOCK_OUT, null, null);

                            if(clicked.isSimilar(filterStockAll)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_STOCK, ShopGuiHandler.GuiIcon.MENUBAR_FILTER_STOCK_IN);
                                reloadPage = true;
                            }
                            else if(clicked.isSimilar(filterStockIn)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_STOCK, ShopGuiHandler.GuiIcon.MENUBAR_FILTER_STOCK_OUT);
                                reloadPage = true;
                            }
                            else if(clicked.isSimilar(filterStockOut)){
                                plugin.getGuiHandler().setIconForOption(player, PlayerSettings.Option.GUI_FILTER_SHOP_STOCK, ShopGuiHandler.GuiIcon.MENUBAR_FILTER_STOCK_ALL);
                                reloadPage = true;
                            }

                            //reload the page with new sorts and filters applied
                            if(reloadPage){
                                window.initInvContents();
                            }

                            //SEARCHING

                            ItemStack searchIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_SEARCH, null, null);

                            //if they click the search icon, close current window and give instruction for searching
                            if(searchIcon != null && clicked.getType() == searchIcon.getType()){
                                plugin.getGuiHandler().closeWindow(player);
                                plugin.getCreativeSelectionListener().putPlayerInCreativeSelection(player, player.getLocation(), true);

                                for(String message : ShopMessage.getUnformattedMessageList("guiSearchSelection", "prompt")){
                                    if(message != null && !message.isEmpty())
                                        ShopMessage.sendMessage(message, player);
                                }
                                return;
                            }

                        }
                    }
                    else if(window instanceof PlayerSettingsWindow){
                        ItemStack ownerIconOn = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_OWNER_ON, null, null);
                        ItemStack ownerIconOff = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_OWNER_OFF, null, null);

                        ItemStack userIconOn = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_USER_ON, null, null);
                        ItemStack userIconOff = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_USER_OFF, null, null);

                        ItemStack stockIconOn = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_STOCK_ON, null, null);
                        ItemStack stockIconOff = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_STOCK_OFF, null, null);

                        PlayerSettings.Option option = PlayerSettings.Option.NOTIFICATION_SALE_OWNER;

                        if(clicked.isSimilar(ownerIconOn)){
                            option = PlayerSettings.Option.NOTIFICATION_SALE_OWNER;
                            event.getInventory().setItem(event.getRawSlot(), ownerIconOff);
                        }
                        else if(clicked.isSimilar(ownerIconOff)){
                            option = PlayerSettings.Option.NOTIFICATION_SALE_OWNER;
                            event.getInventory().setItem(event.getRawSlot(), ownerIconOn);
                        }

                        else if(clicked.isSimilar(userIconOn)){
                            option = PlayerSettings.Option.NOTIFICATION_SALE_USER;
                            event.getInventory().setItem(event.getRawSlot(), userIconOff);
                        }
                        else if(clicked.isSimilar(userIconOff)){
                            option = PlayerSettings.Option.NOTIFICATION_SALE_USER;
                            event.getInventory().setItem(event.getRawSlot(), userIconOn);
                        }

                        else if(clicked.isSimilar(stockIconOn)){
                            option = PlayerSettings.Option.NOTIFICATION_STOCK;
                            event.getInventory().setItem(event.getRawSlot(), stockIconOff);
                        }
                        else if(clicked.isSimilar(stockIconOff)){
                            option = PlayerSettings.Option.NOTIFICATION_STOCK;
                            event.getInventory().setItem(event.getRawSlot(), stockIconOn);
                        }

                        Shop.getPlugin().getGuiHandler().toggleNotificationSetting(player, option);

                        //switch the color
//                        if(clicked.getDurability() == 5){
//                            clicked.setDurability((short)14);
//                        }
//                        else{
//                            clicked.setDurability((short)5);
//                        }

                        player.updateInventory();
                        return;
                    }
                    else if(window instanceof CommandsWindow){
                        String command = Shop.getPlugin().getCommandAlias() + " ";

                        ItemStack currencyIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.COMMANDS_CURRENCY, null, null);
                        ItemStack setCurrencyIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.COMMANDS_SET_CURRENCY, null, null);
                        ItemStack setGambleIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.COMMANDS_SET_GAMBLE, null, null);
                        ItemStack refreshIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.COMMANDS_REFRESH_DISPLAYS, null, null);
                        ItemStack reloadIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.COMMANDS_RELOAD, null, null);

                        ItemStack itemListAddIcon = null;
                        ItemStack itemListRemoveIcon = null;

                        if(plugin.getItemListType() == ItemListType.DENY_LIST){
                            itemListAddIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.COMMANDS_ITEMLIST_DENY_ADD, null, null);
                            itemListRemoveIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.COMMANDS_ITEMLIST_DENY_REMOVE, null, null);
                        }
                        else if(plugin.getItemListType() == ItemListType.ALLOW_LIST){
                            itemListAddIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.COMMANDS_ITEMLIST_ALLOW_ADD, null, null);
                            itemListRemoveIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.COMMANDS_ITEMLIST_ALLOW_REMOVE, null, null);
                        }


                        if(clicked.isSimilar(currencyIcon)){
                            command += "currency";
                        }
                        else if(clicked.isSimilar(setCurrencyIcon)){
                            command += "setcurrency";
                        }
                        else if(clicked.isSimilar(setGambleIcon)){
                            command += "setgamble";
                        }
                        else if(clicked.isSimilar(refreshIcon)){
                            command += "item refresh";
                        }
                        else if(clicked.isSimilar(reloadIcon)){
                            command += "reload";
                        }

                        if(plugin.getItemListType() != ItemListType.NONE) {
                            if (clicked.isSimilar(itemListAddIcon)) {
                                command += "itemlist add";
                            } else if (clicked.isSimilar(itemListRemoveIcon)) {
                                command += "itemlist remove";
                            }
                        }

                        plugin.getGuiHandler().closeWindow(player);
                        Bukkit.getServer().dispatchCommand(player, command);
                        return;
                    }
                    else if(window instanceof SearchWindow){
                        if(window.hasPrevWindow()){
                            plugin.getGuiHandler().setWindow(player, window.prevWindow);
                            return;
                        }
                    }
                }
            }
            //System.out.println("Inventory slot: "+event.getSlot());
            //System.out.println("Inventory raw slot: "+event.getRawSlot());
        }
    }
}
