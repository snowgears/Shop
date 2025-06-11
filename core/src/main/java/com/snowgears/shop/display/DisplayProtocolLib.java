package com.snowgears.shop.display;

import com.snowgears.shop.Shop;
import com.snowgears.shop.util.ArmorStandData;
import com.snowgears.shop.util.NMSBullshitHandler;
import com.snowgears.shop.util.MCVersion;

import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DisplayProtocolLib extends AbstractDisplay {

    NMSBullshitHandler nmsHelper;

    public DisplayProtocolLib(Location shopSignLocation) {
        super(shopSignLocation);
        nmsHelper = Shop.getPlugin().getNmsBullshitHandler();

        Shop.getPlugin().getShopLogger().info("Using item display handler - com.snowgears.shop.display.DisplayProtocolLib");
    }

    @Override
    protected void spawnItemPacket(Player player, ItemStack is, Location location) {
        try {
            ClientSideEntity itemDisplay = new ClientSideEntity(location, is);
            itemDisplay.spawn(player);
            this.addEntityID(player, itemDisplay.getEntityId());
        } catch (Exception e) {
            Shop.getPlugin().getShopLogger().warning("Error while spawning item packet: " + e.getMessage());
        }
    }

    @Override
    protected void spawnArmorStandPacket(Player player, ArmorStandData armorStandData, String text) {
        if (MCVersion.atLeast("1.19.4")) {
            ClientSideEntity textDisplay = ClientSideEntity.createTextDisplay(
                armorStandData.getLocation(), 
                text,
                (float) armorStandData.getYaw()
            );
            textDisplay.spawn(player);
            this.addDisplayTag(player, textDisplay.getEntityId());
            return;
        }

        ClientSideEntity armorStand = ClientSideEntity.createArmorStand(
            armorStandData.getLocation(), 
            armorStandData, 
            text
        );
        armorStand.spawn(player);
        
        // Track entity appropriately
        if (text != null && ChatColor.stripColor(text).length() > 0) {
            this.addDisplayTag(player, armorStand.getEntityId());
        } else {
            this.addEntityID(player, armorStand.getEntityId());
        }   
    }

    @Override
    protected void spawnItemFramePacket(Player player, ItemStack is, Location location, BlockFace facing, boolean isGlowing){
        ClientSideEntity itemFrame = ClientSideEntity.createItemFrame(location, is, facing, isGlowing);
        itemFrame.spawn(player);
        this.addEntityID(player, itemFrame.getEntityId());
    }

    private ArrayList<Integer> getEntitiesToRemove(Player player, boolean onlyDisplayTags) {
        ArrayList<Integer> entityIDs = this.entityIDs.get(player.getUniqueId());
        ArrayList<Integer> displayEntityIDs = this.displayTagEntityIDs.get(player.getUniqueId());

        ArrayList<Integer> entitiesToRemove = new ArrayList<>();
        if (displayEntityIDs != null) { entitiesToRemove.addAll(displayEntityIDs); }
        if (!onlyDisplayTags && entityIDs != null) { entitiesToRemove.addAll(entityIDs); } 

        return entitiesToRemove;
    }

    @Override
    public void removeDisplayEntities(Player player, boolean onlyDisplayTags) {
        ArrayList<Integer> entitiesToRemove = getEntitiesToRemove(player, onlyDisplayTags);
        if (entitiesToRemove.isEmpty()) return;
        
        ClientSideEntity.destroyEntities(player, entitiesToRemove);

        this.displayTagEntityIDs.remove(player.getUniqueId());
        if(!onlyDisplayTags) { this.entityIDs.remove(player.getUniqueId()); }
    }
}
