package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;

public class DisplayUtil {

    //main groups of angles
    public static EulerAngle itemAngle = new EulerAngle(Math.toRadians(-90), Math.toRadians(0), Math.toRadians(0));
    public static EulerAngle toolAngle = new EulerAngle(Math.toRadians(-100), Math.toRadians(-90), Math.toRadians(0));

    //very specific case angles
    public static EulerAngle rodAngle = new EulerAngle(Math.toRadians(-80), Math.toRadians(-90), Math.toRadians(0));
    public static EulerAngle bowAngle = new EulerAngle(Math.toRadians(-90), Math.toRadians(5), Math.toRadians(-10));
    public static EulerAngle crossBowAngle = new EulerAngle(3.193952531149623, 0.0, 0.2792526803190927);
    public static EulerAngle shieldAngle = new EulerAngle(Math.toRadians(90), Math.toRadians(0), Math.toRadians(0));

    //this spawns an armorstand at a location, with the item on it
    public static ArmorStand createDisplay(ItemStack itemStack, Location blockLocation, BlockFace facing){
        EquipmentSlot equipmentSlot = getEquipmentSlot(itemStack);

        Location standLocation = getStandLocation(blockLocation, itemStack.getType(), facing, equipmentSlot);
        ArmorStand stand = null;

        switch (equipmentSlot){
            case HEAD:
                stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.getEquipment().setHelmet(itemStack);

                stand.setSmall(true);
                break;
            case CHEST:
                stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.setSmall(true);
                stand.getEquipment().setChestplate(itemStack);
                break;
            case LEGS:
                stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.setSmall(true);
                stand.getEquipment().setLeggings(itemStack);
                //TODO set legs pose to be slightly spread
                break;
            case FEET:
                stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.setSmall(true);
                stand.getEquipment().setBoots(itemStack);
                //TODO set legs pose to be slightly spread
                break;
            case HAND:
                stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.getEquipment().setItemInHand(itemStack);
                // .setItem() is available in 1.15+, .setItemInHand() is still available in 1.21+
                // stand.getEquipment().setItem(EquipmentSlot.HAND, itemStack);
                stand.setRightArmPose(getArmAngle(itemStack));

                try{
                    if(itemStack.getType() == MaterialUtil.of("SHIELD"))
                        stand.setSmall(true);
                } catch (NoSuchFieldError e) {}

                break;
        }

        if(stand != null) {
            stand.setGravity(false); //use to be false
            stand.setVisible(false);
            stand.setBasePlate(false);
        }

        return stand;
    }

    public static ArmorStandData getArmorStandData(ItemStack itemStack, Location blockLocation, BlockFace facing, boolean isCase){
        EquipmentSlot equipmentSlot = getEquipmentSlot(itemStack);
        Location standLocation = getStandLocation(blockLocation, itemStack.getType(), facing, equipmentSlot);
        standLocation.setY(standLocation.getY() - 0.7);

        ArmorStandData armorStandData = new ArmorStandData();

        switch (equipmentSlot){
            case HEAD:
            case CHEST:
            case LEGS:
            case FEET:
                armorStandData.setSmall(true);
                if (itemStack.getType() == MaterialUtil.of("ELYTRA"))
                    standLocation.setY(standLocation.getY() + 0.7);
                break;
            case HAND:
                armorStandData.setRightArmPose(getArmAngle(itemStack));
                standLocation.setY(standLocation.getY() + 0.7);
                try{
                    if(itemStack.getType() == MaterialUtil.of("SHIELD"))
                        armorStandData.setSmall(true);
                } catch (NoSuchFieldError e) {}

                break;
        }

        if(isCase) {
            standLocation.setY(standLocation.getY() - 0.7);
            armorStandData.setSmall(false);
        }

        armorStandData.setLocation(standLocation);
        armorStandData.setEquipment(itemStack);
        armorStandData.setEquipmentSlot(equipmentSlot);

        boolean isShield = false;
        try{
            if(itemStack.getType() == MaterialUtil.of("SHIELD"))
                isShield = true;
        } catch (NoSuchFieldError e) {}

        //make the stand face the correct direction when it spawns
        armorStandData.setYaw(blockfaceToYaw(facing));
        //fences and bows and shields are always 90 degrees off
        if(isFence(itemStack.getType()) || itemStack.getType() == Material.BOW || isShield){ //used to have banner here too
            armorStandData.setYaw(blockfaceToYaw(nextFace(facing)));
        }

        return armorStandData;
    }

