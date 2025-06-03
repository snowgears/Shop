package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.AbstractDisplay;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ComboShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.DisplayUtil;
import com.snowgears.shop.util.ItemListType;
import com.snowgears.shop.util.ShopLogger;
import com.snowgears.shop.util.UtilMethods;
import com.snowgears.shop.util.CompatibilityUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;


public class ShopHandler {

    public Shop plugin;
    private Class<?> displayClass;

    private ConcurrentHashMap<Location, AbstractShop> allShops = new ConcurrentHashMap<>();
    private ConcurrentHashMap<UUID, List<Location>> playerShops = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, List<Location>> chunkShops = new ConcurrentHashMap<>(); //String key = world_x_z
    private ConcurrentHashMap<UUID, HashSet<Location>> playersWithActiveShopDisplays = new ConcurrentHashMap<>();
    private HashSet<UUID> playersProcessingShopDisplays = new HashSet<>();
    private HashMap<UUID, Location> playersActiveShopDisplayTag = new HashMap<>();

    //all loading of shops happens async at onEnable()
    //shops that still need to calculate their facing direction based on sign are considered "unloaded"
    //we will be loading these shops at time of chunkload and resaving them so they are saved with the 'facing' variable
    private ConcurrentHashMap<String, List<Location>> unloadedShopsByChunk = new ConcurrentHashMap<>();
    private UUID adminUUID;
    private BlockFace[] directions = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private ArrayList<ItemStack> itemListItems = new ArrayList<>();

    private ArrayList<UUID> playersSavingShops = new ArrayList<>();
    
    // Map to track player last processed locations for movement-based display updates
    private ConcurrentHashMap<UUID, Location> lastProcessedLocations = new ConcurrentHashMap<>();

    // Cache for player connections to avoid expensive reflection calls
    private ConcurrentHashMap<UUID, Object> playerConnectionCache = new ConcurrentHashMap<>();

    // Default chunk radius for shop location searches and maximum display distance
    // these values come from the Shop class configuration - see getShopSearchRadius() and getMaxShopDisplayDistance()
    
    // Very close distance for immediate shop display
    private static final double CLOSE_SHOP_DISPLAY_DISTANCE = 2.0;

    // Teleport cooldown map to prevent multiple display updates during teleportation
    private ConcurrentHashMap<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();
    // Cooldown time in milliseconds (500ms = half a second)
    private static final long TELEPORT_COOLDOWN_MS = 500;

    public ShopHandler(Shop instance) {
        plugin = instance;
        adminUUID = UUID.randomUUID();
        initDisplayClass();
        initItemList();

        plugin.getFoliaLib().getScheduler().runLater(() -> {
            loadShops();
        }, 10);
    }

    private void disableDisplayClass() {
        try {
            final Class<?> clazz = Class.forName("com.snowgears.shop.display.DisplayDisabled");
            if (AbstractDisplay.class.isAssignableFrom(clazz))
                this.displayClass = clazz;
        } catch (final Exception e) {
            Shop.getPlugin().getShopLogger().severe("Failed to load DisplayDisabled class.");
            Shop.getPlugin().onDisable();
        } catch (Error e) {
            Shop.getPlugin().getShopLogger().severe("Failed to load DisplayDisabled class.");
            Shop.getPlugin().onDisable();
        }
    }

    private boolean initDisplayClass(){
        String packageName = plugin.getServer().getClass().getPackage().getName();

        // Check if we are on a Paper 1.20.6+ server, or if we are running Spigot v1.20.6 or later :)
        // Now that our new Display class purely uses Class loading to get the appropriate class, we don't
        // need to load a specific revision version class (unless we are old)
        
        boolean isNewVersion = false;
        try {
            // Try to get server version - could be double (modern) or String (legacy)
            Object serverVersion = plugin.getNmsBullshitHandler().getServerVersion();
            if (serverVersion instanceof Double) {
                isNewVersion = ((Double) serverVersion) >= 120.6D;
            } else if (serverVersion instanceof String) {
                // Legacy versions return String - treat as old version
                isNewVersion = false;
            }
        } catch (Exception e) {
            // Fallback to legacy behavior if something goes wrong
            isNewVersion = false;
        }
        
        if (packageName.equals("org.bukkit.craftbukkit") || isNewVersion) {
            // We are on a newer version that does not relocate CB classes, load the default display package
            try {
                Shop.getPlugin().getShopLogger().info("Using item display handler - com.snowgears.shop.display.Display");
                final Class<?> clazz = Class.forName("com.snowgears.shop.display.Display");
                if (AbstractDisplay.class.isAssignableFrom(clazz)) {
                    this.displayClass = clazz;
                    return true;
                }
            } catch (final Exception e) {
                Shop.getPlugin().getShopLogger().severe("Error while loading 'com.snowgears.shop.display.Display'. " + e.getMessage());
                e.printStackTrace();
                disableDisplayClass();
                return false;
            } catch (Error e) {
                Shop.getPlugin().getShopLogger().severe("Error while loading 'com.snowgears.shop.display.Display'. " + e.getMessage());
                e.printStackTrace();
                disableDisplayClass();
                return false;
            }
            return false;
        } else {
            // We are still on an older version, so go ahead
            String nmsVersion = packageName.substring(packageName.lastIndexOf('.') + 1);

            // version did remap even though version number didn't increase
            String mcVersion = Bukkit.getBukkitVersion().substring(0, Bukkit.getBukkitVersion().indexOf('-'));
            //im not doing this right now. I'm only going to support 1.17.1 for now
            //        if (mcVersion.equals("1.17.1")) {
            //            nmsVersion =  "v1_17_R1_2";
            //        }

            try {
                Shop.getPlugin().getShopLogger().info( "Minecraft version is old or Spigot, watch out for bugs!");
                Shop.getPlugin().getShopLogger().info("Using display class - com.snowgears.shop.display.Display_" + nmsVersion);
                final Class<?> clazz = Class.forName("com.snowgears.shop.display.Display_" + nmsVersion);
                if (AbstractDisplay.class.isAssignableFrom(clazz)) {
                    this.displayClass = clazz;
                    return true;
                }
            } catch (final Exception e) {
                Shop.getPlugin().getShopLogger().severe("Error while loading com.snowgears.shop.display.Display_" + nmsVersion + " " + e.getMessage());
                e.printStackTrace();
                disableDisplayClass();
                return false;
            } catch (Error e) {
                Shop.getPlugin().getShopLogger().severe("Error while loading com.snowgears.shop.display.Display_" + nmsVersion + " " + e.getMessage());
                e.printStackTrace();
                disableDisplayClass();
                return false;
            }
            
            Shop.getPlugin().getShopLogger().severe("Unknown issue hooking into Minecraft Packet Classes, disabling display features.");
            disableDisplayClass();
            return false;
        }
    }

    public AbstractDisplay createDisplay(Location loc){
        try {
            AbstractDisplay display = (AbstractDisplay) displayClass.getConstructor(Location.class).newInstance(loc);
            return display;
        } catch (Exception e){
            // e.printStackTrace();
            plugin.getShopLogger().warning("Error creating display at | World: " + loc.getWorld().getName() + " at " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ());
        }
        return null;
    }

    public AbstractShop getShop(Location loc) {
        return allShops.get(loc);
    }

