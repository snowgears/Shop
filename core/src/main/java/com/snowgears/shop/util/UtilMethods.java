package com.snowgears.shop.util;

import net.md_5.bungee.api.ChatColor;
import com.snowgears.shop.Shop;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
// TODO: Phase 2 - Add conditional import for ArmorMeta (Java 17+ only)
// import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.block.Sign;
import net.md_5.bungee.api.chat.TranslatableComponent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.*;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.util.io.BukkitObjectOutputStream;
import org.bukkit.util.io.BukkitObjectInputStream;

public class UtilMethods {

    private static ArrayList<Material> nonIntrusiveMaterials = new ArrayList<Material>();

    public static String trimForSign(String text) {
        final int MAX_SIGN_WIDTH = 80; // Maximum width allowed on a sign line
        int currentWidth = 0;
        StringBuilder result = new StringBuilder();
        
        // Process each character
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // Handle color codes (they don't take up width)
            if ((c == '§' || c == '&') && i + 1 < text.length()) {
                char nextChar = text.charAt(i + 1);
                if ("0123456789abcdefklmnorxABCDEFKLMNORX".indexOf(nextChar) != -1) {
                    result.append(c).append(nextChar);
                    i++; // Skip the next character (color code)
                    continue;
                }
            }
            
            // Get the width of the current character
            int charWidth = getMinecraftCharWidth(c);
            
            // Check if adding this character would exceed the width
            if (currentWidth + charWidth >= MAX_SIGN_WIDTH) {
                break; // We've reached the maximum width for the sign
            }
            
            // Add the character and update the width
            result.append(c);
            currentWidth += charWidth;
        }
        
        return result.toString();
    }

    /**
     * Returns the width of a character in the Minecraft font.
     * Based on the width data from Minecraft's font.
     * From: https://bukkit.org/threads/formatting-plugin-output-text-into-columns.8481/#post-133295
     */
    private static int getMinecraftCharWidth(char c) {
        switch (c) {
            // Narrow characters (width = 2)
            case '!': case ',': case '.': case ':': case ';': case 'i': case '|': case '¡':
                return 3;
                // return 2; // For some reason, this width of 2 is not working as expected!
            
            // Width = 3
            case '\'': case 'l': case 'ì': case 'í':
                return 3;
            
            // Width = 4
            case ' ': case 'I': case '[': case ']': case 'ï': case '×':
                return 4;
            
            // Width = 5
            case '"': case '(': case ')': case '<': case '>': case 'f': case 'k': case '{': case '}':
                return 5;
            
            // Width = 7
            case '@': case '~': case '®':
                return 7;
            
            // All other characters (width = 6)
            default:
                return 6;
        }
    }

    //this is used for formatting numbers like 5000 to 5k
    public static String formatLongToKString(double value, boolean formatZeros) {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Double.MIN_VALUE) return formatLongToKString(Double.MIN_VALUE + 1, formatZeros);
        if (value < 0) return "-" + formatLongToKString(-value, formatZeros);

        //System.out.println("formatting value: "+value);
        //NavigableMap<Double, Pair<Double, Double>> e = Shop.getPlugin().getPriceSuffixes();

        Map.Entry<Double, String> e = Shop.getPlugin().getPriceSuffixes().floorEntry(value);
        Double minimumValue = Shop.getPlugin().getPriceSuffixMinimumValue();;

        if (value < 1000 || e == null || value < minimumValue){
            if(isDecimal(value))
                return new DecimalFormat("0.00").format(value);
            else
                return new DecimalFormat("#.##").format(value);
        }

        //Map.Entry<Double, String> e = suffixes.floorEntry(value);
        Double divideBy = e.getKey();
        String suffix = e.getValue();

        //System.out.println("divideBy: "+divideBy+", suffix: "+suffix);

        double truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);

        String builtString = "";
        double fPrice;
        if(hasDecimal){
            fPrice = (truncated / 10d);
        }
        else{
            fPrice = (truncated / 10);
        }

