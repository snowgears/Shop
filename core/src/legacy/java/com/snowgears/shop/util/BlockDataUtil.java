package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sign;

/**
 * Legacy implementation of BlockDataUtil for MC ≤1.12.2 using MaterialData API.
 * This implementation provides compatibility with older Minecraft versions that don't have BlockData.
 */
public class BlockDataUtil {
    
    /**
     * Checks if a block is a wall sign using legacy material name checking.
     * 
     * @param block The block to check
     * @return true if the block is a wall sign
     */
    public static boolean isWallSign(Block block) {
        if (block == null) {
            return false;
        }
        
        String typeName = block.getType().toString();
        return typeName.contains("WALL_SIGN") || 
               (typeName.contains("_SIGN") && !typeName.equals("SIGN_POST") && !typeName.equals("SIGN"));
    }
    
    /**
     * Gets the facing direction from a wall sign block using legacy MaterialData.
     * 
     * @param block The block to check (should be a wall sign)
     * @return The BlockFace direction the sign is facing, or null if not a wall sign
     */
    public static BlockFace getSignFacing(Block block) {
        if (block == null) {
            return null;
        }
        
        // First try BlockData API with reflection (available in MC 1.14.4)
        if (CompatibilityUtil.HAS_BLOCK_DATA) {
            try {
                Object blockData = block.getBlockData();
                if (blockData != null) {
                    Class<?> wallSignClass = Class.forName("org.bukkit.block.data.type.WallSign");
                    if (wallSignClass.isInstance(blockData)) {
                        Object wallSign = wallSignClass.cast(blockData);
                        return (BlockFace) wallSignClass.getMethod("getFacing").invoke(wallSign);
                    }
                }
            } catch (Exception e) {
                if (Shop.getPlugin() != null) {
                    Shop.getPlugin().getShopLogger().debug("Failed to get BlockData sign facing: " + e.getMessage());
                }
                // Fall through to legacy approach
            }
        }
        
        // Legacy approach using MaterialData
        try {
            MaterialData materialData = block.getState().getData();
            if (materialData instanceof Sign) {
                Sign signData = (Sign) materialData;
                // Fixed: use getFacing() instead of getAttachedFace().getOppositeFace()
                return signData.getFacing();
            }
        } catch (Exception e) {
            if (Shop.getPlugin() != null) {
                Shop.getPlugin().getShopLogger().debug("Failed to get legacy sign facing: " + e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Sets the facing direction of a wall sign using legacy MaterialData.
     * 
     * @param signBlock The wall sign block
     * @param direction The direction the sign should face
     * @return true if successfully set, false otherwise
     */
    public static boolean setSignFacing(Block signBlock, BlockFace direction) {
        if (signBlock == null || direction == null) {
            return false;
        }
        
        try {
            // Use legacy MaterialData approach
            Sign signData = new Sign(signBlock.getType());
            signData.setFacingDirection(direction);
            
            // Update the block data
            BlockState state = signBlock.getState();
            state.setType(signData.getItemType());
            state.setData(signData);
            state.update(true);
            return true;
        } catch (Exception e) {
            if (Shop.getPlugin() != null) {
                Shop.getPlugin().getShopLogger().debug("Failed to set legacy sign facing: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Converts a regular sign to a wall sign and sets its facing direction.
     * Uses legacy material naming and MaterialData.
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
        
        // Convert to wall sign using legacy material naming
        String wallSignType;
        if (currentType.equals("SIGN") || currentType.equals("SIGN_POST")) {
            wallSignType = "WALL_SIGN"; // Legacy naming
        } else {
            wallSignType = currentType.replaceAll("_SIGN$", "_WALL_SIGN");
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
     * Gets block data using legacy MaterialData API.
     * 
     * @param block The block to get data from
     * @return MaterialData object representing the block data
     */
    public static Object getBlockData(Block block) {
        if (block == null) {
            return null;
        }
        
        return block.getState().getData();
    }
    
    /**
     * Sets block data using legacy MaterialData API.
     * 
     * @param block The block to set data on
     * @param data The MaterialData to set
     * @return true if successful
     */
    public static boolean setBlockData(Block block, Object data) {
        if (block == null || data == null) {
            return false;
        }
        
        try {
            if (data instanceof MaterialData) {
                MaterialData materialData = (MaterialData) data;
                BlockState state = block.getState();
                state.setType(materialData.getItemType());
                state.setData(materialData);
                state.update(true);
                return true;
            }
        } catch (Exception e) {
            if (Shop.getPlugin() != null) {
                Shop.getPlugin().getShopLogger().debug("Failed to set legacy block data: " + e.getMessage());
            }
        }
        
        return false;
    }
    
    /**
     * Light blocks are not available in legacy versions (MC ≤1.12.2).
     * This method always returns false for compatibility.
     * 
     * @param block The block (ignored)
     * @param lightLevel The light level (ignored)
     * @return false (light blocks not supported in legacy versions)
     */
    public static boolean setLightBlock(Block block, int lightLevel) {
        // Light blocks don't exist in legacy versions
        return false;
    }
    
    /**
     * Checks if a material is a shulker box using manual material name checking.
     * Legacy versions don't have Tag.SHULKER_BOXES, so we check material names.
     * 
     * @param material The material to check
     * @return true if the material is a shulker box
     */
    public static boolean isShulkerBox(Material material) {
        if (material == null) {
            return false;
        }
        
        // Legacy approach: manual material name checking
        String materialName = material.toString();
        return materialName.endsWith("_SHULKER_BOX") || materialName.equals("SHULKER_BOX");
    }
} 