package com.snowgears.shop.display;

import com.snowgears.shop.Shop;
import com.snowgears.shop.util.ArmorStandData;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class DisplayDisabled extends AbstractDisplay {

    public DisplayDisabled(Location shopSignLocation) {
        super(shopSignLocation);
        if (!Shop.loggedDisplayDisabledWarning) {
            Shop.getPlugin().getShopLogger().warning("[DisplayDisabled] Display is disabled! No display features will be used.");
            Shop.getPlugin().getShopLogger().warning("[DisplayDisabled] This could mean there was an error with the server running an unsupported version or unsupported server software.");
            Shop.getPlugin().getShopLogger().warning("[DisplayDisabled] Shop will attempt to function without display features, but some features may not work as expected.");
            Shop.loggedDisplayDisabledWarning = true;
        }
    }

    @Override
    public boolean isEnabled() { return false; }

    @Override
    protected void spawnItemPacket(Player player, ItemStack is, Location location) {
        Shop.getPlugin().getShopLogger().debug("Display is disabled, item packet not sent");
    }

    @Override
    protected void spawnArmorStandPacket(Player player, ArmorStandData armorStandData, String text) {
        Shop.getPlugin().getShopLogger().debug("Display is disabled, armor stand packet not sent");
    }

    @Override
    protected void spawnItemFramePacket(Player player, ItemStack is, Location location, BlockFace facing, boolean isGlowing){
        Shop.getPlugin().getShopLogger().debug("Display is disabled, item frame packet not sent");
    }

    @Override
    public void removeDisplayEntities(Player player, boolean onlyDisplayTags) {
        Shop.getPlugin().getShopLogger().debug("Display is disabled, removeDisplayEntities not called");
    }
}
