package com.snowgears.shop.display;

import com.snowgears.shop.Shop;
import com.snowgears.shop.util.ArmorStandData;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Legacy-compatible display handler for Java 8 and older Minecraft versions.
 * This version removes all modern NMS dependencies and provides safe fallbacks.
 */
public class DisplayDisabled extends AbstractDisplay {

    public DisplayDisabled(Location shopSignLocation) {
        super(shopSignLocation);
        if (!Shop.loggedDisplayDisabledWarning) {
            Shop.getPlugin().getLogger().warning("[DisplayDisabled] Display is disabled for legacy compatibility!");
            Shop.getPlugin().getLogger().warning("[DisplayDisabled] Running on Java 8/Legacy Minecraft version - display features unavailable.");
            Shop.getPlugin().getLogger().warning("[DisplayDisabled] Shop will function normally without display features.");
            Shop.loggedDisplayDisabledWarning = true;
        }
    }

    @Override
    public boolean isEnabled() { 
        return false; 
    }

    @Override
    protected void spawnItemPacket(Player player, ItemStack is, Location location) {
        Shop.getPlugin().getLogger().debug("[Legacy] Display disabled - item packet not sent");
    }

    @Override
    protected void spawnArmorStandPacket(Player player, ArmorStandData armorStandData, String text) {
        Shop.getPlugin().getLogger().debug("[Legacy] Display disabled - armor stand packet not sent");
    }

    @Override
    protected void spawnItemFramePacket(Player player, ItemStack is, Location location, BlockFace facing, boolean isGlowing){
        Shop.getPlugin().getLogger().debug("[Legacy] Display disabled - item frame packet not sent");
    }

    @Override
    public void removeDisplayEntities(Player player, boolean onlyDisplayTags) {
        Shop.getPlugin().getLogger().debug("[Legacy] Display disabled - removeDisplayEntities not called");
    }

    @Override
    public String getItemNameNMS(ItemStack item) {
        Shop.getPlugin().getLogger().debug("[Legacy] Display disabled - using basic item name");
        if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        } else {
            // Legacy-safe way to get item name
            return item.getType().name().toLowerCase().replace("_", " ");
        }
    }
} 