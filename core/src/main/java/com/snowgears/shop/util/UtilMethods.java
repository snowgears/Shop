package com.snowgears.shop.util;



import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.*;

public class UtilMethods {

    private static ArrayList<Material> nonIntrusiveMaterials = new ArrayList<Material>();

    //this is used for formatting numbers like 5000 to 5k
    private static final NavigableMap<Double, String> suffixes = new TreeMap<>();
    static {
        suffixes.put(1_000D, "k");
        suffixes.put(1_000_000D, "M");
        suffixes.put(1_000_000_000D, "G");
        suffixes.put(1_000_000_000_000D, "T");
        suffixes.put(1_000_000_000_000_000D, "P");
        suffixes.put(1_000_000_000_000_000_000D, "E");
    }

    //this is used for formatting numbers like 5000 to 5k
    public static String formatLongToKString(double value, boolean formatZeros) {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Double.MIN_VALUE) return formatLongToKString(Double.MIN_VALUE + 1, formatZeros);
        if (value < 0) return "-" + formatLongToKString(-value, formatZeros);
        if (value < 1000) return Double.toString(value); //deal with easy case

        Map.Entry<Double, String> e = suffixes.floorEntry(value);
        Double divideBy = e.getKey();
        String suffix = e.getValue();

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

        if(formatZeros)
            builtString = new DecimalFormat("0.00").format(fPrice);
        else
            builtString = new DecimalFormat("#.##").format(fPrice);
        return builtString += suffix;
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
        if(includeWorld)
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
        if(!(signBlock.getType() == Material.WALL_SIGN))
            return 0;
        org.bukkit.material.Sign s = (org.bukkit.material.Sign) signBlock.getState().getData();
        BlockFace attachedFace = s.getFacing().getOppositeFace();
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
        //System.out.println("Side clicked: "+d);

        if(d > 0)
            return 1;
        else if(d < 0)
            return -1;
        else
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
            i++;
            if(i != enchantsMap.size())
                enchants += ", ";
            else
                enchants += "]";
        }
        return enchants;
    }

    public static String getEnchantmentName(Enchantment enchantment){
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

    private static void initializeNonIntrusiveMaterials(){
        for(Material m : Material.values()){
            if(!m.isSolid())
                nonIntrusiveMaterials.add(m);
        }
        nonIntrusiveMaterials.add(Material.WALL_SIGN);
        nonIntrusiveMaterials.remove(Material.WATER);
        nonIntrusiveMaterials.remove(Material.LAVA);
        nonIntrusiveMaterials.remove(Material.FIRE);
        nonIntrusiveMaterials.remove(Material.ENDER_PORTAL);
        nonIntrusiveMaterials.remove(Material.PORTAL);
        nonIntrusiveMaterials.remove(Material.SKULL);
    }

    public static BlockFace getDirectionOfChest(Block block){
        if(block.getState().getData() instanceof Directional){
            return ((Directional)block.getState().getData()).getFacing();
        }
        return null;
    }

    //this takes a dirty (pre-cleaned) string and finds how much to multiply the final by
    //this utility allows the input of numbers like 1.2k (1200)
    public static int getMultiplyValue(String text){
        int multiplyBy = 1;
        for(int i=0; i<text.length(); i++) {
            switch (text.charAt(i)) {
                case 'k':
                case 'K':
                    multiplyBy *= 1000D;
                    break;
                case 'm':
                case 'M':
                    multiplyBy *= 1000000D;
                    break;
                case 'g':
                case 'G':
                    multiplyBy *= 1000000000D;
                    break;
                case 't':
                case 'T':
                    multiplyBy *= 1000000000000D;
                    break;
                case 'p':
                case 'P':
                    multiplyBy *= 1000000000000000D;
                    break;
                case 'e':
                case 'E':
                    multiplyBy *= 1000000000000000000D;
                    break;
            }
        }
        return multiplyBy;
    }

    public static String cleanNumberText(String text){
        String cleaned = "";
        for(int i=0; i<text.length(); i++) {
            if(Character.isDigit(text.charAt(i)))
                cleaned += text.charAt(i);
            else if(text.charAt(i) == '.')
                cleaned += text.charAt(i);
            else if(text.charAt(i) == ' ')
                cleaned += text.charAt(i);
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
}
