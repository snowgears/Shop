package com.snowgears.shop.display;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.ArmorStandData;
import com.snowgears.shop.util.DisplayUtil;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;


public abstract class AbstractDisplay {

    protected Location shopSignLocation;
    protected DisplayType type;
    protected ArrayList<Integer> entityIDs;
    protected HashMap<UUID, ArrayList<Integer>> displayTagEntityIDs; //player UUID. display tags
    protected int chunkX;
    protected int chunkZ;

    public AbstractDisplay(Location shopSignLocation) {
        this.shopSignLocation = shopSignLocation;
        entityIDs = new ArrayList<>();
        displayTagEntityIDs = new HashMap<>();

        chunkX = UtilMethods.floor(shopSignLocation.getBlockX()) >> 4;
        chunkZ = UtilMethods.floor(shopSignLocation.getBlockZ()) >> 4;
    }

    public boolean isInChunk(Chunk chunk){
        return chunk.getX() == chunkX && chunk.getZ() == chunkZ && chunk.getWorld().toString().equals(shopSignLocation.getWorld().toString());
    }

    public boolean isChunkLoaded(){
        return shopSignLocation.getWorld().isChunkLoaded(this.chunkX, this.chunkZ);
    }

    //spawns a floating item packet for a specific player
    //if player is null, all online players will get the packet
    protected abstract void spawnItemPacket(Player player, ItemStack is, Location location);

    //spawns an armor stand packet for a specific player
    //if player is null, all online players will get the packet
    protected abstract void spawnArmorStandPacket(Player player, ArmorStandData armorStandData, String text);

    //spawns an item frame packet for a specific player
    //if player is null, all online players will get the packet
    protected abstract void spawnItemFramePacket(Player player, ItemStack is, Location location, BlockFace facing, boolean isGlowing);

    public abstract void removeDisplayEntities(Player player, boolean onlyDisplayTags);

