package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.UUID;

public class ShopCreationProcess {

    private UUID processUUID;
    private UUID playerUUID;
    private Block clickedChest;
    private BlockFace clickedFace;
    private ItemStack itemStack;
    private ItemStack barterItemStack;
    private ShopType shopType;
    boolean isAdmin;
    private PricePair pricePair;

    private ChatCreationStep step;

    public ShopCreationProcess(Player player, Block clickedChest, BlockFace clickedFace){
        this.processUUID = UUID.randomUUID();
        this.playerUUID = player.getUniqueId();
        this.clickedChest = clickedChest;
        this.clickedFace = clickedFace;
        this.step = ChatCreationStep.ITEM;
        //TODO calculate clicked face from player if blockface is UP or DOWN
        //TODO also that there is room for the sign on the clicked face
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

    public ChatCreationStep getStep() {
        return step;
    }

    public void setPricePair(PricePair pricePair){
        this.pricePair = pricePair;
        if(this.shopType == ShopType.COMBO)
            this.step = ChatCreationStep.ITEM_PRICE_COMBO;
        else
            this.step = ChatCreationStep.FINISHED;
    }

    public void createShop(Player player){
        final ShopCreationProcess process = this;
        Shop.getPlugin().getServer().getScheduler().runTask(Shop.getPlugin(), new Runnable() {
            @Override
            public void run() {
                Block signBlock = clickedChest.getRelative(clickedFace);
                signBlock.setType(Material.WALL_SIGN);

                if(signBlock.getType() == Material.WALL_SIGN) {
                    Directional wallSignData = (Directional) signBlock.getState().getData();
                    wallSignData.setFacing(clickedFace);

                    signBlock.getState().setData((MaterialData)wallSignData);
                    signBlock.getState().update();
                }

                AbstractShop shop = Shop.getPlugin().getShopCreationUtil().createShop(Bukkit.getPlayer(playerUUID), clickedChest, signBlock, getPricePair(), getItemAmount(), isAdmin, shopType, clickedFace, true);
                if(shop == null) {
                    return;
                }

                boolean initializedShop = Shop.getPlugin().getShopCreationUtil().initializeShop(shop, player, itemStack, barterItemStack);

                if(initializedShop) {
                    Shop.getPlugin().getShopCreationUtil().sendCreationSuccess(player, shop);
                }
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

    public enum ChatCreationStep {

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
