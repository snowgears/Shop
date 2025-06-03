package com.snowgears.shop.util;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

/**
 * Legacy implementation for Java 8 and older Minecraft versions.
 * This provides basic functionality without the advanced modern features.
 */
public class LegacyItemDisplayHandler implements ItemDisplayHandler {
    
    @Override
    public void addArmorDisplayInfo(ItemStack item, TextComponent component) {
        // ArmorMeta and armor trims not available in legacy versions
        // No-op for legacy builds
    }
    
    @Override
    public void addPotionDisplayInfo(ItemStack item, TextComponent component) {
        if (item.getItemMeta() instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
            
            // Legacy versions only have custom effects, no base potion types
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
                // Legacy versions use name() instead of getTranslationKey()
                component.addExtra(entry.getKey().getName());
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
        // OminousBottleMeta not available in legacy versions
        // No-op for legacy builds
    }
    
    @Override
    public void addMusicInstrumentDisplayInfo(ItemStack item, TextComponent component) {
        // MusicInstrumentMeta and goat horns not available in legacy versions
        // No-op for legacy builds
    }
    
    private TextComponent getPotionEffectsComponent(List<PotionEffect> effects) {
        TextComponent formattedEffects = new TextComponent("");
        int numEffects = effects.size();
        if (numEffects == 0) return formattedEffects;
        
        formattedEffects.addExtra(" (");
        for (int i = 0; i < numEffects; i++) {
            PotionEffect effect = effects.get(i);
            
            // Legacy versions use getName() instead of getTranslationKey()
            formattedEffects.addExtra(effect.getType().getName());
            
            if (effect.getAmplifier() > 0) {
                formattedEffects.addExtra(UtilMethods.formatRomanNumerals(effect.getAmplifier() + 1));
            }
            
            // Add duration for non-instant effects (using legacy field names)
            boolean isInstantEffect = false;
            try {
                // Legacy way to check for instant effects
                isInstantEffect = effect.getType().getName().equals("INSTANT_HEALTH") ||
                                effect.getType().getName().equals("INSTANT_DAMAGE");
            } catch (Exception e) {
                // Fallback - assume not instant if we can't check
                isInstantEffect = false;
            }
            
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