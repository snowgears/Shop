package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.AbstractDisplay;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.util.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.Sound;
import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;
import com.snowgears.shop.util.CompatibilityUtil;

import java.util.*;
import java.util.logging.Level;

import static com.snowgears.shop.util.UtilMethods.isMCVersion17Plus;

public abstract class AbstractShop {

    protected UUID id = UUID.randomUUID();
    protected boolean needsSave = false;
    protected Location signLocation;
    protected Location chestLocation;
    protected BlockFace facing;
    protected UUID owner;
    protected ItemStack item;
    protected ItemStack secondaryItem;
    protected AbstractDisplay display;
    protected double price;
    protected int amount;
    protected boolean isAdmin;
    protected ShopType type;
    protected String[] signLines;
    protected boolean signLinesRequireRefresh;
    protected boolean isPerformingTransaction;
    protected ItemStack guiIcon;
    protected boolean fakeSign;

    protected int stock;

    public AbstractShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing) {
        this.signLocation = signLoc;
        this.owner = player;
        this.price = pri;
        this.amount = amt;
        this.isAdmin = admin;
        this.item = null;
        this.facing = facing;

        this.signLinesRequireRefresh = true; // Reload signs on load in case config changed!

        display = Shop.getPlugin().getShopHandler().createDisplay(this.signLocation);
        fakeSign = false;

        if(isAdmin)
            owner = Shop.getPlugin().getShopHandler().getAdminUUID();
    }

    //TODO move all this calculation to an load() method something to call later
