package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

public class PlayerExperience {

    private UUID playerUUID;
    private int experience;

    public PlayerExperience(Player player) {
        this.playerUUID = player.getUniqueId();
        this.experience = player.getTotalExperience();
        saveToFile();
    }

    public PlayerExperience(UUID playerUUID, int experience) {
        this.playerUUID = playerUUID;
        this.experience = experience;
    }

    private void saveToFile(){
        try {
            File fileDirectory = new File(Shop.getPlugin().getDataFolder(), "Data");

            File experienceDirectory = new File(fileDirectory, "OfflineExperience");
            if (!experienceDirectory.exists())
                experienceDirectory.mkdir();

            File playerDataFile = new File(experienceDirectory, this.playerUUID.toString() + ".yml");
            if (!playerDataFile.exists())
                playerDataFile.createNewFile();

            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);

            config.set("player.UUID", this.playerUUID.toString());
            config.set("player.experience", this.experience);

            config.save(playerDataFile);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static PlayerExperience loadFromFile(OfflinePlayer player){
        if(player == null)
            return null;
        File fileDirectory = new File(Shop.getPlugin().getDataFolder(), "Data");

        File creativeDirectory = new File(fileDirectory, "OfflineExperience");
        if (!creativeDirectory.exists())
            creativeDirectory.mkdir();

        File playerDataFile = new File(creativeDirectory, player.getUniqueId().toString() + ".yml");

        if (playerDataFile.exists()) {

            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);

            UUID uuid = UUID.fromString(config.getString("player.UUID"));
            int experience = config.getInt("player.experience");

            PlayerExperience data = new PlayerExperience(uuid, experience);
            return data;
        }
        return null;
    }

    //this method is called when the player data is returned to the controlling player
    public void apply() {
        Player player = Bukkit.getPlayer(this.playerUUID);
        if(player == null)
            return;
        player.setTotalExperience(this.experience);
        //System.out.println("[Shop] set old gamemode to "+oldGameMode.toString());
        removeFile();
    }

    private boolean removeFile(){
        File fileDirectory = new File(Shop.getPlugin().getDataFolder(), "Data");
        File creativeDirectory = new File(fileDirectory, "OfflineExperience");
        File playerDataFile = new File(creativeDirectory, this.playerUUID.toString() + ".yml");

        if (!playerDataFile.exists()) {
            return false;
        }
        else{
            playerDataFile.delete();
            return true;
        }
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public int getExperience() {
        return experience;
    }

    public void removeExperienceAmount(int amount) {
        experience = experience - amount;
        saveToFile();
    }

    public void addExperienceAmount(int amount) {
        experience = experience + amount;
        saveToFile();
    }
}
