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
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
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
        String message = ShopMessage.getMessage("interactionIssue", "signRoom", null, player);
        if (message != null && !message.isEmpty())
            player.sendMessage(message);
        return null;
    }

    public boolean shopCanBeCreated(Player player, Block chest) {
        int numberOfShops = plugin.getShopHandler().getNumberOfShops(player);
        int buildPermissionNumber = plugin.getShopListener().getBuildLimit(player);

        //System.out.println("[Shop] "+player.getName()+" currently has "+numberOfShops+" shops.");
        //System.out.println("[Shop] "+player.getName()+" build limit is "+buildPermissionNumber+" shops.");

        if ((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
            if (numberOfShops >= buildPermissionNumber) {
                player.sendMessage(ShopMessage.getMessage("permission", "buildLimit", null, player));
                return false;
            }
        }

        if (plugin.getWorldBlacklist().contains(chest.getWorld().getName())) {
            if ((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
                player.sendMessage(ShopMessage.getMessage("interactionIssue", "worldBlacklist", null, player));
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
                player.sendMessage(ShopMessage.getMessage("permission", "create", null, player));
                return false;
            }
        }


        //do a check for the WorldGuard region (optional hook)
        boolean canCreateShopInRegion = true;
        try {
            if(plugin.hookWorldGuard()) {
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
            player.sendMessage(ShopMessage.getMessage("interactionIssue", "regionRestriction", null, player));
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
                playerMessage = ShopMessage.getMessage("permission", "create", shop, player);
        }

        if (type == ShopType.GAMBLE) {
            isAdmin = true;
            shop.setAdmin(true);
            if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                playerMessage = ShopMessage.getMessage("permission", "create", shop, player);
            }
        }

        //if players must pay to create shops, check that they have enough money first
        double cost = plugin.getCreationCost();
        if (cost > 0) {
            if (!EconomyUtils.hasSufficientFunds(player, player.getInventory(), cost)) {
                playerMessage = ShopMessage.getMessage("interactionIssue", "createInsufficientFunds", shop, player);
            }
        }

        if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
            playerMessage = null;
        }

        //prevent players (even if they are OP) from creating a shop on a double chest with another player
        AbstractShop existingShop = plugin.getShopHandler().getShopByChest(chestBlock);
        if (existingShop != null && !existingShop.isAdmin()) {
            if (!existingShop.getOwnerUUID().equals(player.getUniqueId())) {
                playerMessage = ShopMessage.getMessage("interactionIssue", "createOtherPlayer", null, player);
            }
        }

        if (playerMessage != null) {
            if(!playerMessage.isEmpty())
                player.sendMessage(playerMessage);
            return null;
        }

        //removed all the direction checking code. just make sure its a container
        //make sure that the sign is in front of the chest, unless it is a shulker box
        if (plugin.getShopHandler().isChest(chestBlock)) {
            existingShop = plugin.getShopHandler().getShopByChest(chestBlock);
            if (existingShop != null) {
                //if the block they are adding a sign to is already a shop, do not let them
                if (chestBlock.getLocation().equals(existingShop.getChestLocation())) {
                    String message = ShopMessage.getMessage("interactionIssue", "createOtherPlayer", null, player);
                    if (message != null && !message.isEmpty())
                        player.sendMessage(message);
                    return null;
                }
            }


            if (!(signBlock.getType() == Material.WALL_SIGN)) {
                if (!signBlock.getType().toString().contains("SIGN")) {
                    return null;
                }
                signBlock.setType(Material.WALL_SIGN);

                org.bukkit.material.Sign sign = (org.bukkit.material.Sign) signBlock.getState().getData();
                sign.setFacingDirection(signDirection);
                signBlock.setData(sign.getData(), true);
                signBlock.getState().update();
            }
            Sign signBlockState = (Sign) signBlock.getState();
            signBlockState.update();

            shop.setAdmin(isAdmin);
            shop.load();

            PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
            plugin.getServer().getPluginManager().callEvent(e);

            plugin.getLogHandler().logAction(player, shop, ShopActionType.CREATE);

            if (e.isCancelled())
                return null;

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
            shop.updateSign();
        }
        return shop;
    }

    public void sendCreationSuccess(Player player, AbstractShop shop){
        shop.getDisplay().spawn(player);
        shop.updateSign();
        String message = ShopMessage.getMessage(shop.getType().toString(), "create", shop, player);
        if(message != null && !message.isEmpty())
            player.sendMessage(message);
        Shop.getPlugin().getTransactionHelper().sendEffects(true, player, shop);
        Shop.getPlugin().getShopHandler().saveShops(shop.getOwnerUUID());
    }

    public boolean itemsCanBeInitialized(Player player, ItemStack itemStack, ItemStack barterItemStack){

        //if the item is on the DENY LIST or the item is not on the ALLOW LIST, don't let player initialize with it
        if(((!plugin.usePerms() && player.isOp()) || (plugin.usePerms() && player.hasPermission("shop.operator")))) {
            boolean passesItemList = plugin.getShopHandler().passesItemListCheck(itemStack);
            if(!passesItemList){
                String message = ShopMessage.getMessage("interactionIssue", "itemListDeny", null, player);
                if(message != null && !message.isEmpty())
                    player.sendMessage(message);
                //plugin.getTransactionListener().sendEffects(false, player, shop);
                return false;
            }
        }

        //System.out.println("[shop] item: "+ itemStack.getType().toString()+", otherItem: "+barterItemStack.getType().toString());
        if (InventoryUtils.itemstacksAreSimilar(itemStack, barterItemStack)) {
            String message = ShopMessage.getMessage("interactionIssue", "sameItem", null, player);
            if(message != null && !message.isEmpty())
                player.sendMessage(message);
            //plugin.getTransactionListener().sendEffects(false, player, shop);
            return false;
        }
        return true;
    }

    public boolean initializeShop(AbstractShop shop, Player player , ItemStack item, ItemStack barterItem){
        if (!player.getUniqueId().equals(shop.getOwnerUUID())) {
            //do not allow non operators to initialize other player's shops
            if((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
                String message = ShopMessage.getMessage("interactionIssue", "initialize", null, player);
                if(message != null && !message.isEmpty())
                    player.sendMessage(message);
                plugin.getTransactionHelper().sendEffects(false, player, shop);
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
                    String message = ShopMessage.getMessage("interactionIssue", "displayRoom", null, player);
                    if(message != null && !message.isEmpty())
                        player.sendMessage(message);
                    plugin.getTransactionHelper().sendEffects(false, player, shop);
                    return false;
                }
            }
        }

        //if players must pay to create shops, remove money first
        double cost = plugin.getCreationCost();
        if(cost > 0 && !shop.isAdmin()){
            boolean removed = EconomyUtils.removeFunds(player, player.getInventory(), cost);
            if(!removed){
                String message = ShopMessage.getMessage("interactionIssue", "createInsufficientFunds", shop, player);
                if(message != null && !message.isEmpty())
                    player.sendMessage(message);
                plugin.getTransactionHelper().sendEffects(false, player, shop);
                return false;
            }
        }

        if(!itemsCanBeInitialized(player, item, barterItem)){
            plugin.getTransactionHelper().sendEffects(false, player, shop);
            return false;
        }

        if (shop.getItemStack() == null && item != null) {

            PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
            Bukkit.getServer().getPluginManager().callEvent(e);

            if(e.isCancelled())
                return false;

            shop.setItemStack(item);

            if (shop.getType() == ShopType.BARTER && barterItem == null) {
                String message = ShopMessage.getMessage(shop.getType().toString(), "initializeInfo", shop, player);
                if(message != null && !message.isEmpty())
                    player.sendMessage(message);
                message = ShopMessage.getMessage(shop.getType().toString(), "initializeBarter", shop, player);
                if(message != null && !message.isEmpty())
                    player.sendMessage(message);
                if(plugin.allowCreativeSelection()) {
                    message = ShopMessage.getMessage("BUY", "initializeAlt", shop, player);
                    if(message != null && !message.isEmpty())
                        player.sendMessage(message);
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
                int multiplyValue = UtilMethods.getMultiplyValue(input);
                String line3 = UtilMethods.cleanNumberText(input);

                if (line3.contains("."))
                    price = Double.parseDouble(line3);
                else
                    price = Long.parseLong(line3);

                price *= multiplyValue;
            } catch (NumberFormatException e) {
                player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                return -1;
            }
        } else {
            try {
                String line3 = UtilMethods.cleanNumberText(input);
                price = Long.parseLong(line3);

            } catch (NumberFormatException e) {
                player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                return -1;
            }
        }
        //only allow price to be zero if the type is selling
        //if (price < 0 || (price == 0 && !(type == ShopType.SELL))) {
        if (price < 0 || (price == 0 && shopType == ShopType.BARTER)) {
            player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
            return -1;
        }
        return price;
    }

    public double getShopPriceCombo(Player player, String input, ShopType shopType){
        double priceCombo = 0;
        if (plugin.getCurrencyType() == CurrencyType.VAULT) {
            try {
                int multiplyValue = UtilMethods.getMultiplyValue(input);
                String line3 = UtilMethods.cleanNumberText(input);

                if (line3.contains("."))
                    priceCombo = Double.parseDouble(line3);
                else
                    priceCombo = Long.parseLong(line3);

                priceCombo *= multiplyValue;

            } catch (NumberFormatException e) {
                player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                return -1;
            }
        } else {
            try {
                String line3 = UtilMethods.cleanNumberText(input);
                priceCombo = Long.parseLong(line3);
            } catch (NumberFormatException e) {
                player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
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
                int multiplyValue = UtilMethods.getMultiplyValue(input);
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
                player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
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
                player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                return null;
            }
        }
        //only allow price to be zero if the type is selling
        //if (price < 0 || (price == 0 && !(type == ShopType.SELL))) {
        if (price < 0 || (price == 0 && shopType == ShopType.BARTER)) {
            player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
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
