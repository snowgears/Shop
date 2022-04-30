package com.snowgears.shop.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;

import java.util.HashMap;
import java.util.Map;

public class ItemNameUtil {

    private Map<String, String> names = new HashMap<String, String>();

    public ItemNameUtil() {

        //no longer reading from items.tsv file as all item ids are deprecated. May revisit later with material names but removing for now
//        try {
//            File itemNameFile = new File(Shop.getPlugin().getDataFolder(), "items.tsv");
//            BufferedReader reader = new BufferedReader(new FileReader(itemNameFile));
//
//            String row;
//                while ((row = reader.readLine()) != null) {
//                    row = row.trim();
//                    if (row.isEmpty())
//                        continue;
//                    String[] cols = row.split("\t");
//                    String name = cols[2];
//                    String id = cols[0];
//                    String metadata = cols[1];
//                    //String idAndMetadata = metadata.equals("0") ? id : (id + ":" + metadata);
//                    String idAndMetadata = id+":"+metadata;
//                    names.put(idAndMetadata, name);
//                }
//            } catch (IOException e) {
//                System.out.println("[Shop] ERROR! Unable to initialize item name buffer reader. Using default spigot item names.");
//                return;
//            }
    }

    public String getName(ItemStack item){
        if(item == null)
            return "";

        if(item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null && !item.getItemMeta().getDisplayName().isEmpty())
            return item.getItemMeta().getDisplayName();

        if(item.getItemMeta() != null && item.getItemMeta() instanceof PotionMeta){
            String name = getName(item.getType());
            PotionData data = null;
            try {
                data = ((PotionMeta) item.getItemMeta()).getBasePotionData();

                name += " {" + UtilMethods.capitalize(data.getType().name().replace("_", " ").toLowerCase());
                if (data.isUpgraded())
                    name += " 2";
                if (data.isExtended())
                    name += " - extended";
                name += "}";
                return name;
            } catch(NoSuchMethodError e){ } //1.8 doesn't have getBasePotionData() method
        }
//
//        String format = ""+item.getTypeId()+":"+item.getData().getData();
//        String name = names.get(format);
//        if(name != null)
//            return name;
//        return getBackupName(item.getType());

        return getName(item.getType());
    }

    public String getName(Material material){
        ItemStack is = new ItemStack(material);
        String name = null;
        try {
            is.getItemMeta().getLocalizedName();
        } catch (NoSuchMethodError e) { } //1.8 doesn't have getLocalizedName() method
        if(is.getItemMeta() != null)
            name = is.getItemMeta().getLocalizedName();
        if(name == null || name.isEmpty()){
            return UtilMethods.capitalize(is.getType().name().replace("_", " ").toLowerCase());
        }
        return name;
    }

//    private TranslatableComponent getTranslatableName(ItemStack item){
//        String key = item.getType().getKey().toString();
//        key.replaceAll("^minecraft:", "");
//        String itemKey = (item.getType().isItem() ? "item" : "block") + ".minecraft." + key;
//        System.out.println(itemKey); //like 'item.swordDiamond.name' or 'block.minecraft.stone'
//        TranslatableComponent component = new TranslatableComponent(itemKey);
//        return component;
//    }
}
