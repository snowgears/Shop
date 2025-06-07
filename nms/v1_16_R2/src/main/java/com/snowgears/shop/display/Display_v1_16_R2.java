package com.snowgears.shop.display;

import com.mojang.datafixers.util.Pair;
import com.snowgears.shop.util.ArmorStandData;
import net.minecraft.server.v1_16_R2.*;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.Iterator;

public class Display_v1_16_R2 extends AbstractDisplay {

    public Display_v1_16_R2(Location shopSignLocation) {
        super(shopSignLocation);
    }

    @Override
    protected void spawnItemPacket(Player player, ItemStack is, Location location) {

        net.minecraft.server.v1_16_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(is);
        EntityItem entityItem = new EntityItem(((CraftWorld)location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ(), nmsItemStack);
        int entityID = entityItem.getId();
        this.addEntityID(player, entityID);

        entityItem.setItemStack(nmsItemStack);
        entityItem.setInvulnerable(true);
        entityItem.setFireTicks(-1);
        entityItem.setNoGravity(true);
        entityItem.persist = true;
        entityItem.setMot(new Vec3D(0.0D, 0.0D, 0.0D));
        entityItem.pickupDelay = 2147483647;

        PacketPlayOutEntityDestroy entityDestroyPacket = new PacketPlayOutEntityDestroy(new int[]{entityID});
        PacketPlayOutSpawnEntity entitySpawnPacket = new PacketPlayOutSpawnEntity(entityItem);
        PacketPlayOutEntityVelocity entityVelocityPacket = new PacketPlayOutEntityVelocity(entityItem);
        PacketPlayOutEntityMetadata entityMetadataPacket = new PacketPlayOutEntityMetadata(entityID, entityItem.getDataWatcher(), true);

        sendPacket(player, entityDestroyPacket);
        sendPacket(player, entitySpawnPacket);
        sendPacket(player, entityVelocityPacket);
        sendPacket(player, entityMetadataPacket);

    }

    @Override
    protected void spawnArmorStandPacket(Player player, ArmorStandData armorStandData, String text) {

        Location location = armorStandData.getLocation();
        WorldServer worldServer = ((CraftWorld) location.getWorld()).getHandle();

        EntityArmorStand armorStand = new EntityArmorStand(EntityTypes.ARMOR_STAND, worldServer);
        armorStand.setLocation(location.getX(), location.getY(), location.getZ(), (float)armorStandData.getYaw(), 0);

        if(text != null) {
            armorStand.setCustomName(IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + text + "\"}"));
            this.addDisplayTag(player, armorStand.getId());
        }
        else{
            this.addEntityID(player, armorStand.getId());
        }
        armorStand.setCustomNameVisible(text != null);
        armorStand.setInvisible(true);

        if(armorStandData.getRightArmPose() != null){
            EulerAngle angle = armorStandData.getRightArmPose(); //EulerAngles are in radians
            float x = (float)Math.toDegrees(angle.getX());
            float y = (float)Math.toDegrees(angle.getY());
            float z = (float)Math.toDegrees(angle.getZ());
            armorStand.setRightArmPose(new Vector3f(x, y, z));
        }
        armorStand.setHeadRotation(0.0F);
        armorStand.setHeadPose(new Vector3f(0.0F, 0.0F, 0.0F));
        armorStand.setMarker(true);
        armorStand.setNoGravity(true);
        armorStand.setInvulnerable(true);
        armorStand.setInvisible(true);
        armorStand.persist = true;
        armorStand.collides = false;

        if(armorStandData.isSmall()) {
            armorStand.setSmall(true);
        }

        PacketPlayOutSpawnEntityLiving spawnEntityLivingPacket = new PacketPlayOutSpawnEntityLiving(armorStand);
        PacketPlayOutEntityMetadata spawnEntityMetadataPacket = new PacketPlayOutEntityMetadata(armorStand.getId(), armorStand.getDataWatcher(), true);
        PacketPlayOutEntityEquipment spawnEntityEquipmentPacket = null;

        //armor stand only going to have equipment if text is not populated
        if(text == null){
            ArrayList equipmentList = new ArrayList();
            if(armorStandData.getEquipmentSlot() == EquipmentSlot.HAND){
                equipmentList.add(new Pair(EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(armorStandData.getEquipment())));
            }
            else {
                equipmentList.add(new Pair(EnumItemSlot.valueOf(armorStandData.getEquipmentSlot().toString()), CraftItemStack.asNMSCopy(armorStandData.getEquipment())));
            }
            spawnEntityEquipmentPacket = new PacketPlayOutEntityEquipment(armorStand.getId(), equipmentList);
        }

        sendPacket(player, spawnEntityLivingPacket);
        sendPacket(player, spawnEntityMetadataPacket);
        if(spawnEntityEquipmentPacket != null){
            sendPacket(player, spawnEntityEquipmentPacket);
        }
    }

    @Override
    protected void spawnItemFramePacket(Player player, ItemStack is, Location location, BlockFace facing, boolean isGlowing){

        WorldServer worldServer = ((CraftWorld) location.getWorld()).getHandle();

        EntityItemFrame itemFrame = new EntityItemFrame(EntityTypes.ITEM_FRAME, worldServer);
        int entityID = itemFrame.getId();
        this.addEntityID(player, entityID);
        itemFrame.setLocation(location.getX(), location.getY(), location.getZ(),0f,0f);
        itemFrame.setItem(CraftItemStack.asNMSCopy(is));
        itemFrame.setDirection(EnumDirection.valueOf(facing.toString()));

        PacketPlayOutSpawnEntity entitySpawnPacket = new PacketPlayOutSpawnEntity(itemFrame); //do i need to specify 71 as additional argument for item frame here?
        PacketPlayOutEntityMetadata entityMetadataPacket = new PacketPlayOutEntityMetadata(entityID, itemFrame.getDataWatcher(), true);

        sendPacket(player, entitySpawnPacket);
        sendPacket(player, entityMetadataPacket);

    }

    private void sendPacket(Player player, Packet packet){
        if (player != null) {
            if(isSameWorld(player)) {
                EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
                entityPlayer.playerConnection.sendPacket(packet);
            }
        }
        else {
            for (Player onlinePlayer : this.shopSignLocation.getWorld().getPlayers()) {
                EntityPlayer entityPlayer = ((CraftPlayer) onlinePlayer).getHandle();
                entityPlayer.playerConnection.sendPacket(packet);
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
            PacketPlayOutEntityDestroy destroyEntityPacket = new PacketPlayOutEntityDestroy(new int[]{displayEntityID});
            sendPacket(player, destroyEntityPacket);
            entityIterator.remove();

        }
        if(onlyDisplayTags) {
            if(player != null && displayTagEntityIDs != null)
                displayTagEntityIDs.remove(player.getUniqueId());
        }
    }

    @Override
    public String getItemNameNMS(ItemStack item) {
        net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
        return nmsStack.getItem().f(nmsStack);
    }
}
