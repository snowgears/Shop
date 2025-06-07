package com.snowgears.shop.display;

import com.mojang.datafixers.util.Pair;
import com.snowgears.shop.Shop;
import com.snowgears.shop.util.ArmorStandData;
import com.snowgears.shop.util.NMSBullshitHandler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

public class DisplayProtocolLib extends AbstractDisplay {

    NMSBullshitHandler nmsHelper;

    public DisplayProtocolLib(Location shopSignLocation) {
        super(shopSignLocation);
        nmsHelper = Shop.getPlugin().getNmsBullshitHandler();

        Shop.getPlugin().getShopLogger().info("Using item display handler - com.snowgears.shop.display.DisplayProtocolLib");
    }

    @Override
    protected void spawnItemPacket(Player player, ItemStack is, Location location) {
        ClientSideEntity itemDisplay = new ClientSideEntity(location, is);
        itemDisplay.spawn(player);
        this.addEntityID(player, itemDisplay.getEntityId());
    }

    @Override
    protected void spawnArmorStandPacket(Player player, ArmorStandData armorStandData, String text) {
        boolean hasText = (text != null && ChatColor.stripColor(text).length() > 0);
        boolean hasEquipment = armorStandData.getEquipment() != null;
        
        Location location = armorStandData.getLocation();
        ServerLevel mcServerLevel = nmsHelper.getMCServerLevel(location);

        ArmorStand armorStand = new ArmorStand(mcServerLevel, location.getX(), location.getY(), location.getZ());
        armorStand.setYRot((float)(armorStandData.getYaw()));

        // Just in case overwrite name of "Armor Stand" to a space
        armorStand.setCustomName(nmsHelper.getFormattedChatMessage(" "));
        // Default to not show name
        armorStand.setCustomNameVisible(false);

        if (hasText) {
            // Set name to display text
            armorStand.setCustomName(nmsHelper.getFormattedChatMessage(text));
            // Show name since there is text
            armorStand.setCustomNameVisible(true);
            this.addDisplayTag(player, armorStand.getId());
        } else {
            this.addEntityID(player, armorStand.getId());
        }

        if(armorStandData.getRightArmPose() != null){
            EulerAngle angle = armorStandData.getRightArmPose(); //EulerAngles are in radians
            float x = (float)Math.toDegrees(angle.getX());
            float y = (float)Math.toDegrees(angle.getY());
            float z = (float)Math.toDegrees(angle.getZ());
            armorStand.setRightArmPose(new Rotations(x, y, z));
        }
        //armorStand.setHeadPose(new Rotations(0.0F, 0.0F, 0.0F));
        armorStand.setMarker(true);
        armorStand.setNoGravity(true);
        armorStand.setInvulnerable(true);
        armorStand.setInvisible(true);
        armorStand.persist = true;
        armorStand.collides = false;

        if(armorStandData.isSmall()) {
            armorStand.setSmall(true);
        }

        Shop.getPlugin().getShopLogger().log(java.util.logging.Level.FINE, "Floating Tag Label Location: " + location);

        ClientboundAddEntityPacket spawnEntityLivingPacket = new ClientboundAddEntityPacket(armorStand.getId(), armorStand.getUUID(), location.getX(), location.getY(), location.getZ(), armorStand.getXRot(), armorStand.getYRot(), armorStand.getType(), 0, armorStand.getDeltaMovement(), armorStand.getYHeadRot());
        ClientboundSetEntityDataPacket spawnEntityMetadataPacket = new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().packDirty());
        ClientboundSetEquipmentPacket spawnEntityEquipmentPacket = null;

        //armor stand only going to have equipment if text is not populated
        if(text == null){
            ArrayList equipmentList = new ArrayList();
            net.minecraft.world.item.ItemStack itemStack = nmsHelper.getMCItemStack(armorStandData.getEquipment());
            equipmentList.add(new Pair(getMojangEquipmentSlot(armorStandData.getEquipmentSlot()), itemStack));

            spawnEntityEquipmentPacket = new ClientboundSetEquipmentPacket(armorStand.getId(), equipmentList);
        }

        sendPacket(player, spawnEntityLivingPacket);
        sendPacket(player, spawnEntityMetadataPacket);
        if(spawnEntityEquipmentPacket != null){
            sendPacket(player, spawnEntityEquipmentPacket);
        }
    }

    @Override
    protected void spawnItemFramePacket(Player player, ItemStack is, Location location, BlockFace facing, boolean isGlowing){
        ClientSideEntity itemFrame = ClientSideEntity.createItemFrame(location, is, facing, isGlowing);
        itemFrame.spawn(player);
        this.addEntityID(player, itemFrame.getEntityId());
    }

    private void sendPacket(Player player, Packet packet){
        if (player != null) {
            if(isSameWorld(player)) {
                ServerPlayerConnection connection = (ServerPlayerConnection) Shop.getPlugin().getShopHandler().getCachedPlayerConnection(player);
                if (connection != null) {
                    connection.send(packet); //sendPacket()
                    //System.out.println("Sending player a packet: "+packet.getClass().toString());
                }
            }
        }
        else {
            for (Player onlinePlayer : this.shopSignLocation.getWorld().getPlayers()) {
                ServerPlayerConnection connection = (ServerPlayerConnection) Shop.getPlugin().getShopHandler().getCachedPlayerConnection(onlinePlayer);
                if(connection != null) {
                    connection.send(packet); //sendPacket
                }
            }
        }
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
        
        var destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        destroyPacket.getIntLists().write(0, entitiesToRemove);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, destroyPacket);

        this.displayTagEntityIDs.remove(player.getUniqueId());
        if(!onlyDisplayTags) { this.entityIDs.remove(player.getUniqueId()); }
    }

    private net.minecraft.world.entity.EquipmentSlot getMojangEquipmentSlot(EquipmentSlot equipmentSlot){
        switch(equipmentSlot){
            case HAND:
                return net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case OFF_HAND:
                return net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            case FEET:
                return net.minecraft.world.entity.EquipmentSlot.FEET;
            case LEGS:
                return net.minecraft.world.entity.EquipmentSlot.LEGS;
            case CHEST:
                return net.minecraft.world.entity.EquipmentSlot.CHEST;
            default:
                return net.minecraft.world.entity.EquipmentSlot.HEAD;
        }
    }

    @Override
    public String getItemNameNMS(ItemStack item) {
        net.minecraft.world.item.ItemStack itemStack = nmsHelper.getMCItemStack(item);
        return itemStack.getItem().getName(itemStack).getString();
    }
}
