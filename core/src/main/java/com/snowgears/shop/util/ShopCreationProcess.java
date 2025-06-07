package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.AbstractDisplay;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.snowgears.shop.util.CompatibilityUtil;

public class ShopCreationProcess {

    private ChatCreationStep step;

    private Player player;
    private UUID processUUID;
    private UUID playerUUID;
    private Block clickedChest;
    private BlockFace clickedFace;
    private ItemStack itemStack;
    private ItemStack barterItemStack;
    private ShopType shopType;
    boolean isAdmin;
    private PricePair pricePair;

    public AbstractDisplay display;
    private PlaceholderContext placeholderContext;

    public ShopCreationProcess(Player player, Block clickedChest, BlockFace clickedFace){
        this.player = player;
        this.processUUID = UUID.randomUUID();
        this.playerUUID = player.getUniqueId();
        this.clickedChest = clickedChest;
        this.clickedFace = clickedFace;
        this.step = ChatCreationStep.ITEM;

        // Displays instructions on top of the chest
        this.display = Shop.getPlugin().getShopHandler().createDisplay(clickedChest.getLocation());
        // Setup placeholder context for ShopMessage
        this.placeholderContext = new PlaceholderContext();
        this.placeholderContext.setPlayer(player);
        this.placeholderContext.setProcess(this);
    }

    public void cleanup() {
        if (this.display.isEnabled()) {
            this.display.removeDisplayEntities(player, true);
        }
    }

    public Block getClickedChest() {
        return clickedChest;
    }

    public BlockFace getClickedFace() {
        return clickedFace;
    }

    public ShopType getShopType() {
        return shopType;
    }

