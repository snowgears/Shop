package com.snowgears.shop.listener;


import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import com.snowgears.shop.gui.HomeWindow;
import com.snowgears.shop.gui.ListSearchResultsWindow;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.PlayerData;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class CreativeSelectionListener implements Listener {

    private Shop plugin = Shop.getPlugin();
    private HashMap<UUID, PlayerData> playerDataMap = new HashMap<>();

    public CreativeSelectionListener(Shop instance) {
        plugin = instance;
    }

    //this method calls PlayerCreateShopEvent
    @EventHandler
    public void onPreShopSignClick(PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        if(!plugin.allowCreativeSelection())
            return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            final Block clicked = event.getClickedBlock();

            if (clicked.getType() == Material.WALL_SIGN) {
                AbstractShop shop = plugin.getShopHandler().getShop(clicked.getLocation());
                if (shop == null) {
                    return;
                } else if (shop.isInitialized()) {
                    return;
                }
                String message = null;
                if (!player.getUniqueId().equals(shop.getOwnerUUID())) {
                    if((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
                        message = ShopMessage.getMessage("interactionIssue", "initialize", shop, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                        event.setCancelled(true);
                        return;
                    }
                }
                if (shop.getType() == ShopType.BARTER && shop.getItemStack() == null) {
                    message = ShopMessage.getMessage("interactionIssue", "noItem", shop, player);
                    if(message != null && !message.isEmpty())
                        player.sendMessage(message);
                    event.setCancelled(true);
                    return;
                }

                if (player.getInventory().getItemInHand().getType() == Material.AIR) {
                    if (shop.getType() == ShopType.SELL) {
                        message = ShopMessage.getMessage("interactionIssue", "noItem", shop, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
                    } else {
                        if ((shop.getType() == ShopType.BARTER && shop.getItemStack() != null && shop.getSecondaryItemStack() == null)
                                || shop.getType() == ShopType.BUY) {
                            this.addPlayerData(player, clicked.getLocation(), false);
                        }
                    }
                }
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (playerDataMap.get(player.getUniqueId()) != null) {
            if (event.getFrom().getBlockZ() != event.getTo().getBlockZ()
                    || event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()) {
                player.teleport(event.getFrom());
                sendPlayerLockedMessages(player, playerDataMap.get(player.getUniqueId()));
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event){
        Player player = event.getPlayer();
        if (playerDataMap.get(player.getUniqueId()) != null) {
            if (event.getFrom().distanceSquared(event.getTo()) > 4) {
                event.setCancelled(true);
                sendPlayerLockedMessages(player, playerDataMap.get(player.getUniqueId()));
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (playerDataMap.get(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void inventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;
        //don't remove player data if they just clicked the search icon
        if(event.getView().getTitle().equals(Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.HOME))){
            return;
        }
        //for some reason this event is also called now on PlayerQuitEvent. Check that player didnt quit
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                Player player = plugin.getServer().getPlayer(event.getPlayer().getUniqueId());
                if(player != null) {
                    boolean removed = removePlayerData(player);
                    if (removed)
                        player.updateInventory();
                }
            }
        }, 10L); //0.5 second
    }

    @EventHandler
    public void onShopIntialize(PlayerInitializeShopEvent event){
        removePlayerData(event.getPlayer());
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onCreativeClick(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        if(!plugin.allowCreativeSelection())
            return;
        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = PlayerData.loadFromFile(player);
        if (playerData != null) {

            //player dropped item outside the inventory
            if (event.getSlot() == -999 && event.getCursor() != null) {
                if (!playerData.isGuiSearch()) {
                    AbstractShop shop = playerData.getShop();
                    if (shop != null) {
                        if (shop.getType() == ShopType.BUY) {

                            PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                            Bukkit.getServer().getPluginManager().callEvent(e);

                            if (e.isCancelled()) {
                                event.setResult(Event.Result.DENY);
                                event.setCursor(new ItemStack(Material.AIR));
                                event.setCancelled(true);
                                return;
                            }

                            shop.setItemStack(event.getCursor());
                            shop.getDisplay().spawn(null);
                            shop.updateSign();
                            String message = ShopMessage.getMessage(shop.getType().toString(), "create", shop, player);
                            if (message != null && !message.isEmpty())
                                player.sendMessage(message);
                            plugin.getTransactionListener().sendEffects(true, player, shop);
                            plugin.getShopHandler().saveShops(shop.getOwnerUUID());

                        } else if (shop.getType() == ShopType.BARTER) {

                            PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                            Bukkit.getServer().getPluginManager().callEvent(e);

                            if (e.isCancelled()) {
                                event.setResult(Event.Result.DENY);
                                event.setCursor(new ItemStack(Material.AIR));
                                event.setCancelled(true);
                                return;
                            }

                            shop.setSecondaryItemStack(event.getCursor());
                            shop.getDisplay().spawn(null);
                            shop.updateSign();
                            String message = ShopMessage.getMessage(shop.getType().toString(), "create", shop, player);
                            if (message != null && !message.isEmpty())
                                player.sendMessage(message);
                            plugin.getTransactionListener().sendEffects(true, player, shop);
                            plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                        }
                        removePlayerData(player);
                    }
                }
                //player data is a GUI Search
                else{
                    plugin.getGuiHandler().closeWindow(player);
                    removePlayerData(player);

                    ListSearchResultsWindow searchResultsWindow = new ListSearchResultsWindow(player.getUniqueId(), event.getCursor());
                    searchResultsWindow.setPrevWindow(new HomeWindow(player.getUniqueId()));
                    plugin.getGuiHandler().setWindow(player, searchResultsWindow);
                }
//                event.setResult(Event.Result.DENY);
//                event.setCancelled(true);
            }
            event.setResult(Event.Result.DENY);
            event.setCursor(new ItemStack(Material.AIR));
            event.setCancelled(true);
        }
    }

    public void addPlayerData(Player player, Location shopSignLocation, boolean guiSearch) {
        //System.out.println("Add Player Data called.");
        if(playerDataMap.containsKey(player.getUniqueId()))
            return;
        //System.out.println("Creating new player data.");
        PlayerData data = new PlayerData(player, shopSignLocation, guiSearch);
        playerDataMap.put(player.getUniqueId(), data);

        sendPlayerLockedMessages(player, data);
        player.setGameMode(GameMode.CREATIVE);
    }

    public boolean removePlayerData(Player player){
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if(data != null) {
            playerDataMap.remove(player.getUniqueId());
            data.apply();
            return true;
        }
        return false;
    }

    //make sure that if player somehow quit without getting their old data back, return it to them when they login next
    @EventHandler
    public void onLogin(PlayerLoginEvent event){
        final Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(Shop.getPlugin(), new Runnable() {
            @Override
            public void run() {
                PlayerData data = PlayerData.loadFromFile(player);
                if(data != null){
                    playerDataMap.remove(player.getUniqueId());
                    data.apply();
                }
            }
        }, 20);
    }

    private void sendPlayerLockedMessages(Player player, PlayerData playerData){
        if(playerData.isGuiSearch()){
            for (String message : ShopMessage.getMessageList("guiSearchSelection", "enter", null, null)) {
                if (message != null && !message.isEmpty())
                    player.sendMessage(message);
            }
        }
        else {
            for (String message : ShopMessage.getMessageList("creativeSelection", "enter", null, null)) {
                if (message != null && !message.isEmpty())
                    player.sendMessage(message);
            }
        }
    }
}
