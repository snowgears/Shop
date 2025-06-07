package com.snowgears.shop.util;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.potion.PotionEffect;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

/**
 * Modern implementation using Java 17+ and latest Minecraft APIs.
 * This uses direct API calls instead of reflection for better performance and readability.
 */
public class ModernItemDisplayHandler implements ItemDisplayHandler {
    
    @Override
    public void addArmorDisplayInfo(ItemStack item, TextComponent component) {
        if (item.getItemMeta() instanceof ArmorMeta) {
            ArmorMeta armorMeta = (ArmorMeta) item.getItemMeta();
            if (armorMeta.hasTrim()) {
                ArmorTrim trim = armorMeta.getTrim();
                String materialName = UtilMethods.translate(trim.getMaterial().getTranslationKey()).replace(" Material", "");
                String patternName = UtilMethods.translate(trim.getPattern().getTranslationKey()).replace(" Armor Trim", "");
                component.addExtra(" [" + patternName + " (" + materialName + ")]");
            }
        }
    }
    
    @Override
    public void addPotionDisplayInfo(ItemStack item, TextComponent component) {
        if (item.getItemMeta() instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
            
            // Use modern base potion type API
            if (potionMeta.hasBasePotionType()) {
                List<PotionEffect> effects = potionMeta.getBasePotionType().getPotionEffects();
                component.addExtra(getPotionEffectsComponent(effects));
            }
            
            // Add custom effects
            List<PotionEffect> customEffects = potionMeta.getCustomEffects();
            if (!customEffects.isEmpty()) {
                component.addExtra(getPotionEffectsComponent(customEffects));
            }
        }
    }
    
    @Override
    public void addEnchantmentDisplayInfo(ItemStack item, TextComponent component) {
        if (item.getEnchantments().size() > 0) {
            Map<Enchantment, Integer> enchantsMap = item.getEnchantments();
            component.addExtra(" [");
            int i = 0;
            for (Map.Entry<Enchantment, Integer> entry : enchantsMap.entrySet()) {
                // Use modern translation key API
                try {
                    component.addExtra(new TranslatableComponent(entry.getKey().getTranslationKey()));
                } catch (Error |Exception e) {
                    component.addExtra(entry.getKey().toString());
                }
                component.addExtra(UtilMethods.formatRomanNumerals(entry.getValue()));
                i++;
                if (i != enchantsMap.size()) {
                    component.addExtra(", ");
                } else {
                    component.addExtra("]");
                }
            }
        }
    }
    
    @Override
    public void addOminousBottleDisplayInfo(ItemStack item, TextComponent component) {
        if (item.getItemMeta() instanceof OminousBottleMeta) {
            OminousBottleMeta ominousMeta = (OminousBottleMeta) item.getItemMeta();
            int level = ominousMeta.hasAmplifier() ? ominousMeta.getAmplifier() + 1 : 1;
            component.addExtra(" [Bad Omen" + UtilMethods.formatRomanNumerals(level) + "]");
        }
    }
    
    @Override
    public void addMusicInstrumentDisplayInfo(ItemStack item, TextComponent component) {
        if (MaterialUtil.of("GOAT_HORN") == null) return;

        if (item.getType().name().equals("GOAT_HORN") && item.getItemMeta() instanceof MusicInstrumentMeta) {
            MusicInstrumentMeta instrumentMeta = (MusicInstrumentMeta) item.getItemMeta();
            if (instrumentMeta.getInstrument() != null) {
                String instrumentKey = instrumentMeta.getInstrument().getKey().getKey();
                String soundType = instrumentKey.replace("_goat_horn", "");
                component.addExtra(" [Sound: " + UtilMethods.capitalize(soundType) + "]");
            } else {
                component.addExtra(" [Sound: Unknown]");
            }
        }
    }
    
    private TextComponent getPotionEffectsComponent(List<PotionEffect> effects) {
        TextComponent formattedEffects = new TextComponent("");
        int numEffects = effects.size();
        if (numEffects == 0) return formattedEffects;
        
        formattedEffects.addExtra(" (");
        for (int i = 0; i < numEffects; i++) {
            PotionEffect effect = effects.get(i);
            
            // Use modern translation key API
            formattedEffects.addExtra(new TranslatableComponent(effect.getType().getTranslationKey()));
            
            if (effect.getAmplifier() > 0) {
                formattedEffects.addExtra(UtilMethods.formatRomanNumerals(effect.getAmplifier() + 1));
            }
            
            // Add duration for non-instant effects
            boolean isInstantEffect = effect.getType().equals(org.bukkit.potion.PotionEffectType.INSTANT_HEALTH) ||
                                    effect.getType().equals(org.bukkit.potion.PotionEffectType.INSTANT_DAMAGE);
            
            if (effect.getDuration() > 0 && !isInstantEffect) {
                formattedEffects.addExtra(UtilMethods.formatTickTime(effect.getDuration()));
            }
            
            if (i < numEffects - 1) {
                formattedEffects.addExtra(", ");
            }
        }
        formattedEffects.addExtra(")");
        return formattedEffects;
    }
} 