    public void setShopType(ShopType shopType) {
        this.shopType = shopType;
        if(shopType == ShopType.GAMBLE)
            this.step = ChatCreationStep.ITEM_PRICE;
        else
            this.step = ChatCreationStep.ITEM_AMOUNT;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public int getItemAmount() {
        if(itemStack == null)
            return 0;
        return itemStack.getAmount();
    }

    public void setItemAmount(int itemAmount) {
        this.itemStack.setAmount(itemAmount);
        if(this.shopType == ShopType.BARTER){
            this.step = ChatCreationStep.BARTER_ITEM;
        }
        else {
            this.step = ChatCreationStep.ITEM_PRICE;
        }
    }

    public int getBarterItemAmount() {
        if(barterItemStack == null)
            return 0;
        return barterItemStack.getAmount();
    }

    public void setBarterItemAmount(int barterItemAmount){
        this.barterItemStack.setAmount(barterItemAmount);
        this.step = ChatCreationStep.FINISHED;
    }

    public PricePair getPricePair(){
        if(pricePair == null)
            this.pricePair = new PricePair(0, 0);
        return pricePair;
    }

    public ChatCreationStep getStep() { return step; }
    public void setStep(ChatCreationStep step) { this.step = step; }

    public void setPricePair(PricePair pricePair){
        this.pricePair = pricePair;
        if(this.shopType == ShopType.COMBO)
            this.step = ChatCreationStep.ITEM_PRICE_COMBO;
        else
            this.step = ChatCreationStep.FINISHED;
    }

    public void createShop(Player player){
        final ShopCreationProcess process = this;
        // Run task at the chest block location to ensure it runs in the correct region in Folia
        Shop.getPlugin().getFoliaLib().getScheduler().runAtLocation(clickedChest.getLocation(), task -> {
            //TODO do some calculation here if clickedFace is filled with a block or UP / DOWN was clicked
            Block signBlock = clickedChest.getRelative(clickedFace);
            signBlock.setType(MaterialUtil.of("OAK_WALL_SIGN"));

            SignUtil.setFacing(signBlock, clickedFace);

            AbstractShop shop = Shop.getPlugin().getShopCreationUtil().createShop(Bukkit.getPlayer(playerUUID), clickedChest, signBlock, getPricePair(), getItemAmount(), isAdmin, shopType, clickedFace, true);
            if(shop == null) {
                return;
            }

            boolean initializedShop = Shop.getPlugin().getShopCreationUtil().initializeShop(shop, player, itemStack, barterItemStack);

            if(initializedShop) {
                Shop.getPlugin().getShopCreationUtil().sendCreationSuccess(player, shop);
                Shop.getPlugin().getLogHandler().logAction(player, shop, ShopActionType.INIT);
            }
        });
    }

    public UUID getUniqueID(){
        return processUUID;
    }

    public UUID getPlayerUUID(){
        return playerUUID;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public ItemStack getBarterItemStack() {
        return barterItemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.step = ChatCreationStep.SHOP_TYPE;
    }

    public void setBarterItemStack(ItemStack barterItemStack) {
        this.barterItemStack = barterItemStack.clone();
        this.barterItemStack.setAmount(1);
        this.step = ChatCreationStep.BARTER_ITEM_AMOUNT;
    }

    public void setPrice(double price){
        if(pricePair == null)
            pricePair = new PricePair(price, 0);
        pricePair.setPrice(price);
        if(this.shopType == ShopType.COMBO)
            this.step = ChatCreationStep.ITEM_PRICE_COMBO;
        else
            this.step = ChatCreationStep.FINISHED;
    }

    public void setPriceCombo(double priceCombo){
        if(pricePair == null)
            pricePair = new PricePair(0, priceCombo);
        pricePair.setPriceCombo(priceCombo);
        this.step = ChatCreationStep.FINISHED;
    }

    public void displayFloatingText(String key, String subkey) {
        // Check if feature is enabled or not.
        if (!Shop.getPlugin().getConfig().getBoolean("displayFloatingCreateText") || !this.display.isEnabled()) {
            ShopMessage.sendMessage(key, subkey, this, player);
            return;
        }
        // Build the lines
        String unformatted = ShopMessage.getUnformattedMessage(key, subkey);
        String formatted = ShopMessage.format(unformatted, this.placeholderContext).toLegacyText();
        List<String> lines = UtilMethods.splitStringIntoLines(formatted, ShopMessage.getTargetMaxLength());
        // Display the lines
        displayFloatingLines(lines);
    }

    public void displayFloatingTextList(String key, String subkey) {
        // Check if feature is enabled or not.
        if (!Shop.getPlugin().getConfig().getBoolean("displayFloatingCreateText") || !this.display.isEnabled()) {
            for (String message : ShopMessage.getUnformattedMessageList(key, subkey)) {
                if (message != null && !message.isEmpty())
                    ShopMessage.sendMessage(message, player);
            }
            return;
        }
        List<String> lines = new ArrayList<>();
        // Build the lines
        for (String unformatted : ShopMessage.getUnformattedMessageList(key, subkey)) {
            if (unformatted != null && !unformatted.isEmpty()){
                String formatted = ShopMessage.format(unformatted, this.placeholderContext).toLegacyText();
                lines.addAll(UtilMethods.splitStringIntoLines(formatted, ShopMessage.getTargetMaxLength()));
            }
        }
        // Display the lines
        displayFloatingLines(lines);
    }

    public void displayFloatingLines(List<String> lines) {
        if (!this.display.isEnabled()) {
            Shop.getPlugin().getShopLogger().warning("Unable to display floating text for player " + player.getName() + ", Display is disabled");
            return;
        }
        // Remove any existing text
        this.display.removeDisplayEntities(player, true);

        Location loc = this.clickedChest.getLocation().clone().add(0.5,0.625 + (0.248*lines.size()),0.5);
        int i = 0;
        for (String line : lines) {
            this.display.createTagEntity(player, line, loc.clone().add(0, (i * -0.248), 0));
            i++;
        }
    }

    public enum ChatCreationStep {
        // Sign creation steps
        SIGN_CREATION,
        SIGN_ITEM,
        SIGN_BARTER_ITEM,

        // Chat creation steps
        ITEM,

        SHOP_TYPE,

        ITEM_AMOUNT,

        ITEM_PRICE,

        ITEM_PRICE_COMBO,

        BARTER_ITEM,

        BARTER_ITEM_AMOUNT,

        FINISHED
    }


}
