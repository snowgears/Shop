package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PriceUtil {

    public enum PriceMode { OFF, EXACT, MAXIMUM}

    private PriceMode priceMode;
    private Map<String, Double> prices = new HashMap<>();

    public PriceUtil() {

        String sPriceMode = YamlConfiguration.loadConfiguration(new File(Shop.getPlugin().getDataFolder(), "config.yml")).getString("enforcePrices");
        if(sPriceMode == null) {
            priceMode = PriceMode.OFF;
        }
        else{
            try{
                priceMode = PriceMode.valueOf(sPriceMode);
            } catch (Exception e){
                priceMode = PriceMode.OFF;
            }
        }
        if(priceMode == PriceMode.OFF)
            return;

        try {
            File itemNameFile = new File(Shop.getPlugin().getDataFolder(), "prices.tsv");
            BufferedReader reader = new BufferedReader(new FileReader(itemNameFile));

            String row;
            while ((row = reader.readLine()) != null) {
                row = row.trim();
                if (row.isEmpty())
                    continue;
                String[] cols = row.split("\t");
                String sPrice = cols[2];
                double price = Double.parseDouble(sPrice);
                String id = cols[0];
                String metadata = cols[1];
                //String idAndMetadata = metadata.equals("0") ? id : (id + ":" + metadata);
                String idAndMetadata = id+":"+metadata;
                prices.put(idAndMetadata, price);
            }
        } catch (IOException e) {
            System.out.println("[Shop] ERROR! Unable to initialize prices buffer reader. Turning off price enforcement and ignoring prices.tsv.");
            return;
        }
    }

//    @SuppressWarnings("deprecation")
//    public double getPrice(ItemStack item){
//        if(item == null || priceMode == PriceMode.OFF)
//            return -1;
//
//        String format = ""+item.getTypeId()+":"+item.getData().getData();
//        Double price = prices.get(format);
//        if(price != null)
//            return price;
//        return -1;
//    }

    @SuppressWarnings("deprecation")
    public double getPrice(Material material){
        if(priceMode == PriceMode.OFF)
            return -1;

        String format = ""+material.getId()+":0";
        Double price = prices.get(format);
        if(price != null)
            return price;
        return -1;
    }
}
