package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayTagOption;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DisplayListener implements Listener {

    public Shop plugin = Shop.getPlugin();
    private ArrayList<ItemStack> allServerRecipeResults = new ArrayList<>();
    private int repeatingViewTask;

    public void startRepeatingDisplayViewTask() {
        if (plugin.getDisplayTagOption() == DisplayTagOption.VIEW_SIGN) {
            //run task every 15 ticks
            repeatingViewTask = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player != null) {
                        try {
                            Block block = player.getTargetBlock((Set<Material>)null, 8);
                            if (block.getType() == Material.WALL_SIGN) {
                                AbstractShop shopObj = plugin.getShopHandler().getShop(block.getLocation());
                                if (shopObj != null) {
                                    shopObj.getDisplay().showDisplayTags(player);
                                }
                            }
                        } catch (IllegalStateException e) {
                            //do nothing, the block iterator missed a block for a player
                        }
                    }
                }
            }, 0, 15);
        }
    }

    public DisplayListener(Shop instance) {
        plugin = instance;

        new BukkitRunnable() {
            @Override
            public void run() {
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
            }
        }.runTaskLater(this.plugin, 1); //load all recipes on server once all other plugins are loaded
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

        for(Block pushedBlock : event.getBlocks()){
            shop = plugin.getShopHandler().getShopByChest(pushedBlock.getRelative(event.getDirection()).getRelative(BlockFace.DOWN));
            if (shop != null && shop.getDisplay().getType() != DisplayType.NONE) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getBlock().getRelative(BlockFace.DOWN));
        if (shop != null && shop.getDisplay().getType() != DisplayType.NONE)
            event.setCancelled(true);
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onShopInventoryClose(InventoryCloseEvent event) {
        try {
            if(event.getInventory().getHolder() instanceof Container){
                Container container = ((Container)event.getInventory().getHolder());
                AbstractShop shop = plugin.getShopHandler().getShopByChest(container.getBlock());

                if(shop == null)
                    return;

                //if the sign lines use a variable that requires a refresh (like stock that is dynamically updated), then refresh sign
                if(shop.getSignLinesRequireRefresh())
                    shop.updateSign();

                //set the GUI icon again (in case stock var needs to be updated in the GUI)
                shop.setGuiIcon();
            }
        } catch (NoClassDefFoundError e) {}
    }

    public void cancelRepeatingViewTask(){
        Bukkit.getScheduler().cancelTask(repeatingViewTask);
    }
}
