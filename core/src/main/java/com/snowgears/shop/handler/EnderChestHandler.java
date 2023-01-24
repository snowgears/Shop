package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTCompoundList;
import de.tr7zw.changeme.nbtapi.NBTFile;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class EnderChestHandler {

    private Shop plugin;
    private HashMap<UUID, Inventory> enderChestInventoryCache = new HashMap<>();
    private ArrayList<UUID> playersSavingInventories = new ArrayList<>();

    public EnderChestHandler(Shop plugin){
        this.plugin = plugin;
    }

    public Inventory getInventory(OfflinePlayer player){
        //if player is online, just return their actual enderchest inventory
        if(player.getPlayer() != null) {
            return player.getPlayer().getEnderChest();
        }

        //check if cache has the inventory stored
        if(enderChestInventoryCache.containsKey(player.getUniqueId())){
            return enderChestInventoryCache.get(player.getUniqueId());
        }

        //if player is offline, load their enderchest inventory with NBT API
        File playerDatafolder = new File("world/playerdata");
        if (!playerDatafolder.isDirectory())
            return null;

        File[] files = playerDatafolder.listFiles();
        if (files == null)
            return null;

        try {
            for (File file : files) {
                if (file.getName().equalsIgnoreCase(player.getUniqueId()+".dat")) {

                    //get NBT compounds of EnderItems (enderchest items in a list)
                    NBTFile nbtFile = new NBTFile(file);
                    NBTCompoundList enderListCompounds = nbtFile.getCompoundList("EnderItems");
                    NBTCompound[] enderNBTCompounds = enderListCompounds.stream().toArray(NBTCompound[]::new);

                    //create a new temporary inventory out of that EnderItems list
                    Inventory tempEnderEnv = Bukkit.createInventory(null, 27, "TempEnderInv");

                    for(int i=0; i < enderNBTCompounds.length; i++){
                        ItemStack is = NBTItem.convertNBTtoItem(enderNBTCompounds[i]);
                        int slot = enderListCompounds.get(i).getByte("Slot");
                        tempEnderEnv.setItem(slot, is);
                        //System.out.println(slot+" - "+is.getType().toString());
                    }

                    enderChestInventoryCache.put(player.getUniqueId(), tempEnderEnv);
                    return tempEnderEnv;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveInventory(final OfflinePlayer player){
        //if player is online, do nothing. Their inventory has already been saved when adding or removing items from it
        Player onlinePlayer = player.getPlayer();
        if(onlinePlayer != null){
           return;
        }

        //if there is no cache, that means the players .dat inventory was never loaded in so no need to save it
        if(!enderChestInventoryCache.containsKey(player.getUniqueId())){
            return;
        }

        Inventory playerEnderInventory = enderChestInventoryCache.get(player.getUniqueId());

        //if player is offline, load their enderchest inventory with NBT API
        File playerDatafolder = new File("world/playerdata");
        if (!playerDatafolder.isDirectory())
            return;

        File[] files = playerDatafolder.listFiles();
        if (files == null)
            return;

        //if player is offline, save NBT data
        try {
            for (File file : files) {
                if (file.getName().equalsIgnoreCase(player.getUniqueId()+".dat")) {

                    //get NBT compounds of EnderItems (enderchest items in a list)
                    NBTFile nbtFile = new NBTFile(file);
                    NBTCompoundList enderListCompounds = nbtFile.getCompoundList("EnderItems");
                    NBTCompound[] enderNBTCompounds = enderListCompounds.stream().toArray(NBTCompound[]::new);

                    //clear the ender item list before populating it again
                    enderListCompounds.clear();

                    for(int i=0; i < playerEnderInventory.getSize(); i++){
                        ItemStack is = playerEnderInventory.getItem(i);
                        if(is != null && is.getType() != Material.AIR) {
                            NBTCompound nbtCompound = NBTItem.convertItemtoNBT(is);
                            nbtCompound.setByte("Slot", (byte)i);
                            enderListCompounds.addCompound(nbtCompound);
                            //System.out.println("saving -> " + i + " - " + is.getType().toString());
                        }
                    }
                    nbtFile.save();

                    //after you have saved, remove the inventory from the cache
                    if(enderChestInventoryCache.containsKey(player.getUniqueId())) {
                        enderChestInventoryCache.remove(player.getUniqueId());
                    }
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

//    public void saveInventory(final OfflinePlayer player, Inventory inv){
//        //do not save enderchest contents of admin shops
//        if(player.getUniqueId().equals(plugin.getShopHandler().getAdminUUID()))
//            return;
//
//        enderChestInventories.put(player.getUniqueId(), inv);
//
//        if(playersSavingInventories.contains(player.getUniqueId()))
//            return;
//
//        BukkitScheduler scheduler = plugin.getServer().getScheduler();
//        scheduler.runTaskLaterAsynchronously(plugin, new Runnable() {
//            @Override
//            public void run() {
//                playersSavingInventories.add(player.getUniqueId());
//                saveInventoryDriver(player);
//            }
//        }, 20L);
//    }

//    private void saveInventoryDriver(OfflinePlayer player){
//        try {
//
//            File fileDirectory = new File(plugin.getDataFolder(), "Data");
//            if (!fileDirectory.exists())
//                fileDirectory.mkdir();
//            File enderDirectory = new File(fileDirectory + "/EnderChests");
//            if (!enderDirectory.exists())
//                enderDirectory.mkdir();
//
//            String owner = player.getName();
//            File currentFile = new File(enderDirectory + "/" + owner + " (" + player.getUniqueId().toString() + ").yml");
//
//            if (!currentFile.exists()) // file doesn't exist
//                currentFile.createNewFile();
//
//            YamlConfiguration config = YamlConfiguration.loadConfiguration(currentFile);
//
//            ItemStack[] contents = this.getInventory(player).getContents();
//            try {
//                config.set("enderchest", contents);
//                config.save(currentFile);
//            } catch (NullPointerException npe) {
//                //somehow an item in the inventory has a null tag or something that can't be serialized
//                ItemStack[] strippedContents = new ItemStack[contents.length];
//                //go through all the items and delete the ones that can't be serialized
//                for(int i=0; i < contents.length; i++){
//                    try {
//                        ItemStack itemStack = contents[i];
//                        itemStack.serialize();
//                        strippedContents[i] = itemStack;
//                    } catch (NullPointerException npeNest) {
//                        strippedContents[i] = null;
//                    }
//                }
//
//                config.set("enderchest", strippedContents);
//                config.save(currentFile);
//            }
//
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//
//        if(playersSavingInventories.contains(player.getUniqueId())){
//            playersSavingInventories.remove(player.getUniqueId());
//        }
//    }

//    private void loadEnderChests() {
//        File fileDirectory = new File(plugin.getDataFolder(), "Data");
//        if (!fileDirectory.exists())
//            fileDirectory.mkdir();
//        File enderDirectory = new File(fileDirectory + "/EnderChests");
//        if (!enderDirectory.exists())
//            enderDirectory.mkdir();
//
//        // load all the yml files from the EnderChest directory
//        for (File file : enderDirectory.listFiles()) {
//            if (file.isFile() && file.getName().endsWith(".yml")) {
//                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
//                loadEnderChestFromConfig(config, file.getName());
//            }
//        }
//    }

//    private void loadEnderChestFromConfig(YamlConfiguration config, String fileName) {
//        if (config.get("enderchest") == null)
//            return;
//
//        UUID owner = uidFromString(fileName);
//        ItemStack[] contents = ((List<ItemStack>) config.get("enderchest")).toArray(new ItemStack[0]);
//        Inventory inv = Bukkit.createInventory(null, InventoryType.ENDER_CHEST);
//        inv.setContents(contents);
//        enderChestInventories.put(owner, inv);
//    }

    private UUID uidFromString(String ownerString) {
        int index = ownerString.indexOf("(");
        int lastIndex = ownerString.indexOf(")");
        String uidString = ownerString.substring(index + 1, lastIndex);
        return UUID.fromString(uidString);
    }
}
