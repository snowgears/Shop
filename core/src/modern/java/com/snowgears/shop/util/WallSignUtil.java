package com.snowgears.shop.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Location;

/**
 * Modern implementation of WallSignUtil for MC 1.13+ using direct API calls.
 * This implementation uses the modern WallSign interface without reflection.
 */
public class WallSignUtil {
    
    /**
     * Calculates which side of a wall sign was clicked by a player.
     * 
     * @param player The player who clicked the sign
     * @param signBlock The wall sign block that was clicked
     * @return 1 for left side, -1 for right side, 0 for center
     */
    public static int calculateSideFromClickedSign(Player player, Block signBlock) {
        Object blockData = CompatibilityUtil.getBlockData(signBlock);
        
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData instanceof WallSign) {
            WallSign wallSign = (WallSign) blockData;
            BlockFace attachedFace = wallSign.getFacing().getOppositeFace();
            
            return calculateSideFromDirection(player, signBlock, attachedFace);
        }
        
        return 0; // Fallback
    }
    
    /**
     * Checks if a block is a wall sign using modern API.
     * 
     * @param signBlock The block to check
     * @return true if the block is a wall sign
     */
    public static boolean isWallSign(Block signBlock) {
        if (signBlock == null) {
            return false;
        }
        
        Object blockData = CompatibilityUtil.getBlockData(signBlock);
        return CompatibilityUtil.HAS_BLOCK_DATA && blockData instanceof WallSign;
    }
    
    /**
     * Gets the facing direction of a wall sign.
     * 
     * @param signBlock The wall sign block
     * @return The BlockFace the sign is facing, or null if not a wall sign
     */
    public static BlockFace getWallSignFacing(Block signBlock) {
        Object blockData = CompatibilityUtil.getBlockData(signBlock);
        
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData instanceof WallSign) {
            WallSign wallSign = (WallSign) blockData;
            return wallSign.getFacing();
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