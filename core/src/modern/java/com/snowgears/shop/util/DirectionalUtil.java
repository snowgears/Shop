package com.snowgears.shop.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;

/**
 * Modern implementation of DirectionalUtil for MC 1.13+ using direct API calls.
 * This implementation uses the modern Directional interface without reflection.
 */
public class DirectionalUtil {
    
    /**
     * Gets the facing direction of a directional block using modern API.
     * 
     * @param block The block to examine
     * @return The BlockFace the block is facing, or null if not directional
     */
    public static BlockFace getDirectionOfBlock(Block block) {
        if (block == null) {
            return null;
        }
        
        Object blockData = CompatibilityUtil.getBlockData(block);
        
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData instanceof Directional) {
            Directional directional = (Directional) blockData;
            return directional.getFacing();
        }
        
        return null;
    }
    
    /**
     * Checks if a block is directional using modern API.
     * 
     * @param block The block to check
     * @return true if the block implements Directional
     */
    public static boolean isDirectional(Block block) {
        if (block == null) {
            return false;
        }
        
        Object blockData = CompatibilityUtil.getBlockData(block);
        return CompatibilityUtil.HAS_BLOCK_DATA && blockData instanceof Directional;
    }
    
    /**
     * Sets the facing direction of a directional block using modern API.
     * 
     * @param block The block to modify
     * @param facing The direction to face
     * @return true if successfully set, false otherwise
     */
    public static boolean setDirectionOfBlock(Block block, BlockFace facing) {
        if (block == null || facing == null) {
            return false;
        }
        
        Object blockData = CompatibilityUtil.getBlockData(block);
        
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData instanceof Directional) {
            Directional directional = (Directional) blockData;
            
            // Check if this direction is allowed for this block type
            if (directional.getFaces().contains(facing)) {
                directional.setFacing(facing);
                block.setBlockData((org.bukkit.block.data.BlockData) directional);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets the valid facing directions for a directional block.
     * 
     * @param block The block to examine
     * @return Set of valid BlockFaces, or empty set if not directional
     */
    public static java.util.Set<BlockFace> getValidDirections(Block block) {
        if (block == null) {
            return java.util.Collections.emptySet();
        }
        
        Object blockData = CompatibilityUtil.getBlockData(block);
        
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData instanceof Directional) {
            Directional directional = (Directional) blockData;
            return directional.getFaces();
        }
        
        return java.util.Collections.emptySet();
    }
} 