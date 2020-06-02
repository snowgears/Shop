package com.snowgears.shop;

import java.util.Scanner;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class CreationCheck {
private YamlConfiguration listfile;
private boolean whitelist;

public CreationCheck(YamlConfiguration from,boolean wh) {
	if(from==null) return;
	listfile=from;
	whitelist=wh;
}

public boolean testfor(ItemStack i, double price, int amount) {
	if(i == null) return false;
	ConfigurationSection section = listfile.getConfigurationSection("itemListing");
	for(String sec:section.getKeys(false)) {
		System.out.println(sec);
		ConfigurationSection testfor=listfile.getConfigurationSection("itemListing."+sec);
			//material check
			if(testfor.getString("material")==null) 
				continue;
			if(i.getType() != Material.getMaterial(testfor.getString("material"))) continue;
			
			//lorecheck
			if(testfor.get("lore-contains")!=null) { 
				if(i.hasItemMeta()) {
					if(i.getItemMeta().hasLore()) {
						if(!ChatColor.stripColor(i.getItemMeta().getLore().toString()).toLowerCase().contains(testfor.getString("lore-contains").toLowerCase())){
							continue;
						}
				} else continue;
				} else continue;
			}
			//namecheck
			if(testfor.get("name-contains")!=null) { 
				if(i.hasItemMeta()) {
					if(i.getItemMeta().hasDisplayName()) {
						if(!ChatColor.stripColor(i.getItemMeta().getDisplayName()).toLowerCase().contains(testfor.getString("name-contains").toLowerCase())){
							continue;
						}
				} else continue;
				} else continue;
			}
			//ListType is important
			if(testfor.getString("ListType") != null) {
				String type=testfor.getString("ListType");
				//whitelist
				if(type.equalsIgnoreCase("WhiteList")) {return false;}

				//blacklist
				else if(type.equalsIgnoreCase("BlackList")) {return true;}

				//pricechecks
				else if(type.equalsIgnoreCase("Price")) {
					Double rat=price/amount;
					if(testfor.get("pricemin")==null&&testfor.get("pricemax")==null) {
						System.out.println("pricemin or pricemax for "+sec+". skipping section");
						break;
					}
					if(testfor.get("pricemin")!=null) {
						String min=testfor.getString("pricemin");
						Scanner t=new Scanner(min);
						int minamount=1;
						double minprice=1;
						if(t.hasNextDouble()) {
							minprice=t.nextDouble();
						}else {
							System.out.println("pricemin broken for "+sec);
							break;}
						if(t.hasNextInt()) {
							minamount=t.nextInt();
						}else {
							System.out.println("pricemin broken for "+sec);
							break;}
						t.close();
						Double minrat=minprice/minamount;
						if(rat<=minrat) {
							return true;}
				}
				if(testfor.get("pricemax")!=null) {
					String min=testfor.getString("pricemax");
					Scanner t=new Scanner(min);
					int maxamount=1;
					double maxprice=1;
					if(t.hasNextDouble()) {
						maxprice=t.nextDouble();
					}else {
						System.out.println("pricemax broken for "+sec);
						break;}
					if(t.hasNextInt()) {
						maxamount=t.nextInt();
					}else {
						System.out.println("pricemax broken for "+sec);
						break;}
					t.close();
					Double maxrat=maxprice/maxamount;
					if(rat>=maxrat) {
						return true;}
				}
					
				}
				if(whitelist)return true;
				else return false;
				}else {
				System.out.println("Problem with ListType in "+sec+". Skipping section.");
				break;
			}
			
	}


	if(whitelist) return true;
	return false;
}
}