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
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.List;
import java.util.Map;

/**
 * Modern implementation using Java 17+ and latest Minecraft APIs.
 * This uses direct API calls instead of reflection for better performance and readability.
 */
public class ModernItemDisplayHandler {
    
    public void addArmorDisplayInfo(ItemStack item, TextComponent component) {
        if (!MCVersion.atLeast("1.20")) return;
        // Add armor trim info
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

    public static String getEnchantmentsString(ItemStack is){
        Map<Enchantment, Integer> enchantsMap;
        if(is.getItemMeta() instanceof EnchantmentStorageMeta){
            enchantsMap = ((EnchantmentStorageMeta) is.getItemMeta()).getStoredEnchants();
        }
        else{
            enchantsMap = is.getEnchantments();
        }

        if(enchantsMap == null || enchantsMap.isEmpty())
            return "";

        String enchants = "[";
        int i=0;
        for(Map.Entry<Enchantment, Integer> entry : enchantsMap.entrySet()){
            enchants += getEnchantmentName(entry.getKey()) + " " + entry.getValue();

            //TODO if enchantment name is Unknown, look up enchantment by namedSpaceKey? Looks like other plugins can register enchantments to server similar to Recipes

            i++;
            if(i != enchantsMap.size())
                enchants += ", ";
            else
                enchants += "]";
        }
        return enchants;
    }

    public void addEnchantmentDisplayInfo(ItemStack item, TextComponent component) {
        if (item.getEnchantments().size() > 0) {
            if (!MCVersion.atLeast("1.16")) { 
                component.addExtra(getEnchantmentsString(item));
                return;
            }

            Map<Enchantment, Integer> enchantsMap = item.getEnchantments();
            component.addExtra(" [");
            int i = 0;
            for (Map.Entry<Enchantment, Integer> entry : enchantsMap.entrySet()) {
                Enchantment enchantment = entry.getKey();
                Integer level = entry.getValue();
                component.addExtra(new TranslatableComponent(enchantment.getTranslationKey()));
                
                component.addExtra(UtilMethods.formatRomanNumerals(level));
                i++;
                if (i != enchantsMap.size()) {
                    component.addExtra(", ");
                } else {
                    component.addExtra("]");
                }
            }
        }
    }
    
    public void addOminousBottleDisplayInfo(ItemStack item, TextComponent component) {
        if (!MCVersion.atLeast("1.21")) return;
        
        if (item.getItemMeta() instanceof OminousBottleMeta) {
            OminousBottleMeta ominousMeta = (OminousBottleMeta) item.getItemMeta();
            int level = ominousMeta.hasAmplifier() ? ominousMeta.getAmplifier() + 1 : 1;
            component.addExtra(" [Bad Omen" + UtilMethods.formatRomanNumerals(level) + "]");
        }
    }
    
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
            if (MCVersion.atLeast("1.16")) {
                formattedEffects.addExtra(new TranslatableComponent(effect.getType().getTranslationKey()));
            } else {
                formattedEffects.addExtra(effect.getType().getName());
            }
            
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

    public static String getEnchantmentName(Enchantment enchantment){
        //        System.out.println(enchantment.getName());
        //        System.out.println(enchantment.getKey().getKey());
        //        System.out.println(enchantment.getKey().getNamespace());
        switch (enchantment.getName()) {
            case "ARROW_DAMAGE":
                return "Power";
            case "ARROW_FIRE":
                return "Flame";
            case "ARROW_INFINITE":
                return "Infinity";
            case "ARROW_KNOCKBACK":
                return "Punch";
            case "BINDING_CURSE":
                return "Curse of Binding";
            case "CHANNELING":
                return "Channeling";
            case "DAMAGE_ALL":
                return "Sharpness";
            case "DAMAGE_ARTHROPODS":
                return "Bane of Arthropods";
            case "DAMAGE_UNDEAD":
                return "Smite";
            case "DEPTH_STRIDER":
                return "Depth Strider";
            case "DIG_SPEED":
                return "Efficiency";
            case "DURABILITY":
                return "Unbreaking";
            case "FIRE_ASPECT":
                return "Fire Aspect";
            case "FROST_WALKER":
                return "Frost Walker";
            case "IMPALING":
                return "Impaling";
            case "KNOCKBACK":
                return "Knockback";
            case "LOOT_BONUS_BLOCKS":
                return "Fortune";
            case "LOOT_BONUS_MOBS":
                return "Looting";
            case "LOYALTY":
                return "Loyalty";
            case "LUCK":
                return "Luck of the Sea";
            case "LURE":
                return "Lure";
            case "MENDING":
                return "Mending";
            case "MULTISHOT":
                return "Multishot";
            case "OXYGEN":
                return "Respiration";
            case "PIERCING":
                return "Piercing";
            case "PROTECTION_ENVIRONMENTAL":
                return "Protection";
            case "PROTECTION_EXPLOSIONS":
                return "Blast Protection";
            case "PROTECTION_FALL":
                return "Feather Falling";
            case "PROTECTION_FIRE":
                return "Fire Protection";
            case "PROTECTION_PROJECTILE":
                return "Projectile Protection";
            case "QUICK_CHARGE":
                return "Quick Charge";
            case "RIPTIDE":
                return "Riptide";
            case "SILK_TOUCH":
                return "Silk Touch";
            case "SOUL_SPEED":
                return "Soul Speed";
            case "SWEEPING_EDGE":
                return "Sweeping Edge";
            case "SWIFT_SNEAK":
                return "Swift Sneak";
            case "THORNS":
                return "Thorns";
            case "VANISHING_CURSE":
                return "Cure of Vanishing";
            case "WATER_WORKER":
                return "Aqua Affinity";
            default:
                return "Unknown";
        }
    }
} 