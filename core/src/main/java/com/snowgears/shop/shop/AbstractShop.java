package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.AbstractDisplay;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.util.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractShop {

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

    //this calls BlockData which loads the chunk the shop is in by doing so
    public boolean load() {
        if (signLocation != null) {
            try {
                final org.bukkit.material.Sign sign = (org.bukkit.material.Sign) signLocation.getBlock().getState().getData();
                facing = sign.getFacing();
                chestLocation = signLocation.getBlock().getRelative(facing.getOppositeFace()).getLocation();

                //if shop is made out of a container that is no longer enabled, delete it
                if(chestLocation != null) {
                    if (!Shop.getPlugin().getShopHandler().isChest(chestLocation.getBlock())){
                        this.delete();
                        return false;
                    }
                }
                this.updateSign();
                this.setGuiIcon();
                return true;
            } catch (ClassCastException cce) {
                //this shop has no sign on it. return false
                return false;
            }
        } else {
            //this shop has no sign location defined
            return false;
        }
    }

    //abstract methods that must be implemented in each shop subclass

    public abstract TransactionError executeTransaction(Transaction transaction);

    protected int calculateStock() {
        int oldStock = stock;
        if(this.isAdmin) {
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
            float remaining = (float)InventoryUtils.getAmount(this.getInventory(), this.getItemStack()) / (float)this.getAmount();
            if(remaining > 0){
                stock = 1;
            }
        }
        if(stock != oldStock){
            signLinesRequireRefresh = true;
            Shop.getPlugin().getShopHandler().saveShops(getOwnerUUID());
        }
        return stock;
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

    public org.bukkit.material.Sign getSign(){
        if(this.getSignLocation().getBlock().getType() == Material.WALL_SIGN){
            return (org.bukkit.material.Sign) signLocation.getBlock().getState().getData();
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

    public UUID getOwnerUUID() {
        return owner;
    }

    public String getOwnerName() {
        if(this.isAdmin())
            return "admin";
        if (this.getOwner() != null)
            return Bukkit.getOfflinePlayer(this.owner).getName();
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

    public String getPriceString() {
        if(this.type == ShopType.BARTER && this.isInitialized()){
            return (int)this.getPrice() + " " + Shop.getPlugin().getItemNameUtil().getName(this.getSecondaryItemStack());
        }
        return Shop.getPlugin().getPriceString(this.price, false);
    }

    public String getPricePerItemString() {
        double pricePer = this.getPrice() / this.getAmount();
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
        return guiIcon;
    }

    //setter methods

    public void setItemStack(ItemStack is) {
        this.item = is.clone();
        if(!Shop.getPlugin().checkItemDurability()) {
            if (item.getType().getMaxDurability() > 0)
                item.setDurability((short) 0); //set item to full durability
        }
        setGuiIcon();
    }

    public void setSecondaryItemStack(ItemStack is) {
        this.secondaryItem = is.clone();
        if(!Shop.getPlugin().checkItemDurability()) {
            if (secondaryItem.getType().getMaxDurability() > 0)
                secondaryItem.setDurability((short) 0); //set item to full durability
        }
        //this.display.spawn();
        setGuiIcon();
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

    public void setGuiIcon(){

        this.calculateStock();

        //also set marker in here if using a marker integration
        if(Shop.getPlugin().getBluemapHookListener() != null) {
            Shop.getPlugin().getBluemapHookListener().updateMarker(this);
        }

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
            if(loreLine.contains("[barter item]") && this.getType() != ShopType.BARTER) {}
            else {
                lore.add(ShopMessage.formatMessage(loreLine, this, null, false));
            }
        }
        lore.add(ChatColor.GRAY+""+UtilMethods.getCleanLocation(this.signLocation, true));

        ItemMeta iconMeta = guiIcon.getItemMeta();
        iconMeta.setDisplayName(name);
        iconMeta.setLore(lore);

        guiIcon.setItemMeta(iconMeta);
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
        signLines = ShopMessage.getSignLines(this, this.type);

        Shop.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(Shop.getPlugin(), new Runnable() {
            public void run() {

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

                signBlock.update(true);
                signLinesRequireRefresh = false;
            }
        }, 2L);
    }

    public void delete() {
        display.remove(null);

        Block b = this.getSignLocation().getBlock();
        if (b.getType() == Material.WALL_SIGN) {
            Sign signBlock = (Sign) b.getState();
            signBlock.setLine(0, "");
            signBlock.setLine(1, "");
            signBlock.setLine(2, "");
            signBlock.setLine(3, "");
            signBlock.update(true);
        }

        //finally remove the shop from the shop handler
        Shop.getPlugin().getShopHandler().removeShop(this);
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
            if (message != null && !message.isEmpty())
                formatAndSendFancyMessage(message, player);
        }
    }

    public boolean isFakeSign(){
        return fakeSign;
    }

    public void setFakeSign(boolean fakeSign){
        this.fakeSign = fakeSign;
    }

    protected void formatAndSendFancyMessage(String message, Player player){
        if(message == null)
            return;

        String[] parts = message.split("(?=&[0-9A-FK-ORa-fk-or])");
        TextComponent fancyMessage = new TextComponent("");

        for(String part : parts){
            ComponentBuilder builder = new ComponentBuilder("");
            org.bukkit.ChatColor cc = UtilMethods.getChatColor(part);
            if(cc != null)
                part = part.substring(2, part.length());
            boolean barterItem = false;
            if(part.contains("[barter item]"))
                barterItem = true;
            part = ShopMessage.formatMessage(part, this, player, false);
            //part = ChatColor.stripColor(part);
            builder.append(part);
            if(cc != null) {
                builder.color(ChatColor.valueOf(cc.name()));
            }

            if(part.startsWith("[")) {
                String itemJson;
                if (barterItem) {
                    itemJson = ReflectionUtil.convertItemStackToJson(this.secondaryItem);
                } else {
                    itemJson = ReflectionUtil.convertItemStackToJson(this.item);
                }
                // Prepare a BaseComponent array with the itemJson as a text component
                BaseComponent[] hoverEventComponents = new BaseComponent[]{ new TextComponent(itemJson) }; // The only element of the hover events basecomponents is the item json
                HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverEventComponents);

                builder.event(event);
            }

            for(BaseComponent b : builder.create()) {
                fancyMessage.addExtra(b);
            }
        }
        player.spigot().sendMessage(fancyMessage);
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
}
