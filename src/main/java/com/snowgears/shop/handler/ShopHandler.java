package com.snowgears.shop.handler;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.ComboShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.display.Display;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.util.Comparator;
import java.util.*;


public class ShopHandler {

    public Shop plugin = Shop.getPlugin();

    private HashMap<UUID, List<Location>> playerShops = new HashMap<>();
    private HashMap<Location, AbstractShop> allShops = new HashMap<>();
    private ArrayList<Material> shopMaterials = new ArrayList<Material>();
    private UUID adminUUID;

    private ArrayList<UUID> playersSavingShops = new ArrayList<>();

    public ShopHandler(Shop instance) {
        plugin = instance;

        shopMaterials.add(Material.CHEST);
        shopMaterials.add(Material.TRAPPED_CHEST);
        if(plugin.useEnderChests())
            shopMaterials.add(Material.ENDER_CHEST);

        adminUUID = UUID.randomUUID();

        new BukkitRunnable() {
            @Override
            public void run() {
                loadShops();
            }
        }.runTaskLater(this.plugin, 10);
    }

    public AbstractShop getShop(Location loc) {
        return allShops.get(loc);
    }

    public AbstractShop getShopByChest(Block shopChest) {

        try {
            if(shopChest.getState() instanceof ShulkerBox){
                BlockFace[] directions = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
                AbstractShop shop = null;
                for(BlockFace direction : directions){
                    shop = this.getShop(shopChest.getRelative(direction).getLocation());
                    if(shop != null) {
                        //make sure the shop sign you found is actually attached to the correct shop
                        if(shop.getChestLocation().equals(shopChest.getLocation()))
                            return shop;
                    }
                }
                return null;
            }
        } catch (NoClassDefFoundError e) {}

        if (this.isChest(shopChest)) {
            BlockFace chestFacing = UtilMethods.getDirectionOfChest(shopChest);

            ArrayList<Block> chestBlocks = new ArrayList<>();
            chestBlocks.add(shopChest);

            InventoryHolder ih = null;
            if(shopChest.getState() instanceof Chest) {
                Chest chest = (Chest) shopChest.getState();
                ih = chest.getInventory().getHolder();

                if (ih instanceof DoubleChest) {
                    DoubleChest dc = (DoubleChest) ih;
                    Chest leftChest = (Chest) dc.getLeftSide();
                    Chest rightChest = (Chest) dc.getRightSide();
                    if (chest.getLocation().equals(leftChest.getLocation()))
                        chestBlocks.add(rightChest.getBlock());
                    else
                        chestBlocks.add(leftChest.getBlock());
                }
            }

            for (Block chestBlock : chestBlocks) {
                Block signBlock = chestBlock.getRelative(chestFacing);
                if (signBlock.getBlockData() instanceof WallSign) {
                    WallSign sign = (WallSign) signBlock.getBlockData();
                    if (chestFacing == sign.getFacing()) {
                        AbstractShop shop = this.getShop(signBlock.getLocation());
                        if (shop != null)
                            return shop;
                    }
                } else if(!(ih instanceof DoubleChest)){
                    AbstractShop shop = this.getShop(signBlock.getLocation());
                    //delete the shop if it doesn't have a sign
                    if (shop != null)
                        shop.delete();
                }
            }
        }
        return null;
    }

    public AbstractShop getShopNearBlock(Block block){
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

        List<Location> shopLocations = getShopLocations(shop.getOwnerUUID());
        if(!shopLocations.contains(shop.getSignLocation())) {
            shopLocations.add(shop.getSignLocation());
            playerShops.put(shop.getOwnerUUID(), shopLocations);
        }
    }

