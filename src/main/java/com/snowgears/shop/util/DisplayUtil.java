package com.snowgears.shop.util;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

public class DisplayUtil {
    //main groups of angles
    private static EulerAngle itemAngle = new EulerAngle(Math.toRadians(-90), Math.toRadians(0), Math.toRadians(0));
    private static EulerAngle toolAngle = new EulerAngle(Math.toRadians(-100), Math.toRadians(-90), Math.toRadians(0));

    //very specific case angles
    private static EulerAngle rodAngle = new EulerAngle(Math.toRadians(-80), Math.toRadians(-90), Math.toRadians(0));
    private static EulerAngle bowAngle = new EulerAngle(Math.toRadians(-90), Math.toRadians(5), Math.toRadians(-10));
    private static EulerAngle shieldAngle = new EulerAngle(Math.toRadians(90), Math.toRadians(0), Math.toRadians(0));

    //this spawns an armorstand at a location, with the item on it
    public static ArmorStand createDisplay(ItemStack itemStack, Location blockLocation, BlockFace facing) {
        ItemType itemType = getItemType(itemStack);

        Location standLocation = getStandLocation(blockLocation, itemStack.getType(), facing, itemType);
        ArmorStand stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);

        stand.setSmall(true);
        stand.setGravity(false);
        stand.setVisible(false);
        stand.setBasePlate(false);

        switch (itemType) {
            case HEAD:
                stand.setHelmet(itemStack);
                break;
            case BODY:
                stand.setChestplate(itemStack);
                break;
            case LEGS:
                stand.setLeggings(itemStack);
                //TODO set legs pose to be slightly spread
                break;
            case FEET:
                stand.setBoots(itemStack);
                //TODO set legs pose to be slightly spread
                break;
            case HAND:
                //noinspection deprecation
                stand.setItemInHand(itemStack);
                stand.setRightArmPose(getArmAngle(itemStack));
                if (itemStack.getType() != Material.SHIELD) {
                    stand.setSmall(false);
                }
                break;
        }

        return stand;
    }

    private static ItemType getItemType(ItemStack itemStack) {
        Material type = itemStack.getType();
        String sType = type.toString().toUpperCase();

        if (sType.contains("_HELMET") || type == Material.PLAYER_HEAD || type.isBlock()) {
            return ItemType.HEAD;
        } else if (sType.contains("_CHESTPLATE") || type == Material.ELYTRA) {
            return ItemType.BODY;
        } else if (sType.contains("_LEGGINGS")) {
            return ItemType.LEGS;
        } else if (sType.contains("_BOOTS")) {
            return ItemType.FEET;
        }
        return ItemType.HAND;
    }

    private static Location getStandLocation(Location blockLocation, Material material, BlockFace facing, ItemType itemType) {
        Location standLocation = null;
        switch (itemType) {
            case HEAD:
                standLocation = blockLocation.clone().add(0.5, -.7, 0.5);
                break;
            case BODY:
                standLocation = blockLocation.clone().add(0.5, -0.3, 0.5);
                break;
            case LEGS:
                standLocation = blockLocation.clone().add(0.5, -0.1, 0.5);
                break;
            case FEET:
                standLocation = blockLocation.clone().add(0.5, 0.05, 0.5);
                break;
            case HAND:
                standLocation = blockLocation;

                if (isTool(material)) {
                    double rodOffset = 0.1;
                    switch (facing) {
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.7, -1.3, 0.6);
                            if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK)
                                standLocation = standLocation.add(rodOffset, 0, 0);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.425, -1.3, 0.7);
                            if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK)
                                standLocation = standLocation.add(0, 0, rodOffset);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.3, -1.3, 0.42);
                            if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK)
                                standLocation = standLocation.add(-rodOffset, 0, 0);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.6, -1.3, 0.3);
                            if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK)
                                standLocation = standLocation.add(0, 0, -rodOffset);
                            break;
					default:
						break;
                    }
                } else if (material == Material.BOW) {
                    switch (facing) {
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
					default:
						break;
                    }
                } else if (Tag.BANNERS.isTagged(material)) {
                    switch (facing) {
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
					default:
						break;
                    }
                }
                //the material is a simple, default item
                else {
                    switch (facing) {
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
					default:
						break;
                    }
                }
                if (material == Material.SHIELD) {
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
					default:
						break;
                    }
                }
                break;
        }

        if (facing == null) {
            facing = BlockFace.NORTH;
        }

        //make the stand face the correct direction when it spawns
        standLocation.setYaw(blockfaceToYaw(facing));
        //fences and bows and shields are always 90 degrees off
        if (isFence(material) || material == Material.BOW || Tag.BANNERS.isTagged(material) || material == Material.SHIELD) {
            standLocation.setYaw(blockfaceToYaw(nextFace(facing)));
        }

        if (blockLocation.getBlock().getState() instanceof ShulkerBox || blockLocation.getBlock().getRelative(BlockFace.DOWN).getState() instanceof ShulkerBox) {
            standLocation.add(0, 0.1, 0);
        }

        //material is an item
        return standLocation;
    }

    private static EulerAngle getArmAngle(ItemStack itemStack) {
        Material material = itemStack.getType();
        if (isTool(material) && !(material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK)) {
            return toolAngle;
        } else if (material == Material.BOW) {
            return bowAngle;
        } else if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK) {
            return rodAngle;
        } else if (material == Material.SHIELD) {
            return shieldAngle;
        }
        return itemAngle;
    }

    /**
     * Converts a BlockFace direction to a yaw (float) value
     * Return:
     * - float: the yaw value of the BlockFace direction provided
     */
    private static float blockfaceToYaw(BlockFace bf) {
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

    private static boolean isTool(Material material) {
        String sMaterial = material.name();
        return (sMaterial.contains("_AXE") || sMaterial.contains("_HOE") || sMaterial.contains("_PICKAXE")
                || sMaterial.contains("_SPADE") || sMaterial.contains("_SWORD")
                || material == Material.BONE || material == Material.STICK || material == Material.BLAZE_ROD
                || material == Material.CARROT_ON_A_STICK || material == Material.FISHING_ROD);
    }

    private static boolean isFence(Material material) {
        String sMaterial = material.toString().toUpperCase();
        return (sMaterial.contains("FENCE") && (material != Material.IRON_BARS) && !sMaterial.contains("GATE"));
    }

    private static BlockFace nextFace(BlockFace face) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST};
        BlockFace direction = null;
        if (face == faces[faces.length - 1])
            direction = faces[0];
        else {
            for (int i = 0; i < faces.length; i++) {
                if (face == faces[i]) {
                    direction = faces[i + 1];
                    break;
                }
            }
        }
        return direction;
    }

    public enum ItemType {
        HEAD, BODY, LEGS, FEET, HAND
    }
}
