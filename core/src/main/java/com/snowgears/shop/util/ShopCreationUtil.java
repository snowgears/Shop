package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.event.PlayerCreateShopEvent;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import com.snowgears.shop.hook.TownyHook;
import com.snowgears.shop.hook.WorldGuardHook;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ShopCreationUtil {

    private Shop plugin;
    private BlockFace[] wallFaces = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    public ShopCreationUtil(Shop plugin){
        this.plugin = plugin;
    }

    public BlockFace calculateBlockFaceForSign(Player player, Block chest, BlockFace facePreference){
        if(facePreference == BlockFace.UP || facePreference == BlockFace.DOWN)
            facePreference = BlockFace.NORTH;
        Block futureSign = chest.getRelative(facePreference);
        if(UtilMethods.materialIsNonIntrusive(futureSign.getType()))
            return facePreference;
        for(BlockFace face : wallFaces){
            futureSign = chest.getRelative(face);
            if(UtilMethods.materialIsNonIntrusive(futureSign.getType()))
                return face;
        }
        ShopMessage.sendMessage("interactionIssue", "signRoom", player, null);
        return null;
    }

    public boolean shopCanBeCreated(Player player, Block chest) {
        int numberOfShops = plugin.getShopHandler().getNumberOfShops(player);
        int buildPermissionNumber = plugin.getShopListener().getBuildLimit(player);

        //System.out.println("[Shop] "+player.getName()+" currently has "+numberOfShops+" shops.");
        //System.out.println("[Shop] "+player.getName()+" build limit is "+buildPermissionNumber+" shops.");

        if ((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
            if (numberOfShops >= buildPermissionNumber) {
                ShopMessage.sendMessage("permission", "buildLimit", player, null);
                return false;
            }
        }

        if (plugin.getWorldBlacklist().contains(chest.getWorld().getName())) {
            if ((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
                ShopMessage.sendMessage("interactionIssue", "worldBlacklist", player, null);
                return false;
            }
        }

        if (plugin.usePerms() && !player.hasPermission("shop.operator")) {
            boolean canCreate = false;
            if(!player.hasPermission("shop.create")){
                for(ShopType shopType : ShopType.values()){
                    if(player.hasPermission("shop.create."+shopType.toString().toLowerCase()))
                        canCreate = true;
                }
            }
            else {
                canCreate = true;
            }
            if(!canCreate){
                ShopMessage.sendMessage("permission", "create", player, null);
                return false;
            }
        }


        //do a check for the WorldGuard region (optional hook)
        boolean canCreateShopInRegion = true;
        try {
            if(plugin.worldGuardExists()) {
                canCreateShopInRegion = WorldGuardHook.canCreateShop(player, chest.getLocation());
            }
        } catch (NoClassDefFoundError e) {
            //tried to hook world guard but it was not registered
            e.printStackTrace();
        }

        //do a check for the Towny region (optional hook)
        try {
            if(plugin.hookTowny()) {
                canCreateShopInRegion = TownyHook.canCreateShop(player, chest.getLocation());
            }
        } catch (NoClassDefFoundError e) {
            //tried to hook towny but it was not registered
            e.printStackTrace();
        }

        if (!canCreateShopInRegion) {
            ShopMessage.sendMessage("interactionIssue", "regionRestriction", player, null);
            return false;
        }

        return true;
    }

    public AbstractShop createShop(Player player, Block chestBlock, Block signBlock, PricePair pricePair, int amount, boolean isAdmin, ShopType type, BlockFace signDirection, boolean isFakeSign){
        String playerMessage = null;
        if(type == null)
            type = ShopType.SELL;
        final AbstractShop shop = AbstractShop.create(signBlock.getLocation(), player.getUniqueId(), pricePair.getPrice(), pricePair.getPriceCombo(), amount, isAdmin, type, signDirection);
        shop.setFakeSign(isFakeSign);

        if (plugin.usePerms()) {
            if (!(player.hasPermission("shop.create." + type.toString().toLowerCase()) || player.hasPermission("shop.create")))
                playerMessage = ShopMessage.getUnformattedMessage("permission", "create");
        }

        if (type == ShopType.GAMBLE) {
            isAdmin = true;
            shop.setAdmin(true);
            if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                playerMessage = ShopMessage.getUnformattedMessage("permission", "create");
            }
        }

        //if players must pay to create shops, check that they have enough money first
        double cost = plugin.getCreationCost();
        if (cost > 0) {
            if (!EconomyUtils.hasSufficientFunds(player, player.getInventory(), cost)) {
                playerMessage = ShopMessage.getUnformattedMessage("interactionIssue", "createInsufficientFunds");
            }
        }

        if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
            playerMessage = null;
        }

        //prevent players (even if they are OP) from creating a shop on a double chest with another player
        AbstractShop existingShop = plugin.getShopHandler().getShopByChest(chestBlock);
        if (existingShop != null && !existingShop.isAdmin()) {
            if (!existingShop.getOwnerUUID().equals(player.getUniqueId())) {
                playerMessage = ShopMessage.getUnformattedMessage("interactionIssue", "createOtherPlayer");
            }
        }

        if (playerMessage != null) {
            if(!playerMessage.isEmpty())
                ShopMessage.sendMessage(playerMessage, player, shop);
            return null;
        }

        //removed all the direction checking code. just make sure its a container
        //make sure that the sign is in front of the chest, unless it is a shulker box
        if (chestBlock.getType().toString().contains("CHEST") 
            || (plugin.useEnderChests() && chestBlock.getType().toString().contains("ENDER_CHEST"))
            || chestBlock.getType().toString().contains("BARREL")
            || chestBlock.getType().toString().contains("SHULKER_BOX")
        ) {
            //System.out.println("Chest of shop was a container.");
            existingShop = plugin.getShopHandler().getShopByChest(chestBlock);
            if (existingShop != null) {
                //if the block they are adding a sign to is already a shop, do not let them
                if (chestBlock.getLocation().equals(existingShop.getChestLocation())) {
                    ShopMessage.sendMessage("interactionIssue", "createOtherPlayer", player, shop);
                    return null;
                }
            }

            SignUtil.setFacing(signBlock, signDirection);

            shop.setAdmin(isAdmin);
            shop.load();

            PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
            plugin.getServer().getPluginManager().callEvent(e);

            plugin.getLogHandler().logAction(player, shop, ShopActionType.CREATE);

            if (e.isCancelled())
                return null;

            if (MCVersion.atLeast("1.17") && plugin.getDisplayLightLevel() > 0) {
                Block displayBlock = shop.getChestLocation().getBlock().getRelative(BlockFace.UP);
                MaterialUtil.setLightBlockData(displayBlock, plugin.getDisplayLightLevel());
            }

            if (type == ShopType.GAMBLE) {
                shop.setItemStack(plugin.getGambleDisplayItem());
                shop.setAmount(1);
                plugin.getShopHandler().addShop(shop);
                shop.getDisplay().setType(DisplayType.LARGE_ITEM, false);

                plugin.getShopCreationUtil().sendCreationSuccess(player, shop);
                plugin.getLogHandler().logAction(player, shop, ShopActionType.INIT);
                return null;
            }

            plugin.getShopHandler().addShop(shop);
            Shop.getPlugin().getShopLogger().trace("[ShopCreationUtil.createShop] updateSign");
            shop.updateSign();
        }
        return shop;
    }

    public void cleanupShopCreationProcess(Player player){
        ShopCreationProcess process = plugin.getMiscListener().getShopCreationProcess(player);
        if (process != null) {
            process.cleanup();
            plugin.getMiscListener().removeShopCreationProcess(player);
        }
    }

    public void sendCreationSuccess(Player player, AbstractShop shop){
        if (shop.getDisplay() != null) shop.getDisplay().spawn(player);
        Shop.getPlugin().getShopLogger().trace("[ShopCreationUtil.sendCreationSuccess] updateSign");
        shop.setSignLinesRequireRefresh(true);
        shop.updateSign();
        shop.setNeedsSave(true);
        ShopMessage.sendMessage(shop.getType().toString(), "create", player, shop);
        shop.sendEffects(true, player);
        // Save the shop to disk
        Shop.getPlugin().getShopHandler().saveShops(shop.getOwnerUUID(), true);
        // Cleanup the shop creation process
        cleanupShopCreationProcess(player);
    }

    public boolean itemsCanBeInitialized(Player player, ItemStack itemStack, ItemStack barterItemStack){
        boolean isAdmin = (!plugin.usePerms() && player.isOp()) || (plugin.usePerms() && player.hasPermission("shop.operator"));

        //if the item is on the DENY LIST or the item is not on the ALLOW LIST, don't let player initialize with it
        // Only perform this check for non admins
        if (!isAdmin) {
            boolean passesItemList = plugin.getShopHandler().passesItemListCheck(itemStack);
            if (!passesItemList) {
                ShopMessage.sendMessage("interactionIssue", "itemListDeny", player, null);
                //shop.sendEffects(false, player);
                return false;
            }
        }

        //System.out.println("[shop] item: "+ itemStack.getType().toString()+", otherItem: "+barterItemStack.getType().toString());
        // Always perform this check, even if admin!
        if (InventoryUtils.itemstacksAreSimilar(itemStack, barterItemStack)) {
            ShopMessage.sendMessage("interactionIssue", "sameItem", player, null);
            //shop.sendEffects(false, player);
            return false;
        }
        return true;
    }

    public boolean initializeShop(AbstractShop shop, Player player , ItemStack item, ItemStack barterItem){
        if (!player.getUniqueId().equals(shop.getOwnerUUID())) {
            //do not allow non operators to initialize other player's shops
            if((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
                ShopMessage.sendMessage("interactionIssue", "initialize", player, shop);
                shop.sendEffects(false, player);
                return false;
            }
        }

        if (item.getType() == Material.AIR) {
            return false;
        }

        if(plugin.getDisplayType() != DisplayType.NONE) {
            //make sure there is room above the shop for the display
            Block aboveShop = shop.getChestLocation().getBlock().getRelative(BlockFace.UP);
            if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                if(plugin.forceDisplayToNoneIfBlocked()){
                    shop.getDisplay().setType(DisplayType.NONE, false);
                    //shop.getDisplay().spawn(player);
                    //shop.updateSign();
                }
                else {
                    ShopMessage.sendMessage("interactionIssue", "displayRoom", player, shop);
                    shop.sendEffects(false, player);
                    return false;
                }
            }
        }

        //if players must pay to create shops, remove money first
        double cost = plugin.getCreationCost();
        // Check if the shop is not an admin shop and if the shop is not a barter shop or the barter item is not null
        // When creating a barter shop with a sign, initializeShop is called twice, we only want to charge them once both items are selected
        if(cost > 0 && !shop.isAdmin() && !(shop.getType() == ShopType.BARTER && barterItem == null)){
            boolean removed = EconomyUtils.removeFunds(player, player.getInventory(), cost);
            if(!removed){
                ShopMessage.sendMessage("interactionIssue", "createInsufficientFunds", player, shop);
                shop.sendEffects(false, player);
                return false;
            }
        }

        try {
            //stop the edge case of shulker boxes being able to be used in shulker chests
            if (item.getType().toString().contains("SHULKER_BOX")) {
                if (shop.getChestLocation().getBlock().getType().toString().contains("SHULKER_BOX")) {
                    return false;
                }
            }
        } catch (NoSuchFieldError e) {}

        if(!itemsCanBeInitialized(player, item, barterItem)){
            shop.sendEffects(false, player);
            return false;
        }

        if (shop.getItemStack() == null && item != null) {

            PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
            Bukkit.getServer().getPluginManager().callEvent(e);

            if(e.isCancelled())
                return false;

            shop.setItemStack(item);

            ShopCreationProcess process = plugin.getMiscListener().getShopCreationProcess(player);
            if (shop.getType() == ShopType.BARTER && barterItem == null) {
                ShopMessage.sendMessage(shop.getType().toString(), "initializeInfo", player, shop);
                process.setStep(ShopCreationProcess.ChatCreationStep.SIGN_BARTER_ITEM);
                process.displayFloatingText(shop.getType().toString(), "initializeBarter");
                // ShopMessage.sendMessage(shop.getType().toString(), "initializeBarter", player, shop);
                if(plugin.allowCreativeSelection()) {
                    ShopMessage.sendMessage("BUY", "initializeAlt", player, shop);
                }
            }
            else if(shop.getType() != ShopType.BARTER){
                return true;
            }
        }
        if (shop.getSecondaryItemStack() == null && barterItem != null) {

                PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                Bukkit.getServer().getPluginManager().callEvent(e);

                if(e.isCancelled())
                    return false;

                shop.setSecondaryItemStack(barterItem);
                return true;
        }
        return false;
    }

    public ShopType getShopType(String input){
        ShopType type = null;
        if (input.toLowerCase().contains(ShopMessage.getCreationWord("BUY")))
            type = ShopType.BUY;
        else if (input.toLowerCase().contains(ShopMessage.getCreationWord("BARTER")))
            type = ShopType.BARTER;
        else if (input.toLowerCase().contains(ShopMessage.getCreationWord("GAMBLE")))
            type = ShopType.GAMBLE;
        else if (input.toLowerCase().contains(ShopMessage.getCreationWord("COMBO")))
            type = ShopType.COMBO;
        else if (input.toLowerCase().contains(ShopMessage.getCreationWord("SELL")))
            type = ShopType.SELL;
        return type;
    }

    public double getShopPrice(Player player, String input, ShopType shopType){
        double price = 0;
        if (plugin.getCurrencyType() == CurrencyType.VAULT) {
            try {
                double multiplyValue = UtilMethods.getMultiplyValue(input);
                String line3 = UtilMethods.cleanNumberText(input);

                if (line3.contains("."))
                    price = Double.parseDouble(line3);
                else
                    price = Long.parseLong(line3);

                price *= multiplyValue;
            } catch (NumberFormatException e) {
                ShopMessage.sendMessage("interactionIssue", "line3", player, null);
                return -1;
            }
        } else {
            try {
                String line3 = UtilMethods.cleanNumberText(input);
                price = Long.parseLong(line3);

            } catch (NumberFormatException e) {
                ShopMessage.sendMessage("interactionIssue", "line3", player, null);
                return -1;
            }
        }
        //only allow price to be zero if the type is selling
        //if (price < 0 || (price == 0 && !(type == ShopType.SELL))) {
        if (price < 0 || (price == 0 && shopType == ShopType.BARTER)) {
            ShopMessage.sendMessage("interactionIssue", "line3", player, null);
            return -1;
        }
        return price;
    }

    public double getShopPriceCombo(Player player, String input, ShopType shopType){
        double priceCombo = 0;
        if (plugin.getCurrencyType() == CurrencyType.VAULT) {
            try {
                double multiplyValue = UtilMethods.getMultiplyValue(input);
                String line3 = UtilMethods.cleanNumberText(input);

                if (line3.contains("."))
                    priceCombo = Double.parseDouble(line3);
                else
                    priceCombo = Long.parseLong(line3);

                priceCombo *= multiplyValue;

            } catch (NumberFormatException e) {
                ShopMessage.sendMessage("interactionIssue", "line3", player, null);
                return -1;
            }
        } else {
            try {
                String line3 = UtilMethods.cleanNumberText(input);
                priceCombo = Long.parseLong(line3);
            } catch (NumberFormatException e) {
                ShopMessage.sendMessage("interactionIssue", "line3", player, null);
                return -1;
            }
        }
        return priceCombo;
    }

    public PricePair getShopPricePair(Player player, String input, ShopType shopType){
        PricePair pricePair = null;
        double price = 0;
        double priceCombo = 0;
        if (plugin.getCurrencyType() == CurrencyType.VAULT) {
            try {
                double multiplyValue = UtilMethods.getMultiplyValue(input);
                String line3 = UtilMethods.cleanNumberText(input);

                String[] multiplePrices = line3.split(" ");
                if (multiplePrices.length > 1) {
                    if (multiplePrices[0].contains("."))
                        price = Double.parseDouble(multiplePrices[0]);
                    else
                        price = Long.parseLong(multiplePrices[0]);

                    if (multiplePrices[1].contains("."))
                        priceCombo = Double.parseDouble(multiplePrices[1]);
                    else
                        priceCombo = Long.parseLong(multiplePrices[1]);
                } else {
                    if (line3.contains("."))
                        price = Double.parseDouble(line3);
                    else
                        price = Long.parseLong(line3);
                }

                price *= multiplyValue;
                priceCombo *= multiplyValue;

            } catch (NumberFormatException e) {
                ShopMessage.sendMessage("interactionIssue", "line3", player, null);
                return null;
            }
        } else {
            try {
                String line3 = UtilMethods.cleanNumberText(input);

                String[] multiplePrices = line3.split(" ");
                if (multiplePrices.length > 1) {
                    price = Long.parseLong(multiplePrices[0]);
                    priceCombo = Long.parseLong(multiplePrices[1]);
                } else {
                    price = Long.parseLong(line3);
                }
            } catch (NumberFormatException e) {
                ShopMessage.sendMessage("interactionIssue", "line3", player, null);
                return null;
            }
        }
        //only allow price to be zero if the type is selling
        //if (price < 0 || (price == 0 && !(type == ShopType.SELL))) {
        if (price < 0 || (price == 0 && shopType == ShopType.BARTER)) {
            ShopMessage.sendMessage("interactionIssue", "line3", player, null);
            return null;
        }
        return new PricePair(price, priceCombo);
    }

    public boolean getShopIsAdmin(String input){
        if (input.toLowerCase().contains(ShopMessage.getCreationWord("ADMIN")))
            return true;
        return false;
    }
}