    public void spawn(Player player) {
        if(player != null){
            //don't spawn the display if the player is in a different world
            if(!player.getWorld().getUID().equals(this.shopSignLocation.getWorld().getUID()))
                return;
        }
        remove(player);

        AbstractShop shop = this.getShop();

        if (shop.getItemStack() == null || shop.getChestLocation() == null)
            return;

        //define the initial display item
        ItemStack item = shop.getItemStack().clone();
        item.setAmount(1);

        DisplayType displayType = this.type;
        if(displayType == null)
            displayType = Shop.getPlugin().getDisplayType();

        //two display entities on the chest
        if (shop.getSecondaryItemStack() != null) {

            //define the barter display item
            ItemStack barterItem = shop.getSecondaryItemStack().clone();
            barterItem.setAmount(1);

            switch (displayType){
                case NONE:
                    //do nothing
                    break;
                case ITEM:
                    //drop first item on left
                    spawnItemPacket(player, item, this.getItemDropLocation(false));

                    //drop second item on right
                    spawnItemPacket(player, barterItem, this.getItemDropLocation(true));
                    break;
                case LARGE_ITEM:
                    //put first large display down
                    Location leftLoc = shop.getChestLocation().clone().add(0,1,0);
                    leftLoc.add(getLargeItemBarterOffset(false));
                    ArmorStandData armorStandData = DisplayUtil.getArmorStandData(item, leftLoc, shop.getFacing(), false);
                    spawnArmorStandPacket(player, armorStandData, null);

                    //put second large display down
                    Location rightLoc = shop.getChestLocation().clone().add(0,1,0);
                    rightLoc.add(getLargeItemBarterOffset(true));
                    ArmorStandData armorStandData2 = DisplayUtil.getArmorStandData(barterItem, rightLoc, shop.getFacing(), false);
                    spawnArmorStandPacket(player, armorStandData2, null);
                    break;
                case GLASS_CASE:
                    //put the extra large glass casing down
                    Location caseLoc = shop.getChestLocation().clone().add(0,1,0);
                    ArmorStandData caseStandData = DisplayUtil.getArmorStandData(new ItemStack(Material.GLASS), caseLoc, shop.getFacing(), true);
                    spawnArmorStandPacket(null, caseStandData, null);

                    //Drop initial display item
                    spawnItemPacket(player, item, this.getItemDropLocation(false));

                    //Drop the barter display item
                    spawnItemPacket(player, barterItem, this.getItemDropLocation(true));
                    break;
            }
        }
        //one display entity on the chest
        else {
            switch (displayType){
                case NONE:
                    //do nothing
                    break;
                case ITEM:
                    spawnItemPacket(player, item, this.getItemDropLocation(false));
                    break;
                case LARGE_ITEM:
                    ArmorStandData armorStandData = DisplayUtil.getArmorStandData(item, shop.getChestLocation().clone().add(0,1,0), shop.getFacing(), false);
                    spawnArmorStandPacket(player, armorStandData, null);
                    break;
                case GLASS_CASE:
                    //put the extra large glass casing down
                    Location caseLoc = shop.getChestLocation().clone().add(0,1,0);
                    ArmorStandData caseStandData = DisplayUtil.getArmorStandData(new ItemStack(Material.GLASS), caseLoc, shop.getFacing(), true);
                    spawnArmorStandPacket(player, caseStandData, null);

                    //drop the display item in the glass case
                    spawnItemPacket(player, item, this.getItemDropLocation(false));
                    break;
                case ITEM_FRAME:
                    Location frameLocation;
                    //only calculate the item frame location if the shop is in a loaded chunk (because Block is used)
                    if(this.isChunkLoaded()) {
                        Block aboveShop = shop.getChestLocation().getBlock().getRelative(BlockFace.UP);
                        frameLocation = aboveShop.getLocation();
                        //if display is blocked, put item frame on front
                        if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                            frameLocation = aboveShop.getRelative(shop.getFacing()).getLocation();
                        }
                    }
                    else{
                        frameLocation = shop.getChestLocation().clone().add(0,1,0);
                    }

                    spawnItemFramePacket(player, shop.getItemStack(), frameLocation, shop.getFacing(), false);
                    break;
            }
        }
        //shop.updateSign();
    }

    public void showDisplayTags(Player player){
        if(displayTagsVisible(player) || !getShop().isInitialized() || Shop.getPlugin().getDisplayTagOption() == DisplayTagOption.NONE || getShop().getFacing() == null) {
            return;
        }

        try {
            ArrayList<String> displayTags = ShopMessage.getDisplayTags(getShop(), getShop().getType());

            Location lowerTagLocation = getShop().getChestLocation().clone().add(0,1,0);
            lowerTagLocation = lowerTagLocation.add(0.5, 0.5, 0.5);

            //push the tag slightly closer to the front of the shop so it doesnt collide with the display and hide the text
            lowerTagLocation = UtilMethods.pushLocationInDirection(lowerTagLocation, this.getShop().getFacing(), 0.2);

            double verticalAddition = 0;
            //iterate through list backwards to build from bottom -> up
            for (int i = displayTags.size() - 1; i >= 0; i--) {
                Location asTagLocation = lowerTagLocation.clone();

                String tagLine = displayTags.get(i);
                if (tagLine.contains("[lshift]")) {
                    asTagLocation = asTagLocation.add(getLargeItemBarterOffset(false));
                    asTagLocation = asTagLocation.add(getLargeItemBarterOffset(false));
                    tagLine = tagLine.replace("[lshift]", "");
                }
                if (tagLine.contains("[rshift]")) {
                    asTagLocation = asTagLocation.add(getLargeItemBarterOffset(true));
                    asTagLocation = asTagLocation.add(getLargeItemBarterOffset(true));
                    tagLine = tagLine.replace("[rshift]", "");
                }

                asTagLocation = asTagLocation.add(0, verticalAddition, 0);
                createTagEntity(player, tagLine, asTagLocation);
                verticalAddition += 0.3;
            }

            //this handles getting rid of the display tags after a configured amount of time after the player looks away from the shop sign
            kickoffViewSignTask(player);
        } catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    private void createTagEntity(Player player, String text, Location location){
        ArmorStandData caseStandData = new ArmorStandData();
        caseStandData.setSmall(false);
        caseStandData.setLocation(location);

        spawnArmorStandPacket(player, caseStandData, text);
    }

    public DisplayType getType(){
        return type;
    }

    public AbstractShop getShop(){
        return Shop.getPlugin().getShopHandler().getShop(this.shopSignLocation);
    }

    public void setType(DisplayType type, boolean checkDisplayBlock){
        DisplayType oldType = this.type;

        if(checkDisplayBlock && getShop().getChestLocation() != null) {
            if ((oldType == DisplayType.NONE && type != DisplayType.ITEM_FRAME) || (oldType == DisplayType.ITEM_FRAME && type != DisplayType.NONE)) {
                if(this.isChunkLoaded()) {
                    //make sure there is room above the shop for the display
                    Block aboveShop = this.getShop().getChestLocation().getBlock().getRelative(BlockFace.UP);
                    if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                        return;
                    }
                }
            }
        }

        this.type = type;
    }

    public void cycleType(){
        if(getShop().getFacing() == null)
            return;
        DisplayType[] cycle = Shop.getPlugin().getDisplayCycle();
        DisplayType displayType = this.type;
        if(displayType == null) {
            displayType = Shop.getPlugin().getDisplayType();
        }

        int index = -1;
        if(displayType == DisplayType.NONE){
            //make sure there is room above the shop for the display
            Block aboveShop = this.getShop().getChestLocation().getBlock().getRelative(BlockFace.UP);
            if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                //if the cycle contains the ITEM_FRAME display type
                for(int i=0; i<cycle.length; i++){
                    if(cycle[i] == DisplayType.ITEM_FRAME){
                        index = i;
                    }
                }
                //there is no ITEM_FRAME in cycle, return because display is blocked
                if(index == -1)
                    return;
            }
        }
        else if(displayType == DisplayType.ITEM_FRAME){
            //make sure there is room above the shop for the display
            Block aboveShop = this.getShop().getChestLocation().getBlock().getRelative(BlockFace.UP);
            if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                //if the cycle contains the NONE display type
                for(int i=0; i<cycle.length; i++){
                    if(cycle[i] == DisplayType.NONE){
                        index = i;
                    }
                }
                //there is no NONE in cycle, return because display is blocked
                if(index == -1)
                    return;
            }
        }

        //index is still not set, continue and cycle index to next display type
        if(index == -1) {
            index = 0;
            for (int i = 0; i < cycle.length; i++) {
                if (cycle[i] == displayType)
                    index = i + 1;
            }
            if (index >= cycle.length)
                index = 0;
        }

        //don't allow barter shops to have ITEM_FRAME display types (for NOW)
        if(cycle[index] == DisplayType.ITEM_FRAME){

            boolean skip = false;
            if(getShop().getType() == ShopType.BARTER){
                skip = true;
            }
            else {
                //calculate where ITEM_FRAME display may be
                for(Entity e : this.getShop().getChestLocation().getWorld().getNearbyEntities(this.getItemDropLocation(false), 1, 1, 1)){
                    if(e.getType() == EntityType.ITEM_FRAME){
                        ItemFrame i = (ItemFrame)e;
                        if(i.getAttachedFace() == getShop().getSign().getFacing().getOppositeFace()) {
                            skip = true;
                            break;
                        }
                    }
                }
            }

            if(skip) {
                index++;
                if (index >= cycle.length)
                    index = 0;
            }
        }

        this.setType(cycle[index], true);
        this.spawn(null);
        getShop().updateSign();

        Shop.getPlugin().getShopHandler().saveShops(getShop().getOwnerUUID());
    }

    public void remove(Player player) {
        removeDisplayEntities(player, false);
        removeDisplayEntities(player, true);

        //if(player == null)
        //    entityIDs.clear();
        //if(displayTagEntityIDs != null) {
        //    if(player == null)
        //        displayTagEntityIDs.clear();
        //}
    }

    private Location getItemDropLocation(boolean isBarterItem) {
        AbstractShop shop = this.getShop();

        if(shop.getFacing() == null)
            return null;

        //calculate which x,z to drop items at depending on direction of the shop sign
        double dropY = 0.9;
        double dropX = 0.5;
        double dropZ = 0.5;
        if (shop.getType() == ShopType.BARTER) {
            switch (shop.getFacing()) {
                case NORTH:
                    if (isBarterItem)
                        dropX = 0.3;
                    else
                        dropX = 0.7;
                    break;
                case EAST:
                    if (isBarterItem)
                        dropZ = 0.3;
                    else
                        dropZ = 0.7;
                    break;
                case SOUTH:
                    if (isBarterItem)
                        dropX = 0.7;
                    else
                        dropX = 0.3;
                    break;
                case WEST:
                    if (isBarterItem)
                        dropZ = 0.7;
                    else
                        dropZ = 0.3;
                    break;
                default:
                    dropX = 0.5;
                    dropZ = 0.5;
                    break;
            }
        }
        return shop.getChestLocation().clone().add(dropX, dropY, dropZ);
    }

    private Vector getLargeItemBarterOffset(boolean isBarterItem){
        AbstractShop shop = this.getShop();

        Vector offset = new Vector(0,0,0);
        double space = 0.24;
        if (shop.getType() == ShopType.BARTER) {
            switch (shop.getFacing()) {
                case NORTH:
                    if (isBarterItem)
                        offset.setX(-space);
                    else
                        offset.setX(space);
                    break;
                case EAST:
                    if (isBarterItem)
                        offset.setZ(-space);
                    else
                        offset.setZ(space);
                    break;
                case SOUTH:
                    if (isBarterItem)
                        offset.setX(space);
                    else
                        offset.setX(-space);
                    break;
                case WEST:
                    if (isBarterItem)
                        offset.setZ(space);
                    else
                        offset.setZ(-space);
                    break;
            }
        }
        return offset;
    }

    protected boolean playerIsViewingSign(Player player) {
        Block block = player.getTargetBlock((Set<Material>)null, 8);
        if (block.getType() == Material.WALL_SIGN) {
            AbstractShop shopObj = Shop.getPlugin().getShopHandler().getShop(block.getLocation());
            if (shopObj != null) {
                AbstractShop ownShopObj = Shop.getPlugin().getShopHandler().getShop(this.shopSignLocation);
                if (ownShopObj != null) {
                    if (block.getLocation().equals(this.shopSignLocation))
                        return true;
                }
            }
        }
        return false;
    }

    protected void kickoffViewSignTask(Player player) {
        //remove all armor stand name tag entities after x seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if(Shop.getPlugin().getDisplayTagOption() == DisplayTagOption.VIEW_SIGN) {
                    if (playerIsViewingSign(player))
                        kickoffViewSignTask(player);
                    else
                        removeDisplayEntities(player, true);
                }
                else{
                    removeDisplayEntities(player, true);
                }
            }
        }.runTaskLater(Shop.getPlugin(), (Shop.getPlugin().getDisplayTagLifespan() * 20));
    }

    protected boolean displayTagsVisible(Player player){
        if(player == null || displayTagEntityIDs == null)
            return false;
        ArrayList<Integer> entityTagIDs = displayTagEntityIDs.get(player.getUniqueId());
        if(entityTagIDs == null || entityTagIDs.isEmpty())
            return false;
        return true;
    }

    protected void addDisplayTag(Player player, int displayTagID){
        if(player == null)
            return;
        ArrayList<Integer> tagIDs = displayTagEntityIDs.get(player.getUniqueId());
        if(tagIDs == null){
            tagIDs = new ArrayList<>();
        }
        tagIDs.add(displayTagID);
        displayTagEntityIDs.put(player.getUniqueId(), tagIDs);
    }

    protected ArrayList<Integer> getAllDisplayTagEntityIDs(){
        ArrayList<Integer> allDisplayTagEntityIDs = new ArrayList<>();
        if(displayTagEntityIDs != null) {
            for (Map.Entry<UUID, ArrayList<Integer>> entry : displayTagEntityIDs.entrySet()) {
                allDisplayTagEntityIDs.addAll(entry.getValue());
            }
        }
        return allDisplayTagEntityIDs;
    }

    protected Iterator<Integer> getDisplayEntityIDIterator(Player player, boolean onlyDisplayTags){
        Iterator<Integer> entityIterator;
        if(onlyDisplayTags){
            if(player == null){
                entityIterator = getAllDisplayTagEntityIDs().iterator();
            }
            else{
                if(!this.displayTagsVisible(player))
                    return null;
                entityIterator = this.displayTagEntityIDs.get(player.getUniqueId()).iterator();
            }
        }
        else {
            entityIterator = this.entityIDs.iterator();
        }
        return entityIterator;
    }

    protected boolean isSameWorld(Player player){
        return player.getWorld().getUID().equals(this.shopSignLocation.getWorld().getUID());
    }

}