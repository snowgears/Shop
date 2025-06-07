package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayTagOption;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.GambleShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.snowgears.shop.util.CompatibilityUtil;
import com.snowgears.shop.util.MCVersion;

public class DisplayListener implements Listener {

    public Shop plugin;
    private ArrayList<ItemStack> allServerRecipeResults = new ArrayList<>();
    private WrappedTask repeatingViewTask;
    private WrappedTask repeatingDisplayTask;

    public void startRepeatingDisplayViewTask() {
        if (plugin.getDisplayTagOption() == DisplayTagOption.VIEW_SIGN) {
            //run task every 15 ticks
            repeatingViewTask = plugin.getFoliaLib().getScheduler().runTimer(() -> {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player != null) {
                        try {
                            Block block;
                            if (MCVersion.atLeast("1.13")) {
                                block = player.getTargetBlockExact(8);
                            } else {
                                // transparent should be `null` so that only air is transparent
                                block = player.getTargetBlock(null, 8);
                            }
                            if (block != null && block.getType().toString().contains("WALL_SIGN")) {
                                AbstractShop shopObj = plugin.getShopHandler().getShop(block.getLocation());
                                if (shopObj != null) {
                                    shopObj.getDisplay().showDisplayTags(player);
                                }
                            }
                        } catch (IllegalStateException e) {
                            //do nothing, the block iterator missed a block for a player
                        } catch (Exception e) {
                            //do nothing, the block iterator missed a block for a player
                        }
                    }
                }
            }, 1, 15);
        }

        // Run shop displays processing using configurable interval from config
        repeatingDisplayTask = plugin.getFoliaLib().getScheduler().runTimerAsync(() -> {
            // Process players in a staggered fashion to avoid overwhelming the server or client
            List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
            
            for (int i = 0; i < onlinePlayers.size(); i++) {
                Player player = onlinePlayers.get(i);
                if (player != null && player.isOnline()) {
                    // Add a slight staggered delay for each player to distribute packet sending
                    final int playerIndex = i;
                    plugin.getFoliaLib().getScheduler().runLater(() -> {
                        if (player.isOnline()) {
                            plugin.getShopHandler().processShopDisplaysNearPlayer(player);
                        }
                    }, playerIndex); // Stagger by 1 tick per player
                }
            }
        }, 1, (long)(plugin.getDisplayProcessInterval() * 20)); // Convert seconds to ticks
    }

    public DisplayListener(Shop instance) {
        plugin = instance;

        // Load all recipes on server once all other plugins are loaded
        plugin.getFoliaLib().getScheduler().runLater(task -> {
            HashMap<ItemStack, Boolean> recipes = new HashMap();
            Iterator<Recipe> recipeIterator = plugin.getServer().recipeIterator();
            while(recipeIterator.hasNext()) {
                ItemStack result = recipeIterator.next().getResult();
                //System.out.println("[Shop] adding recipe for gamble. "+result.getType().toString()+" (x"+result.getAmount()+")");
                if(result.getAmount() != 0)
                    recipes.put(result, true);
            }
            allServerRecipeResults.addAll(recipes.keySet());
            Collections.shuffle(allServerRecipeResults);
        }, 1);
    }

    public ItemStack getRandomItem(AbstractShop shop){
        if (shop == null)
            return new ItemStack(Material.AIR);


        if(InventoryUtils.isEmpty(shop.getInventory())) {
            int index = new Random().nextInt(allServerRecipeResults.size());
            //TODO maybe later on add random amount between 1-64 depending on item type
            //like you could get 46 stack of dirt but not 46 stack of swords
            return allServerRecipeResults.get(index);
        } else {
            return InventoryUtils.getRandomItem(shop.getInventory());
        }
    }

    @EventHandler
    public void onWaterFlow(BlockFromToEvent event) {
        AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getToBlock().getRelative(BlockFace.DOWN));
        if (shop != null)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getBlock().getRelative(event.getDirection()).getRelative(BlockFace.DOWN));
        if (shop != null && shop.getDisplay().getType() != DisplayType.NONE)
            event.setCancelled(true);

        shop = plugin.getShopHandler().getShopByChest(event.getBlock().getRelative(event.getDirection()).getRelative(BlockFace.UP));
        if (shop != null)
            event.setCancelled(true);

        for(Block pushedBlock : event.getBlocks()){
            shop = plugin.getShopHandler().getShopByChest(pushedBlock.getRelative(event.getDirection()).getRelative(BlockFace.DOWN));
            if (shop != null && shop.getDisplay().getType() != DisplayType.NONE) {
                event.setCancelled(true);
                return;
            }

            shop = plugin.getShopHandler().getShopByChest(pushedBlock.getRelative(event.getDirection()).getRelative(BlockFace.UP));
            if (shop != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Block pulledBlock = event.getBlock().getRelative(event.getDirection().getOppositeFace()).getRelative(event.getDirection().getOppositeFace()).getRelative(BlockFace.UP);
        AbstractShop shop = plugin.getShopHandler().getShopByChest(pulledBlock);
        if (shop != null)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getBlock().getRelative(BlockFace.DOWN));
        if(shop != null){
            if(shop.getDisplay().getType() == null && plugin.getDisplayType() != DisplayType.NONE)
                event.setCancelled(true);
            else if (shop.getDisplay().getType() != null && shop.getDisplay().getType() != DisplayType.NONE)
                event.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onShopInventoryClose(InventoryCloseEvent event) {
        try {
            if(event.getInventory().getHolder() instanceof Container){
                Container container = ((Container)event.getInventory().getHolder());
                AbstractShop shop = plugin.getShopHandler().getShopByChest(container.getBlock());

                if(shop == null) {
                    return;
                }

                shop.updateStock();

                //make sure to set gamble item again if player set it to new custom items
                if(shop.getType() == ShopType.GAMBLE){
                    ((GambleShop)shop).setGambleItem();
                }
            }
            //for some reason, DoubleChest does not extend Container like Chest does
            else if(event.getInventory().getHolder() instanceof DoubleChest){
                DoubleChest doubleChest = ((DoubleChest)event.getInventory().getHolder());
                AbstractShop shop = plugin.getShopHandler().getShopByChest(doubleChest.getLocation().getBlock());

                if(shop == null) {
                    return;
                }

                shop.updateStock();

                //make sure to set gamble item again if player set it to new custom items
                if(shop.getType() == ShopType.GAMBLE){
                    ((GambleShop)shop).setGambleItem();
                }
            }
            //for some reason, EnderChest also does not extend Container
            else if(event.getInventory().getType() == InventoryType.ENDER_CHEST){
                AbstractShop shop;
                if(event.getInventory().getLocation() == null){ return; }
                shop = plugin.getShopHandler().getShopByChest(event.getInventory().getLocation().getBlock());
                if(shop == null) { return; }

                shop.updateStock();

                //make sure to set gamble item again if player set it to new custom items
                if(shop.getType() == ShopType.GAMBLE){
                    ((GambleShop)shop).setGambleItem();
                }
            }
        } catch (NoClassDefFoundError e) {}
    }

    public void cancelRepeatingViewTask(){
        if (repeatingViewTask != null) {
            plugin.getFoliaLib().getScheduler().cancelTask(repeatingViewTask);
        }
        if (repeatingDisplayTask != null) {
            plugin.getFoliaLib().getScheduler().cancelTask(repeatingDisplayTask);
        }
    }
}
