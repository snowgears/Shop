package com.snowgears.shop.listener;


import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import com.snowgears.shop.gui.HomeWindow;
import com.snowgears.shop.gui.ListSearchResultsWindow;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.PlayerData;
import com.snowgears.shop.util.ShopActionType;
import com.snowgears.shop.util.ShopCreationProcess;
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
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class CreativeSelectionListener implements Listener {

    private Shop plugin;
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
                        plugin.getTransactionHelper().sendEffects(false, player, shop);
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
                                || shop.getType() == ShopType.BUY || shop.getType() == ShopType.COMBO) {
                            this.putPlayerInCreativeSelection(player, clicked.getLocation(), false);
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
    public void onPlayerDropItem(PlayerDropItemEvent event){
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
        if(event.getView().getTitle().equals(Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.HOME)) || event.getView().getTitle().equals(Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.LIST_SHOPS))){
            return;
        }
        //for some reason this event is also called now on PlayerQuitEvent. Check that player didnt quit
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                Player player = plugin.getServer().getPlayer(event.getPlayer().getUniqueId());
                if(player != null) {
                    boolean removed = removePlayerFromCreativeSelection(player);
                    if (removed)
                        player.updateInventory();
                }
            }
        }, 10L); //0.5 second
    }

    @EventHandler
    public void inventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;

        Player player = (Player)event.getPlayer();

        if (event.getInventory().getType() != InventoryType.CREATIVE){
            removePlayerFromCreativeSelection(player);
        }
    }

    @EventHandler
    public void onShopIntialize(PlayerInitializeShopEvent event){
        removePlayerFromCreativeSelection(event.getPlayer());
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
                        if (shop.getType() == ShopType.BUY || shop.getType() == ShopType.COMBO) {

                            PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                            Bukkit.getServer().getPluginManager().callEvent(e);

                            if (e.isCancelled()) {
                                event.setResult(Event.Result.DENY);
                                event.setCursor(new ItemStack(Material.AIR));
                                event.setCancelled(true);
                                return;
                            }

                            shop.setItemStack(event.getCursor());
                            plugin.getShopCreationUtil().sendCreationSuccess(player, shop);

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
                            plugin.getShopCreationUtil().sendCreationSuccess(player, shop);
                            plugin.getLogHandler().logAction(player, shop, ShopActionType.INIT);
                        }
                        removePlayerFromCreativeSelection(player);
                    }
                    //there is a chest creation process
                    else if(plugin.getMiscListener().getShopCreationProcess(player) != null) {

                        //they just hit a chest with an open hand (so they are making a BUY shop) and now need to choose an item from creative selection
                        ShopCreationProcess currentProcess = plugin.getMiscListener().getShopCreationProcess(player);
                        if(currentProcess.getStep() == ShopCreationProcess.ChatCreationStep.ITEM){
                            currentProcess.setItemStack(event.getCursor());
                            currentProcess.setShopType(ShopType.BUY);

                            String message = ShopMessage.getUnformattedMessage(currentProcess.getShopType().toString(), "createHitChestAmount");
                            message = ShopMessage.formatMessage(message, currentProcess, player);
                            if (message != null && !message.isEmpty())
                                player.sendMessage(message);

                            removePlayerFromCreativeSelection(player);
                        }
                        //they just hit a chest with an open hand when creating a barter shop and need choose a barter item from creative selection
                        else if(currentProcess.getStep() == ShopCreationProcess.ChatCreationStep.BARTER_ITEM){
                            currentProcess.setBarterItemStack(event.getCursor());

                            String message = ShopMessage.getUnformattedMessage(currentProcess.getShopType().toString(), "createHitChestBarterAmount");
                            message = ShopMessage.formatMessage(message, currentProcess, player);
                            if (message != null && !message.isEmpty())
                                player.sendMessage(message);

                            removePlayerFromCreativeSelection(player);
                        }
                    }
                }
                //player data is a GUI Search
                else{
                    removePlayerFromCreativeSelection(player);

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

    public void putPlayerInCreativeSelection(Player player, Location shopSignLocation, boolean guiSearch) {
        if(playerDataMap.containsKey(player.getUniqueId())) {
            return;
        }
        //System.out.println("Creating new player data.");
        PlayerData data = new PlayerData(player, shopSignLocation, guiSearch);
        playerDataMap.put(player.getUniqueId(), data);

        sendPlayerLockedMessages(player, data);
        player.setGameMode(GameMode.CREATIVE);
    }

    public boolean removePlayerFromCreativeSelection(Player player){
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if(data != null) {
            playerDataMap.remove(player.getUniqueId());
            data.apply();
            player.closeInventory();
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