    public AbstractShop getShopByChest(Block shopChest) {

        try {
            if(isChest(shopChest)) {

                AbstractShop shop = null;
                InventoryHolder ih = null;

                //if the shop is a single chest or double chest, add the chest blocks to check
                if (shopChest.getState() instanceof Chest) {
                    Chest chest = (Chest) shopChest.getState();
                    ih = chest.getInventory().getHolder();

                    if (ih instanceof DoubleChest) {

                        DoubleChest dc = (DoubleChest) ih;
                        Chest leftChest = (Chest) dc.getLeftSide();
                        Chest rightChest = (Chest) dc.getRightSide();

                        for (BlockFace direction : directions) {
                            shop = this.getShop(leftChest.getBlock().getRelative(direction).getLocation());
                            if (shop != null) {
                                //make sure the shop sign you found is actually attached to the correct shop
                                if (leftChest.getLocation().equals(shop.getChestLocation()) || rightChest.getLocation().equals(shop.getChestLocation()))
                                    return shop;
                            }
                            shop = this.getShop(rightChest.getBlock().getRelative(direction).getLocation());
                            if (shop != null) {
                                //make sure the shop sign you found is actually attached to the correct shop
                                if (shop.getChestLocation().equals(leftChest.getLocation()) || shop.getChestLocation().equals(rightChest.getLocation()))
                                    return shop;
                            }
                        }
                        return null;
                    }
                }

                for (BlockFace direction : directions) {
                    shop = this.getShop(shopChest.getRelative(direction).getLocation());
                    if (shop != null) {
                        //make sure the shop sign you found is actually attached to the correct shop
                        if (shopChest.getLocation().equals(shop.getChestLocation()))
                            return shop;
                    }
                }
                return null;
            }
        } catch (NoClassDefFoundError e) {}

// TODO not sure what this code was even doing here. might have been leftover

//        if (this.isChest(shopChest)) {
//            //BlockFace chestFacing = UtilMethods.getDirectionOfChest(shopChest);
//
//            ArrayList<Block> chestBlocks = new ArrayList<>();
//            chestBlocks.add(shopChest);
//
//            InventoryHolder ih = null;
//            if(shopChest.getState() instanceof Chest) {
//                Chest chest = (Chest) shopChest.getState();
//                ih = chest.getInventory().getHolder();
//
//                if (ih instanceof DoubleChest) {
//                    DoubleChest dc = (DoubleChest) ih;
//                    Chest leftChest = (Chest) dc.getLeftSide();
//                    Chest rightChest = (Chest) dc.getRightSide();
//                    if (chest.getLocation().equals(leftChest.getLocation()))
//                        chestBlocks.add(rightChest.getBlock());
//                    else
//                        chestBlocks.add(leftChest.getBlock());
//                }
//            }
//
//            for (Block chestBlock : chestBlocks) {
//                Block signBlock = chestBlock.getRelative(chestFacing);
//                if (signBlock.getBlockData() instanceof WallSign) {
//                    WallSign sign = (WallSign) signBlock.getBlockData();
//                    //if (chestFacing == sign.getFacing()) {
//                    AbstractShop shop = this.getShop(signBlock.getLocation());
//                    if (shop != null)
//                        return shop;
//                    //}
//                } else if(!(ih instanceof DoubleChest)){
//                    AbstractShop shop = this.getShop(signBlock.getLocation());
//                    //delete the shop if it doesn't have a sign
//                    if (shop != null)
//                        shop.delete();
//                }
//            }
//        }
        return null;
    }

    public AbstractShop getShopTouchingBlock(Block block){
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for(BlockFace face : faces){
            if(this.isChest(block.getRelative(face))){
                Block shopChest = block.getRelative(face);
                for(BlockFace newFace : faces){
                    if(shopChest.getRelative(newFace).getBlockData() instanceof WallSign){
                        AbstractShop shop = getShop(shopChest.getRelative(newFace).getLocation());
                        if(shop != null)
                            return shop;
                    }
                }
            }
        }
        return null;
    }

    public void addShop(AbstractShop shop) {

        //this is to remove a bug that caused one shop to be saved to multiple files at one point
        AbstractShop s = getShop(shop.getSignLocation());
        if(s != null) {
            return;
        }
        allShops.put(shop.getSignLocation(), shop);

        List<Location> playerShopLocations = getShopLocations(shop.getOwnerUUID());
        if(!playerShopLocations.contains(shop.getSignLocation())) {
            playerShopLocations.add(shop.getSignLocation());
            playerShops.put(shop.getOwnerUUID(), playerShopLocations);
        }

        String chunkKey = getChunkKey(shop.getSignLocation());
        List<Location> chunkShopLocations = getShopLocations(chunkKey);
        //System.out.println("[Shop] 1 - chunkShopLocations "+chunkShopLocations);
        if(!chunkShopLocations.contains(shop.getSignLocation())) {
            chunkShopLocations.add(shop.getSignLocation());
            chunkShops.put(chunkKey, chunkShopLocations);
        }

        plugin.getGuiHandler().reloadPlayerHeadIcon(shop);
    }

    //This method should only be used by AbstractShop object to delete
    public boolean removeShop(AbstractShop shop) {
        if (allShops.containsKey(shop.getSignLocation())) {
            allShops.remove(shop.getSignLocation());
        }
        if(playerShops.containsKey(shop.getOwnerUUID())){
            List<Location> playerShopLocations = getShopLocations(shop.getOwnerUUID());
            if(playerShopLocations.contains(shop.getSignLocation())) {
                playerShopLocations.remove(shop.getSignLocation());
                playerShops.put(shop.getOwnerUUID(), playerShopLocations);
            }
        }
        String chunkKey = getChunkKey(shop.getSignLocation());
        if(chunkShops.containsKey(chunkKey)){
            List<Location> chunkShopLocations = getShopLocations(chunkKey);
            if(chunkShopLocations.contains(shop.getSignLocation())) {
                chunkShopLocations.remove(shop.getSignLocation());
                chunkShops.put(chunkKey, chunkShopLocations);
            }
        }

        return false;
    }

    public void processUnloadedShopsInChunk(Chunk chunk){
        String key = getChunkKey(chunk);
        if(unloadedShopsByChunk.containsKey(key)){
            List<UUID> playerUUIDs = new ArrayList<>();
            List<Location> shopLocations = getUnloadedShopsByChunk(key);
            for(Location shopLocation : shopLocations) {
                AbstractShop shop = getShop(shopLocation);
                if(shop != null){
                    // Run at the shop's location to ensure it works in the correct region in Folia
                    plugin.getFoliaLib().getScheduler().runAtLocation(shopLocation, task -> {
                        boolean signExists = shop.load();
                        if(signExists) {
                            if (!playerUUIDs.contains(shop.getOwnerUUID())) {
                                playerUUIDs.add(shop.getOwnerUUID());
                            }
                        }
                        else{
                            removeShop(shop);
                        }
                    });
                }
            }
            unloadedShopsByChunk.remove(key);
        }
    }

    public void addUnloadedShopToChunkList(AbstractShop shop){
        String chunkKey = getChunkKey(shop.getSignLocation());
        List<Location> shopLocations = getUnloadedShopsByChunk(chunkKey);
        if(!shopLocations.contains(shop.getSignLocation())) {
            shopLocations.add(shop.getSignLocation());
            unloadedShopsByChunk.put(chunkKey, shopLocations);
        }
    }

    public List<AbstractShop> getAllShops(){
        return allShops.values().stream().collect(
                Collectors.toCollection(ArrayList::new)
        );
    }

