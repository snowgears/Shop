package com.snowgears.shop.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * Legacy implementation of DirectionalUtil for MC 1.13.2-1.14.
 * This implementation uses reflection for Directional interface compatibility.
 */
public class DirectionalUtil {
    
    /**
     * Gets the facing direction of a directional block using reflection.
     * 
     * @param block The block to examine
     * @return The BlockFace the block is facing, or null if not directional
     */
    public static BlockFace getDirectionOfBlock(Block block) {
        if (block == null) {
            return null;
        }
        
        Object blockData = CompatibilityUtil.getBlockData(block);
        
        // Use reflection to access Directional interface
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData != null) {
            try {
                Class<?> directionalClass = Class.forName("org.bukkit.block.data.Directional");
                if (directionalClass.isInstance(blockData)) {
                    Object directional = directionalClass.cast(blockData);
                    return (BlockFace) directionalClass.getMethod("getFacing").invoke(directional);
                }
            } catch (Exception e) {
                // Not supported in this version
            }
        }
        
        return null;
    }
    
    /**
     * Checks if a block is directional using reflection.
     * 
     * @param block The block to check
     * @return true if the block implements Directional
     */
    public static boolean isDirectional(Block block) {
        if (block == null) {
            return false;
        }
        
        Object blockData = CompatibilityUtil.getBlockData(block);
        
        // Use reflection to access Directional interface
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData != null) {
            try {
                Class<?> directionalClass = Class.forName("org.bukkit.block.data.Directional");
                return directionalClass.isInstance(blockData);
            } catch (Exception e) {
                // Not supported in this version
            }
        }
        
        return false;
    }
    
    /**
     * Sets the facing direction of a directional block using reflection.
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

        // Use reflection to access Directional interface
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData != null) {
            try {
                Class<?> directionalClass = Class.forName("org.bukkit.block.data.Directional");
                if (directionalClass.isInstance(blockData)) {
                    Object directional = directionalClass.cast(blockData);
                    
                    // Check if this direction is valid (getFaces method)
                    try {
                        @SuppressWarnings("unchecked")
                        java.util.Set<BlockFace> validFaces = (java.util.Set<BlockFace>) directionalClass.getMethod("getFaces").invoke(directional);
                        if (validFaces.contains(facing)) {
                            directionalClass.getMethod("setFacing", BlockFace.class).invoke(directional, facing);
                            block.setBlockData((org.bukkit.block.data.BlockData) directional);
                            return true;
                        }
                    } catch (Exception e) {
                        // If we can't check validity, just try to set it
                        directionalClass.getMethod("setFacing", BlockFace.class).invoke(directional, facing);
                        block.setBlockData((org.bukkit.block.data.BlockData) directional);
                        return true;
                    }
                }
            } catch (Exception e) {
                // Not supported in this version
            }
        }
        
        return false;
    }
    
    /**
     * Gets the valid facing directions for a directional block using reflection.
     * 
     * @param block The block to examine
     * @return Set of valid BlockFaces, or empty set if not directional
     */
    @SuppressWarnings("unchecked")
    public static java.util.Set<BlockFace> getValidDirections(Block block) {
        if (block == null) {
            return java.util.Collections.emptySet();
        }
        
        Object blockData = CompatibilityUtil.getBlockData(block);
        
        // Use reflection to access Directional interface
        if (CompatibilityUtil.HAS_BLOCK_DATA && blockData != null) {
            try {
                Class<?> directionalClass = Class.forName("org.bukkit.block.data.Directional");
                if (directionalClass.isInstance(blockData)) {
                    Object directional = directionalClass.cast(blockData);
                    return (java.util.Set<BlockFace>) directionalClass.getMethod("getFaces").invoke(directional);
                }
            } catch (Exception e) {
                // Not supported in this version
            }
        }
        
        return java.util.Collections.emptySet();
    }
} 