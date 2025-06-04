package com.snowgears.shop.display;

import com.mojang.datafixers.util.Pair;
import com.snowgears.shop.Shop;
import com.snowgears.shop.util.ArmorStandData;
import com.snowgears.shop.util.NMSBullshitHandler;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.Iterator;

public class Display extends AbstractDisplay {

    NMSBullshitHandler nmsHelper;

    public Display(Location shopSignLocation) {
        super(shopSignLocation);
        nmsHelper = Shop.getPlugin().getNmsBullshitHandler();
    }

    @Override
    protected void spawnItemPacket(Player player, ItemStack is, Location location) {
        net.minecraft.world.item.ItemStack itemStack = nmsHelper.getMCItemStack(is);
        Level serverLevel = nmsHelper.getMCLevel(location);

        ItemEntity entityItem = new ItemEntity(serverLevel, location.getX(), location.getY(), location.getZ(), itemStack);
        int entityID = entityItem.getId();
        this.addEntityID(player, entityID);
        entityItem.setInvulnerable(true);
        entityItem.setRemainingFireTicks(-1);
        entityItem.setNoGravity(true);
        entityItem.persist = true;
        entityItem.setDeltaMovement(new Vec3(0.0D, 0.0D, 0.0D)); //setDeltaMovements() //not sure if this is the same as setMot() that was there first
        entityItem.setPickUpDelay(32767);
        entityItem.setTicksFrozen(2147483647);

        Shop.getPlugin().getShopLogger().log(java.util.logging.Level.FINE, "Item Location: " + location);

        ClientboundRemoveEntitiesPacket entityDestroyPacket = new ClientboundRemoveEntitiesPacket(entityID);
        ClientboundAddEntityPacket entitySpawnPacket = new ClientboundAddEntityPacket(entityItem.getId(), entityItem.getUUID(), location.getX(), location.getY(), location.getZ(), entityItem.getXRot(), entityItem.getYRot(), entityItem.getType(), 0, entityItem.getDeltaMovement(), entityItem.getYHeadRot());
        ClientboundSetEntityMotionPacket entityVelocityPacket = new ClientboundSetEntityMotionPacket(entityItem);
        ClientboundSetEntityDataPacket entityMetadataPacket = new ClientboundSetEntityDataPacket(entityID, entityItem.getEntityData().packDirty());

        sendPacket(player, entityDestroyPacket);
        sendPacket(player, entitySpawnPacket);
        sendPacket(player, entityVelocityPacket);
        sendPacket(player, entityMetadataPacket);
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
        ServerLevel worldServer = nmsHelper.getMCServerLevel(location);
        BlockPos blockPosition = new BlockPos((int) location.getX(), (int) location.getY(), (int) location.getZ());
        ItemFrame itemFrame;

        if (isGlowing) {
            itemFrame = new GlowItemFrame(worldServer, blockPosition, getMojangDirection(facing));
        } else {
            itemFrame = new ItemFrame(worldServer, blockPosition, getMojangDirection(facing));
        }

        int entityID = itemFrame.getId();
        this.addEntityID(player, entityID);
        itemFrame.setPos(location.getX(), location.getY(), location.getZ());

        net.minecraft.world.item.ItemStack itemStack = nmsHelper.getMCItemStack(is);

        itemFrame.setItem(itemStack);
        itemFrame.setDirection(getMojangDirection(facing));

        Shop.getPlugin().getShopLogger().log(java.util.logging.Level.FINE, "ItemFrame Location: " + location);

        ClientboundAddEntityPacket entitySpawnPacket = new ClientboundAddEntityPacket(itemFrame.getId(), itemFrame.getUUID(), location.getX(), location.getY(), location.getZ(), itemFrame.getXRot(), itemFrame.getYRot(), itemFrame.getType(), itemFrame.getDirection().get3DDataValue(), itemFrame.getDeltaMovement(), itemFrame.getYHeadRot());
        ClientboundSetEntityDataPacket entityMetadataPacket = new ClientboundSetEntityDataPacket(entityID, itemFrame.getEntityData().packDirty());

        sendPacket(player, entitySpawnPacket);
        sendPacket(player, entityMetadataPacket);
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

    @Override
    public void removeDisplayEntities(Player player, boolean onlyDisplayTags) {
        Iterator<Integer> entityIterator = this.getDisplayEntityIDIterator(player, onlyDisplayTags);
        if(entityIterator == null)
            return;

        while(entityIterator.hasNext()) {
            int displayEntityID = entityIterator.next();
            ClientboundRemoveEntitiesPacket destroyEntityPacket;
            try{
                destroyEntityPacket = new ClientboundRemoveEntitiesPacket(displayEntityID);
            } catch(NoSuchMethodError e){
                throw new RuntimeException(e);
            }
            sendPacket(player, destroyEntityPacket);
            entityIterator.remove();
        }
        if(onlyDisplayTags) {
            if(player != null && displayTagEntityIDs != null)
                displayTagEntityIDs.remove(player.getUniqueId());
        }
    }

    private Direction getMojangDirection(BlockFace facing){
        switch (facing){
            case NORTH:
                return Direction.NORTH;
            case SOUTH:
                return Direction.SOUTH;
            case WEST:
                return Direction.WEST;
            case EAST:
                return Direction.EAST;
            case DOWN:
                return Direction.DOWN;
            default:
                return Direction.UP;
        }
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