    public List<AbstractShop> getShops(UUID player){
        List<AbstractShop> shops = new ArrayList<>();
        for(Location shopSign : getShopLocations(player)){
            AbstractShop shop = getShop(shopSign);
            if(shop != null)
                shops.add(shop);
        }
        return shops;
    }

    public int numShopsNeedSave(UUID player){
        List<AbstractShop> shops = getShops(player);

        // Default does not need to be saved;
        int needToBeSaved = 0;
        for (AbstractShop shop : shops) {
            if (shop.needsSave()) { needToBeSaved++; }
        }

        return needToBeSaved;
    }

    public List<AbstractShop> getShopsByItem(ItemStack itemStack){
        List<AbstractShop> shops = new ArrayList<>();
        for(AbstractShop shop : allShops.values()){
            if(shop.getItemStack() != null && shop.getItemStack().getType() == itemStack.getType())
                shops.add(shop);
            else if(shop.getSecondaryItemStack() != null && shop.getSecondaryItemStack().getType() == itemStack.getType())
                shops.add(shop);
        }
        return shops;
    }

    //TODO this is too resource intensive on large servers
    public List<OfflinePlayer> getShopOwners(){
        ArrayList<OfflinePlayer> owners = new ArrayList<>();
        for(UUID player : playerShops.keySet()) {
            owners.add(Bukkit.getOfflinePlayer(player));
        }
        return owners;
    }

    public List<UUID> getShopOwnerUUIDs(){
        ArrayList<UUID> owners = new ArrayList<>();
        for(UUID player : playerShops.keySet()) {
            owners.add(player);
        }
        return owners;
    }

    private List<Location> getShopLocations(UUID player){
        List<Location> shopLocations;
        if(playerShops.containsKey(player)) {
            shopLocations = playerShops.get(player);
        }
        else
            shopLocations = new ArrayList<>();
        return shopLocations;
    }

    private List<Location> getShopLocations(Location locationInChunk){
        String chunkKey = getChunkKey(locationInChunk);
        return getShopLocations(chunkKey);
    }

    private List<Location> getShopLocations(String chunkKey){
        List<Location> shopLocations;
        if(chunkShops.containsKey(chunkKey)) {
            shopLocations = chunkShops.get(chunkKey);
        }
        else {
            shopLocations = new ArrayList<>();
        }
        return shopLocations;
    }

    /**
     * Gets shop locations near a specific location within a default radius of 1 chunk
     * (which covers a 3x3 chunk area)
     * 
     * @param location The center location to search around
     * @return HashSet of shop locations in the surrounding chunks
     */
    public HashSet<Location> getShopLocationsNearLocation(Location location) {
        return getShopLocationsNearLocation(location, plugin.getShopSearchRadius());
    }

