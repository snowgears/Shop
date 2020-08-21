package com.snowgears.shop.gui;


import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.util.PlayerSettings;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class ShopGUIListener implements Listener {

    private Shop plugin = Shop.getPlugin();

    public ShopGUIListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event){
        if(event.getWhoClicked() instanceof Player){
            Player player = (Player)event.getWhoClicked();

            ShopGuiWindow window = plugin.getGuiHandler().getWindow(player);

            if(event.getView().getTitle().equals(window.getTitle())){

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
                        ItemStack listShopsIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_LIST_OWN_SHOPS, null, null);
                        ItemStack listPlayersIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_LIST_PLAYERS, null, null);
                        ItemStack settingsIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_SETTINGS, null, null);
                        ItemStack commandsIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_COMMANDS, null, null);

                        if(clicked.getType() == listShopsIcon.getType()){
                            ListShopsWindow shopsWindow = new ListShopsWindow(player.getUniqueId(), player.getUniqueId());
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
                        ItemStack playerIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_PLAYER, null, null);
                        ItemStack adminPlayerIcon = plugin.getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_PLAYER_ADMIN, null, null);

                        if(clicked.getType() == playerIcon.getType() || clicked.getType() == adminPlayerIcon.getType()){
                            String name = clicked.getItemMeta().getDisplayName();
                            UUID uuid = null;
                            if(name.equals(adminPlayerIcon.getItemMeta().getDisplayName())){
                                uuid = Shop.getPlugin().getShopHandler().getAdminUUID();
                            }
                            else {
                                OfflinePlayer p = Bukkit.getOfflinePlayer(name);
                                if(p != null)
                                    uuid = p.getUniqueId();
                            }

                            if(uuid == null)
                                return;

                            ListShopsWindow shopsWindow = new ListShopsWindow(player.getUniqueId(), uuid);
                            shopsWindow.setPrevWindow(window);
                            plugin.getGuiHandler().setWindow(player, shopsWindow);
                            return;
                        }
                    }
                    else if(window instanceof ListShopsWindow){
                        List<String> lore = clicked.getItemMeta().getLore();
                        if(lore != null){
                            for(String line : lore){
                                if(line.startsWith("Location: ")){
                                    line = line.substring(10, line.length());
                                    Location loc = UtilMethods.getLocation(line);
                                    AbstractShop shop = plugin.getShopHandler().getShop(loc);

                                    if(shop != null){
                                        if(Shop.getPlugin().usePerms()){
                                            if(player.hasPermission("shop.operator") || player.hasPermission("shop.gui.teleport")){
                                                shop.teleportPlayer(player);
                                            }
                                        }
                                        else{
                                            if(player.isOp()){
                                                shop.teleportPlayer(player);
                                            }
                                        }
                                        return;
                                    }
                                }
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

                        PlayerSettings.Option option = PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS;

                        if(clicked.isSimilar(ownerIconOn)){
                            option = PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS;
                            event.getInventory().setItem(event.getRawSlot(), ownerIconOff);
                        }
                        else if(clicked.isSimilar(ownerIconOff)){
                            option = PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS;
                            event.getInventory().setItem(event.getRawSlot(), ownerIconOn);
                        }

                        else if(clicked.isSimilar(userIconOn)){
                            option = PlayerSettings.Option.SALE_USER_NOTIFICATIONS;
                            event.getInventory().setItem(event.getRawSlot(), userIconOff);
                        }
                        else if(clicked.isSimilar(userIconOff)){
                            option = PlayerSettings.Option.SALE_USER_NOTIFICATIONS;
                            event.getInventory().setItem(event.getRawSlot(), userIconOn);
                        }

                        else if(clicked.isSimilar(stockIconOn)){
                            option = PlayerSettings.Option.STOCK_NOTIFICATIONS;
                            event.getInventory().setItem(event.getRawSlot(), stockIconOff);
                        }
                        else if(clicked.isSimilar(stockIconOff)){
                            option = PlayerSettings.Option.STOCK_NOTIFICATIONS;
                            event.getInventory().setItem(event.getRawSlot(), stockIconOn);
                        }

                        Shop.getPlugin().getGuiHandler().toggleSettingsOption(player, option);

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

                        player.closeInventory();
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