    public static EquipmentSlot getEquipmentSlot(ItemStack itemStack){

        Material type = itemStack.getType();
        String sType = type.toString().toUpperCase();

        if(isHeldBlock(type)){
            return EquipmentSlot.HAND;
        }
        else if(sType.contains("_HELMET") || type == MaterialUtil.of("PLAYER_HEAD") || type.isBlock()){
            return EquipmentSlot.HEAD;
        }
        else if(sType.contains("_CHESTPLATE") || type == MaterialUtil.of("ELYTRA")){
            return EquipmentSlot.CHEST;
        }
        else if(sType.contains("_LEGGINGS")){
            return EquipmentSlot.LEGS;
        }
        else if(sType.contains("_BOOTS")){
            return EquipmentSlot.FEET;
        }
        return EquipmentSlot.HAND;
    }

    public static Location getStandLocation(Location blockLocation, Material material, BlockFace facing, EquipmentSlot equipmentSlot){

        Location standLocation = null;
        switch (equipmentSlot) {
            case HEAD:
                standLocation = blockLocation.clone().add(0.5, 0, 0.5);
                break;
            case CHEST:
                standLocation = blockLocation.clone().add(0.5, 0.4, 0.5);
                if(material == MaterialUtil.of("ELYTRA")){
                    switch (facing){
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.5, -0.1, 0.2);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.7, -0.1, 0.5);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.5, -0.1, 0.7);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.3, -0.1, 0.5);
                            break;
                    }
                }
                break;
            case LEGS:
                standLocation = blockLocation.clone().add(0.5, 0.7 ,0.5);
                break;
            case FEET:
                standLocation = blockLocation.clone().add(0.5, 0.8 ,0.5);
                break;
            case HAND:
                standLocation = blockLocation;

                if(isTool(material)){
                    double rodOffset = 0.1;
                    switch (facing){
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.7, -1.3, 0.6);
                            if(material == Material.FISHING_ROD || material == MaterialUtil.of("CARROT_ON_A_STICK"))
                                standLocation = standLocation.add(rodOffset, 0, 0);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.425, -1.3, 0.7);
                            if(material == Material.FISHING_ROD || material == MaterialUtil.of("CARROT_ON_A_STICK"))
                                standLocation = standLocation.add(0, 0, rodOffset);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.3, -1.3, 0.42);
                            if(material == Material.FISHING_ROD || material == MaterialUtil.of("CARROT_ON_A_STICK"))
                                standLocation = standLocation.add(-rodOffset, 0, 0);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.6, -1.3, 0.3);
                            if(material == Material.FISHING_ROD || material == MaterialUtil.of("CARROT_ON_A_STICK"))
                                standLocation = standLocation.add(0, 0, -rodOffset);
                            break;
                    }
                }
                else if(material == Material.BOW){
                    switch (facing){
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.99, -0.8, 0.84);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.15, -0.8, 0.99);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0, -0.8, 0.15);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.85, -0.8, 0);
                            break;
                    }
                }
                else if(MCVersion.atLeast("1.14") && material == MaterialUtil.of("CROSSBOW")){
                    switch (facing){
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.25, -1.6, 0.44);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.6, -1.6, 0.25);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.8, -1.6, 0.6);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.4, -1.6, 0.75);
                            break;
                    }
                }
                else if(material.toString().contains("BANNER") && !material.toString().contains("PATTERN")){
                    switch (facing){
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.99, -1.4, 0.86);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.12, -1.4, 1);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.01, -1.4, 0.12);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.88, -1.4, 0);
                            break;
                    }
                }
                //the material is a simple, default item
                else{
                    switch (facing){
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.125, -1.4, 0.95);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.005, -1.4, 0.11);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.88, -1.4, 0.005);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.99, -1.4, 0.88);
                            break;
                    }
                }
                try {
                    if (material == MaterialUtil.of("SHIELD")) {
                        standLocation.add(0, 1, 0);
                        switch (facing) {
                            case NORTH:
                                standLocation.add(0.17, 0, -0.2);
                                break;
                            case EAST:
                                standLocation.add(0.2, 0, 0.17);
                                break;
                            case SOUTH:
                                standLocation.add(-0.17, 0, 0.2);
                                break;
                            case WEST:
                                standLocation.add(-0.2, 0, -0.17);
                                break;
                        }
                    }
                } catch (NoSuchFieldError e) {}
                break;
        }

        boolean isShield = false;
        try{
            if(material == MaterialUtil.of("SHIELD"))
                isShield = true;
        } catch (NoSuchFieldError e) {}

        if(facing == null)
            facing = BlockFace.NORTH;

        //make the stand face the correct direction when it spawns
        standLocation.setYaw(blockfaceToYaw(facing));
        //fences and bows and shields are always 90 degrees off
        if(isFence(material) || material == Material.BOW || material.toString().contains("BANNER") || isShield){
            standLocation.setYaw(blockfaceToYaw(nextFace(facing)));
        }

        //TODO come back to this
        //removed barrel and shulker box check because it requires getBlock() which ends up loading the chunk
