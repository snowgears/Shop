package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.data.type.Light;
import org.bukkit.Tag;

/**
 * Modern implementation of BlockDataUtil for MC 1.13+ using BlockData API.
 * This implementation uses direct API calls without reflection for better performance.
 */
public class BlockDataUtil {
    
    /**
     * Checks if a block is a wall sign using modern BlockData API.
     * 
     * @param block The block to check
     * @return true if the block is a wall sign
     */
    public static boolean isWallSign(Block block) {
        if (block == null) {
            return false;
        }
        
        try {
            BlockData blockData = block.getBlockData();
            return blockData instanceof WallSign;
        } catch (Error | Exception e) {
            return block.getType().toString().contains("WALL_SIGN");
        }
    }
    
    /**
     * Gets the facing direction from a wall sign block using BlockData API.
     * 
     * @param block The block to check (should be a wall sign)
     * @return The BlockFace direction the sign is facing, or null if not a wall sign
     */
    public static BlockFace getSignFacing(Block block) {
        if (block == null) {
            return null;
        }
        
        BlockData blockData = block.getBlockData();
        if (blockData instanceof WallSign) {
            WallSign wallSign = (WallSign) blockData;
            return wallSign.getFacing();
        }
        
        return null;
    }
    
    /**
     * Sets the facing direction of a wall sign using BlockData API.
     * 
     * @param signBlock The wall sign block
     * @param direction The direction the sign should face
     * @return true if successfully set, false otherwise
     */
    public static boolean setSignFacing(Block signBlock, BlockFace direction) {
        if (signBlock == null || direction == null) {
            return false;
        }
        
        BlockData blockData = signBlock.getBlockData();
        if (blockData instanceof Directional) {
            Directional directional = (Directional) blockData;
            directional.setFacing(direction);
            signBlock.setBlockData(directional);
            return true;
        }
        
        return false;
    }
    
    /**
     * Converts a regular sign to a wall sign and sets its facing direction.
     * Uses modern material naming and BlockData API.
     * 
     * @param signBlock The sign block to convert
     * @param direction The direction the wall sign should face
     * @return true if successfully converted, false otherwise
     */
    public static boolean convertToWallSign(Block signBlock, BlockFace direction) {
        if (signBlock == null || direction == null) {
            return false;
        }
        
        String currentType = signBlock.getType().toString();
        if (!currentType.contains("_SIGN")) {
            return false; // Not a sign block
        }
        
        // Already a wall sign, just set facing
        if (isWallSign(signBlock)) {
            return setSignFacing(signBlock, direction);
        }
        
        // Convert to wall sign using modern material naming
        String wallSignType = currentType.replaceAll("_SIGN$", "_WALL_SIGN");
        if (currentType.equals("SIGN") || currentType.equals("SIGN_POST")) {
            wallSignType = "WALL_SIGN"; // Legacy fallback
        }
        
        try {
            Material wallSignMaterial = Material.valueOf(wallSignType);
            signBlock.setType(wallSignMaterial);
            return setSignFacing(signBlock, direction);
        } catch (IllegalArgumentException e) {
            // Material doesn't exist, try fallback
            try {
                signBlock.setType(Material.valueOf("WALL_SIGN"));
                return setSignFacing(signBlock, direction);
            } catch (Exception fallbackException) {
                return false;
            }
        }
    }
    
    /**
     * Gets block data using modern BlockData API.
     * 
     * @param block The block to get data from
     * @return BlockData object representing the block data
     */
    public static Object getBlockData(Block block) {
        if (block == null) {
            return null;
        }
        
        return block.getBlockData();
    }
    
    /**
     * Sets block data using modern BlockData API.
     * 
     * @param block The block to set data on
     * @param data The BlockData to set
     * @return true if successful
     */
    public static boolean setBlockData(Block block, Object data) {
        if (block == null || data == null) {
            return false;
        }
        
        if (data instanceof BlockData) {
            block.setBlockData((BlockData) data);
            return true;
        }
        
        return false;
    }
    
    /**
     * Sets a light block with the specified light level using modern Light block type.
     * Only works on MC 1.17+ where light blocks are available.
     * 
     * @param block The block to set as a light block
     * @param lightLevel The light level (0-15)
     * @return true if successfully set, false if not supported or failed
     */
    public static boolean setLightBlock(Block block, int lightLevel) {
        if (block == null || lightLevel < 0 || lightLevel > 15) {
            return false;
        }
        
        try {
            // Set to light block material
            block.setType(Material.LIGHT);
            
            // Set the light level using BlockData
            BlockData blockData = block.getBlockData();
            if (blockData instanceof Light) {
                Light lightData = (Light) blockData;
                lightData.setLevel(lightLevel);
                block.setBlockData(lightData);
                return true;
            }
        } catch (Exception e) {
            // Light blocks not available on this version
            if (Shop.getPlugin() != null) {
                Shop.getPlugin().getShopLogger().debug("Light blocks not supported: " + e.getMessage());
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a material is a shulker box using modern Tag.SHULKER_BOXES.
     * 
     * @param material The material to check
     * @return true if the material is a shulker box
     */
    public static boolean isShulkerBox(Material material) {
        if (material == null) {
            return false;
        }
        
        // Modern approach using Tag.SHULKER_BOXES
        return Tag.SHULKER_BOXES.isTagged(material);
    }
} 