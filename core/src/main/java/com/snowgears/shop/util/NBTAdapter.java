package com.snowgears.shop.util;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import com.snowgears.shop.Shop;

public class NBTAdapter {
    private Shop plugin;
    private boolean useNBTAPIPlugin = false;
    private int errorCount = 0;
    
    public NBTAdapter(Shop shop) {
        plugin = shop;
        // Check if NBTAPI plugin is installed
        if (Bukkit.getPluginManager().getPlugin("NBTAPI") != null) {
            plugin.getShopLogger().helpful("[NBTAdapter] NBTAPI plugin is installed, using NBTAPI");
            useNBTAPIPlugin = true;
        } else {
            plugin.getShopLogger().helpful("[NBTAdapter] NBTAPI plugin is not installed, using built in NBTAPI library");
            useNBTAPIPlugin = false;
        }
    }

    public boolean haveErrorsOccured() {
        return errorCount > 0;
    }

    public String getNBTforItem(ItemStack item) {
        try {
            if (useNBTAPIPlugin) {
                return de.tr7zw.nbtapi.NBT.itemStackToNBT(item).toString();
            } else {
                return de.tr7zw.changeme.nbtapi.NBT.itemStackToNBT(item).toString();
            }
        } catch (Exception e) { handleException(e.getMessage()); }
        catch (Error e) { handleException(e.getMessage()); }
        // Error while getting NBT for item, return a dummy NBT string
        return "{count:1,id:\"minecraft:dirt\"}";
    }

    public Inventory getEnderChestNBT(File playerFile) {
        if (useNBTAPIPlugin) {
            plugin.getShopLogger().severe("[NBTAdapter] EnderChestNBT reading is not supported with the NBTAPI plugin! Attempting to fallback to built in NBTAPI library.");
        }
        return builtIn_getEnderChestNBT(playerFile);
    }

    public Inventory saveEnderChestNBT(File playerFile, Inventory enderInventory) {
        if (useNBTAPIPlugin) {
            plugin.getShopLogger().severe("[NBTAdapter] EnderChestNBT saving is not supported with the NBTAPI plugin! Attempting to fallback to built in NBTAPI library.");
        }
        return builtIn_saveEnderChestNBT(playerFile, enderInventory);
    }

    public Inventory builtIn_getEnderChestNBT(File playerFile) {
        try {
            //get NBT compounds of EnderItems (enderchest items in a list)
            de.tr7zw.changeme.nbtapi.NBTFile nbtFile = new de.tr7zw.changeme.nbtapi.NBTFile(playerFile);
            de.tr7zw.changeme.nbtapi.NBTCompoundList enderListCompounds = nbtFile.getCompoundList("EnderItems");
            de.tr7zw.changeme.nbtapi.NBTCompound[] enderNBTCompounds = enderListCompounds.stream().toArray(de.tr7zw.changeme.nbtapi.NBTCompound[]::new);

            //create a new temporary inventory out of that EnderItems list
            Inventory tempEnderEnv = Bukkit.createInventory(null, 27, "TempEnderInv");

            for(int i=0; i < enderNBTCompounds.length; i++){
                ItemStack is = de.tr7zw.changeme.nbtapi.NBTItem.convertNBTtoItem(enderNBTCompounds[i]);
                int slot = enderListCompounds.get(i).getByte("Slot");
                tempEnderEnv.setItem(slot, is);
                //System.out.println(slot+" - "+is.getType().toString());
            }
            return tempEnderEnv;
        } catch (Exception e) { handleException(e.getMessage()); }
        catch (NoClassDefFoundError e) { handleException(e.getMessage()); }
        catch (Error e) { handleException(e.getMessage()); }
        plugin.getShopLogger().severe("[NBTAdapter] Error while trying to use built in NBTAPI library! Unable to read EnderChest player data!");
        plugin.getShopLogger().severe("[NBTAdapter] Please disable Ender Chests as a supported container, or install the latest version of Shop!");
        plugin.getShopLogger().severe("[NBTAdapter] Shutting Down Shop!");
        Bukkit.getPluginManager().disablePlugin(plugin);
        return null;
    }

    public Inventory builtIn_saveEnderChestNBT(File playerFile, Inventory playerEnderInventory) {
        try {
            //get NBT compounds of EnderItems (enderchest items in a list)
            //get NBT compounds of EnderItems (enderchest items in a list)
            de.tr7zw.changeme.nbtapi.NBTFile nbtFile = new de.tr7zw.changeme.nbtapi.NBTFile(playerFile);
            de.tr7zw.changeme.nbtapi.NBTCompoundList enderListCompounds = nbtFile.getCompoundList("EnderItems");

            //clear the ender item list before populating it again
            enderListCompounds.clear();

            for(int i=0; i < playerEnderInventory.getSize(); i++){
                ItemStack is = playerEnderInventory.getItem(i);
                if(is != null && is.getType() != Material.AIR) {
                    de.tr7zw.changeme.nbtapi.NBTCompound nbtCompound = de.tr7zw.changeme.nbtapi.NBTItem.convertItemtoNBT(is);
                    nbtCompound.setByte("Slot", (byte)i);
                    enderListCompounds.addCompound(nbtCompound);
                    //System.out.println("saving -> " + i + " - " + is.getType().toString());
                }
            }
            nbtFile.save();
        } catch (Exception e) { handleException(e.getMessage()); }
        catch (NoClassDefFoundError e) { handleException(e.getMessage()); }
        catch (Error e) { handleException(e.getMessage()); }
        // Note, should have shut down shop already, but just in case
        plugin.getShopLogger().severe("[NBTAdapter] Error while trying to use built in NBTAPI library! Unable to save EnderChest player data!");
        plugin.getShopLogger().severe("[NBTAdapter] Please disable Ender Chests as a supported container, or install the latest version of Shop!");
        plugin.getShopLogger().severe("[NBTAdapter] Shutting Down Shop!");
        Bukkit.getPluginManager().disablePlugin(plugin);
        return null;
    }

    public void handleException(String message) {
        errorCount++;
        if (errorCount < 5){
            plugin.getShopLogger().severe("[NBTAdapter] Error while trying to use NBTAPI, NBTAPI might not be up to date for your Minecraft version! Some features of Shop will not work as expected!");
            if (useNBTAPIPlugin) {
                plugin.getShopLogger().severe("[NBTAdapter] Shop is attempting to use the installed NBTAPI Plugin.");
            } else {
                plugin.getShopLogger().severe("[NBTAdapter] Shop is using its built in NBTAPI library and may be out of date.");
            }
            plugin.getShopLogger().severe("[NBTAdapter] Please install the latest version of the NBTAPI plugin! https://www.spigotmc.org/resources/nbt-api.7939/");
            plugin.getShopLogger().severe("[NBTAdapter] Error message: " + message);
        }
    }
}