//        try {
//            Block chestBlock = blockLocation.getBlock();
//            if (chestBlock.getRelative(BlockFace.DOWN).getState() instanceof ShulkerBox) {
//                standLocation.add(0, 0.1, 0);
//            }
//            else if(UtilMethods.isMCVersion14Plus() && (chestBlock.getType() == Material.BARREL || chestBlock.getRelative(BlockFace.DOWN).getType() == Material.BARREL)){
//                standLocation.add(0, 0.1, 0); //y was 0.22 for items before packets were used
//            }
//        } catch (NoClassDefFoundError e) {}

        //material is an item
        return standLocation;
    }

    public static EulerAngle getArmAngle(ItemStack itemStack){

        Material material = itemStack.getType();

        boolean isShield = false;
        try{
            if(material == MaterialUtil.of("SHIELD"))
                isShield = true;
        } catch (NoSuchFieldError e) {}

        if(isTool(material) && !(material == Material.FISHING_ROD || material == MaterialUtil.of("CARROT_ON_A_STICK"))){
            return toolAngle;
        }
        else if(material == Material.BOW){
            return bowAngle;
        }
        else if(MCVersion.atLeast("1.14") && material == MaterialUtil.of("CROSSBOW")){
            return crossBowAngle;
        }
        else if(material == Material.FISHING_ROD || material == MaterialUtil.of("CARROT_ON_A_STICK")){
            return rodAngle;
        }
        else if(isShield){
            //shield angles are different in MC 1.10 and MC 1.11+
            if (MCVersion.atLeast("1.11")) {
                return shieldAngle;
            } else {
                //server is below MC 1.10. (Use different shield angle)
                return new EulerAngle(Math.toRadians(70), Math.toRadians(0), Math.toRadians(0));
            }
        }
        return itemAngle;
    }

    /**
     * Converts a BlockFace direction to a yaw (float) value
     * Return:
     * - float: the yaw value of the BlockFace direction provided
     */
    public static float blockfaceToYaw(BlockFace bf) {
        if (bf.equals(BlockFace.SOUTH))
            return 0F;
        else if (bf.equals(BlockFace.WEST))
            return 90F;
        else if (bf.equals(BlockFace.NORTH))
            return 180F;
        else if (bf.equals(BlockFace.EAST))
            return 270F;
        return 0F;
    }

    public static boolean isBlock(Material material){
        if(material.isBlock() && !material.toString().toUpperCase().contains("CHEST"))
            return true;
        return false;
    }

    public static boolean isHeldNonItem(Material material){
        String sType = material.toString().toUpperCase();
        if(
            isTool(material) 
            || sType.contains("SWORD") 
            || material == Material.BOW 
            || material == Material.FISHING_ROD 
            || material == MaterialUtil.of("CARROT_ON_A_STICK") 
            || material == MaterialUtil.of("CROSSBOW"))
            return true;
        return false;
    }

    public static boolean isTool(Material material){
        String sMaterial = material.toString().toUpperCase();
        return (sMaterial.contains("_AXE") || sMaterial.contains("_HOE") 
                || sMaterial.contains("_PICKAXE") || sMaterial.contains("_SPADE") 
                || sMaterial.contains("_SWORD") || sMaterial.contains("MACE")
                || material == Material.BONE || material == Material.STICK  || material == Material.BLAZE_ROD
                || sMaterial.contains("CARROT_ON_A_STICK") || material == Material.FISHING_ROD);
    }

    public static boolean isChest(Material material){
        String sMaterial = material.toString().toUpperCase();
        return (sMaterial.contains("CHEST") && !sMaterial.contains("CHESTPLATE"));
    }

    public static boolean isFence(Material material){
        String sMaterial = material.toString().toUpperCase();
        return (sMaterial.contains("FENCE") && (material != MaterialUtil.of("IRON_BARS")) && !sMaterial.contains("GATE"));
    }

    public static boolean isArmor(Material material){
        String sMaterial = material.toString().toUpperCase();
        if(material == MaterialUtil.of("PLAYER_HEAD")){
            return true;
        }
        else if(sMaterial.contains("_BOOTS") || sMaterial.contains("_CHESTPLATE") || sMaterial.contains("_LEGGINGS") || sMaterial.contains("HELMET")){
            return true;
        }
        return false;
    }

    public static boolean isHeldBlock(Material material){
        if(material.isBlock()) {
            if(material.toString().contains("THIN_GLASS") || material.toString().contains("GLASS_PANE") || material.toString().contains("SAPLING"))
                return true;
            // Need to do some refactoring here to split apart the material versions and conditionally check the materials 
            switch (material) {
                case LADDER:
                case VINE:
                case RAIL: // not in 1.8
                case POWERED_RAIL:
                case ACTIVATOR_RAIL:
                case DETECTOR_RAIL:
                case TRIPWIRE_HOOK:
                case LEVER:
                case TORCH:
                case COBWEB: // not in 1.8
                case TALL_GRASS: // not in 1.8
                case DEAD_BUSH:
                case POPPY: // not in 1.8
                case DANDELION: // not in 1.8
                case BROWN_MUSHROOM:
                case RED_MUSHROOM:
                case REDSTONE_TORCH: // not in 1.8
                case IRON_BARS: // not in 1.8
                case LILY_PAD: // not in 1.8
                case HOPPER:
                case BARRIER:
                case CHORUS_PLANT: // not in 1.8
                case STRUCTURE_VOID: // not in 1.8
                case END_ROD: // not in 1.8
                    return true;
            }
        }
        return false;
    }

    public static BlockFace nextFace(BlockFace face){
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST};
        BlockFace direction = null;
        if(face == faces[faces.length-1])
            direction = faces[0];
        else{
            for(int i=0; i<faces.length; i++){
                if(face == faces[i]){
                    direction = faces[i+1];
                    break;
                }
            }
        }
        return direction;
    }

    public static boolean isDisplay(Entity entity){
        // @ToDo: Cleanup this weird code
        boolean foundLegacy = isDisplayLegacy(entity);
        if(foundLegacy)
            return true;
        
        if(MCVersion.atLeast("1.14")) {
            PersistentDataContainer container = entity.getPersistentDataContainer();
            NamespacedKey namespacedKey = new NamespacedKey(Shop.getPlugin(), "display");
            Integer displayValue = container.get(namespacedKey, PersistentDataType.INTEGER);
            return displayValue != null && displayValue == 1;
        }
        
        return false;
    }

    private static boolean isDisplayLegacy(Entity entity){
        try {
            if (CompatibilityUtil.hasItemEntityType() && entity.getType().name().equals("ITEM")) {
                ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
                if (itemMeta != null && UtilMethods.containsLocation(itemMeta.getDisplayName())) {
                    return true;
                }
            } else if (entity.getType() == EntityType.ARMOR_STAND) {
                if (UtilMethods.containsLocation(entity.getCustomName())) {
                    return true;
                }
            }
        } catch (NoSuchFieldError error){
            //do nothing
        }
        return false;
    }
}
