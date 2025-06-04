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
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

import static com.snowgears.shop.util.ShopCreationProcess.ChatCreationStep.BARTER_ITEM;
import static com.snowgears.shop.util.ShopCreationProcess.ChatCreationStep.ITEM;

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

            if (clicked.getBlockData() instanceof WallSign) {
                AbstractShop shop = plugin.getShopHandler().getShop(clicked.getLocation());
                if (shop == null) {
                    return;
                } else if (shop.isInitialized()) {
                    return;
                }
                String message = null;
                if (!player.getUniqueId().equals(shop.getOwnerUUID())) {
                    if((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
                        ShopMessage.sendMessage("interactionIssue", "initialize", player, shop);
                        shop.sendEffects(false, player);
                        event.setCancelled(true);
                        return;
                    }
                }
                if (shop.getType() == ShopType.BARTER && shop.getItemStack() == null) {
                    ShopMessage.sendMessage("interactionIssue", "noItem", player, shop);
                    event.setCancelled(true);
                    return;
                }

                if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                    if (shop.getType() == ShopType.SELL) {
                        ShopMessage.sendMessage("interactionIssue", "noItem", player, shop);
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


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (playerDataMap.get(player.getUniqueId()) != null) {
            // Check if player is trying to move to a different block
            if (event.getFrom().getBlockZ() != event.getTo().getBlockZ()
                    || event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()) {
                
                // Instead of teleporting, set the "to" location to match the "from" location
                // This effectively cancels the movement without triggering a teleport event
                Location fixedLocation = event.getFrom().clone();
                
                // Preserve the player's head rotation so they can still look around
                fixedLocation.setYaw(event.getTo().getYaw());
                fixedLocation.setPitch(event.getTo().getPitch());
                
                event.setTo(fixedLocation);
                
                // Send message to inform the player they're locked in place
                sendPlayerLockedMessages(player, playerDataMap.get(player.getUniqueId()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTeleport(PlayerTeleportEvent event){
        Player player = event.getPlayer();
        if (playerDataMap.get(player.getUniqueId()) != null) {
            // Always allow teleports that we're doing internally for shop selection
            PlayerData data = playerDataMap.get(player.getUniqueId());
            Location shopLocation = data.getShopSignLocation();
            
            // If this is a teleport from another plugin or command going far away, cancel it
            if (event.getFrom().distanceSquared(event.getTo()) > 4 &&
                (shopLocation == null || event.getTo().distanceSquared(shopLocation) > 4)) {
                
                // Cancel the teleport event entirely instead of teleporting them back
                event.setCancelled(true);
                
                // Send message to inform the player they're locked in place
                sendPlayerLockedMessages(player, data);
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
        
        Player player = (Player) event.getPlayer();
        
        //don't remove player data if they just clicked the search icon
        try {
            Object view = event.getView();
            Method getTitle = view.getClass().getMethod("getTitle");
            getTitle.setAccessible(true);
            // Check if they're in a special GUI window, if so return early
            if(getTitle.invoke(view).equals(Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.HOME)) || 
               getTitle.invoke(view).equals(Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.LIST_SHOPS))){
                return;
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            plugin.getShopLogger().log(Level.SEVERE, "Error checking inventory title", e);
        }
        
        // Check if player is online - this handles the PlayerQuitEvent case as well
        if (player.isOnline()) {
            // Immediately remove from creative selection without delay
            boolean removed = removePlayerFromCreativeSelection(player);
            if (removed) {
                player.updateInventory();
            }
            
            // Check if we have a chat shop creation in process, and cancel it
            ShopCreationProcess process = plugin.getMiscListener().getShopCreationProcess(player);
            // If we are in the ITEM/BARTER_ITEM selection stage, then cancel the shop chat creation process!
            if (process != null && (process.getStep() == ITEM || process.getStep() == BARTER_ITEM)) {
                plugin.getMiscListener().cancelShopCreationProcess(player);
            }
        }
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
        
        // If the player is not in a creative selection mode we're tracking, ignore the event
        if (playerData == null) {
            return;
        }

        // Store the cursor item before cancellation for use in shop initialization
        ItemStack selectedItem = event.getCursor() != null ? event.getCursor().clone() : null;
        
        // Set event cancellation flags early to ensure nothing slips through
        // even if there's an exception in the processing code
        event.setResult(Event.Result.DENY);
        event.setCursor(new ItemStack(Material.AIR));
        event.setCancelled(true);

        // Only allow slot -999 (dropping outside the inventory) which is our intended
        // mechanism for selecting an item
        if (event.getSlot() == -999 && selectedItem != null) {
            if (!playerData.isGuiSearch()) {
                AbstractShop shop = playerData.getShop();
                if (shop != null) {
                    if (shop.getType() == ShopType.BUY || shop.getType() == ShopType.COMBO) {

                        PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                        Bukkit.getServer().getPluginManager().callEvent(e);

                        if (e.isCancelled()) {
                            return;
                        }

                        shop.setItemStack(selectedItem);
                        plugin.getShopCreationUtil().sendCreationSuccess(player, shop);

                    } else if (shop.getType() == ShopType.BARTER) {

                        PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                        Bukkit.getServer().getPluginManager().callEvent(e);

                        if (e.isCancelled()) {
                            return;
                        }

                        shop.setSecondaryItemStack(selectedItem);
                        plugin.getShopCreationUtil().sendCreationSuccess(player, shop);
                        plugin.getLogHandler().logAction(player, shop, ShopActionType.INIT);
                    }
                    removePlayerFromCreativeSelection(player);
                }
                //there is a chest creation process
                else if(plugin.getMiscListener().getShopCreationProcess(player) != null) {

                    //they just hit a chest with an open hand (so they are making a BUY shop) and now need to choose an item from creative selection
                    ShopCreationProcess currentProcess = plugin.getMiscListener().getShopCreationProcess(player);
                    if(currentProcess.getStep() == ITEM){
                        currentProcess.setItemStack(selectedItem);
                        currentProcess.setShopType(ShopType.BUY);
                        currentProcess.displayFloatingText(ShopType.BUY.toString(), "createHitChestAmount");
                        removePlayerFromCreativeSelection(player);
                    }
                    //they just hit a chest with an open hand when creating a barter shop and need choose a barter item from creative selection
                    else if(currentProcess.getStep() == ShopCreationProcess.ChatCreationStep.BARTER_ITEM){
                        currentProcess.setBarterItemStack(selectedItem);
                        currentProcess.displayFloatingText(ShopType.BARTER.toString(), "createHitChestBarterAmount");
                        removePlayerFromCreativeSelection(player);
                    }
                }
            }
            //player data is a GUI Search
            else{
                removePlayerFromCreativeSelection(player);

                ListSearchResultsWindow searchResultsWindow = new ListSearchResultsWindow(player.getUniqueId(), selectedItem);
                searchResultsWindow.setPrevWindow(new HomeWindow(player.getUniqueId()));
                plugin.getGuiHandler().setWindow(player, searchResultsWindow);
            }
        }
    }

    public void putPlayerInCreativeSelection(Player player, Location shopSignLocation, boolean guiSearch) {
        // Sanity check, make sure players don't somehow go into creative mode when it's disabled!
        if (!plugin.allowCreativeSelection()) {
            ShopMessage.sendMessage(ShopMessage.getUnformattedMessage("creativeSelection", "disabled"), player);
            return;
        }
        // Don't put them in creative if they are already in creative.
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

    // Check if the player logged out, if so, put them back in survival before they finish logging out
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if(playerDataMap.containsKey(player.getUniqueId())) {
            this.removePlayerFromCreativeSelection(player);
        }
    }

    /**
     * Make sure that if player somehow quit without getting their old data back, return it to them when they login next
     */
    @EventHandler
    public void onLogin(PlayerLoginEvent event){
        final Player player = event.getPlayer();
        
        // Immediately attempt to restore PlayerData if it exists
        PlayerData data = PlayerData.loadFromFile(player);
        if(data != null){
            // Use minimal delay (1 tick) since we need to ensure player is fully logged in
            // This is much safer than the previous 20-tick delay
            Shop.getPlugin().getFoliaLib().getScheduler().runLater(() -> {
                if (player.isOnline()) {
                    playerDataMap.remove(player.getUniqueId());
                    data.apply();
                    player.updateInventory();
                }
            }, 1); // 1 tick = 0.05 seconds
        }
    }

    private void sendPlayerLockedMessages(Player player, PlayerData playerData){
        // check when we sent them the last "locked in place" message, if it is within the past 2 seconds, don't send another message
        long lastMessageTime = playerData.getLastMessageTime();
        if(System.currentTimeMillis() - lastMessageTime < 2000){ return; }
        playerData.setLastMessageTime(System.currentTimeMillis());

        // display the floating text for the shop type, get the process
        ShopCreationProcess process = plugin.getMiscListener().getShopCreationProcess(player);
        if(process != null){
            if(playerData.isGuiSearch()){
                process.displayFloatingTextList("guiSearchSelection", "enter");
            }
            else {
                process.displayFloatingTextList("creativeSelection", "enter");
            }
        }
    }

    /**
     * Prevents players in creative selection mode from dragging items across inventory slots
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // If player is in creative selection mode, cancel the drag event
        if (playerDataMap.containsKey(player.getUniqueId())) {
            // Cancel the event immediately to prevent any item dragging
            event.setCancelled(true);
        }
    }

    /**
     * Handle general inventory clicks to provide an additional layer of protection
     * beyond the specific creative mode handler
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if player is in creative selection mode
        if (playerDataMap.containsKey(player.getUniqueId())) {
            PlayerData data = playerDataMap.get(player.getUniqueId());
            
            // For regular inventory clicks (not drop outside inventory)
            if (event.getSlot() != -999) {
                // Cancel all regular clicks to prevent inventory manipulation
                event.setCancelled(true);
                return;
            }
            
            // Let the InventoryCreativeEvent handler manage clicks to drop items outside the inventory
            // This is slot -999, which is handled by the onCreativeClick method
        }
    }

    /**
     * Prevent players from executing commands while in creative selection mode
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in creative selection mode
        if (playerDataMap.containsKey(player.getUniqueId())) {
            // Cancel all command execution
            event.setCancelled(true);
            
            // Inform the player
            ShopMessage.sendMessage(ShopMessage.getUnformattedMessage("creativeSelection", "noCommands"), player);
            
            // Reset the message time to prevent spamming locked in place messages
            PlayerData data = playerDataMap.get(player.getUniqueId());
            data.setLastMessageTime(System.currentTimeMillis());
        }
    }

    /**
     * Prevent players from swapping items between hands while in creative selection mode
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in creative selection mode
        if (playerDataMap.containsKey(player.getUniqueId())) {
            // Cancel the hand swap event
            event.setCancelled(true);
        }
    }

    /**
     * Prevent players from changing gamemode while in creative selection mode
     * and enforce creative mode if they're in the selection process
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in creative selection mode
        if (playerDataMap.containsKey(player.getUniqueId())) {
            // If they're trying to change to anything but creative mode, cancel it
            if (event.getNewGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
                
                // Force them back to creative mode to be sure
                player.setGameMode(GameMode.CREATIVE);
            }
        }
    }

    /**
     * Prevent players from switching items in their hotbar during creative selection
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in creative selection mode
        if (playerDataMap.containsKey(player.getUniqueId())) {
            // Cancel the hotbar slot change
            event.setCancelled(true);
        }
    }

    /**
     * Prevent players from interacting with entities during creative selection
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in creative selection mode
        if (playerDataMap.containsKey(player.getUniqueId())) {
            // Cancel all entity interactions
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevent players from interacting with entities at specific positions
     * during creative selection (more specific entity interaction)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in creative selection mode
        if (playerDataMap.containsKey(player.getUniqueId())) {
            // Cancel all specific entity interactions
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevent players from picking up items during creative selection
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Check if player is in creative selection mode
        if (playerDataMap.containsKey(player.getUniqueId())) {
            // Cancel all item pickups
            event.setCancelled(true);
        }
    }

    /**
     * If a player dies during creative selection, ensure their state is properly restored
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Check if player is in creative selection mode
        if (playerDataMap.containsKey(player.getUniqueId())) {
            // Make sure we restore the player's state correctly
            PlayerData data = playerDataMap.get(player.getUniqueId());
            
            // We're going to handle the player data restoration during respawn
            // so we store the UUID in case they disconnect
            plugin.getShopLogger().log(Level.WARNING, 
                "Player " + player.getName() + " died during creative selection. " +
                "Their gamemode will be restored on respawn.");
        }
    }
    
    /**
     * When a player respawns after dying in creative selection mode, ensure their state is restored
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in creative selection mode
        if (playerDataMap.containsKey(player.getUniqueId())) {
            // Restore the player's original gamemode and state
            boolean removed = removePlayerFromCreativeSelection(player);
            if (removed) {
                player.updateInventory();
                plugin.getShopLogger().log(Level.INFO, 
                    "Player " + player.getName() + "'s gamemode restored after death during creative selection.");
            }
        }
    }

    /**
     * Prevent players from toggling flight while in creative selection mode
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in creative selection mode
        if (playerDataMap.containsKey(player.getUniqueId())) {
            // Cancel the flight toggle event
            event.setCancelled(true);
        }
    }

    /**
     * Public method to check if a player is currently in creative selection mode
     * This can be used by other listeners to prevent unnecessary operations
     * 
     * @param player The player to check
     * @return true if the player is in creative selection mode, false otherwise
     */
    public boolean isPlayerInCreativeSelection(Player player) {
        if (player == null) {
            return false;
        }
        return playerDataMap.containsKey(player.getUniqueId());
    }
    
    /**
     * Public method to check if a player UUID is currently in creative selection mode
     * 
     * @param playerUuid The UUID of the player to check
     * @return true if the player is in creative selection mode, false otherwise
     */
    public boolean isPlayerInCreativeSelection(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        return playerDataMap.containsKey(playerUuid);
    }
}