    /**
     * Gets shop locations near a specific location within a specified chunk radius
     * 
     * @param location The center location to search around
     * @param chunkRadius The radius (in chunks) to search around the center location
     *                    A radius of 1 means a 3x3 chunk area, 2 means 5x5, etc.
     * @return HashSet of shop locations in the surrounding chunks
     */
    public HashSet<Location> getShopLocationsNearLocation(Location location, int chunkRadius) {
        if (chunkRadius < 0) {
            throw new IllegalArgumentException("Chunk radius cannot be negative");
        }
        
        int chunkX = getChunkX(location);
        int chunkZ = getChunkZ(location);
        String worldName = location.getWorld().getName();
        
        HashSet<Location> shopsNearLocation = new HashSet<>();
        
        // Loop through all chunks in the specified radius
        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                String chunkKey = createChunkKey(worldName, chunkX + x, chunkZ + z);
                List<Location> shopLocations = getShopLocations(chunkKey);
                shopsNearLocation.addAll(shopLocations);
            }
        }
        
        return shopsNearLocation;
    }

    /**
     * Gets actual shop objects near a specific location within the default radius
     * 
     * @param location The center location to search around
     * @return List of shops in the surrounding chunks
     */
    public List<AbstractShop> getShopsNearLocation(Location location) {
        return getShopsNearLocation(location, plugin.getShopSearchRadius());
    }

    /**
     * Gets actual shop objects near a specific location within a specified chunk radius
     * 
     * @param location The center location to search around
     * @param chunkRadius The radius (in chunks) to search around the center location
     * @return List of shops in the surrounding chunks
     */
    public List<AbstractShop> getShopsNearLocation(Location location, int chunkRadius) {
        List<AbstractShop> shopsNearLocation = new ArrayList<>();
        
        // Get shop locations in the specified radius
        for (Location shopLocation : getShopLocationsNearLocation(location, chunkRadius)) {
            AbstractShop shop = getShop(shopLocation);
            if (shop != null) {
                shopsNearLocation.add(shop);
            }
        }
        
        return shopsNearLocation;
    }

    /**
     * Gets shop locations near a specific location within a specified chunk radius,
     * filtered by maximum distance in blocks.
     * 
     * @param location The center location to search around
     * @param chunkRadius The radius (in chunks) to search around the center location
     * @param maxDistanceSquared The maximum squared distance (in blocks) to include shops
     *                          Using squared distance avoids expensive square root calculations
     * @return HashSet of shop locations within the distance limit
     */
    public HashSet<Location> getShopLocationsNearLocationWithinDistance(Location location, int chunkRadius, double maxDistanceSquared) {
        HashSet<Location> nearbyLocations = getShopLocationsNearLocation(location, chunkRadius);
        HashSet<Location> filteredLocations = new HashSet<>();
        
        // Filter by distance
        for (Location shopLocation : nearbyLocations) {
            // Using distanceSquared is more efficient than distance
            if (location.distanceSquared(shopLocation) <= maxDistanceSquared) {
                filteredLocations.add(shopLocation);
            }
        }
        
        return filteredLocations;
    }

    /**
     * Gets actual shop objects near a specific location within a specified radius in blocks
     * 
     * @param location The center location to search around
     * @param chunkRadius The radius (in chunks) to search around the center location
     * @param maxDistance The maximum distance (in blocks) to include shops
     * @return List of shops within the distance limit
     */
    public List<AbstractShop> getShopsNearLocationWithinDistance(Location location, int chunkRadius, double maxDistance) {
        List<AbstractShop> shops = new ArrayList<>();
        double maxDistanceSquared = maxDistance * maxDistance;
        
        for (Location shopLocation : getShopLocationsNearLocationWithinDistance(location, chunkRadius, maxDistanceSquared)) {
            AbstractShop shop = getShop(shopLocation);
            if (shop != null) {
                shops.add(shop);
            }
        }
        
        return shops;
    }

    public void processShopDisplaysNearPlayer(Player player){
        // If the player is already being processed, don't start another process
        if (playersProcessingShopDisplays.contains(player.getUniqueId())) {
            return;
        }
        
        // Get current player location
        Location currentLocation = player.getLocation();
        
        // Check if player has moved enough to warrant processing
        Location lastLocation = lastProcessedLocations.get(player.getUniqueId());
        double movementThreshold = plugin.getDisplayMovementThreshold();
        
        // Skip processing if player hasn't moved enough and this isn't the first check
        if (lastLocation != null && 
            lastLocation.getWorld().equals(currentLocation.getWorld()) && 
            lastLocation.distanceSquared(currentLocation) < (movementThreshold * movementThreshold)) {
            return;
        }
        
        // Mark player as being processed to prevent concurrent processing
        playersProcessingShopDisplays.add(player.getUniqueId());
        
        // Schedule display processing task at the player's entity
        plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
            try {
                // Use a local variable for current location to avoid race conditions
                Location playerLocation = player.getLocation();
                
                // Update the last processed location immediately to prevent multiple processings
                lastProcessedLocations.put(player.getUniqueId(), playerLocation.clone());
                
                // Get all shop locations within the maximum display distance in one batch
                HashSet<Location> nearbyShopLocations = getShopLocationsNearLocationWithinDistance(
                    playerLocation, 
                    plugin.getShopSearchRadius(), 
                    plugin.getMaxShopDisplayDistance() * plugin.getMaxShopDisplayDistance()
                );
                
                // Create a batch operation for all displays to minimize interference
                // This helps prevent the "bouncing" effect when displays are created one by one
                processBatchDisplayUpdates(player, playerLocation, nearbyShopLocations);
                
            } catch (Exception e) {
                plugin.getShopLogger().warning("Error processing shop displays for player " + player.getName());
                e.printStackTrace();
            } finally {
                // Always ensure player is removed from processing list
                playersProcessingShopDisplays.remove(player.getUniqueId());
            }
        }, 1);
    }

    /**
     * Process all shop displays in a single coordinated batch to minimize visual artifacts
     * @param player The player to update displays for
     * @param playerLocation The player's current location
     * @param shopLocations Set of shop locations to process
     */
    private void processBatchDisplayUpdates(Player player, Location playerLocation, HashSet<Location> shopLocations) {
        if (!player.isOnline()) return;
        
        // Log the processing if in debug mode
        plugin.getShopLogger().debug("Processing batch display update for " + player.getName() + 
            " at " + playerLocation.getWorld().getName() + 
            " [" + playerLocation.getBlockX() + "," + playerLocation.getBlockY() + "," + playerLocation.getBlockZ() + "]" +
            " with " + shopLocations.size() + " nearby shops");
        
        // First, collect all displays that need to be shown and those that need to be removed
        HashSet<Location> displaysToShow = new HashSet<>();
        HashSet<Location> displaysToRemove = new HashSet<>();
        
        // Determine which displays to show and which to remove
        for (Location shopLocation : shopLocations) {
            AbstractShop shop = getShop(shopLocation);
            if (shop == null) continue;
            
            double distance = playerLocation.distance(shop.getSignLocation());
            
            if (distance < plugin.getMaxShopDisplayDistance()) {
                // Within display distance, should be shown
                displaysToShow.add(shopLocation);
            } else {
                // Too far, should be removed
                displaysToRemove.add(shopLocation);
            }
        }
        
        // Also identify any current displays that are no longer in range
        if (playersWithActiveShopDisplays.containsKey(player.getUniqueId())) {
            HashSet<Location> activeDisplays = new HashSet<>(playersWithActiveShopDisplays.get(player.getUniqueId()));
            for (Location displayLocation : activeDisplays) {
                if (!shopLocations.contains(displayLocation)) {
                    displaysToRemove.add(displayLocation);
                }
            }
        }
        
        // Process removals first to prevent interference with new spawns
        for (Location locationToRemove : displaysToRemove) {
            AbstractShop shop = getShop(locationToRemove);
            if (shop != null) {
                shop.getDisplay().remove(player);
                removeActiveShopDisplay(player, locationToRemove);
            }
        }
        
        // Short delay before processing additions to ensure removals are complete
        // This helps prevent the visual "refresh" effect
        plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
            // Now process additions in priority order (closest first)
            List<Map.Entry<Location, Double>> sortedLocations = new ArrayList<>();
            
            for (Location locationToShow : displaysToShow) {
                if (!hasActiveDisplay(player, locationToShow)) {
                    double distance = playerLocation.distance(locationToShow);
                    sortedLocations.add(new SimpleEntry<>(locationToShow, distance));
                }
            }
            
            // Sort by distance (closest first)
            sortedLocations.sort(Comparator.comparing(Map.Entry::getValue));
            
            // Process in distance order with small delays between batches to reduce visual clutter
            // Use configurable batch size from config
            int batchSize = plugin.getDisplayBatchSize();
            int batchDelay = plugin.getDisplayBatchDelay();
            int totalBatches = (sortedLocations.size() + batchSize - 1) / batchSize;
            
            plugin.getShopLogger().debug("Creating " + sortedLocations.size() + " displays in " + totalBatches + " batches for " + player.getName());
            
            for (int batch = 0; batch < totalBatches; batch++) {
                final int currentBatch = batch;
                
                // Add a configurable delay between batches
                plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
                    if (!player.isOnline()) return;
                    
                    int startIndex = currentBatch * batchSize;
                    int endIndex = Math.min(startIndex + batchSize, sortedLocations.size());
                    
                    for (int i = startIndex; i < endIndex; i++) {
                        Location locationToShow = sortedLocations.get(i).getKey();
                        AbstractShop shop = getShop(locationToShow);
                        
                        if (shop != null && player.isOnline()) {
                            shop.getDisplay().spawn(player);
                            addActiveShopDisplay(player, locationToShow);
                        }
                    }
                }, batch * batchDelay); // Configurable delay between batches
            }
        }, 2); // 2 tick delay after removals
    }

    public void clearShopDisplaysNearPlayer(Player player){
        if(playersWithActiveShopDisplays.containsKey(player.getUniqueId()))
            playersWithActiveShopDisplays.remove(player.getUniqueId());
        
        // Also remove player from last processed locations
        lastProcessedLocations.remove(player.getUniqueId());
        
        // Also remove from processing list to avoid any potential deadlocks
        playersProcessingShopDisplays.remove(player.getUniqueId());
        
        // Clear teleport cooldown as well
        teleportCooldowns.remove(player.getUniqueId());

        // Clear the cached connection when displays are removed
        removeCachedPlayerConnection(player);
    }

    /**
     * Force shop display processing for a player, ignoring movement threshold checks.
     * This should be called after teleportation or world changes.
     * 
     * @param player The player to process shop displays for
     */
    public void forceProcessShopDisplaysNearPlayer(Player player) {
        // Check if player is on teleport cooldown
        Long lastTeleport = teleportCooldowns.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();
        
        // If player is on cooldown, skip this update
        if (lastTeleport != null && currentTime - lastTeleport < TELEPORT_COOLDOWN_MS) {
            plugin.getShopLogger().debug("Skipping display update for " + player.getName() + " - on teleport cooldown");
            return;
        }
        
        // Set teleport cooldown
        teleportCooldowns.put(player.getUniqueId(), currentTime);
        
        // Remove from processing list if somehow still in there
        playersProcessingShopDisplays.remove(player.getUniqueId());
        
        // Remove any previous location tracking
        lastProcessedLocations.remove(player.getUniqueId());
        
        plugin.getShopLogger().debug("Force processing shop displays for " + player.getName() + " after teleport");
        
        // Clear all existing displays for this player to ensure a clean slate
        plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
            if (player.isOnline()) {
                if (playersWithActiveShopDisplays.containsKey(player.getUniqueId())) {
                    HashSet<Location> displays = playersWithActiveShopDisplays.get(player.getUniqueId());
                    if (displays != null) {
                        plugin.getShopLogger().debug("Removing " + displays.size() + " existing displays for " + player.getName());
                        for (Location displayLoc : new HashSet<>(displays)) {
                            AbstractShop shop = getShop(displayLoc);
                            if (shop != null) {
                                shop.getDisplay().remove(player);
                            }
                        }
                    }
                    playersWithActiveShopDisplays.remove(player.getUniqueId());
                }
                
                // Process displays after a short delay to ensure all removals are complete
                plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
                    if (player.isOnline()) {
                        processShopDisplaysNearPlayer(player);
                    }
                }, 10); // Increased from 5 to 10 ticks to ensure all chunks are loaded
            }
        }, 1);
    }

    public boolean hasActiveDisplay(Player player, Location shopSignLocation) { 
        HashSet<Location> shops = playersWithActiveShopDisplays.get(player.getUniqueId());
        return shops != null && shops.contains(shopSignLocation);
    }

    public void addActiveShopDisplay(Player player, Location shopSignLocation){
        HashSet<Location> shops;
        if(playersWithActiveShopDisplays.containsKey(player.getUniqueId())){
            shops = playersWithActiveShopDisplays.get(player.getUniqueId());
        }
        else{
            shops = new HashSet<>();
        }
        shops.add(shopSignLocation);
        playersWithActiveShopDisplays.put(player.getUniqueId(), shops);
    }

    public void removeActiveShopDisplay(Player player, Location shopSignLocation){
        HashSet<Location> shops;
        if(playersWithActiveShopDisplays.containsKey(player.getUniqueId())){
            shops = playersWithActiveShopDisplays.get(player.getUniqueId());
            shops.remove(shopSignLocation);
        }
        else{
            shops = new HashSet<>();
        }
        playersWithActiveShopDisplays.put(player.getUniqueId(), shops);
    }

    public void addActiveShopDisplayTag(Player player, Location shopSignLocation) {
        if (playersActiveShopDisplayTag.containsKey(player.getUniqueId())) {
            Location oldShopSignLocation = playersActiveShopDisplayTag.get(player.getUniqueId());

            if (!oldShopSignLocation.equals(shopSignLocation)) {
                AbstractShop oldShop = getShop(oldShopSignLocation);
                if (oldShop != null && oldShop.getDisplay() != null) {
                    // Use a separate task to remove the old display to avoid interference
                    plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
                        if (player.isOnline()) {
                            oldShop.getDisplay().removeDisplayEntities(player, true);
                        }
                    }, 1);
                }
            }
        }
        
        // Only update after a short delay to prevent visual glitches
        plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
            if (player.isOnline()) {
                playersActiveShopDisplayTag.put(player.getUniqueId(), shopSignLocation);
            }
        }, 2);
    }

