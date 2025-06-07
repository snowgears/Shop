package com.snowgears.shop.display;

import com.mojang.datafixers.util.Pair;
import com.snowgears.shop.Shop;
import com.snowgears.shop.util.ArmorStandData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
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
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

public class Display_v1_18_R2 extends AbstractDisplay {

    public Display_v1_18_R2(Location shopSignLocation) {
        super(shopSignLocation);
        //printDirs();
    }

    @Override
    protected void spawnItemPacket(Player player, ItemStack is, Location location) {
        net.minecraft.world.item.ItemStack itemStack;
        try {
            Method asNMSCopy = Shop.getPlugin().getNmsBullshitHandler().getCraftItemStackClass().getMethod("asNMSCopy", ItemStack.class);
            if (asNMSCopy != null) {
                itemStack = (net.minecraft.world.item.ItemStack) asNMSCopy.invoke(asNMSCopy.getClass(), is);
                if (itemStack != null) {
                    Object craftWorld = Shop.getPlugin().getNmsBullshitHandler().getCraftWorldClass().cast(location.getWorld());
                    if (craftWorld != null) {
                        Method serverWorld = craftWorld.getClass().getMethod("getHandle");
                        if (serverWorld != null) {
                            ItemEntity entityItem = new ItemEntity((Level) serverWorld.invoke(craftWorld), location.getX(), location.getY(), location.getZ(), itemStack);
                            int entityID = entityItem.getId();
                            this.addEntityID(player, entityID);
                            entityItem.setInvulnerable(true);
                            entityItem.setRemainingFireTicks(-1);
                            entityItem.setNoGravity(true);
                            entityItem.persist = true;
                            entityItem.setDeltaMovement(new Vec3(0.0D, 0.0D, 0.0D)); //setDeltaMovements() //not sure if this is the same as setMot() that was there first
                            entityItem.setPickUpDelay(32767);
                            entityItem.setTicksFrozen(2147483647);

                            ClientboundRemoveEntitiesPacket entityDestroyPacket = new ClientboundRemoveEntitiesPacket(entityID);
                            ClientboundAddEntityPacket entitySpawnPacket = new ClientboundAddEntityPacket(entityItem);
                            ClientboundSetEntityMotionPacket entityVelocityPacket = new ClientboundSetEntityMotionPacket(entityItem);
                            ClientboundSetEntityDataPacket entityMetadataPacket = new ClientboundSetEntityDataPacket(entityID, entityItem.getEntityData(), true); //entityItem.ai() = entityItem.getDataWatcher()

                            sendPacket(player, entityDestroyPacket);
                            sendPacket(player, entitySpawnPacket);
                            sendPacket(player, entityVelocityPacket);
                            sendPacket(player, entityMetadataPacket);
                        }
                    }
                }
            }
        } catch (NoSuchMethodException e){
            e.printStackTrace();
        } catch(InvocationTargetException e){
            e.printStackTrace();
        } catch(IllegalAccessException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void spawnArmorStandPacket(Player player, ArmorStandData armorStandData, String text) {

        Location location = armorStandData.getLocation();
        ServerLevel worldServer = ((CraftWorld) location.getWorld()).getHandle();

        ArmorStand armorStand = new ArmorStand(worldServer, location.getX(), location.getY(), location.getZ());
        armorStand.setYRot((float)(armorStandData.getYaw()));

        if(text != null) {
            armorStand.setCustomName(Component.Serializer.fromJson("{\"text\": \"" + text + "\"}"));
            this.addDisplayTag(player, armorStand.getId());
        }
        else{
            this.addEntityID(player, armorStand.getId());
        }
        armorStand.setCustomNameVisible(text != null);

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

        ClientboundAddMobPacket spawnEntityLivingPacket = new ClientboundAddMobPacket(armorStand);
        ClientboundSetEntityDataPacket spawnEntityMetadataPacket = new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData(), true);
        ClientboundSetEquipmentPacket spawnEntityEquipmentPacket = null;

        //armor stand only going to have equipment if text is not populated
        if(text == null){
            ArrayList equipmentList = new ArrayList();
            equipmentList.add(new Pair(getMojangEquipmentSlot(armorStandData.getEquipmentSlot()), CraftItemStack.asNMSCopy(armorStandData.getEquipment())));

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
        ServerLevel worldServer = ((CraftWorld) location.getWorld()).getHandle();

        BlockPos blockPosition = new BlockPos(location.getX(), location.getY(), location.getZ());

        ItemFrame itemFrame;

        if(isGlowing){
            itemFrame = new GlowItemFrame(worldServer, blockPosition, getMojangDirection(facing));
        }
        else{
            itemFrame = new ItemFrame(worldServer, blockPosition, getMojangDirection(facing));
        }

        int entityID = itemFrame.getId();
        this.addEntityID(player, entityID);
        itemFrame.setPos(location.getX(), location.getY(), location.getZ());
        itemFrame.setItem(CraftItemStack.asNMSCopy(is));
        itemFrame.setDirection(getMojangDirection(facing));

        ClientboundAddEntityPacket entitySpawnPacket = new ClientboundAddEntityPacket(itemFrame, itemFrame.getDirection().get3DDataValue());
        ClientboundSetEntityDataPacket entityMetadataPacket = new ClientboundSetEntityDataPacket(entityID, itemFrame.getEntityData(), true);

        sendPacket(player, entitySpawnPacket);
        sendPacket(player, entityMetadataPacket);

    }

    private void sendPacket(Player player, Packet packet){
        if (player != null) {
            if(isSameWorld(player)) {
                ServerPlayerConnection connection = getPlayerConnection(player);
                if (connection != null) {
                    connection.send(packet); //sendPacket()
                    //System.out.println("Sending player a packet: "+packet.getClass().toString());
                }
            }
        }
        else {
            for (Player onlinePlayer : this.shopSignLocation.getWorld().getPlayers()) {
                ServerPlayerConnection connection = getPlayerConnection(onlinePlayer);
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

    private ServerPlayerConnection getPlayerConnection(Player player) {
        try {
            Object craftPlayer = Shop.getPlugin().getNmsBullshitHandler().getCraftPlayerClass().cast(player);
            if (craftPlayer != null) {
                Method getHandle = craftPlayer.getClass().getMethod("getHandle");
                if (getHandle != null) {
                    Object entityPlayer = getHandle.invoke(craftPlayer);
                    if (entityPlayer != null) {
                        Field playerConnection = entityPlayer.getClass().getField("b");
                        if(playerConnection != null)
                        return (ServerPlayerConnection) playerConnection.get(entityPlayer);
                    }
                }
            }
        } catch(NoSuchMethodException e){
            e.printStackTrace();
        } catch(IllegalAccessException e){
            e.printStackTrace();
        } catch(InvocationTargetException e){
            e.printStackTrace();
        } catch (NoSuchFieldException e){
            e.printStackTrace();
        }
        return null;
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
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
        return nmsStack.getItem().getName(nmsStack).getString();
    }
}