//    public AbstractShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing) {
//        signLocation = signLoc;
//        owner = player;
//        price = pri;
//        amount = amt;
//        isAdmin = admin;
//        item = null;
//
//        if(facing == null){
//            if(signLocation != null) {
//                try {
//                    facing = ((WallSign) signLocation.getBlock().getBlockData()).getFacing();
//                } catch(ClassCastException cce){}
//            }
//        }
//        this.facing = facing;
//
//        display = DisplayUtil.getDisplayForNMSVersion(this.signLocation);
//
//        if(isAdmin){
//            owner = Shop.getPlugin().getShopHandler().getAdminUUID();
//        }
//
//        if(signLocation != null) {
//            try {
//                WallSign sign = (WallSign) signLocation.getBlock().getBlockData();
//                chestLocation = signLocation.getBlock().getRelative(sign.getFacing().getOppositeFace()).getLocation();
//            } catch(ClassCastException cce){
//                signLocation = null;
//                chestLocation = null;
//            }
//        }
//    }

    public static AbstractShop create(Location signLoc, UUID player, double pri, double priCombo, int amt, Boolean admin, ShopType shopType, BlockFace facing) {

        switch(shopType){
            case SELL:
                return new SellShop(signLoc, player, pri, amt, admin, facing);
            case BUY:
                return new BuyShop(signLoc, player, pri, amt, admin, facing);
            case BARTER:
                return new BarterShop(signLoc, player, pri, amt, admin, facing);
            case GAMBLE:
                return new GambleShop(signLoc, player, pri, amt, admin, facing);
            case COMBO:
                return new ComboShop(signLoc, player, pri, priCombo, amt, admin, facing);
        }
        return null;
    }

    public void setId(UUID newId) {
        this.id = newId;
    }

    public UUID getId() {
        return id;
    }

    //this calls BlockData which loads the chunk the shop is in by doing so
    public boolean load() {
        if (signLocation != null) {
            try {
                facing = CompatibilityUtil.getWallSignFacing(signLocation.getBlock());
                if (facing != null) {
                    chestLocation = signLocation.getBlock().getRelative(facing.getOppositeFace()).getLocation();
                } else {
                    //this shop has no valid wall sign. return false
                    Shop.getPlugin().getShopLogger().warning("Failed to load shop, invalid sign: " + this);
                    return false;
                }

                //if shop is made out of a container that is no longer enabled, delete it
                if(chestLocation != null) {
                    if (!Shop.getPlugin().getShopHandler().isChest(chestLocation.getBlock())){
                        this.delete();
                        return false;
                    }
                }
                this.updateStock();
                Shop.getPlugin().getShopLogger().debug("Loaded shop: " + this);
                return true;
            } catch (Exception e) {
                //this shop has no sign on it. return false
                Shop.getPlugin().getShopLogger().warning("Failed to load shop, error with sign: " + this);
                return false;
            }
        } else {
            //this shop has no sign location defined
            Shop.getPlugin().getShopLogger().warning("Failed to load shop, no signLocation: " + this);
            return false;
        }
    }

    public boolean needsSave() {
        return needsSave;
    }

    public void setNeedsSave(boolean shouldSave) {
        needsSave = shouldSave;
    }

    //abstract methods that must be implemented in each shop subclass

    protected int calculateStock() {
        if(this.isAdmin) {
            // There is always stock in the admin shop!
            stock = Integer.MAX_VALUE;
            return stock;
        }
        if(this.getInventory() == null || this.getItemStack() == null) {
            //if stock is already calculated but now inventory is null, use old stock value
            if(stock != -1)
                return stock;
            else
                stock = -1;
            return stock;
        }
        stock = InventoryUtils.getAmount(this.getInventory(), this.getItemStack()) / this.getAmount();
        if(stock == 0 && Shop.getPlugin().getAllowPartialSales()){
            // Calculate the minimum items required to show as in stock
            int minItemAmountRequired = (int) Math.ceil(1 / this.getPricePerItem());
            int itemsInShop = InventoryUtils.getAmount(this.getInventory(), this.getItemStack());

            if(itemsInShop >= minItemAmountRequired){
                stock = 1;
            }
        }
        return stock;
    }

    public void updateStock() {
        int oldStock = stock;

        // Update the stock
        this.calculateStock();

        // Update sign if needed
        boolean hasStockChange = stock != oldStock;
        if(hasStockChange){
            signLinesRequireRefresh = true;
            Shop.getPlugin().getShopLogger().trace("[AbstractShop.updateStock] updateSign, new stock != oldStock! newStock: " + stock + " old stock: " + oldStock + "\n" + this);
            this.updateSign();

            //also set marker in here if using a marker integration
            if(Shop.getPlugin().getBluemapHookListener() != null) {
                Shop.getPlugin().getBluemapHookListener().updateMarker(this);
            }

            needsSave = true;
            return;
        }

        // If there is not a stock change but we need to refresh the signs, then update the sign.
        // This is triggered upon a server restart to make sure that the signs are updated.
        if (!hasStockChange && signLinesRequireRefresh) {
            this.updateSign();
        }
    }

    public int getStock(){
        if(isAdmin){
            return Integer.MAX_VALUE;
        }
        return stock;
    }

    public void setStockOnLoad(int stock){
        this.stock = stock;
    }

    public boolean isInitialized(){
        return (item != null);
    }

    //getter methods

    public Location getSignLocation() {
        return signLocation;
    }

    public Object getSign(){
        try {
            if (CompatibilityUtil.isWallSignBlock(signLocation.getBlock())) {
                return CompatibilityUtil.getBlockData(signLocation.getBlock());
            }
        } catch (Exception e) {
            // Legacy versions or invalid sign
        }
        return null;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public Inventory getInventory() {
        if(chestLocation == null || signLocation == null)
            return null;
        Block chestBlock = chestLocation.getBlock();
        if(chestBlock.getType() == Material.ENDER_CHEST) {
            OfflinePlayer ownerPlayer = this.getOwner();
            if(ownerPlayer != null)
                return Shop.getPlugin().getEnderChestHandler().getInventory(ownerPlayer);
        }
        else if(chestBlock.getState() instanceof InventoryHolder){
            return ((InventoryHolder)(chestBlock.getState())).getInventory();
        }
        return null;
    }

    public Material getContainerType() {
        if(chestLocation == null)
            return null;
        try {
            return chestLocation.getBlock().getType();
        } catch (Exception e) {
            return null;
        }
    }

    public UUID getOwnerUUID() {
        return owner;
    }

    public String getOwnerName() {
        if(this.isAdmin())
            return "admin";
        if (this.getOwner() != null){
            // If we can load the owner name, just use that
            if (this.getOwner().getName() != null) return this.getOwner().getName();
            // Return unknown player text
            String shortId = this.getOwnerUUID().toString();
            shortId = shortId.substring(0,3) + "..." + shortId.substring(shortId.length()-3);
            return "Unknown Player (" + shortId + ")";
        }
        return ChatColor.RED + "CLOSED";
    }

    public OfflinePlayer getOwner() {
        return Bukkit.getOfflinePlayer(this.owner);
    }

    public ItemStack getItemStack() {
        if (item != null) {
            ItemStack is = item.clone();
            is.setAmount(this.getAmount());
            return is;
        }
        return null;
    }

    public ItemStack getSecondaryItemStack() {
        if (secondaryItem != null) {
            ItemStack is = secondaryItem.clone();
            is.setAmount((int)this.getPrice());
            return is;
        }
        return null;
    }

    public AbstractDisplay getDisplay() {
        return display;
    }

    public double getPrice() {
        return price;
    }

    public double getPricePerItem() {
        // Calculate pricePerItem for partial sales, round up!
        double pricePer = this.getPrice() / this.getAmount();

        return pricePer;
    }

    public double getItemsPerPriceUnit() {
        // Calculate items you can get for each price unit, round down!
        double pricePer = this.getAmount() / this.getPrice();

        return pricePer;
    }

    public String getPriceString() {
        if(this.type == ShopType.BARTER && this.isInitialized()){
            return (int)this.getPrice() + " " + Shop.getPlugin().getItemNameUtil().getName(this.getSecondaryItemStack()).toPlainText();
        }
        return Shop.getPlugin().getPriceString(this.price, false);
    }

    public String getPricePerItemString() {
        double pricePer = this.getPricePerItem();
        return Shop.getPlugin().getPriceString(pricePer, true);
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    //only use this method if the shop has not been added to the main handler maps yet
    public void setAdmin(boolean isAdmin){
        this.isAdmin = isAdmin;
        if(isAdmin)
            this.owner = Shop.getPlugin().getShopHandler().getAdminUUID();
    }

    public ShopType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public BlockFace getFacing(){
        return facing;
    }

    public ItemStack getGuiIcon(){
        // Load it when it is first called
        if (guiIcon == null) { this.refreshGuiIcon(); }
        return guiIcon;
    }

    //setter methods

    public void setItemStack(ItemStack is) {
        // If the item stack passed is null, go ahead and just skip it.
        if (is == null) return;

        // Remove "0 Damage" from item meta (old config bug)
        this.item = this.removeZeroDamageMeta(is.clone());
        this.signLinesRequireRefresh = true;
        this.calculateStock();
    }

    public void setSecondaryItemStack(ItemStack is) {
        this.secondaryItem = this.removeZeroDamageMeta(is.clone());
        this.signLinesRequireRefresh = true;
        this.calculateStock();
    }

    public ItemStack removeZeroDamageMeta(ItemStack item) {
        try {
            // In the past we used to explicitly set the durability of an item to be 0, this caused blocks/items to be saved
            // with extra NBT data that we don't actually want. For example, dirt shouldn't have a damage of 0.
            // Detect if we set it to 0, and if so, remove it from the ItemMeta!
            if (item.getItemMeta() instanceof Damageable && ((Damageable) item.getItemMeta()).getDamage() == 0) {
                
                // Check if modern component string methods are available
                if (CompatibilityUtil.hasGetAsComponentString() && CompatibilityUtil.hasCreateItemStackFromString()) {
                    try {
                        // Use reflection to avoid compile-time dependency on modern methods
                        ItemMeta itemMeta = item.getItemMeta();
                        java.lang.reflect.Method getAsComponentStringMethod = itemMeta.getClass().getMethod("getAsComponentString");
                        String components = (String) getAsComponentStringMethod.invoke(itemMeta); // example: "[minecraft:damage=53]"

                        // Remove it from the array
                        components = components.replace(",minecraft:damage=0", ""); // Middle of an array
                        components = components.replace("minecraft:damage=0,", ""); // Start of an array
                        components = components.replace("minecraft:damage=0", ""); // Only object in array

                        // Convert it back into an item using reflection
                        String itemTypeKey = item.getType().getKey().toString(); // example: "minecraft:diamond_sword"
                        String itemAsString = itemTypeKey + components; // results in: "minecraft:diamond_sword[minecraft:damage=53]"
                        
                        java.lang.reflect.Method createItemStackMethod = Bukkit.getItemFactory().getClass().getMethod("createItemStack", String.class);
                        return (ItemStack) createItemStackMethod.invoke(Bukkit.getItemFactory(), itemAsString);
                    } catch (Exception reflectionError) {
                        Shop.getPlugin().getShopLogger().debug("Reflection failed for component string methods: " + reflectionError.getMessage());
                        // Fall through to legacy approach
                    }
                }
                
                // Legacy fallback: create a new item without the damage
                ItemStack newItem = new ItemStack(item.getType(), item.getAmount());
                ItemMeta newMeta = newItem.getItemMeta();
                ItemMeta originalMeta = item.getItemMeta();
                
                // Copy over basic properties that exist in legacy versions
                if (originalMeta.hasDisplayName()) {
                    newMeta.setDisplayName(originalMeta.getDisplayName());
                }
                if (originalMeta.hasLore()) {
                    newMeta.setLore(originalMeta.getLore());
                }
                if (originalMeta.hasEnchants()) {
                    newMeta.getEnchants().forEach((enchantment, level) -> 
                        newMeta.addEnchant(enchantment, level, true));
                }
                
                newItem.setItemMeta(newMeta);
                return newItem;
            }

            // Default return original item
            return item;
        } catch (Exception e) {
            Shop.getPlugin().getShopLogger().warning("Error removing zero damage meta from item: " + item);
            Shop.getPlugin().getShopLogger().warning("checkItemDurability feature may be unsupported on your version of Paper/Spigot!");
            return item;
        } catch (Error e) {
            Shop.getPlugin().getShopLogger().warning("Error removing zero damage meta from item: " + item);
            Shop.getPlugin().getShopLogger().warning("checkItemDurability feature may be unsupported on your version of Paper/Spigot!");
            return item;
        }
    }

    public void setOwner(UUID newOwner){
        this.owner = newOwner;
    }

    public void setPrice(double price){
        this.price = price;
    }

    public void setAmount(int amount){
        this.amount = amount;
    }

    public void refreshGuiIcon() {
        if(this.type != ShopType.GAMBLE) {
            if (this.getItemStack() == null)
                return;
            guiIcon = this.getItemStack().clone();
            guiIcon.setAmount(1);
        }
        else{
            guiIcon = Shop.getPlugin().getGambleDisplayItem().clone();
            guiIcon.setAmount(1);
        }

        //get the placeholder icon with all of the unformatted fields
        ItemStack placeHolderIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.ALL_SHOP_ICON, null, null);

        String name = ShopMessage.formatMessage(placeHolderIcon.getItemMeta().getDisplayName(), this, null, false);
        List<String> lore = new ArrayList<>();
        for(String loreLine : placeHolderIcon.getItemMeta().getLore()){
            // Don't add barter line to non barter shops
            if(loreLine.contains("[barter item]") && this.getType() != ShopType.BARTER) continue;
            // Add all lore lines
            PlaceholderContext context = new PlaceholderContext();
            context.setShop(this);
            lore.add(ShopMessage.format(loreLine, context).toLegacyText());
        }

        ItemMeta itemMeta = guiIcon.getItemMeta();
        itemMeta.setDisplayName(name);
        itemMeta.setLore(lore);

        // Store sign location in item data using the new helper method  
        CompatibilityUtil.setItemData(guiIcon, "signLocation", UtilMethods.getCleanLocation(this.getSignLocation(), true));

        guiIcon.setItemMeta(itemMeta);
    }

    public int getItemDurabilityPercent(){
        ItemStack item = this.getItemStack().clone();
        return UtilMethods.getDurabilityPercent(item);
    }

    public int getSecondaryItemDurabilityPercent(){
        ItemStack item = this.getSecondaryItemStack().clone();
        return UtilMethods.getDurabilityPercent(item);
    }

    public void setSignLinesRequireRefresh(boolean signLinesRequireRefresh){
        this.signLinesRequireRefresh = signLinesRequireRefresh;
    }

    public boolean getSignLinesRequireRefresh(){
        return this.signLinesRequireRefresh;
    }

    public boolean isPerformingTransaction(){
        return isPerformingTransaction;
    }

    public void updateSign() {
        // If we don't need to update the lines, then don't update them!
        if (!signLinesRequireRefresh) { return; }

        signLines = ShopMessage.getSignLines(this, this.type);

        // Use the sign's location to ensure the update runs in the correct region in Folia
        Shop.getPlugin().getFoliaLib().getScheduler().runAtLocationLater(signLocation, task -> {
            // Update the GUI Icon since the sign needs an update.
            refreshGuiIcon();

            Sign signBlock;
            try {
                signBlock = (Sign) signLocation.getBlock().getState();
            } catch (ClassCastException e){
                Shop.getPlugin().getShopHandler().removeShop(AbstractShop.this);
                return;
            }

            String[] lines = signLines.clone();

            if (!isInitialized()) {
                signBlock.setLine(0, ChatColor.RED + ChatColor.stripColor(lines[0]));
                signBlock.setLine(1, ChatColor.RED + ChatColor.stripColor(lines[1]));
                signBlock.setLine(2, ChatColor.RED + ChatColor.stripColor(lines[2]));
                signBlock.setLine(3, ChatColor.RED + ChatColor.stripColor(lines[3]));
            } else {
                signBlock.setLine(0, lines[0]);
                signBlock.setLine(1, lines[1]);
                signBlock.setLine(2, lines[2]);
                signBlock.setLine(3, lines[3]);
            }

            if(isMCVersion17Plus()) {
                if (CompatibilityUtil.hasGlowingText()) {
                    try {
                        // Use reflection to avoid compile-time dependency on modern methods
                        java.lang.reflect.Method setGlowingTextMethod = signBlock.getClass().getMethod("setGlowingText", boolean.class);
                        setGlowingTextMethod.invoke(signBlock, Shop.getPlugin().getGlowingSignText());
                    } catch (Exception reflectionError) {
                        Shop.getPlugin().getShopLogger().debug("Reflection failed for setGlowingText: " + reflectionError.getMessage());
                    }
                }
            }

            signBlock.update(true);
            signLinesRequireRefresh = false;

            // Update the floating holograms for anybody who currently has them open
            if (display != null) display.updateDisplayTags();
        }, 2);
    }

    public void delete() {
        display.remove(null);

        if(UtilMethods.isMCVersion17Plus() && Shop.getPlugin().getDisplayLightLevel() > 0) {
            Block displayBlock = this.getChestLocation().getBlock().getRelative(BlockFace.UP);
            if(UtilMethods.materialIsNonIntrusive(displayBlock.getType())) {
                displayBlock.setType(Material.AIR);
            }
        }

        Block b = this.getSignLocation().getBlock();
        if (CompatibilityUtil.isWallSignBlock(b)) {
            Sign signBlock = (Sign) b.getState();
            signBlock.setLine(0, "");
            signBlock.setLine(1, "");
            signBlock.setLine(2, "");
            signBlock.setLine(3, "");
            signBlock.update(true);
        }

        //finally remove the shop from the shop handler
        Shop.getPlugin().getShopHandler().removeShop(this);
        Shop.getPlugin().getShopLogger().debug("Deleted Shop " + this);
    }

    public void teleportPlayer(Player player){
        if(player == null)
            return;

        if(chestLocation == null) {
            this.load();
            Location loc = this.getSignLocation().getBlock().getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5);
            player.teleport(loc);
        }
        else {
            Location loc = this.getSignLocation().getBlock().getRelative(facing).getLocation().add(0.5, 0, 0.5);
            loc.setYaw(UtilMethods.faceToYaw(facing.getOppositeFace()));
            loc.setPitch(25.0f);

            player.teleport(loc);
        }
        Shop.getPlugin().getShopListener().addTeleportCooldown(player);
    }

    public void printSalesInfo(Player player) {
        for (String message : ShopMessage.getUnformattedMessageList(this.getType().toString(), "description")) {
            if (message != null && !message.isEmpty()) {
                Map<ItemStack, Integer> items = new HashMap<>();
                items.put(this.item, this.amount);
                if (this.getSecondaryItemStack() != null) { items.put(this.getSecondaryItemStack(), (int) this.price); }
//                String formattedMsg = ShopMessage.formatMessage(message, this, player, false);
                (new ShopMessage(Shop.getPlugin())).sendMessage(message, player, this);
//                ShopMessage.embedAndSendHoverItemsMessage(formattedMsg, player, items);
            }
        }
    }

    public boolean isFakeSign(){
        return fakeSign;
    }

    public void setFakeSign(boolean fakeSign){
        this.fakeSign = fakeSign;
    }

    public boolean executeClickAction(PlayerInteractEvent event, ShopClickType clickType){
        ShopAction action = Shop.getPlugin().getShopAction(clickType);
        if(action == null)
            return false; //there is no action mapped to this click type
        Player player = event.getPlayer();

        switch(action) {
            case TRANSACT:
                Shop.getPlugin().getTransactionHelper().executeTransactionFromEvent(event, this, false);
                break;
            case TRANSACT_FULLSTACK:
                Shop.getPlugin().getTransactionHelper().executeTransactionFromEvent(event, this, true);
                break;
            case VIEW_DETAILS:
                this.printSalesInfo(player);
                break;
            case CYCLE_DISPLAY:
                //player clicked another player's shop sign
                if (!this.getOwnerName().equals(player.getName())) {
                    //player has permission to change another player's shop display
                    if((!Shop.getPlugin().usePerms() && player.isOp()) || (Shop.getPlugin().usePerms() && player.hasPermission("shop.operator"))) {
                        this.getDisplay().cycleType(player);
                    }
                //player clicked own shop sign
                } else {
                    if(Shop.getPlugin().usePerms() && !player.hasPermission("shop.setdisplay"))
                        return false;

                    this.getDisplay().cycleType(player);
                }
                break;
            default:
                break;
        }
        return true;
    }

    public void sendEffects(boolean success, Player player){
        try {
            if (success) {
                if (Shop.getPlugin().playSounds()) player.playSound(this.getSignLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                if (Shop.getPlugin().playEffects()) player.getWorld().playEffect(this.getChestLocation(), Effect.STEP_SOUND, Material.EMERALD_BLOCK);
            } else {
                if (Shop.getPlugin().playSounds()) player.playSound(this.getSignLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
                if (Shop.getPlugin().playEffects()) player.getWorld().playEffect(this.getChestLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
            }
        } catch (Error e){
        } catch (Exception e) {}
    }

    @Override
    public String toString() {
        return "AbstractShop{" +
                "type=" + type.toString().toUpperCase() +
                ", item=" + item +
                ", price=" + price +
                (secondaryItem != null ? ", secondaryItem=" + secondaryItem : "") +
                (isAdmin ? ", isAdmin=" + isAdmin : "") +
                ", stock=" + stock +
                ", owner=" + owner +
                ", chestLocation=" + ((chestLocation != null) ? chestLocation.getWorld().getName() + ":" + chestLocation.getBlockX() + "/" + chestLocation.getBlockY() + "/" + chestLocation.getBlockZ() : "null") +
                '}';
    }
}