//    public boolean shopDisplayTagIsActive(Player player, Location shopSignLocation){
//
//        if(playersActiveShopDisplayTag.containsKey(player.getUniqueId())){
//            Location oldShopSignLocation = playersActiveShopDisplayTag.get(player.getUniqueId());
//            return oldShopSignLocation.equals(shopSignLocation);
//        }
//        return false;
//    }

    private List<Location> getUnloadedShopsByChunk(String chunkKey){
        List<Location> unloadedShopsInChunk;
        if(unloadedShopsByChunk.containsKey(chunkKey)) {
            unloadedShopsInChunk = unloadedShopsByChunk.get(chunkKey);
        }
        else
            unloadedShopsInChunk = new ArrayList<>();
        return unloadedShopsInChunk;
    }

    public int getNumberOfShops() {
        return allShops.size();
    }

    public int getNumberOfShops(Player player) {
        return getShopLocations(player.getUniqueId()).size();
    }

    public int getNumberOfShops(UUID playerUUID) {
        return getShopLocations(playerUUID).size();
    }

    public int getNumberOfShops(ShopType shopType) {
        int shopsWithType = 0;
        for (AbstractShop shop : allShops.values()) {
            if (shop.getType() == shopType) { shopsWithType++; }
        }
        return shopsWithType;
    }

    public int getNumberOfShopDisplayTypes(DisplayType displayType) {
        int shopsWithDisplayType = 0;
        for (AbstractShop shop : allShops.values()) {
            if (shop.getDisplay().getType() == displayType) { shopsWithDisplayType++; }
        }
        return shopsWithDisplayType;
    }

    public Map<String, Integer> getShopContainerCounts() {
        int chestShops = 0;
        int barrelShops = 0;
        int enderChestShops = 0;
        int shulkerBoxShops = 0;
        for (AbstractShop shop : allShops.values()) {
            Material containerType = shop.getContainerType();
            if (containerType == null) continue;
            if (containerType == Material.CHEST || containerType == Material.TRAPPED_CHEST) { chestShops++; }
            if (containerType == Material.BARREL) { barrelShops++; }
            if (containerType == Material.ENDER_CHEST) { enderChestShops++; }
            if (containerType.name().endsWith("SHULKER_BOX")) { shulkerBoxShops++; }
        }
        // Return a map of the container types
        Map<String, Integer> containerTypes = new HashMap<>();
        containerTypes.put("Chest Shops", chestShops);
        containerTypes.put("Barrel Shops", barrelShops);
        containerTypes.put("Ender Chest Shops", enderChestShops);
        containerTypes.put("Shulker Box Shops", shulkerBoxShops);
        return containerTypes;
    }

    private ArrayList<AbstractShop> orderedShopList() {
        ArrayList<AbstractShop> list = new ArrayList<AbstractShop>(allShops.values());
        Collections.sort(list, new Comparator<AbstractShop>() {
            @Override
            public int compare(AbstractShop o1, AbstractShop o2) {
                if(o1 == null || o2 == null)
                    return 0;
                //could have something to do with switching between online and offline mode
                return o1.getOwnerName().toLowerCase().compareTo(o2.getOwnerName().toLowerCase());
            }
        });
        return list;
    }