    //This method should only be used by AbstractShop object to delete
    public boolean removeShop(AbstractShop shop) {
        if (allShops.containsKey(shop.getSignLocation())) {
            allShops.remove(shop.getSignLocation());
        }
        if(playerShops.containsKey(shop.getOwnerUUID())){
            List<Location> shopLocations = getShopLocations(shop.getOwnerUUID());
            if(shopLocations.contains(shop.getSignLocation())) {
                shopLocations.remove(shop.getSignLocation());
                playerShops.put(shop.getOwnerUUID(), shopLocations);
            }
        }

        return false;
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

    public List<OfflinePlayer> getShopOwners(){
        ArrayList<OfflinePlayer> owners = new ArrayList<>();
        for(UUID player : playerShops.keySet()) {
            owners.add(Bukkit.getOfflinePlayer(player));
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

    public int getNumberOfShops() {
        return allShops.size();
    }

    public int getNumberOfShops(Player player) {
        return getShopLocations(player.getUniqueId()).size();
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

    public void refreshShopDisplays() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if(Display.isDisplay(entity)){
                    entity.remove();
                }
                //make to sure to clear items from old version of plugin too
                else if (entity.getType() == EntityType.DROPPED_ITEM) {
                    ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
                    if (UtilMethods.stringStartsWithUUID(itemMeta.getDisplayName())) {
                        entity.remove();
                    }
                }
            }
        }
        for (AbstractShop shop : allShops.values()) {
            shop.getDisplay().spawn();
        }
    }

    public void saveShops(final UUID player){
        if(playersSavingShops.contains(player))
            return;

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskLaterAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                playersSavingShops.add(player);
                saveShopsDriver(player);
            }
        }, 20L);
    }

    private void saveShopsDriver(UUID player){
        try {

            File fileDirectory = new File(plugin.getDataFolder(), "Data");
            //UtilMethods.deleteDirectory(fileDirectory);
            if (!fileDirectory.exists())
                fileDirectory.mkdir();

            String owner = null;
            File currentFile = null;
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

            if (!currentFile.exists()) // file doesn't exist
                currentFile.createNewFile();
            else{
                currentFile.delete();
                currentFile.createNewFile();
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(currentFile);

            List<AbstractShop> shopList = getShops(player);
            if (shopList.isEmpty()) {
                currentFile.delete();
                if(playersSavingShops.contains(player)){
                    playersSavingShops.remove(player);
                }
                return;
            }

            int shopNumber = 1;
            for (AbstractShop shop : shopList) {

                //this is to remove a bug that caused one shop to be saved to multiple files at one point
                if(!shop.getOwnerUUID().equals(player))
                    continue;

                //don't save shops that are not initialized with items
                if (shop.isInitialized()) {
                    config.set("shops." + owner + "." + shopNumber + ".location", locationToString(shop.getSignLocation()));
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
                    shopNumber++;
                }
            }
            config.save(currentFile);
        } catch (Exception e){
            e.printStackTrace();
        }

        if(playersSavingShops.contains(player)){
            playersSavingShops.remove(player);
        }
    }

    public void saveAllShops() {
        HashMap<UUID, Boolean> allPlayersWithShops = new HashMap<>();
        for(AbstractShop shop : allShops.values()){
            allPlayersWithShops.put(shop.getOwnerUUID(), true);
        }

        for(UUID player : allPlayersWithShops.keySet()){
            saveShops(player);
        }
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
        boolean convertLegacySaves = false;
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
                    UUID playerUUID = null;
                    String fileNameNoExt = null;
                    try {
                        int dotIndex = file.getName().lastIndexOf('.');
                        fileNameNoExt = file.getName().substring(0, dotIndex); //remove .yml

                        //all files are saved as UUID.yml except for admin shops which are admin.yml
                        if (!fileNameNoExt.equals("admin")) {
                            playerUUID = UUID.fromString(fileNameNoExt);
                            //file names are in UUID format. Load from new save files -> ownerUUID.yml
                        }
                        else{
                            playerUUID = this.adminUUID;
                        }
                    } catch (IllegalArgumentException iae) {
                        //file names are not in UUID format. Load from legacy save files -> ownerName + " (" + ownerUUID + ").yml
                        isLegacyConfig = true;
                        convertLegacySaves = true;
                        playerUUID = this.uidFromString(fileNameNoExt);
                    }
                    loadShopsFromConfig(config, isLegacyConfig);
                    if(isLegacyConfig){
                        //save new file
                        saveShops(playerUUID);
                        //delete old file
                        file.delete();
                    }
                }
            }
        }
        if(convertLegacySaves)
            convertLegacyShopSaves();

        new BukkitRunnable() {
            @Override
            public void run() {
                refreshShopDisplays();
            }
        }.runTaskLater(this.plugin, 20);
    }


    private void loadShopsFromConfig(YamlConfiguration config, boolean isLegacy) {
        if (config.getConfigurationSection("shops") == null)
            return;
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);

        for (String shopOwner : allShopOwners) {
            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            for (String shopNumber : allShopNumbers) {
                Location signLoc = locationFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".location"));
                if(signLoc != null) {
                    try {
                        Block b = signLoc.getBlock();
                        if (b.getBlockData() instanceof WallSign) {

                            UUID owner;
                            if (shopOwner.equals("admin"))
                                owner = this.getAdminUUID();
                            else if(isLegacy)
                                owner = uidFromString(shopOwner);
                            else
                                owner = UUID.fromString(shopOwner);

                            String type = config.getString("shops." + shopOwner + "." + shopNumber + ".type");
                            double price = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".price"));
                            double priceSell = 0;
                            if (config.getString("shops." + shopOwner + "." + shopNumber + ".priceSell") != null) {
                                priceSell = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".priceSell"));
                            }
                            int amount = Integer.parseInt(config.getString("shops." + shopOwner + "." + shopNumber + ".amount"));

                            boolean isAdmin = false;
                            if (type.contains("admin"))
                                isAdmin = true;
                            ShopType shopType = typeFromString(type);

                            ItemStack itemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".item");
                            if (shopType == ShopType.GAMBLE) {
                                itemStack = plugin.getGambleDisplayItem();
                            }

                            AbstractShop shop = AbstractShop.create(signLoc, owner, price, priceSell, amount, isAdmin, shopType);

                            if (this.isChest(shop.getChestLocation().getBlock())) {

                                shop.setItemStack(itemStack);
                                if (shop.getType() == ShopType.BARTER) {
                                    ItemStack barterItemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".itemBarter");
                                    shop.setSecondaryItemStack(barterItemStack);
                                }

                                this.addShop(shop);

                                //final is necessary for use inside the BukkitRunnable class
                                final AbstractShop finalShop = shop;

                                final String displayType = config.getString("shops." + shopOwner + "." + shopNumber + ".displayType");
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {

                                        if (finalShop != null && displayType != null) {
                                            finalShop.getDisplay().setType(DisplayType.valueOf(displayType));
                                        }
                                    }
                                }.runTaskLater(this.plugin, 2);
                            }
                        }
                    } catch (NullPointerException e) {}
                }
            }
        }
    }

    public UUID getAdminUUID(){
        return adminUUID;
    }


    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
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
        try{
            if(b.getState() instanceof ShulkerBox){
                return true;
            }
        } catch (NoClassDefFoundError e) {}
        return shopMaterials.contains(b.getType());
    }
}