package com.snowgears.shop.util;

import com.snowgears.shop.Shop;

public class NMSBullshitHandler {

    private Shop plugin;
    //for use with version and nms stuff
    private String nmsVersionString;
    private double serverVersion;

    private Class<?> craftItemStackClass;
    private Class<?> craftWorldClass;
    private Class<?> craftPlayerClass;
    private Class<?> enumDirectionClass;

    public NMSBullshitHandler(Shop plugin){
        this.plugin = plugin;
        init();
    }

    public void init() {
        String mcVersion = plugin.getServer().getClass().getPackage().getName();
        nmsVersionString = mcVersion.substring(mcVersion.lastIndexOf('.') + 2);

        serverVersion = Double.parseDouble(plugin.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3].replace("_R", ".").replaceAll("[rvV_]*", ""));

        try {
            String versionString = ("v" + String.valueOf(this.getServerVersion()).charAt(0) + "_" + String.valueOf(this.getServerVersion()).substring(1)).replace(".", "_R");
            this.craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + versionString + ".inventory.CraftItemStack");
            if (Math.floor(this.getServerVersion()) >= 117.0D) {
                this.craftWorldClass = Class.forName("org.bukkit.craftbukkit." + versionString + ".CraftWorld");
                this.craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + versionString + ".entity.CraftPlayer");
                //this.craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + versionString + ".entity.CraftPlayer");

                // java.lang.ClassNotFoundException: net.minecraft.server.v1_17_R1.ItemStack
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("[Shop] [ERROR] Unable to retrieve a NMS class used for NBT data.");
        }
    }

    public String getNmsVersion(){
        return nmsVersionString;
    }

    public double getServerVersion() {
        return this.serverVersion;
    }


    public Class<?> getCraftItemStackClass() {
        return craftItemStackClass;
    }

    public Class<?> getCraftWorldClass() {
        return craftWorldClass;
    }

    public Class<?> getCraftPlayerClass() {
        return craftPlayerClass;
    }

}