//    public void refreshShopDisplays(Player player) {
//        for (AbstractShop shop : allShops.values()) {
//            //check that the shop is loaded first
//            if(shop.getChestLocation() != null)
//                shop.getDisplay().spawn(player);
//        }
//    }

    private String getChunkKey(Location location){
        int chunkX = getChunkX(location);
        int chunkZ = getChunkZ(location);
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown_world";
        return createChunkKey(worldName, chunkX, chunkZ);
    }

    private String getChunkKey(Chunk chunk){
        return createChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    /**
     * Creates a chunk key from world name and chunk coordinates
     * 
     * @param worldName The name of the world
     * @param chunkX The x-coordinate of the chunk
     * @param chunkZ The z-coordinate of the chunk
     * @return A string key uniquely identifying the chunk
     */
    private String createChunkKey(String worldName, int chunkX, int chunkZ) {
        return worldName + "_" + chunkX + "_" + chunkZ;
    }

    /**
     * Gets the chunk X coordinate for a location
     * 
     * @param location The location
     * @return The chunk X coordinate
     */
    private int getChunkX(Location location) {
        return UtilMethods.floor(location.getBlockX()) >> 4;
    }

    /**
     * Gets the chunk Z coordinate for a location
     * 
     * @param location The location
     * @return The chunk Z coordinate
     */
    private int getChunkZ(Location location) {
        return UtilMethods.floor(location.getBlockZ()) >> 4;
    }

    public void removeAllDisplays(Player player) {
        for (AbstractShop shop : allShops.values()) {
            shop.getDisplay().remove(player);
        }
    }

    public void removeLegacyDisplays(){
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if(DisplayUtil.isDisplay(entity)){
                    entity.remove();
                }
                //make to sure to clear items from old version of plugin too
                else if (CompatibilityUtil.hasItemEntityType() && entity.getType().name().equals("ITEM")) {
                    ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
                    if (UtilMethods.stringStartsWithUUID(itemMeta.getDisplayName())) {
                        entity.remove();
                    }
                }
            }
        }
        for(UUID shopOwnerUUID : plugin.getShopHandler().getShopOwnerUUIDs()){
            for(AbstractShop shop : plugin.getShopHandler().getShops(shopOwnerUUID)){
                if(shop.getChestLocation().getChunk().isLoaded()) {
                    plugin.getShopLogger().debug("[ShopHander.removeLegacyDisplays] updateSign");
                    shop.updateSign();
                }
            }
        }
    }

    public int saveShops(final UUID player){ return saveShops(player, false); }
    public int saveShops(final UUID player, boolean force){
        return saveShopsDriver(player, force);
//        // async code
//        if(playersSavingShops.contains(player))
//            return 0;
//
//        playersSavingShops.add(player);
//        saveShopsDriver(player, force);
//
//        BukkitScheduler scheduler = plugin.getServer().getScheduler();
//        scheduler.runTaskAsynchronously(plugin, new Runnable() {
//            @Override
//            public void run() {
//                playersSavingShops.add(player);
//                saveShopsDriver(player, force);
//            }
//        });
    }

    private int saveShopsDriver(UUID player, boolean force) {
        // Check if any of the players shops want to be saved
        String playerName = player == this.getAdminUUID() ? "admin" : plugin.getServer().getOfflinePlayer(player).getName();
        int numWantingToUpdate = numShopsNeedSave(player);
        if (!force && numWantingToUpdate == 0) {
            plugin.getShopLogger().trace("save shops for player (" + playerName + ") was called, but no shops for player need updating! " + player.toString());
//            // async code
//            if(playersSavingShops.contains(player)){
//                playersSavingShops.remove(player);
//            }
            return 0;
        }

        // There are shops that need to be saved, so go ahead and save the file!
        plugin.getShopLogger().debug("attempting to save shops for player " + playerName + " (" + player.toString() + ") isAdmin: " + (player == Shop.getPlugin().getShopHandler().getAdminUUID()));
        File currentFile = null;
        try {

            File fileDirectory = new File(plugin.getDataFolder(), "Data");
            //UtilMethods.deleteDirectory(fileDirectory);
            if (!fileDirectory.exists())
                fileDirectory.mkdir();

            String owner = null;
            if(player.equals(adminUUID)) {
                owner = "admin";
                currentFile = new File(fileDirectory + "/admin.yml");
            }
            else {
                owner = player.toString();
                //currentFile = new File(fileDirectory + "/" + owner + " (" + player.toString() + ").yml");
                currentFile = new File(fileDirectory + "/" + player.toString() + ".yml");
            }
            //owner = currentFile.getName().substring(0, currentFile.getName().length()-4); //remove .yml

            plugin.getShopLogger().trace("    current file " + currentFile);

            if (!currentFile.exists()) // file doesn't exist
                currentFile.createNewFile();
            else{
                currentFile.delete();
                currentFile.createNewFile();
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(currentFile);
            plugin.getShopLogger().trace("    loaded yaml... " + currentFile);

            List<AbstractShop> shopList = getShops(player);
            if (shopList.isEmpty()) {
                currentFile.delete();
                plugin.getShopLogger().debug("    no shops exist for player (" + playerName + "), deleting file... " + currentFile);
//                // async code
//                if(playersSavingShops.contains(player)){
//                    playersSavingShops.remove(player);
//                }
                return 0;
            }

            int shopNumber = 1;
            for (AbstractShop shop : shopList) {

                //this is to remove a bug that caused one shop to be saved to multiple files at one point
                if(!shop.getOwnerUUID().equals(player))
                    continue;

                //don't save shops that are not initialized with items
                if (shop.isInitialized()) {
                    config.set("shops." + owner + "." + shopNumber + ".id", shop.getId().toString());
                    config.set("shops." + owner + "." + shopNumber + ".location", locationToString(shop.getSignLocation()));
                    if(shop.getFacing() != null)
                        config.set("shops." + owner + "." + shopNumber + ".facing", shop.getFacing().toString());
                    config.set("shops." + owner + "." + shopNumber + ".price", shop.getPrice());
                    if(shop.getType() == ShopType.COMBO){
                        config.set("shops." + owner + "." + shopNumber + ".priceSell", ((ComboShop)shop).getPriceSell());
                    }
                    config.set("shops." + owner + "." + shopNumber + ".amount", shop.getAmount());
                    String type = "";
                    if (shop.isAdmin())
                        type = "admin ";
                    type = type + shop.getType().toString();
                    config.set("shops." + owner + "." + shopNumber + ".type", type);
                    if(shop.getDisplay().getType() != null) {
                        config.set("shops." + owner + "." + shopNumber + ".displayType", shop.getDisplay().getType().toString());
                    }
                    else{ //not sure why I have to do this but if I don't it will be set to LARGE_ITEM for some reason (I cannot find right now)
                        config.set("shops." + owner + "." + shopNumber + ".displayType", null);
                    }
                    //only write the variable if true
                    if(shop.isFakeSign()){
                        config.set("shops." + owner + "." + shopNumber + ".fakeSign", shop.isFakeSign());
                    }

                    config.set("shops." + owner + "." + shopNumber + ".stock", shop.getStock());

                    ItemStack itemStack = shop.getItemStack();
                    itemStack.setAmount(1);
                    if(shop.getType() == ShopType.GAMBLE)
                        itemStack = new ItemStack(Material.AIR);
                    config.set("shops." + owner + "." + shopNumber + ".item", itemStack);

                    if (shop.getType() == ShopType.BARTER) {
                        ItemStack barterItemStack = shop.getSecondaryItemStack();
                        barterItemStack.setAmount(1);
                        config.set("shops." + owner + "." + shopNumber + ".itemBarter", barterItemStack);
                    }

                    shop.setNeedsSave(false);
                    shopNumber++;
                }
            }
            
            // Only generate the stringified config for logging if spam logging is enabled
            if (plugin.getShopLogger().isLevelEnabled(ShopLogger.SPAM)) {
                plugin.getShopLogger().spam("    built config to save... \n" + config.saveToString());
            }
            
            config.save(currentFile);
            plugin.getShopLogger().helpful("Saved " + shopNumber + " Shops for Player " + playerName + " to file: " + currentFile);
            return shopNumber;
        } catch (Exception e){
            plugin.getShopLogger().severe("Unable to save player shop file: " + currentFile);
            e.printStackTrace();
            return 0;
        }
    }

    public int saveAllShops() {
        HashMap<UUID, Boolean> allPlayersWithShops = new HashMap<>();
        for (AbstractShop shop : allShops.values()) {
            allPlayersWithShops.put(shop.getOwnerUUID(), true);
        }

        int numberUpdated = 0;
        int playersWithUpdate = 0;
        for (UUID player : allPlayersWithShops.keySet()) {
            int shopsUpdated = saveShops(player);

            if (shopsUpdated > 0) {
                numberUpdated += shopsUpdated;
                playersWithUpdate++;
            }
        }
        if (playersWithUpdate > 0) plugin.getShopLogger().info("Saved " + playersWithUpdate + " Player Shop file updates to disk");
        return numberUpdated;
    }

    public void convertLegacyShopSaves(){
        //save to new format
        saveAllShops();

        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists())
            return;

        // load all the yml files from the data directory
        for (File file : fileDirectory.listFiles()) {
            if (file.isFile()) {
                if (file.getName().endsWith(".yml")
                        && !file.getName().contains("enderchests")
                        && !file.getName().contains("itemCurrency")
                        && !file.getName().contains("gambleDisplay")) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    boolean isLegacyConfig = false;
                }
            }
        }
    }

    public void loadShops() {
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            int numShopsLoaded = 0;

            boolean convertLegacySaves = false;
            File fileDirectory = new File(plugin.getDataFolder(), "Data");
            if (!fileDirectory.exists())
                return;

            // load all the yml files from the data directory
            for (File file : fileDirectory.listFiles()) {
                Shop.getPlugin().getShopLogger().debug("Loading player shops from file: " + file.getName());
                try {
                    if (file.isFile()) {
                        if (file.getName().endsWith(".yml")
                                && !file.getName().contains("enderchests")
                                && !file.getName().contains("itemCurrency")
                                && !file.getName().contains("gambleDisplay")) {
                            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                            boolean isLegacyConfig = false;
                            UUID playerUUID = null;
                            String fileNameNoExt = null;
                            try {
                                int dotIndex = file.getName().lastIndexOf('.');
                                fileNameNoExt = file.getName().substring(0, dotIndex); //remove .yml

                                //all files are saved as UUID.yml except for admin shops which are admin.yml
                                if (!fileNameNoExt.equals("admin")) {
                                    playerUUID = UUID.fromString(fileNameNoExt);
                                    //file names are in UUID format. Load from new save files -> ownerUUID.yml
                                } else {
                                    playerUUID = adminUUID;
                                }
                            } catch (IllegalArgumentException iae) {
                                //file names are not in UUID format. Load from legacy save files -> ownerName + " (" + ownerUUID + ").yml
                                isLegacyConfig = true;
                                convertLegacySaves = true;
                                playerUUID = uidFromString(fileNameNoExt);
                            }
                            numShopsLoaded += loadShopsFromConfig(config, isLegacyConfig);
                            if (isLegacyConfig) {
                                //save new file
                                saveShops(playerUUID, true);
                                //delete old file
                                file.delete();
                            }
                            if (plugin.getDebug_forceResaveAll()) {
                                saveShops(playerUUID, true);
                            }
                        }
                    }
                }
                catch (Exception e) {
                    Shop.getPlugin().getShopLogger().log(Level.SEVERE, "Error while loading Shop from player file (" + file.getName() + "), please report this error to the Shop developers: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            if(convertLegacySaves)
                convertLegacyShopSaves();

            Shop.getPlugin().getShopLogger().log(Level.INFO, "Loaded " + numShopsLoaded + " Shops!");

            //dont refresh displays at load time anymore. they are now loaded in client side on login
    //                new BukkitRunnable() {
    //                    @Override
    //                    public void run() {
    //                        refreshShopDisplays(null);
    //                    }
    //                }.runTaskLater(plugin, 20);
        });
    }


    private int loadShopsFromConfig(YamlConfiguration config, boolean isLegacy) {
        if (config.getConfigurationSection("shops") == null)
            return 0;

        int numShopsLoaded = 0;
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);

        for (String shopOwner : allShopOwners) {
            UUID owner = null;
            //System.out.println("[Shop] loading shops for player - "+shopOwner);

            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            int playerLoadedShops = 0;
            for (String shopNumber : allShopNumbers) {
                Location signLoc = locationFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".location"));
                if(signLoc != null) {
                    try {
                        // Track each shop using a uuid so that we can log purchases better
                        String idString = config.getString("shops." + shopOwner + "." + shopNumber + ".id");
                        UUID id = null;
                        if (idString != null && !idString.isEmpty()){
                            id = UUID.fromString(idString);
                        }

                        if (shopOwner.equals("admin"))
                            owner = this.getAdminUUID();
                        else if(isLegacy)
                            owner = uidFromString(shopOwner);
                        else
                            owner = UUID.fromString(shopOwner);

                        BlockFace facing = null;
                        String facingStr = config.getString("shops." + shopOwner + "." + shopNumber + ".facing");
                        if(facingStr != null)
                            facing = BlockFace.valueOf(facingStr);

                        String type = config.getString("shops." + shopOwner + "." + shopNumber + ".type");
                        double price = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".price"));
                        double priceSell = 0;
                        if (config.getString("shops." + shopOwner + "." + shopNumber + ".priceSell") != null) {
                            priceSell = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".priceSell"));
                        }
                        int amount = Integer.parseInt(config.getString("shops." + shopOwner + "." + shopNumber + ".amount"));

                        boolean isAdmin = shopOwner.equals("admin");

                        ShopType shopType = typeFromString(type);

                        ItemStack itemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".item");
                        if (shopType == ShopType.GAMBLE) {
                            itemStack = plugin.getGambleDisplayItem();
                        }

                        if (itemStack == null) {
                            Shop.getPlugin().getShopLogger().log(Level.WARNING, "Unable to load Shop #" + shopNumber + " for owner '" + shopOwner + "'! Skipping!");
                            continue;
                        }

                        //this inits a new shop but wont calculate anything yet
                        AbstractShop shop = AbstractShop.create(signLoc, owner, price, priceSell, amount, isAdmin, shopType, facing);
                        if (id != null) shop.setId(id);
                        shop.setItemStack(itemStack);
                        if (shop.getType() == ShopType.BARTER) {
                            ItemStack barterItemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".itemBarter");
                            shop.setSecondaryItemStack(barterItemStack);
                        }
                        String displayType = config.getString("shops." + shopOwner + "." + shopNumber + ".displayType");
                        if(displayType != null)
                            shop.getDisplay().setType(DisplayType.valueOf(displayType), false);

                        boolean isFakeSign = config.getBoolean("shops." + shopOwner + "." + shopNumber + ".fakeSign");
                        if(isFakeSign){
                            shop.setFakeSign(true);
                        }

                        int stock = config.getInt("shops." + shopOwner + "." + shopNumber + ".stock");
                        shop.setStockOnLoad(stock);
                        // Load the GUI Icon so that it appears when players perform a search, even if the chunks haven't loaded yet.
                        shop.refreshGuiIcon();

                        // We will need to save the file again if we are a legacy config
                        if (isLegacy) { shop.setNeedsSave(true); }
                        // If we just added an ID to a shop for the first time, then it will need to be saved/updated as well
                        if (idString == null || idString.isEmpty()) { shop.setNeedsSave(true); }

                        //if chunk its in is already loaded, calculate it here
                        if(shop.getDisplay().isChunkLoaded()) {
                            //run this task synchronously
                            plugin.getFoliaLib().getScheduler().runAtLocation(shop.getSignLocation(), task -> {
                                boolean signDefined = shop.load();
                                if(signDefined)
                                    addShop(shop);
                            });
                        }
                        //if the chunk is not already loaded, add it to a list to calculate it at chunkloadevent later
                        else {
                            //System.out.println("[Shop] chunk not loaded. Adding to unloadedList");
                            addUnloadedShopToChunkList(shop);
                            addShop(shop);
                        }

                        numShopsLoaded++;
                        playerLoadedShops++;
                    } catch (NullPointerException e) {e.printStackTrace();}
                }
            }
            String ownerName = shopOwner.equals("admin") ? "admin" : plugin.getServer().getOfflinePlayer(UUID.fromString(shopOwner)).getName();
            plugin.getShopLogger().helpful("Loaded (" + playerLoadedShops + ") shops for Player " + ownerName + " from: " + shopOwner + ".yml");
        }

        return numShopsLoaded;
    }

    public UUID getAdminUUID(){
        return adminUUID;
    }


    private String locationToString(Location loc) {
        String worldName = loc.getWorld() == null ? "unknown_world" : loc.getWorld().getName();
        return worldName + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location locationFromString(String locString) {
        String[] parts = locString.split(",");
        return new Location(plugin.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    private UUID uidFromString(String ownerString) {
        int index = ownerString.indexOf("(");
        String uidString = ownerString.substring(index + 1, ownerString.length() - 1);
        return UUID.fromString(uidString);
    }

    private ShopType typeFromString(String typeString) {
        if (typeString.contains("sell"))
            return ShopType.SELL;
        else if (typeString.contains("buy"))
            return ShopType.BUY;
        else if(typeString.contains("barter"))
            return ShopType.BARTER;
        else if(typeString.contains("combo"))
            return ShopType.COMBO;
        else
            return ShopType.GAMBLE;
    }

    public boolean isChest(Block b){
        return plugin.getEnabledContainers().contains(b.getType());
    }

    public boolean passesItemListCheck(ItemStack is){
        if(plugin.getItemListType() == ItemListType.NONE)
            return true;

        for(ItemStack itemInList : itemListItems){
            if(itemInList.isSimilar(is)){
                if(plugin.getItemListType() == ItemListType.ALLOW_LIST)
                    return true;
                else if(plugin.getItemListType() == ItemListType.DENY_LIST)
                    return false;
            }
        }

        //item not similar to anything in our item list
        if(plugin.getItemListType() == ItemListType.ALLOW_LIST)
            return false;

        return true;
    }

    public void addInventoryToItemList(Inventory inventory){
        for(ItemStack is : inventory.getContents()) {
            if(is != null && is.getType() != Material.AIR) {
                ItemStack itemClone = is.clone();
                itemClone.setAmount(1);
                boolean doNotAdd = false;
                for (ItemStack itemInList : itemListItems) {
                    if (itemInList.isSimilar(itemClone)) {
                        doNotAdd = true;
                    }
                }
                if(!doNotAdd){
                    itemListItems.add(itemClone);
                }
            }
        }
        saveItemList();
    }

    public void removeInventoryFromItemList(Inventory inventory){
        Iterator<ItemStack> itemIterator = itemListItems.iterator();
        while(itemIterator.hasNext()){
            ItemStack listItem = itemIterator.next();
            for(ItemStack toRemove : inventory.getContents()) {
                if(toRemove != null && toRemove.getType() != Material.AIR) {
                    ItemStack itemClone = toRemove.clone();
                    itemClone.setAmount(1);
                    if (listItem.isSimilar(itemClone)) {
                        itemIterator.remove();
                    }
                }
            }
        }
        saveItemList();
    }

    public void initItemList(){
        if(plugin.getItemListType() == ItemListType.NONE)
            return;

        try {
            File itemListFile = new File(plugin.getDataFolder() + "/itemList.yml");

            if (!itemListFile.exists()) { // file doesn't exist{
                itemListFile.createNewFile();
                return;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(itemListFile);
            for(String key : config.getKeys(false)){
                ItemStack is = config.getItemStack(key);
                if(is != null) {
                    is.setAmount(1);
                    itemListItems.add(is);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveItemList(){
        if(plugin.getItemListType() == ItemListType.NONE)
            return;

        try {
            File itemListFile = new File(plugin.getDataFolder() + "/itemList.yml");

            if (!itemListFile.exists()) { // file doesn't exist{
                itemListFile.createNewFile();
            }
            else{
                itemListFile.delete();
                itemListFile.createNewFile();
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(itemListFile);
            for(int i=0; i< itemListItems.size(); i++){
                ItemStack is = itemListItems.get(i);
                if(is != null) {
                    config.set(""+i, is);
                }
            }

            config.save(itemListFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if a player is within the specified distance of a chunk
     * 
     * @param player The player to check
     * @param chunk The chunk to check against
     * @param maxDistance The maximum distance in blocks
     * @return True if the player is near the chunk
     */
    private boolean isPlayerNearChunk(Player player, Chunk chunk, double maxDistance) {
        if (!player.getWorld().equals(chunk.getWorld())) {
            return false;
        }
        
        // Get chunk boundaries
        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        
        // Get player location
        Location playerLoc = player.getLocation();
        int playerX = playerLoc.getBlockX();
        int playerZ = playerLoc.getBlockZ();
        
        // Calculate minimum distance to chunk border
        double distance;
        
        // Player is outside chunk on X axis
        if (playerX < minX) {
            distance = minX - playerX;
        } else if (playerX > maxX) {
            distance = playerX - maxX;
        } else {
            // Player is within chunk X range
            distance = 0;
        }
        
        // Player is outside chunk on Z axis
        if (playerZ < minZ) {
            double zDistance = minZ - playerZ;
            distance = Math.sqrt(distance * distance + zDistance * zDistance);
        } else if (playerZ > maxZ) {
            double zDistance = playerZ - maxZ;
            distance = Math.sqrt(distance * distance + zDistance * zDistance);
        }
        
        return distance <= maxDistance;
    }
    
    /**
     * Rebuilds all shop displays in a chunk for nearby players
     * Called after a chunk has been loaded to ensure displays are shown
     * 
     * @param chunk The chunk that was loaded
     */
    public void rebuildDisplaysInChunk(Chunk chunk) {
        // Get shop locations in this chunk
        String chunkKey = getChunkKey(chunk);
        List<Location> shopLocations = getShopLocations(chunkKey);
        
        // Only proceed if this chunk has shops
        if (shopLocations.isEmpty()) {
            return;
        }
        
        // Process for all players who might be able to see shops in this chunk
        for (Player player : chunk.getWorld().getPlayers()) {
            // Skip players who recently teleported
            Long lastTeleport = teleportCooldowns.get(player.getUniqueId());
            if (lastTeleport != null && System.currentTimeMillis() - lastTeleport < TELEPORT_COOLDOWN_MS) {
                plugin.getShopLogger().debug("Skipping chunk display update for " + player.getName() + " - on teleport cooldown");
                continue;
            }
            
            if (isPlayerNearChunk(player, chunk, plugin.getMaxShopDisplayDistance())) {
                plugin.getShopLogger().debug("Rebuilding shop displays for " + player.getName() + " in chunk " + chunkKey);
                
                // Don't force a refresh - just run the normal process which respects all the checks
                processShopDisplaysNearPlayer(player);
            }
        }
    }

    /**
     * Gets the cached player connection for packet sending
     * @param player The player to get connection for
     * @return The player's network connection object
     */
    public Object getCachedPlayerConnection(Player player) {
        UUID playerId = player.getUniqueId();
        // Check if we have a cached connection
        Object connection = playerConnectionCache.get(playerId);
        if (connection == null) {
            // If not cached, get it and store it
            connection = plugin.getNmsBullshitHandler().getPlayerConnection(player);
            if (connection != null) {
                playerConnectionCache.put(playerId, connection);
            }
        }
        return connection;
    }

    /**
     * Removes the cached player connection
     * @param player The player whose connection to remove
     */
    public void removeCachedPlayerConnection(Player player) {
        playerConnectionCache.remove(player.getUniqueId());
    }

    /**
     * Removes the cached player connection by UUID
     * @param playerId UUID of the player whose connection to remove
     */
    public void removeCachedPlayerConnection(UUID playerId) {
        playerConnectionCache.remove(playerId);
    }
}