//        if(formatZeros)
//            builtString = new DecimalFormat("0.00").format(fPrice);
//        else
//            builtString = new DecimalFormat("#.##").format(fPrice);
        builtString = new DecimalFormat("#.##").format(fPrice);
        builtString += suffix;
        return builtString;
    }

    public static boolean isDecimal(double d){
        return (d % 1 != 0);
    }

    public static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static BlockFace yawToFace(float yaw) {
        final BlockFace[] axis = {BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST};
        return axis[Math.round(yaw / 90f) & 0x3];
    }

    public static float faceToYaw(BlockFace bf) {
        switch(bf){
            case NORTH:
                return 180;
            case NORTH_EAST:
                return 225;
            case EAST:
                return 270;
            case SOUTH_EAST:
                return 315;
            case SOUTH:
                return 0;
            case SOUTH_WEST:
                return 45;
            case WEST:
                return 90;
            case NORTH_WEST:
                return 135;
        }
        return 180;
    }

    public static String capitalize(String line) {
        String[] spaces = line.split("\\s+");
        String capped = "";
        for (String s : spaces) {
            if (s.length() > 1)
                capped = capped + Character.toUpperCase(s.charAt(0)) + s.substring(1) + " ";
            else {
                capped = capped + s.toUpperCase() + " ";
            }
        }
        return capped.substring(0, capped.length()-1);
    }

    public static String getCleanLocation(Location loc, boolean includeWorld){
        String text = "";
        if (loc == null) { return text; }
        if(includeWorld && loc.getWorld() != null)
            text = loc.getWorld().getName() + " - ";
        text = text + "("+ loc.getBlockX() + ", "+loc.getBlockY() + ", "+loc.getBlockZ() + ")";
        return text;
    }

    public static Location getLocation(String cleanLocation){
        World world = null;

        if(cleanLocation.contains(" - ")) {
            int dashIndex = cleanLocation.indexOf(" - ");
            world = Bukkit.getWorld(cleanLocation.substring(0, dashIndex));
            cleanLocation = cleanLocation.substring(dashIndex+1, cleanLocation.length());
        }
        else {
            world = Bukkit.getWorld("world");
        }
        cleanLocation = cleanLocation.replaceAll("[^\\d-]", " ");

        String[] sp = cleanLocation.split("\\s+");

        try {
            return new Location(world, Integer.valueOf(sp[1]), Integer.valueOf(sp[2]), Integer.valueOf(sp[3]));
        } catch (Exception e){
            return null;
        }
    }

    public static int floor(double num) {
        int floor = (int) num;
        return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    public static String getEulerAngleString(EulerAngle angle){
        return "EulerAngle("+angle.getX() + ", " + angle.getY() + ", " + angle.getZ() + ")";
    }

    // Returns whether or not a player clicked the left or right side of a wall sign
    // 1 - LEFT SIDE
    // -1 - RIGHT SIDE
    // 0 - EXACT CENTER
    public static int calculateSideFromClickedSign(Player player, Block signBlock){
        Object blockData = CompatibilityUtil.getBlockData(signBlock);
        if(CompatibilityUtil.HAS_BLOCK_DATA && blockData != null) {
            // Modern approach: Use WallSign interface (MC 1.13+)
            try {
                Class<?> wallSignClass = Class.forName("org.bukkit.block.data.type.WallSign");
                if(wallSignClass.isInstance(blockData)) {
                    Object wallSign = wallSignClass.cast(blockData);
                    Object facing = wallSignClass.getMethod("getFacing").invoke(wallSign);
                    BlockFace attachedFace = (BlockFace) facing.getClass().getMethod("getOppositeFace").invoke(facing);
                    
                    Location chest = signBlock.getRelative(attachedFace).getLocation().add(0.5,0.5,0.5);
                    Location head = player.getLocation().add(0, player.getEyeHeight(), 0);

                    Vector direction = head.subtract(chest).toVector().normalize();
                    Vector look = player.getLocation().getDirection().normalize();

                    Vector cp = direction.crossProduct(look);

                    double d = 0;
                    switch(attachedFace){
                        case NORTH:
                            d = cp.getZ();
                            break;
                        case SOUTH:
                            d = cp.getZ() * -1;
                            break;
                        case EAST:
                            d = cp.getX() * -1;
                            break;
                        case WEST:
                            d = cp.getX();
                            break;
                        default:
                            break;
                    }

                    if(player.getLocation().getPitch() < 0)
                        d = -d;

                    if(d > 0)
                        return 1;
                    else if(d < 0)
                        return -1;
                    else
                        return 0;
                }
            } catch (Exception e) {
                // Fallback to legacy approach
            }
        }
        
        // Legacy approach: Use MaterialData (MC ≤1.12.2)
        // For legacy versions, we'll use a simpler approach since WallSign interface doesn't exist
        if(signBlock.getType().toString().contains("WALL_SIGN")) {
            // Try to determine facing from block data
            org.bukkit.material.MaterialData materialData = (org.bukkit.material.MaterialData) blockData;
            if(materialData instanceof org.bukkit.material.Sign) {
                org.bukkit.material.Sign signData = (org.bukkit.material.Sign) materialData;
                BlockFace attachedFace = signData.getAttachedFace();
                
                Location chest = signBlock.getRelative(attachedFace).getLocation().add(0.5,0.5,0.5);
                Location head = player.getLocation().add(0, player.getEyeHeight(), 0);

                Vector direction = head.subtract(chest).toVector().normalize();
                Vector look = player.getLocation().getDirection().normalize();

                Vector cp = direction.crossProduct(look);

                double d = 0;
                switch(attachedFace){
                    case NORTH:
                        d = cp.getZ();
                        break;
                    case SOUTH:
                        d = cp.getZ() * -1;
                        break;
                    case EAST:
                        d = cp.getX() * -1;
                        break;
                    case WEST:
                        d = cp.getX();
                        break;
                    default:
                        break;
                }

                if(player.getLocation().getPitch() < 0)
                    d = -d;

                if(d > 0)
                    return 1;
                else if(d < 0)
                    return -1;
                else
                    return 0;
            }
        }
        
        // Default fallback
        return 0;
    }

    public static String convertDurationToString(int duration) {
        duration = duration / 20;
        if (duration < 10)
            return "0:0" + duration;
        else if (duration < 60)
            return "0:" + duration;
        double mins = duration / 60;
        double secs = (mins - (int) mins);
        secs = (double) Math.round(secs * 100000) / 100000; //round to 2 decimal places
        if (secs == 0)
            return (int) mins + ":00";
        else if (secs < 10)
            return (int) mins + ":0" + (int) secs;
        else
            return (int) mins + ":" + (int) secs;
    }

    public static Location pushLocationInDirection(Location location, BlockFace direction, double add){
        switch (direction){
            case NORTH:
                location = location.add(-add, 0, -add); //subtract x as a hack for display tags being shifted
            case EAST:
                location = location.add(add, 0, -add); //subtract z as a hack for display tags being shifted
            case SOUTH:
                location = location.add(add, 0, add);  //add to x as a hack for display tags being shifted
            case WEST:
                location = location.add(-add, 0, 0);
        }
        return location;
    }

    public static int getDurabilityPercent(ItemStack item) {
        if (item.getType().getMaxDurability() > 0) {
            double dur = ((double)(item.getType().getMaxDurability() - item.getDurability()) / (double)item.getType().getMaxDurability());
            return (int)(dur * 100);
        }
        return 100;
    }

    public static String getItemName(ItemStack is){
        ItemMeta itemMeta = is.getItemMeta();

        if (itemMeta.getDisplayName() == null || itemMeta.getDisplayName().isEmpty())
            return capitalize(is.getType().name().replace("_", " ").toLowerCase());
        else
            return itemMeta.getDisplayName();
    }

    public static boolean stringStartsWithUUID(String name){
        if (name != null && name.length() > 35){
            try {
                if (UUID.fromString(name.substring(0, 36)) != null)
                    return true;
            } catch (Exception ex) {
                return false;
            }
        }
        return false;
    }

    public static boolean containsLocation(String s){
        if(s == null)
            return false;
        if(s.startsWith("***{")){
            if((s.indexOf(',') != s.lastIndexOf(',')) && s.indexOf('}') != -1)
                return true;
        }
        return false;
    }

    public static boolean basicLocationMatch(Location loc1, Location loc2){
        return (loc1.getBlockX() == loc2.getBlockX() && loc1.getBlockY() == loc2.getBlockY() && loc1.getBlockZ() == loc2.getBlockZ());
    }

    public static boolean materialIsNonIntrusive(Material material){
        if(nonIntrusiveMaterials.isEmpty()){
            initializeNonIntrusiveMaterials();
        }

        return (nonIntrusiveMaterials.contains(material));
    }

    public static String getLoreString(ItemStack is){
        if(is.getItemMeta() == null || is.getItemMeta().getLore() == null || is.getItemMeta().getLore().isEmpty())
            return "";
        return is.getItemMeta().getLore().toString();
    }

    public static String translate(String key){
        return new TranslatableComponent(key).toPlainText();
    }

    public static String formatTickTime(int ticks){
        // Convert ticks to seconds (20 ticks = 1 second)
        int totalSeconds = ticks / 20;
        
        // Calculate hours, minutes, and seconds
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        
        // Format the time string
        if (hours > 0) {
            return " " + String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return " " + String.format("%d:%02d", minutes, seconds);
        }
    }

    public static String formatRomanNumerals(int number){
        // only format 2-5, after that just show the number
        if (number < 2) return ""; // dont return on 1
        if(number > 5)
            return " " + String.valueOf(number);
        String[] romanNumerals = {"I", "II", "III", "IV", "V"};
        return " " + romanNumerals[number - 1];
    }

    // Remove white color codes if the message only contains white color codes
    // This can occur since we are building up TextComponents and it adds white color codes to the start of messages
    public static String removeColorsIfOnlyWhite(String message){
        String COLOR_CODE_REGEX_NO_WHITE = "([&§][0-9A-EK-ORXa-ek-orx])";
        // Check if there are any non-white color codes in the message
        boolean hasOtherColors = Pattern.compile(COLOR_CODE_REGEX_NO_WHITE).matcher(message).find();
        String msgStr = message;
        if (!hasOtherColors) { msgStr = ChatColor.stripColor(msgStr); }
        return msgStr;
    }

    public static TextComponent getEnchantmentsComponent(ItemStack item){
        TextComponent formattedMessage = new TextComponent(getItemName(item));
        
        // Use our clean hybrid pattern for version-specific functionality
        ItemDisplayHandler displayHandler = ItemDisplayFactory.getInstance();
        
        // Add enchantments using the appropriate handler
        displayHandler.addEnchantmentDisplayInfo(item, formattedMessage);
        
        // Add armor information (trims, etc.) using the appropriate handler
        displayHandler.addArmorDisplayInfo(item, formattedMessage);
        
        // Add support for displaying music disc information and goat horn sounds
        if(item.getItemMeta() != null) {
            String itemType = item.getType().name();
            
            // Add support for displaying music disc information
            if(itemType.startsWith("MUSIC_DISC_")) {
                String trackName = itemType.replace("MUSIC_DISC_", "");
                String formattedName = capitalize(trackName.toLowerCase().replace("_", " "));
                formattedMessage.addExtra(" [Song: " + formattedName + "]");
            }
            // Handle legacy music disc naming that doesn't follow the MUSIC_DISC_ prefix pattern
            else if(itemType.equals("MUSIC_DISC")) { formattedMessage.addExtra(" [Song: Unknown]"); }
            else if(itemType.equals("PIGSTEP")) { formattedMessage.addExtra(" [Song: Pigstep]"); }
            else if(itemType.equals("OTHERSIDE")) { formattedMessage.addExtra(" [Song: Otherside]"); }
            else if(itemType.equals("FIVE")) { formattedMessage.addExtra(" [Song: 5]"); }
            else if(itemType.equals("RELIC")) { formattedMessage.addExtra(" [Song: Relic]"); }
            
            // Add goat horn support using the appropriate handler
            displayHandler.addMusicInstrumentDisplayInfo(item, formattedMessage);
            
            // Add support for displaying bee hive/nest information (MC 1.15+ only)
            if(itemType.equals("BEE_NEST") || itemType.equals("BEEHIVE")) {
                try {
                    // Check if Beehive class exists (MC 1.15+)
                    if (CompatibilityUtil.hasClass("org.bukkit.block.Beehive")) {
                        if(item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta) {
                            org.bukkit.inventory.meta.BlockStateMeta blockStateMeta = (org.bukkit.inventory.meta.BlockStateMeta) item.getItemMeta();
                            
                            if(blockStateMeta.hasBlockState()) {
                                try {
                                    // Use reflection to safely access Beehive class
                                    Class<?> beehiveClass = Class.forName("org.bukkit.block.Beehive");
                                    if(beehiveClass.isInstance(blockStateMeta.getBlockState())) {
                                        Object beehive = beehiveClass.cast(blockStateMeta.getBlockState());
                                        
                                        int honeyLevel = 0;
                                        int beeCount = 0;
                                        
                                        // Get honey level using reflection for BlockData
                                        try {
                                            Class<?> beehiveDataClass = Class.forName("org.bukkit.block.data.type.Beehive");
                                            Object blockData = beehive.getClass().getMethod("getBlockData").invoke(beehive);
                                            if(beehiveDataClass.isInstance(blockData)) {
                                                honeyLevel = (Integer) beehiveDataClass.getMethod("getHoneyLevel").invoke(blockData);
                                            }
                                        } catch (Exception e) { /* Silently handle */ }
                                        
                                        // Get bee count using reflection
                                        try { 
                                            beeCount = (Integer) beehiveClass.getMethod("getEntityCount").invoke(beehive);
                                        } catch (Exception e) { /* Silently handle */ }
                                        
                                        // Format the message
                                        if(honeyLevel > 0 || beeCount > 0) {
                                            StringBuilder beeInfo = new StringBuilder(" [");
                                            if(honeyLevel > 0) {
                                                beeInfo.append("Honey: ").append(honeyLevel).append("/5");
                                                if(beeCount > 0) { beeInfo.append(", "); }
                                            }
                                            if(beeCount > 0) { beeInfo.append("Bees: ").append(beeCount); }
                                            beeInfo.append("]");
                                            formattedMessage.addExtra(beeInfo.toString());
                                        }
                                    }
                                } catch (Exception e) { /* Silently handle for backward compatibility */ }
                            }
                        }
                    }
                } catch (Exception e) { /* Silently handle any exceptions for backward compatibility */ }
            }
        }

        // Add ominous bottle support using the appropriate handler
        displayHandler.addOminousBottleDisplayInfo(item, formattedMessage);

        // Add potion support using the appropriate handler
        displayHandler.addPotionDisplayInfo(item, formattedMessage);

        // Add detailed firework effect information
        if(item.getItemMeta() != null) {
            // Handle Firework Stars
            if(item.getItemMeta() instanceof org.bukkit.inventory.meta.FireworkEffectMeta) {
                org.bukkit.inventory.meta.FireworkEffectMeta fireworkMeta = (org.bukkit.inventory.meta.FireworkEffectMeta) item.getItemMeta();
                if(fireworkMeta.hasEffect()) {
                    formattedMessage.addExtra(getFormattedFireworkEffect(fireworkMeta.getEffect(), true));
                }
            }
            // Handle Fireworks
            else if(item.getItemMeta() instanceof FireworkMeta) {
                FireworkMeta fireworkMeta = (FireworkMeta) item.getItemMeta();
                int power = fireworkMeta.getPower();
                
                // Display duration
                if (power == 0) power = 1;
                formattedMessage.addExtra(" [Duration " + power + "]");
                
                // Display effects
                List<org.bukkit.FireworkEffect> effects = fireworkMeta.getEffects();
                if(effects != null && !effects.isEmpty()) {
                    int effectCount = effects.size();
                    if(effectCount <= 2) {
                        // If there's only one-two effects, show their details
                        for (org.bukkit.FireworkEffect effect : effects) {
                            formattedMessage.addExtra(getFormattedFireworkEffect(effect, false));
                        }
                    } else {
                        // If there are multiple effects, just show the count
                        formattedMessage.addExtra(" [" + effectCount + " Effects]");
                    }
                }
            }
        }

        return new TextComponent(ChatColor.stripColor(formattedMessage.toLegacyText()));
    }

    private static TextComponent getPotionEffects(List<PotionEffect> effects){
        TextComponent formattedEffects = new TextComponent("");
        int numEffects = effects.size();
        if (numEffects == 0) return formattedEffects;
        formattedEffects.addExtra(" (");
        for (int i = 0; i < numEffects; i++) {
            PotionEffect effect = effects.get(i);
            
            // Use CompatibilityUtil check for translation key support
            if (CompatibilityUtil.hasGetTranslationKey()) {
                try {
                    String translationKey = (String) effect.getType().getClass().getMethod("getTranslationKey").invoke(effect.getType());
                    formattedEffects.addExtra(new TranslatableComponent(translationKey));
                } catch (Exception e) {
                    // Fallback to effect type name
                    formattedEffects.addExtra(effect.getType().getName());
                }
            } else {
                // Fallback to effect type name for older versions
                formattedEffects.addExtra(effect.getType().getName());
            }
            
            // Show level for all potions, not just those with amplifier > 0
            // For potions with amplifier 0, we don't add any suffix (it's the base level)
            if(effect.getAmplifier() > 0) {
                formattedEffects.addExtra(formatRomanNumerals(effect.getAmplifier() + 1)); // +1 because amplifier is 0-based
            }
            
            // Only add duration for non-instant effects
            // Use CompatibilityUtil checks for instant effect types
            boolean isInstantEffect = false;
            if (CompatibilityUtil.hasInstantHealth() && CompatibilityUtil.hasInstantDamage()) {
                try {
                    Object instantHealth = org.bukkit.potion.PotionEffectType.class.getField("INSTANT_HEALTH").get(null);
                    Object instantDamage = org.bukkit.potion.PotionEffectType.class.getField("INSTANT_DAMAGE").get(null);
                    isInstantEffect = effect.getType().equals(instantHealth) || effect.getType().equals(instantDamage);
                } catch (Exception e) {
                    // Fallback - assume not instant if we can't check
                    isInstantEffect = false;
                }
            }
            
            if(effect.getDuration() > 0 && !isInstantEffect) {
                formattedEffects.addExtra(formatTickTime(effect.getDuration()));
            }
            
            // if we have more than one effect, add a comma, dont add a comma after the last effect
            if(i < numEffects - 1)
                formattedEffects.addExtra(", ");
        }
        formattedEffects.addExtra(")");
        return formattedEffects;
    }

    /**
     * Formats a firework effect into a readable string
     * @param effect The firework effect to format
     * @param isFireworkStar Whether this is for a firework star (true) or a firework (false)
     * @return Formatted text component with firework effect information
     */
    private static TextComponent getFormattedFireworkEffect(org.bukkit.FireworkEffect effect, boolean isFireworkStar) {
        TextComponent formattedEffect = new TextComponent("");
        
        if(effect == null) return formattedEffect;
        
        StringBuilder sb = new StringBuilder();
        
        // Start the formatted string
        sb.append(" [");
        
        // Add the shape
        String shapeName = formatFireworkShape(effect.getType());
        sb.append(shapeName);
        
        // Add special effects
        List<String> specialEffects = new ArrayList<>();
        if(effect.hasTrail()) specialEffects.add("Trail");
        if(effect.hasFlicker()) specialEffects.add("Twinkle");
        
        if(!specialEffects.isEmpty()) {
            sb.append(" (");
            sb.append(String.join(", ", specialEffects));
            sb.append(")");
        }
        
        // Add color information if we have it
        List<org.bukkit.Color> colors = effect.getColors();
        if(colors != null && !colors.isEmpty()) {
            if(colors.size() == 1) {
                // If there's just one color, add it directly
                sb.append(" ").append(formatFireworkColor(colors.get(0)));
            } else if(colors.size() <= 3) {
                // If there are 2-3 colors, list them
                sb.append(" ");
                for(int i = 0; i < colors.size(); i++) {
                    sb.append(formatFireworkColor(colors.get(i)));
                    if(i < colors.size() - 1) sb.append(", ");
                }
            } else {
                // If there are many colors, just show the count
                sb.append(" ").append(colors.size()).append(" Colors");
            }
        }
        
        // Add fade information if available
        List<org.bukkit.Color> fadeColors = effect.getFadeColors();
        if(fadeColors != null && !fadeColors.isEmpty()) {
            if(fadeColors.size() == 1) {
                // If there's just one fade color, add it directly
                sb.append("→").append(formatFireworkColor(fadeColors.get(0)));
            } else if(fadeColors.size() <= 2) {
                // If there are 2 fade colors, list them
                sb.append("→");
                for(int i = 0; i < fadeColors.size(); i++) {
                    sb.append(formatFireworkColor(fadeColors.get(i)));
                    if(i < fadeColors.size() - 1) sb.append(", ");
                }
            } else {
                // If there are many fade colors, just show the count
                sb.append(" → ").append(fadeColors.size()).append(" Fade Colors");
            }
        }
        
        sb.append("]");
        
        formattedEffect.addExtra(sb.toString());
        return formattedEffect;
    }
    
    /**
     * Formats a firework shape into a readable string
     * @param type The firework effect type
     * @return Formatted shape name
     */
    private static String formatFireworkShape(org.bukkit.FireworkEffect.Type type) {
        switch(type) {
            case BALL:
                return "Small";
            case BALL_LARGE:
                return "Large";
            case STAR:
                return "Star";
            case BURST:
                return "Burst";
            case CREEPER:
                return "Creeper";
            default:
                return capitalize(type.toString().toLowerCase().replace("_", " "));
        }
    }
    
    /**
     * Formats a color into a readable string
     * @param color The color to format
     * @return Formatted color name
     */
    private static String formatFireworkColor(org.bukkit.Color color) {
        Shop.getPlugin().getShopLogger().debug("[formatFireworkColor]     color: " + color.toString());

        // Map common RGB values to color names
        if(color.equals(org.bukkit.Color.WHITE)) return "White";
        if(color.equals(org.bukkit.Color.SILVER)) return "Silver";
        if(color.equals(org.bukkit.Color.GRAY)) return "Gray";
        if(color.equals(org.bukkit.Color.BLACK)) return "Black";
        if(color.equals(org.bukkit.Color.RED)) return "Red";
        if(color.equals(org.bukkit.Color.MAROON)) return "Maroon";
        if(color.equals(org.bukkit.Color.YELLOW)) return "Yellow";
        if(color.equals(org.bukkit.Color.OLIVE)) return "Olive";
        if(color.equals(org.bukkit.Color.LIME)) return "Lime";
        if(color.equals(org.bukkit.Color.GREEN)) return "Green";
        if(color.equals(org.bukkit.Color.AQUA)) return "Aqua";
        if(color.equals(org.bukkit.Color.TEAL)) return "Teal";
        if(color.equals(org.bukkit.Color.BLUE)) return "Blue";
        if(color.equals(org.bukkit.Color.NAVY)) return "Navy";
        if(color.equals(org.bukkit.Color.FUCHSIA)) return "Fuchsia";
        if(color.equals(org.bukkit.Color.PURPLE)) return "Purple";
        if(color.equals(org.bukkit.Color.ORANGE)) return "Orange";

        
        // For dye colors (Minecraft 1.8+)
        try {
            for(org.bukkit.DyeColor dyeColor : org.bukkit.DyeColor.values()) {
                if(dyeColor.getColor().equals(color)) {
                    return capitalize(dyeColor.toString().toLowerCase().replace("_", " "));
                }
                if(dyeColor.getFireworkColor().equals(color)) {
                    return capitalize(dyeColor.toString().toLowerCase().replace("_", " "));
                }
            }
        } catch(NoSuchMethodError e) {
            // Fallback for older versions that might not have getFireworkColor()
        }
        
        // If no match is found, return a generic "Custom"
        return "Custom";
    }

    private static void initializeNonIntrusiveMaterials(){
        for(Material m : Material.values()){
            if(!m.isSolid())
                nonIntrusiveMaterials.add(m);
        }
        
        // Add wall sign materials - these were added in MC 1.13+ (The Flattening)
        try{
            nonIntrusiveMaterials.add(Material.WARPED_WALL_SIGN);
        } catch(NoSuchFieldError e){ /* Material doesn't exist in this version */ }
        try{
            nonIntrusiveMaterials.add(Material.ACACIA_WALL_SIGN);
        } catch(NoSuchFieldError e){ /* Material doesn't exist in this version */ }
        try{
            nonIntrusiveMaterials.add(Material.BIRCH_WALL_SIGN);
        } catch(NoSuchFieldError e){ /* Material doesn't exist in this version */ }
        try{
            nonIntrusiveMaterials.add(Material.CRIMSON_WALL_SIGN);
        } catch(NoSuchFieldError e){ /* Material doesn't exist in this version */ }
        try{
            nonIntrusiveMaterials.add(Material.DARK_OAK_WALL_SIGN);
        } catch(NoSuchFieldError e){ /* Material doesn't exist in this version */ }
        try{
            nonIntrusiveMaterials.add(Material.JUNGLE_WALL_SIGN);
        } catch(NoSuchFieldError e){ /* Material doesn't exist in this version */ }
        try{
            nonIntrusiveMaterials.add(Material.OAK_WALL_SIGN);
        } catch(NoSuchFieldError e){ /* Material doesn't exist in this version */ }
        try{
            nonIntrusiveMaterials.add(Material.SPRUCE_WALL_SIGN);
        } catch(NoSuchFieldError e){ /* Material doesn't exist in this version */ }
        
        // Fallback for legacy versions - add the old WALL_SIGN material
        try{
            nonIntrusiveMaterials.add(Material.LEGACY_WALL_SIGN);
        } catch(NoSuchFieldError e){ 
            // Try the pre-legacy WALL_SIGN material for very old versions using valueOf
            try{
                nonIntrusiveMaterials.add(Material.valueOf("WALL_SIGN"));
            } catch(IllegalArgumentException e2){ /* Material doesn't exist, skip */ }
        }
        
        nonIntrusiveMaterials.remove(Material.WATER);
        nonIntrusiveMaterials.remove(Material.LAVA);
        nonIntrusiveMaterials.remove(Material.FIRE);
        
        // Remove dangerous materials that exist across versions
        try { nonIntrusiveMaterials.remove(Material.END_PORTAL); } catch(NoSuchFieldError e) {}
        try { nonIntrusiveMaterials.remove(Material.NETHER_PORTAL); } catch(NoSuchFieldError e) {}
        try { nonIntrusiveMaterials.remove(Material.SKELETON_SKULL); } catch(NoSuchFieldError e) {}
        try { nonIntrusiveMaterials.remove(Material.WITHER_SKELETON_SKULL); } catch(NoSuchFieldError e) {}
        try { nonIntrusiveMaterials.remove(Material.PLAYER_HEAD); } catch(NoSuchFieldError e) {}
        try { nonIntrusiveMaterials.remove(Material.CREEPER_HEAD); } catch(NoSuchFieldError e) {}

        // Use CompatibilityUtil to safely add LIGHT material if available
        if (CompatibilityUtil.hasLightMaterial()) {
            try {
                nonIntrusiveMaterials.add(Material.valueOf("LIGHT"));
            } catch (Exception e) {
                // Silently ignore if LIGHT material isn't available
            }
        }
    }

    public static BlockFace getDirectionOfChest(Block block){
        Object blockData = CompatibilityUtil.getBlockData(block);
        if(CompatibilityUtil.HAS_BLOCK_DATA && blockData != null) {
            // Modern approach: Use Directional interface (MC 1.13+)
            try {
                Class<?> directionalClass = Class.forName("org.bukkit.block.data.Directional");
                if(directionalClass.isInstance(blockData)) {
                    Object directional = directionalClass.cast(blockData);
                    return (BlockFace) directionalClass.getMethod("getFacing").invoke(directional);
                }
            } catch (Exception e) {
                // Fallback to legacy approach
            }
        }
        
        // Legacy approach: Use MaterialData (MC ≤1.12.2)
        if(blockData instanceof org.bukkit.material.MaterialData) {
            org.bukkit.material.MaterialData materialData = (org.bukkit.material.MaterialData) blockData;
            if(materialData instanceof org.bukkit.material.Directional) {
                org.bukkit.material.Directional directional = (org.bukkit.material.Directional) materialData;
                return directional.getFacing();
            }
        }
        
        return null;
    }

    //returns if Minecraft version 1.17 or above
    public static boolean isMCVersion17Plus(){
        //LIGHT only available in MC 1.17+
        return CompatibilityUtil.hasLightBlock();
    }

    //returns if Minecraft version 1.14 or above
    public static boolean isMCVersion14Plus(){
        //BARREL only available in MC 1.14+
        try {
            if(Material.BARREL != null)
                return true;
        } catch (NoSuchFieldError e) {
            return false;
        }
        return false;
    }

    //this takes a dirty (pre-cleaned) string and finds how much to multiply the final by
    //this utility allows the input of numbers like 1.2k (1200)
    public static double getMultiplyValue(String text){
        // Remove color formatting, whitespace, and make sure the string is lowercase for matching our suffixes below
        String priceString = ChatColor.stripColor(text).replaceAll("\\s", "").toLowerCase();
        // Get just the suffix from the price string, remove all numbers and decimals
        String priceSuffix = priceString.replaceAll("[0-9.]", "");

        // Load the suffixes from the config values
        NavigableMap<Double, String> configPriceSuffixes = Shop.getPlugin().getPriceSuffixes();

        // Search for a suffix match
        for (Map.Entry<Double, String> entry : configPriceSuffixes.entrySet()) {
            Double configPriceValue = entry.getKey();
            String configSuffix = entry.getValue().toLowerCase();

            if (priceSuffix.equals(configSuffix)) {
                // Return the value for the suffix from the config
                return configPriceValue;
            }
        }

        // No match so our multiplier is just 1
        return 1;
    }

    public static String cleanNumberText(String text){
        String cleaned = "";
        String toClean = ChatColor.stripColor(text);
        for(int i=0; i<toClean.length(); i++) {
            if(Character.isDigit(toClean.charAt(i)))
                cleaned += toClean.charAt(i);
            else if(toClean.charAt(i) == '.')
                cleaned += toClean.charAt(i);
            else if(toClean.charAt(i) == ' ')
                cleaned += toClean.charAt(i);
        }
        return cleaned;
    }

    public static ChatColor getChatColorByCode(String colorCode) {
        switch (colorCode) {
            case "&b":
                return ChatColor.AQUA;
            case "&0":
                return ChatColor.BLACK;
            case "&9":
                return ChatColor.BLUE;
            case "&l":
                return ChatColor.BOLD;
            case "&3":
                return ChatColor.DARK_AQUA;
            case "&1":
                return ChatColor.DARK_BLUE;
            case "&8":
                return ChatColor.DARK_GRAY;
            case "&2":
                return ChatColor.DARK_GREEN;
            case "&5":
                return ChatColor.DARK_PURPLE;
            case "&4":
                return ChatColor.DARK_RED;
            case "&6":
                return ChatColor.GOLD;
            case "&7":
                return ChatColor.GRAY;
            case "&a":
                return ChatColor.GREEN;
            case "&o":
                return ChatColor.ITALIC;
            case "&d":
                return ChatColor.LIGHT_PURPLE;
            case "&k":
                return ChatColor.MAGIC;
            case "&c":
                return ChatColor.RED;
            case "&r":
                return ChatColor.RESET;
            case "&m":
                return ChatColor.STRIKETHROUGH;
            case "&n":
                return ChatColor.UNDERLINE;
            case "&f" :
                return ChatColor.WHITE;
            case "&e":
                return ChatColor.YELLOW;
            default:
                return ChatColor.RESET;
        }
    }

    public static ChatColor getChatColor(String message) {
        if(message.startsWith("&") && message.length() > 1){
            ChatColor cc = getChatColorByCode(message.substring(0,2));
            if(cc != ChatColor.RESET)
                return cc;
        }
        return null;
    }

    public static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }

    public static void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String itemStackToBase64(ItemStack item) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

        // Set max item stack size to 64 if its higher than 64
        // Otherwise the serialization complains...
        if (item.getAmount() > 64) { item.setAmount(64); }

        // Write the ItemStack to the ObjectOutputStream
        dataOutput.writeObject(item);
        dataOutput.close();

        // Encode the byte array to a Base64 string
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static ItemStack itemStackFromBase64(String data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

        // Read the ItemStack from the ObjectInputStream
        ItemStack item = (ItemStack) dataInput.readObject();
        dataInput.close();
        return item;
    }

    public static List<String> splitStringIntoLines(String text, int maxLineLength) {
        final String HEX_COLOR_CODE_REGEX = "(§x§.§.§.§.§.§.)"; // Hex code format using mc color codes
        final String COLOR_CODE_REGEX = "([&§][0-9A-FK-ORa-fk-or])";

        Matcher matcher = Pattern
            .compile(HEX_COLOR_CODE_REGEX + "|" + COLOR_CODE_REGEX + "| |[^&§\\s]+")
            .matcher(ChatColor.translateAlternateColorCodes('&', text));
        List<String> words = new ArrayList<>();
        while (matcher.find()) {
            words.add(matcher.group());
        }

        StringBuilder currentLine = new StringBuilder();
        List<String> linesByColor = new ArrayList<>();

        String latestColors = "";
        String latestHexColor = ""; // For tracking hex colors
        ChatColor latestColor = ChatColor.WHITE; // Initially white in case user provides no color codes
        boolean isBold = false;
        boolean isItalic = false;
        boolean isStrikethrough = false;
        boolean isUnderlined = false;
        boolean isObfuscated = false;
        for (String word : words) {
            if (Shop.getPlugin() != null) Shop.getPlugin().getShopLogger().hyper("[ShopMessage.format]     word: " + word);
            
            boolean isStandardColor = word.matches(COLOR_CODE_REGEX);
            boolean isHexColor = word.matches(HEX_COLOR_CODE_REGEX);
            if (isStandardColor || isHexColor) {
                if (isStandardColor) {
                    ChatColor newColor = ChatColor.getByChar(word.charAt(1));
                    if (newColor == ChatColor.BOLD) isBold = true;
                    else if (newColor == ChatColor.ITALIC) isItalic = true;
                    else if (newColor == ChatColor.STRIKETHROUGH) isStrikethrough = true;
                    else if (newColor == ChatColor.UNDERLINE) isUnderlined = true;
                    else if (newColor == ChatColor.MAGIC) isObfuscated = true;
                    else if (newColor == ChatColor.RESET) {
                        if (Shop.getPlugin() != null) Shop.getPlugin().getShopLogger().hyper("[ShopMessage.format]     matched RESET color code: " + word);
                        latestColor = ChatColor.WHITE;
                        latestHexColor = ""; // Reset hex color when RESET code is found
                        isBold = false;
                        isItalic = false;
                        isStrikethrough = false;
                        isUnderlined = false;
                        isObfuscated = false;
                    } else {
                        latestColor = newColor;
                        latestHexColor = ""; // Clear hex color when a standard color is set
                    }
                } else if (isHexColor) {
                    latestColor = null;
                    latestHexColor = word;
                }

                String newColors = "";
                // Follow vanilla behavior, add colors first, then formatting codes
                // FIRST Add standard color OR hex color
                if (latestColor != null) newColors += latestColor.toString();
                else if (!latestHexColor.isEmpty()) newColors += latestHexColor;
                // SECOND Add formatting codes
                if (isBold) newColors += ChatColor.BOLD;
                if (isItalic) newColors += ChatColor.ITALIC;
                if (isStrikethrough) newColors += ChatColor.STRIKETHROUGH;
                if (isUnderlined) newColors += ChatColor.UNDERLINE;
                if (isObfuscated) newColors += ChatColor.MAGIC;

                if (!latestColors.equals(newColors)) {
                    latestColors = newColors;
                    if (currentLine.toString().contains(" ")) {
                        // New color, add the line and start a new line
                        if (ChatColor.stripColor(currentLine.toString()).length() > 0) {
                            linesByColor.add(currentLine.toString());
                        }
                        // Set the current line to the new color, ignore any old colors
                        currentLine = new StringBuilder(latestColors);
                    } else {
                        // If our current line has text other than color codes, add the latest color code.
                        if (ChatColor.stripColor(currentLine.toString()).length() > 0) {
                            currentLine.append(word);
                        } else {
                            // We are just useless colors, wipe them and start the line again with our current colors.
                            currentLine = new StringBuilder(latestColors);
                        }
                    }
                }

                continue;
            }

            // Also split if the single color line is too long!
            int currentLineLength = ChatColor.stripColor(currentLine.toString()).length();
            int nextWordLength = ChatColor.stripColor(word).length();
            int potentialLength = currentLineLength + nextWordLength;
            if (Shop.getPlugin() != null) Shop.getPlugin().getShopLogger().spam("[ShopMessage.format]     potentialLength: " + potentialLength + " maxLineLength: " + maxLineLength);
            
            if (word.matches(" ") && potentialLength > maxLineLength) {
                if (Shop.getPlugin() != null) Shop.getPlugin().getShopLogger().spam("[ShopMessage.format]     adding line: " + currentLine.toString().trim(), true);
                linesByColor.add(currentLine.toString());
                currentLine = new StringBuilder(latestColors);
            } else {
                currentLine.append(word);
            }
        }

        // Append the last line if there's any content left
        if (currentLine.length() > 0) {
            if (Shop.getPlugin() != null) Shop.getPlugin().getShopLogger().spam("[ShopMessage.format]     adding line: " + currentLine.toString().trim(), true);
            linesByColor.add(currentLine.toString());
        }

        // Now we need to start taking the "blocks" of text and combining them into lines, limited by maxLineLength
        List<String> result = new ArrayList<>();
        currentLine = new StringBuilder();
        for (String line : linesByColor) {
            int lineLengthNoColors = ChatColor.stripColor(line).length();
            int currentLineLengthNoColors = ChatColor.stripColor(currentLine.toString()).length();
            // If we are less than the max line length
            // OR if the line/currentLine is empty add it
            if (
                currentLineLengthNoColors + lineLengthNoColors <= maxLineLength 
                || lineLengthNoColors == 0
                || currentLineLengthNoColors == 0
            ) {
                currentLine.append(line);
            } else {
                result.add(currentLine.toString().trim()); // only trim on final add
                currentLine = new StringBuilder(line);
            }
        }
        if (currentLine.length() > 0) {
            result.add(currentLine.toString().trim()); // only trim on final add
        }
        return result;
    }
}
