package com.snowgears.shop.display;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.AbstractDisplay;
import com.snowgears.shop.util.ArmorStandData;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.Iterator;

public class Display_v1_8_R3 extends AbstractDisplay {

    public Display_v1_8_R3(Location shopSignLocation) {
        super(shopSignLocation);
    }

    @Override
    protected void spawnItemPacket(Player player, ItemStack is, Location location) {

        net.minecraft.server.v1_8_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(is);
        EntityItem entityItem = new EntityItem(((CraftWorld)location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ(), nmsItemStack);
        int entityID = entityItem.getId();
        this.addEntityID(player, entityID);

        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        nbtTagCompound.setBoolean("Marker", true);
        nbtTagCompound.setBoolean("PersistenceRequired", true);
        nbtTagCompound.setBoolean("NoGravity", true);
        nbtTagCompound.setBoolean("Gravity", false);
        nbtTagCompound.setBoolean("Invulnerable", true);

        entityItem.setItemStack(nmsItemStack);
        entityItem.setOnFire(-1);

        entityItem.c(nbtTagCompound);
        entityItem.f(nbtTagCompound);

        entityItem.motX = 0.0D;
        entityItem.motY = 0.0D;
        entityItem.motZ = 0.0D;
        entityItem.pickupDelay = 2147483647;

        PacketPlayOutEntityDestroy entityDestroyPacket = new PacketPlayOutEntityDestroy(new int[]{entityID});
        PacketPlayOutSpawnEntity entitySpawnPacket = new PacketPlayOutSpawnEntity(entityItem, 2, 1);
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

        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        nbtTagCompound.setBoolean("Marker", true);
        nbtTagCompound.setBoolean("PersistenceRequired", true);
        nbtTagCompound.setBoolean("NoGravity", true);
        nbtTagCompound.setBoolean("Gravity", false);
        nbtTagCompound.setBoolean("Invulnerable", true);

        EntityArmorStand armorStand = new EntityArmorStand(worldServer, location.getX(), location.getY(), location.getZ());
        // If we are just a text display we need an offset to make the text not appear in the air
        // For some reason, using `f` seems to alter the position of the armor stand, so since we disabled
        // it we have to manually offset the position of the armor stand.
        float textYOffset = text != null ? 1.8f : 0.0f;
        armorStand.setLocation(location.getX(), location.getY() - (textYOffset), location.getZ(), (float)armorStandData.getYaw(), 0);
        if(text != null) {
            armorStand.setCustomName(ChatColor.translateAlternateColorCodes('&', text));
            //armorStand.setCustomName(IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + text + "\"}"));
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
        // Don't use setPositionRotation, this changes the actual x,y,z position of the armor stand
        // which was causing armor stands to not appear in the correct location!!!
        // armorStand.setPositionRotation(0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
        armorStand.setHeadPose(new Vector3f(0.0F, 0.0F, 0.0F));
        armorStand.setGravity(false);
        armorStand.setInvisible(true);
        //armorStand.collides = false;
        armorStand.c(nbtTagCompound);
        // For some reason, using `f` makes glass on armor stand head appear with lighting glitch
        // Disabling this caused an odd issue where the nametag now displays high up in the air
        // so we added a manual offset to the y position of the armor stand up above.
        // armorStand.f(nbtTagCompound);

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
                spawnEntityEquipmentPacket = new PacketPlayOutEntityEquipment(armorStand.getId(), 0, CraftItemStack.asNMSCopy(armorStandData.getEquipment())); //since no Enum exists for MAINHAND, find int that matches that. (Head is 4)
            }
            else {
                spawnEntityEquipmentPacket = new PacketPlayOutEntityEquipment(armorStand.getId(), getEquipmentSlot(armorStandData.getEquipmentSlot()), CraftItemStack.asNMSCopy(armorStandData.getEquipment()));
            }
        }

        sendPacket(player, spawnEntityLivingPacket);
        sendPacket(player, spawnEntityMetadataPacket);
        if(spawnEntityEquipmentPacket != null){
            sendPacket(player, spawnEntityEquipmentPacket);
        }
    }

    private int getEquipmentSlot(EquipmentSlot equipmentSlot) {
        if (equipmentSlot == EquipmentSlot.HAND) { return 0; } 
        else if (equipmentSlot == EquipmentSlot.FEET) { return 1; }
        else if (equipmentSlot == EquipmentSlot.LEGS) { return 2; }
        else if (equipmentSlot == EquipmentSlot.CHEST) { return 3; }
        else if (equipmentSlot == EquipmentSlot.HEAD) { return 4; }
        return 0;
    }

    @Override
    protected void spawnItemFramePacket(Player player, ItemStack is, Location location, BlockFace facing, boolean isGlowing){

        WorldServer worldServer = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(location.getX(), location.getY(), location.getZ());

        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        nbtTagCompound.setBoolean("Marker", true);
        nbtTagCompound.setBoolean("PersistenceRequired", true);
        nbtTagCompound.setBoolean("NoGravity", true);
        nbtTagCompound.setBoolean("Gravity", false);
        nbtTagCompound.setBoolean("Invulnerable", true);

        EntityItemFrame itemFrame = new EntityItemFrame(worldServer, blockPosition, EnumDirection.valueOf(facing.toString()));
        int entityID = itemFrame.getId();
        this.addEntityID(player, entityID);
        itemFrame.setLocation(location.getX(), location.getY(), location.getZ(),0f,0f);
        itemFrame.setItem(CraftItemStack.asNMSCopy(is));
        itemFrame.setDirection(EnumDirection.valueOf(facing.toString()));
        itemFrame.c(nbtTagCompound);
        itemFrame.f(nbtTagCompound);

        PacketPlayOutSpawnEntity entitySpawnPacket = new PacketPlayOutSpawnEntity(itemFrame, 71);
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

    public static String getNMSItemName(ItemStack is) {
        // Only used for 1.8 and below.
        // 1.9 can use ItemMeta.getLocalizedName()
        // 1.16 and above can use .getTranslationKey()
        return CraftItemStack.asNMSCopy(is).getName();
    }
}
