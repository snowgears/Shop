package com.snowgears.shop.listener;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.CreationCheck;
import com.snowgears.shop.GambleShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import com.snowgears.shop.util.WorldGuardHook;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Hopper;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;


public class ShopListener implements Listener {
    private final Shop plugin;
    private HashMap<String, Integer> shopBuildLimits = new HashMap<>();

    public ShopListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.usePerms()) {
            Player player = event.getPlayer();
            int buildPermissionNumber = -1;
            for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
                if (permInfo.getPermission().contains("shop.buildlimit.")) {
                    try {
                        int tempNum = Integer.parseInt(permInfo.getPermission().substring(permInfo.getPermission().lastIndexOf(".") + 1));
                        if (tempNum > buildPermissionNumber) {
                            buildPermissionNumber = tempNum;
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
            if (buildPermissionNumber == -1)
                shopBuildLimits.put(player.getName(), 10000);
            else
                shopBuildLimits.put(player.getName(), buildPermissionNumber);
        }
    }

    public int getBuildLimit(Player player) {
        if (shopBuildLimits.get(player.getName()) != null)
            return shopBuildLimits.get(player.getName());
        return Integer.MAX_VALUE;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDisplayChange(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return; // off hand packet, ignore.
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            if (clicked == null) {
                return;
            }

            if (Tag.WALL_SIGNS.isTagged(clicked.getType())) {
                AbstractShop shop = plugin.getShopHandler().getShop(clicked.getLocation());
                if (shop == null || !shop.isInitialized())
                    return;
                Player player = event.getPlayer();

                //player clicked another player's shop sign
                if (!shop.getOwnerName().equals(player.getName())) {
                    if (!player.isSneaking())
                        return;

                    //player has permission to change another player's shop display
                    if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                        shop.getDisplay().cycleType();
                        event.setCancelled(true);
                    }
                    //player clicked own shop sign
                } else {
                    if (plugin.usePerms() && !player.hasPermission("shop.setdisplay")) {
                        return;
                    }

                    shop.getDisplay().cycleType();
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShopOpen(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            if (clicked == null) {
                return;
            }

            if (plugin.getShopHandler().isChest(clicked)) {
                if (event.getHand() == EquipmentSlot.OFF_HAND) {
                    return; // off hand packet, ignore.
                }

                Player player = event.getPlayer();
                AbstractShop shop = plugin.getShopHandler().getShopByChest(clicked);
                if (shop == null)
                    return;

                boolean canUseShopInRegion = true;
                try {
                    canUseShopInRegion = WorldGuardHook.canUseShop(player, shop.getSignLocation());
                } catch (NoClassDefFoundError ignore) {
                }

                //check that player can use the shop if it is in a WorldGuard region
                if (!canUseShopInRegion) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "regionRestriction", null, player));
                    event.setCancelled(true);
                    return;
                }

                if ((!plugin.getShopHandler().isChest(shop.getChestLocation().getBlock())) || !Tag.WALL_SIGNS.isTagged(shop.getSignLocation().getBlock().getType())) {
                    shop.delete();
                    return;
                }

                if (shop.getChestLocation().getBlock().getType() == Material.ENDER_CHEST) {
                    if (player.isSneaking()) {
                        shop.printSalesInfo(player);
                        event.setCancelled(true);
                    }
                    return;
                }

                //player is sneaking and clicks a chest of a shop
                if (player.isSneaking()) {
                    if (!Tag.WALL_SIGNS.isTagged(player.getInventory().getItemInMainHand().getType())) {
                        shop.printSalesInfo(player);
                        event.setCancelled(true);
                        return;
                    }
                }
                //non-owner is trying to open shop
                if (!shop.getOwnerName().equals(player.getName())) {
                    if ((plugin.usePerms() && player.hasPermission("shop.operator")) || player.isOp()) {
                        if (shop.isAdmin()) {
                            if (shop.getType() == ShopType.GAMBLE) {
                                //allow gamble shops to be opened by operators
                                return;
                            }
                            event.setCancelled(true);
                            shop.printSalesInfo(player);
                        } else {
                            player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "opOpen", shop, player));

                        }
                    } else {
                        event.setCancelled(true);
                        shop.printSalesInfo(player);
                        //player.sendMessage(ChatColor.RED + "You do not have access to open this shop.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onShopClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Chest) {
            Chest chest = (Chest) holder;
            AbstractShop shop = plugin.getShopHandler().getShopByChest(chest.getBlock());
            if (shop == null)
                return;
            if (shop.getType() == ShopType.GAMBLE) {
                ((GambleShop) shop).shuffleGambleItem();
            }
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        //save all potential shop blocks (for sake of time during explosion)
        Iterator<Block> blockIterator = event.blockList().iterator();
        AbstractShop shop = null;
        while (blockIterator.hasNext()) {

            Block block = blockIterator.next();
            if (Tag.WALL_SIGNS.isTagged(block.getType())) {
                shop = plugin.getShopHandler().getShop(block.getLocation());
            } else if (plugin.getShopHandler().isChest(block)) {
                shop = plugin.getShopHandler().getShopByChest(block);
            }

            if (shop != null) {
                blockIterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void signDetachCheck(BlockPhysicsEvent event) {
        Block b = event.getBlock();
        if (UtilMethods.isWallSign(b.getType())) { // Tags are too slow here, lets try this
            if (plugin.getShopHandler() != null) {
                AbstractShop shop = plugin.getShopHandler().getShop(b.getLocation());
                if (shop != null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    //prevent hoppers from stealing inventory from shops
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        InventoryHolder holder = event.getSource().getHolder();
        if (holder instanceof Hopper) {
            return; // Short circuit hoppers since they trigger so often
        }

        if (event.getDestination().getType() == InventoryType.PLAYER) {
            return; // Short circuit moving to player inventories
        }

        AbstractShop shop = null;
        if (holder instanceof Chest) {
            shop = plugin.getShopHandler().getShopByChest(((Chest) holder).getBlock());
        } else if (holder instanceof DoubleChest) {
            shop = plugin.getShopHandler().getShopByChest(((DoubleChest) holder).getLocation().getBlock());
        } else if (holder instanceof ShulkerBox) {
            shop = plugin.getShopHandler().getShopByChest(((ShulkerBox) holder).getBlock());
        } else if (holder instanceof Barrel) {
            shop = plugin.getShopHandler().getShopByChest(((Barrel) holder).getBlock());
        }

        if (shop != null) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onShopCreate(PlayerInitializeShopEvent event) {
    	CreationCheck ItemList= Shop.getPlugin().getCreationCheck();
    	AbstractShop shop=event.getShop();
    	if(ItemList.testfor(event.getPlayer().getInventory().getItemInMainHand(), shop.getPrice(), shop.getAmount(),shop.getType())) {
    		event.setCancelled(true);
    		shop.delete();
    		
    		Player player=event.getPlayer();
    		String message=ShopMessage.getMessage("interactionIssue", "createIssue", null, player);
    		if(message.isEmpty())
    			message=ChatColor.GOLD+"Shop creation failed";
    		player.sendMessage(message);
    	}
    		
    	
    }
    //===================================================================================//
    //              ENDER CHEST HANDLING EVENTS
    //===================================================================================//

    @EventHandler
    public void onCloseEnderChest(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
                if (plugin.useEnderChests()) {
                    plugin.getEnderChestHandler().saveInventory(player, event.getInventory());
                }
            }
        }
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (!plugin.useEnderChests()) {
            return;
        }

        final Player player = event.getPlayer();
        final Inventory inv = plugin.getEnderChestHandler().getInventory(player);

        new BukkitRunnable() {
            public void run() {
                if (inv != null) {
                    player.getEnderChest().setContents(inv.getContents());
                    plugin.getEnderChestHandler().saveInventory(player, inv);
                }
            }
        }.runTaskLater(plugin, 2L);
    }
}