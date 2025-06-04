package com.snowgears.shop.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Location;

/**
 * Legacy implementation of WallSignUtil for MC 1.13.2-1.14.
 * This implementation uses reflection for WallSign interface compatibility.
 */
public class WallSignUtil {
    
    /**
     * Calculates which side of a wall sign was clicked by a player using reflection.
     * 
     * @param player The player who clicked the sign
     * @param signBlock The wall sign block that was clicked
     * @return 1 for left side, -1 for right side, 0 for center
     */
    public static int calculateSideFromClickedSign(Player player, Block signBlock) {
        Object blockData = CompatibilityUtil.getBlockData(signBlock);
        
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData != null) {
            // Use reflection to access WallSign interface
            try {
                Class<?> wallSignClass = Class.forName("org.bukkit.block.data.type.WallSign");
                if (wallSignClass.isInstance(blockData)) {
                    Object wallSign = wallSignClass.cast(blockData);
                    Object facing = wallSignClass.getMethod("getFacing").invoke(wallSign);
                    BlockFace attachedFace = (BlockFace) facing.getClass().getMethod("getOppositeFace").invoke(facing);
                    
                    return calculateSideFromDirection(player, signBlock, attachedFace);
                }
            } catch (Exception e) {
                // Fallback to legacy approach
            }
        }
        
        // Legacy approach: Use MaterialData (MC ≤1.12.2)
        if (signBlock.getType().toString().contains("WALL_SIGN")) {
            try {
                org.bukkit.material.MaterialData materialData = (org.bukkit.material.MaterialData) blockData;
                if (materialData instanceof org.bukkit.material.Sign) {
                    org.bukkit.material.Sign signData = (org.bukkit.material.Sign) materialData;
                    BlockFace attachedFace = signData.getAttachedFace();
                    
                    return calculateSideFromDirection(player, signBlock, attachedFace);
                }
            } catch (Exception e) {
                // Fallback
            }
        }
        
        return 0; // Default fallback
    }
    
    /**
     * Checks if a block is a wall sign using reflection fallback.
     * 
     * @param signBlock The block to check
     * @return true if the block is a wall sign
     */
    public static boolean isWallSign(Block signBlock) {
        if (signBlock == null) {
            return false;
        }
        
        Object blockData = CompatibilityUtil.getBlockData(signBlock);
        
        // Try modern API using reflection
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData != null) {
            try {
                Class<?> wallSignClass = Class.forName("org.bukkit.block.data.type.WallSign");
                return wallSignClass.isInstance(blockData);
            } catch (Exception e) {
                // Fall through to legacy check
            }
        }
        
        // Legacy check - check material name
        return signBlock.getType().toString().contains("WALL_SIGN");
    }
    
    /**
     * Gets the facing direction of a wall sign using reflection.
     * 
     * @param signBlock The wall sign block
     * @return The BlockFace the sign is facing, or null if not a wall sign
     */
    public static BlockFace getWallSignFacing(Block signBlock) {
        Object blockData = CompatibilityUtil.getBlockData(signBlock);
        
        // Try modern API using reflection
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData != null) {
            try {
                Class<?> wallSignClass = Class.forName("org.bukkit.block.data.type.WallSign");
                if (wallSignClass.isInstance(blockData)) {
                    Object wallSign = wallSignClass.cast(blockData);
                    return (BlockFace) wallSignClass.getMethod("getFacing").invoke(wallSign);
                }
            } catch (Exception e) {
                // Fall through to legacy approach
            }
        }
        
        // Legacy approach using MaterialData
        try {
            if (blockData instanceof org.bukkit.material.MaterialData) {
                org.bukkit.material.MaterialData materialData = (org.bukkit.material.MaterialData) blockData;
                if (materialData instanceof org.bukkit.material.Sign) {
                    org.bukkit.material.Sign signData = (org.bukkit.material.Sign) materialData;
                    return signData.getFacing();
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        
        return null;
    }
    
    /**
     * Helper method to calculate side based on player position and attached face.
     * 
     * @param player The player
     * @param signBlock The sign block
     * @param attachedFace The face the sign is attached to
     * @return 1 for left side, -1 for right side, 0 for center
     */
    private static int calculateSideFromDirection(Player player, Block signBlock, BlockFace attachedFace) {
        Location chest = signBlock.getRelative(attachedFace).getLocation().add(0.5, 0.5, 0.5);
        Location head = player.getLocation().add(0, player.getEyeHeight(), 0);

        Vector direction = head.subtract(chest).toVector().normalize();
        Vector look = player.getLocation().getDirection().normalize();

        Vector cp = direction.crossProduct(look);

        double d = 0;
        switch (attachedFace) {
            case NORTH:
                d = cp.getZ();
                break;
            case SOUTH:
                d = cp.getZ() * -1;
                break;
            case EAST:
                d = cp.getX() * -1;
                break;
            case WEST:
                d = cp.getX();
                break;
            default:
                break;
        }

        if (player.getLocation().getPitch() < 0)
            d = -d;

        if (d > 0)
            return 1;
        else if (d < 0)
            return -1;
        else
            return 0;
    }
} 