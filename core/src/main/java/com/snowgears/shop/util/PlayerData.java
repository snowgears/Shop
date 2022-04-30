package com.snowgears.shop.util;


import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

public class PlayerData {

    private UUID playerUUID;
    private Location shopSignLocation;
    private GameMode oldGameMode;
    private boolean guiSearch;
    private boolean allowFlight;
    private boolean isFlying;

    public PlayerData(Player player, Location shopSignLocation, boolean guiSearch) {
        this.playerUUID = player.getUniqueId();
        this.shopSignLocation = shopSignLocation;
        this.oldGameMode = player.getGameMode();
        this.guiSearch = guiSearch;
        this.allowFlight = player.getAllowFlight();
        this.isFlying = player.isFlying();
        saveToFile();
    }

    private PlayerData(UUID playerUUID, GameMode oldGameMode, Location shopSignLocation, boolean allowFlight, boolean isFlying, boolean guiSearch) {
        this.playerUUID = playerUUID;
        this.oldGameMode = oldGameMode;
        this.shopSignLocation = shopSignLocation;
        this.allowFlight = allowFlight;
        this.isFlying = isFlying;
        this.guiSearch = guiSearch;
    }

    private void saveToFile(){
        try {
            File fileDirectory = new File(Shop.getPlugin().getDataFolder(), "Data");

            File creativeDirectory = new File(fileDirectory, "LimitedCreative");
            if (!creativeDirectory.exists())
                creativeDirectory.mkdir();

            File playerDataFile = new File(creativeDirectory, this.playerUUID.toString() + ".yml");
            if (!playerDataFile.exists())
                playerDataFile.createNewFile();

            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);

            config.set("player.UUID", this.playerUUID.toString());
            config.set("player.gamemode", this.oldGameMode.toString());
            config.set("player.shopSignLocation", locationToString(this.shopSignLocation));
            config.set("player.allowFlight", allowFlight);
            config.set("player.isFlying", isFlying);
            config.set("player.guiSearch", guiSearch);

            config.save(playerDataFile);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static PlayerData loadFromFile(Player player){
        if(player == null)
            return null;
        File fileDirectory = new File(Shop.getPlugin().getDataFolder(), "Data");

        File creativeDirectory = new File(fileDirectory, "LimitedCreative");
        if (!creativeDirectory.exists())
            creativeDirectory.mkdir();

        File playerDataFile = new File(creativeDirectory, player.getUniqueId().toString() + ".yml");

        if (playerDataFile.exists()) {

            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);

            UUID uuid = UUID.fromString(config.getString("player.UUID"));
            GameMode gamemode = GameMode.valueOf(config.getString("player.gamemode"));
            Location signLoc = locationFromString(config.getString("player.shopSignLocation"));
            boolean allowFlight = config.getBoolean("player.allowFlight");
            boolean isFlying = config.getBoolean("player.isFlying");
            boolean guiSearch = config.getBoolean("player.guiSearch");

            PlayerData data = new PlayerData(uuid, gamemode, signLoc, allowFlight, isFlying, guiSearch);
            return data;
        }
        return null;
    }

    //this method is called when the player data is returned to the controlling player
    public void apply() {
        Player player = Bukkit.getPlayer(this.playerUUID);
        if(player == null)
            return;
        player.setGameMode(oldGameMode);
        player.setAllowFlight(allowFlight);
        player.setFlying(isFlying);
        //System.out.println("[Shop] set old gamemode to "+oldGameMode.toString());
        removeFile();
    }

    private boolean removeFile(){
        File fileDirectory = new File(Shop.getPlugin().getDataFolder(), "Data");
        File creativeDirectory = new File(fileDirectory, "LimitedCreative");
        File playerDataFile = new File(creativeDirectory, this.playerUUID.toString() + ".yml");

        if (!playerDataFile.exists()) {
            return false;
        }
        else{
            playerDataFile.delete();
            return true;
        }
    }

    private static String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static Location locationFromString(String locString) {
        String[] parts = locString.split(",");
        return new Location(Bukkit.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Location getShopSignLocation() {
        return shopSignLocation;
    }

    public AbstractShop getShop() {
        return Shop.getPlugin().getShopHandler().getShop(shopSignLocation);
    }

    public GameMode getOldGameMode() {
        return oldGameMode;
    }

    public boolean isGuiSearch(){
        return guiSearch;
    }
}
