package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ComboShop;
import com.snowgears.shop.shop.ShopType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;


public class ShopMessage {

    private static HashMap<String, String> messageMap = new HashMap<String, String>();
    private static HashMap<String, String[]> shopSignTextMap = new HashMap<String, String[]>();
    private static HashMap<String, List<String>> displayTextMap = new HashMap<String, List<String>>();
    private static String freePriceWord;
    private static String adminStockWord;
    private static String serverDisplayName;
    private static HashMap<String, String> creationWords = new HashMap<String, String>();
    private static YamlConfiguration chatConfig;
    private static YamlConfiguration signConfig;
    private static YamlConfiguration displayConfig;

    public ShopMessage(Shop plugin) {

        File chatConfigFile = new File(plugin.getDataFolder(), "chatConfig.yml");
        chatConfig = YamlConfiguration.loadConfiguration(chatConfigFile);
        File signConfigFile = new File(plugin.getDataFolder(), "signConfig.yml");
        signConfig = YamlConfiguration.loadConfiguration(signConfigFile);
        File displayConfigFile = new File(plugin.getDataFolder(), "displayConfig.yml");
        displayConfig = YamlConfiguration.loadConfiguration(displayConfigFile);

        loadMessagesFromConfig();
        loadSignTextFromConfig();
        loadDisplayTextFromConfig();
        loadCreationWords();

        freePriceWord = signConfig.getString("sign_text.zeroPrice");
        adminStockWord = signConfig.getString("sign_text.adminStock");
        serverDisplayName = signConfig.getString("sign_text.serverDisplayName");
    }

    public static String getCreationWord(String type){
        return creationWords.get(type);
    }

    public static String getFreePriceWord(){
        return freePriceWord;
    }

    public static String getAdminStockWord(){
        return adminStockWord;
    }

    public static String getServerDisplayName(){
        return serverDisplayName;
    }

    public static String getMessage(String key, String subKey, AbstractShop shop, Player player) {
        String message = "";
        String mainKey = key;
        if (subKey != null) {
            mainKey = key + "_" + subKey;
        }

        if(messageMap.containsKey(mainKey))
            message = messageMap.get(mainKey);
        else
            return message;

        message = formatMessage(message, shop, player, false);
        return message;
    }

    public static String getUnformattedMessage(String key, String subKey) {
        String message;
        if (subKey != null)
            message = messageMap.get(key + "_" + subKey);
        else
            message = messageMap.get(key);
        return message;
    }

    public static String formatMessage(String unformattedMessage, ShopCreationProcess process, Player player) {
        if (unformattedMessage == null) {
            loadMessagesFromConfig();
            return "";
        }

        if (process != null) {
            unformattedMessage = unformattedMessage.replace("[item amount]", "" + process.getItemAmount());
            unformattedMessage = unformattedMessage.replace("[item]", "" + Shop.getPlugin().getItemNameUtil().getName(process.getItemStack()));
            unformattedMessage = unformattedMessage.replace("[barter item]", "" + Shop.getPlugin().getItemNameUtil().getName(process.getBarterItemStack()));
            unformattedMessage = unformattedMessage.replace("[barter item amount]", "" + process.getBarterItemAmount());
            if(process.getShopType() != null) {
                unformattedMessage = unformattedMessage.replace("[shop type]", "" + process.getShopType().toString());
            }
            if(process.getClickedChest() != null) {
                unformattedMessage = unformattedMessage.replace("[location]", UtilMethods.getCleanLocation(process.getClickedChest().getLocation(), false));
                unformattedMessage = unformattedMessage.replace("[world]", process.getClickedChest().getWorld().getName());
            }
        }

        if(player != null){
            unformattedMessage = unformattedMessage.replace("[owner]", "" + Bukkit.getOfflinePlayer(process.getPlayerUUID().toString()));

            if(unformattedMessage.contains("[shop types]")) {
                List<ShopType> typeList = Arrays.asList(ShopType.values());
                if((!Shop.getPlugin().usePerms() && !player.isOp()) || (Shop.getPlugin().usePerms() && !player.hasPermission("shop.operator"))){
                    typeList.remove(ShopType.GAMBLE);
                }
                if(Shop.getPlugin().usePerms()){
                    Iterator<ShopType> typeIterator = typeList.iterator();
                    while(typeIterator.hasNext()){
                        ShopType type = typeIterator.next();
                        if(!player.hasPermission("shop.operator") && !player.hasPermission("shop.create."+type.toString())){
                            typeIterator.remove();
                        }
                    }
                }
                String types = "";
                for(int i=0; i<typeList.size(); i++){
                    if(i < typeList.size() - 1)
                        types += typeList.get(i).toCreationWord() + ", ";
                    else
                        types += typeList.get(i).toCreationWord();
                }
                unformattedMessage = unformattedMessage.replace("[shop types]", types);
            }
        }
        unformattedMessage = ChatColor.translateAlternateColorCodes('&', unformattedMessage);
        return unformattedMessage;
    }

    public static String formatMessage(String unformattedMessage, AbstractShop shop, Player player, boolean forSign){
        if(unformattedMessage == null) {
            loadMessagesFromConfig();
            return "";
        }
        if(shop != null && shop.getItemStack() != null) {
            unformattedMessage = unformattedMessage.replace("[item amount]", "" + shop.getItemStack().getAmount());
            unformattedMessage = unformattedMessage.replace("[location]", UtilMethods.getCleanLocation(shop.getSignLocation(), false));
            unformattedMessage = unformattedMessage.replace("[item enchants]", UtilMethods.getEnchantmentsString(shop.getItemStack()));
            unformattedMessage = unformattedMessage.replace("[item lore]", UtilMethods.getLoreString(shop.getItemStack()));
            //dont replace [item] tag on first run through if its for a sign
            if(!forSign)
                unformattedMessage = unformattedMessage.replace("[item]", "" + Shop.getPlugin().getItemNameUtil().getName(shop.getItemStack()));
            unformattedMessage = unformattedMessage.replace("[item durability]", "" + shop.getItemDurabilityPercent());
            if(shop.getType() == ShopType.GAMBLE)
                unformattedMessage = unformattedMessage.replace("[item type]", "???");
            else
                unformattedMessage = unformattedMessage.replace("[item type]", "" + Shop.getPlugin().getItemNameUtil().getName(shop.getItemStack().getType()));

            if(shop.getType() == ShopType.GAMBLE) {
                unformattedMessage = unformattedMessage.replace("[gamble item amount]", "" + shop.getAmount());
                unformattedMessage = unformattedMessage.replace("[gamble item]", "" + Shop.getPlugin().getItemNameUtil().getName(Shop.getPlugin().getGambleDisplayItem()));
            }
        }

        if(shop != null && shop.getSecondaryItemStack() != null) {
            if(shop.getType() == ShopType.BARTER) {
                unformattedMessage = unformattedMessage.replace("[barter item amount]", "" + shop.getSecondaryItemStack().getAmount());
                //dont replace [barter item] tag on first run through if its for a sign
                if (!forSign)
                    unformattedMessage = unformattedMessage.replace("[barter item]", "" + Shop.getPlugin().getItemNameUtil().getName(shop.getSecondaryItemStack()));
                unformattedMessage = unformattedMessage.replace("[barter item durability]", "" + shop.getSecondaryItemDurabilityPercent());
                unformattedMessage = unformattedMessage.replace("[barter item type]", "" + Shop.getPlugin().getItemNameUtil().getName(shop.getSecondaryItemStack().getType()));
                unformattedMessage = unformattedMessage.replace("[barter item enchants]", UtilMethods.getEnchantmentsString(shop.getSecondaryItemStack()));
                unformattedMessage = unformattedMessage.replace("[barter item lore]", UtilMethods.getLoreString(shop.getSecondaryItemStack()));
            }
        }
        if(shop != null) {
            if(shop.isAdmin())
                unformattedMessage = unformattedMessage.replace("[owner]", "" + ShopMessage.getServerDisplayName());
            else
                unformattedMessage = unformattedMessage.replace("[owner]", "" + shop.getOwnerName());

            unformattedMessage = unformattedMessage.replace("[amount]", "" + shop.getAmount());
            if(shop.getSignLocation() != null) {
                unformattedMessage = unformattedMessage.replace("[location]", "" + UtilMethods.getCleanLocation(shop.getSignLocation(), false));
                unformattedMessage = unformattedMessage.replace("[world]", "" + shop.getSignLocation().getWorld().getName());
            }

            if(shop.getType() == ShopType.COMBO) {
                unformattedMessage = unformattedMessage.replace("[price sell]", "" + ((ComboShop)shop).getPriceSellString());
                unformattedMessage = unformattedMessage.replace("[price sell per item]", "" + ((ComboShop)shop).getPriceSellPerItemString());
                unformattedMessage = unformattedMessage.replace("[price combo]", "" + ((ComboShop)shop).getPriceComboString());
            }
            if(shop.getType() == ShopType.BARTER) {
                String amountPerString = new DecimalFormat("#.##").format(shop.getPrice() / shop.getAmount()).toString();
                amountPerString = amountPerString + " " + Shop.getPlugin().getItemNameUtil().getName(shop.getSecondaryItemStack());
                unformattedMessage = unformattedMessage.replace("[price per item]", "" + amountPerString);
                unformattedMessage = unformattedMessage.replace("[price]", "" + (int) shop.getPrice());
            }
            else {
                unformattedMessage = unformattedMessage.replace("[price per item]", "" + shop.getPricePerItemString());
                unformattedMessage = unformattedMessage.replace("[price]", "" + shop.getPriceString());
            }
            unformattedMessage = unformattedMessage.replace("[shop type]", "" + ShopMessage.getCreationWord(shop.getType().toString().toUpperCase())); //sub in user's word for SELL,BUY,BARTER
            if(unformattedMessage.contains("[stock]")) {
                if(shop.isAdmin()){
                    unformattedMessage = unformattedMessage.replace("[stock]", "" + adminStockWord);
                }
                else {
                    unformattedMessage = unformattedMessage.replace("[stock]", "" + shop.getStock());
                    //if shop is displaying stock on sign, it will require a sign refresh on transactions
                    shop.setSignLinesRequireRefresh(true);
                }
            }
            if(unformattedMessage.contains("[stock color]")) {
                if(shop.getStock() > 0 || shop.isAdmin())
                    unformattedMessage = unformattedMessage.replace("[stock color]", "" + ChatColor.GREEN);
                else
                    unformattedMessage = unformattedMessage.replace("[stock color]", "" + ChatColor.DARK_RED);
                //if shop is displaying stock on sign, it will require a sign refresh on transactions
                shop.setSignLinesRequireRefresh(true);
            }
        }
        if(player != null) {
            unformattedMessage = unformattedMessage.replace("[user]", "" + player.getName());
            unformattedMessage = unformattedMessage.replace("[user amount]", "" + Shop.getPlugin().getShopHandler().getNumberOfShops(player));
            unformattedMessage = unformattedMessage.replace("[build limit]", "" + Shop.getPlugin().getShopListener().getBuildLimit(player));
            if(unformattedMessage.contains("[shop types]")) {
                List<ShopType> typeList = Arrays.asList(ShopType.values());
                if((!Shop.getPlugin().usePerms() && !player.isOp()) || (Shop.getPlugin().usePerms() && !player.hasPermission("shop.operator"))){
                    typeList.remove(ShopType.GAMBLE);
                }
                if(Shop.getPlugin().usePerms()){
                    Iterator<ShopType> typeIterator = typeList.iterator();
                    while(typeIterator.hasNext()){
                        ShopType type = typeIterator.next();
                        if(!player.hasPermission("shop.operator") && !player.hasPermission("shop.create."+type.toString())){
                            typeIterator.remove();
                        }
                    }
                }
                String types = "";
                for(int i=0; i<typeList.size(); i++){
                    if(i < typeList.size() - 1)
                        types += typeList.get(i).toCreationWord() + ", ";
                    else
                        types += typeList.get(i).toCreationWord();
                }
                unformattedMessage = unformattedMessage.replace("[shop types]", types);
            }

            if(unformattedMessage.contains("[notify")) {

                String text_on = getUnformattedMessage("command", "notify_on");
                String text_off = getUnformattedMessage("command", "notify_off");

                boolean active = Shop.getPlugin().getGuiHandler().getSettingsOption(player, PlayerSettings.Option.SALE_USER_NOTIFICATIONS);
                if(active)
                    unformattedMessage = unformattedMessage.replace("[notify user]", "" + text_on);
                else
                    unformattedMessage = unformattedMessage.replace("[notify user]", "" + text_off);

                active = Shop.getPlugin().getGuiHandler().getSettingsOption(player, PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS);
                if(active)
                    unformattedMessage = unformattedMessage.replace("[notify owner]", "" + text_on);
                else
                    unformattedMessage = unformattedMessage.replace("[notify owner]", "" + text_off);

                active = Shop.getPlugin().getGuiHandler().getSettingsOption(player, PlayerSettings.Option.STOCK_NOTIFICATIONS);
                if(active)
                    unformattedMessage = unformattedMessage.replace("[notify stock]", "" + text_on);
                else
                    unformattedMessage = unformattedMessage.replace("[notify stock]", "" + text_off);
            }
        }
        unformattedMessage = unformattedMessage.replace("[server name]", "" + ShopMessage.getServerDisplayName());
        unformattedMessage = unformattedMessage.replace("[currency name]", "" + Shop.getPlugin().getCurrencyName());
        unformattedMessage = unformattedMessage.replace("[currency item]", "" + Shop.getPlugin().getItemNameUtil().getName(Shop.getPlugin().getItemCurrency()));
        unformattedMessage = unformattedMessage.replace("[total shops]", "" + Shop.getPlugin().getShopHandler().getNumberOfShops());
        unformattedMessage = unformattedMessage.replace("[plugin]", "" + Shop.getPlugin().getCommandAlias());

        if(forSign){
            if(unformattedMessage.contains("[item]") && shop != null && shop.getItemStack() != null){
                int tagLength = "[item]".length();
                String itemName = Shop.getPlugin().getItemNameUtil().getName(shop.getItemStack());
                int itemNameLength = itemName.length();
                int totalLength = unformattedMessage.length() - tagLength + itemNameLength;
                if(totalLength > 17){
                    String cutItemName = itemName.substring(0, (itemName.length()-(Math.abs(17 - totalLength))));
                    unformattedMessage = unformattedMessage.replace("[item]", cutItemName);
                }
                else{
                    unformattedMessage = unformattedMessage.replace("[item]", "" + Shop.getPlugin().getItemNameUtil().getName(shop.getItemStack()));
                }
            }
            if(unformattedMessage.contains("[barter item]") && shop != null && shop.getSecondaryItemStack() != null){
                if(shop.getType() == ShopType.BARTER) {
                    int tagLength = "[barter item]".length();
                    String itemName = Shop.getPlugin().getItemNameUtil().getName(shop.getSecondaryItemStack());
                    int itemNameLength = itemName.length();
                    int totalLength = unformattedMessage.length() - tagLength + itemNameLength;
                    if (totalLength > 17) {
                        String cutItemName = itemName.substring(0, (itemName.length() - (Math.abs(17 - totalLength))));
                        unformattedMessage = unformattedMessage.replace("[barter item]", cutItemName);
                    } else {
                        unformattedMessage = unformattedMessage.replace("[barter item]", "" + Shop.getPlugin().getItemNameUtil().getName(shop.getSecondaryItemStack()));
                    }
                }
            }
        }

        unformattedMessage = ChatColor.translateAlternateColorCodes('&', unformattedMessage);
        return unformattedMessage;
    }

    public static String partialFormatMessageUUID(String unformattedMessage, UUID playerUUID){
        if(playerUUID.equals(Shop.getPlugin().getShopHandler().getAdminUUID()))
            unformattedMessage = unformattedMessage.replace("[owner]", "" + creationWords.get("ADMIN"));
        else
            unformattedMessage = unformattedMessage.replace("[owner]", "" + Bukkit.getOfflinePlayer(playerUUID).getName());
        unformattedMessage = unformattedMessage.replace("[user amount]", "" + Shop.getPlugin().getShopHandler().getNumberOfShops(playerUUID));
        return unformattedMessage;
    }

    //      # [amount] : The amount of items the shop is selling/buying/bartering #
    //      # [price] : The price of the items the shop is selling (adjusted to match virtual or physical currency) #
    //      # [owner] : The name of the shop owner #
    //      # [server name] : The name of the server #
    public static String[] getSignLines(AbstractShop shop, ShopType shopType){

        DisplayType displayType = shop.getDisplay().getType();
        if(displayType == null)
            displayType = Shop.getPlugin().getDisplayType();

        String shopFormat;
        if(shop.isAdmin())
            shopFormat = "admin";
        else
            shopFormat = "normal";

        if(displayType == DisplayType.NONE){
            shopFormat += "_no_display";
        }

        String[] lines = getUnformattedShopSignLines(shopType, shopFormat);

        for(int i=0; i<lines.length; i++) {
            lines[i] = formatMessage(lines[i], shop, null, true);
            lines[i] = ChatColor.translateAlternateColorCodes('&', lines[i]);

            //TODO have smart way of cutting lines if too long so at least some of word can go on
//            if(lines[i].length() > 15)
//                lines[i]
        }
        return lines;
    }

    public static String[] getTimeoutSignLines(AbstractShop shop){

        String[] lines = shopSignTextMap.get("timeout");

        for(int i=0; i<lines.length; i++) {
            lines[i] = formatMessage(lines[i], shop, null, true);
            lines[i] = ChatColor.translateAlternateColorCodes('&', lines[i]);

            //TODO have smart way of cutting lines if too long so at least some of word can go on
//            if(lines[i].length() > 15)
//                lines[i]
        }
        return lines;
    }

    public static ArrayList<String> getDisplayTags(AbstractShop shop, ShopType shopType){

        //in future may need to add more options here like "admin" or "no stock" or "no display" other than normal

//        String shopFormat;
//        if(shop.isAdmin())
//            shopFormat = "admin";
//        else
            String shopFormat = "normal";

//        if(displayType == DisplayType.NONE){
//            shopFormat += "_no_display";
//        }

        ArrayList<String> formattedLines = new ArrayList<>();
        List<String> lines = displayTextMap.get(shopType.toString().toUpperCase()+"_"+shopFormat);

        String formattedLine;
        for(String line : lines) {
            formattedLine = formatMessage(line, shop, null, false);
            formattedLine = ChatColor.translateAlternateColorCodes('&', formattedLine);

            if(!formattedLine.isEmpty() && !ChatColor.stripColor(formattedLine).isEmpty() && (ChatColor.stripColor(formattedLine).trim().length() > 0))
                formattedLines.add(formattedLine);
        }
        return formattedLines;
    }

    public static List<String> getMessageList(String key, String subKey, AbstractShop shop, Player player){
        List<String> messages = new ArrayList<>();

        int count = 1;
        String message = "-1";
        while (message != null && !message.isEmpty()) {
            message = getMessage(key, subKey + count, shop, player);
            if (message != null && !message.isEmpty())
                messages.add(message);
            count++;
        }
        return messages;
    }

    public static List<String> getUnformattedMessageList(String key, String subKey){
        List<String> messages = new ArrayList<>();

        int count = 1;
        String message = "-1";
        while (message != null && !message.isEmpty()) {
            message = getUnformattedMessage(key, subKey + count);
            if (message != null && !message.isEmpty())
                messages.add(message);
            count++;
        }
        return messages;
    }

    private static String[] getUnformattedShopSignLines(ShopType type, String subtype) {
        return shopSignTextMap.get(type.toString()+"_"+subtype).clone();
    }

    private static void loadMessagesFromConfig() {

        for (ShopType type : ShopType.values()) {
            messageMap.put(type.toString() + "_user", chatConfig.getString("transaction." + type.toString().toUpperCase() + ".user"));
            messageMap.put(type.toString() + "_owner", chatConfig.getString("transaction." + type.toString().toUpperCase() + ".owner"));

            messageMap.put(type.toString() + "_initialize", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".initialize"));
            if(type == ShopType.BUY || type == ShopType.COMBO)
                messageMap.put(type.toString() + "_initializeAlt", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".initializeAlt"));
            else if(type == ShopType.BARTER) {
                messageMap.put(type.toString() + "_initializeInfo", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".initializeInfo"));
                messageMap.put(type.toString() + "_initializeBarter", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".initializeBarter"));
                messageMap.put(type.toString() + "_createHitChest", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".createHitChest"));
                messageMap.put(type.toString() + "_createHitChestBarterAmount", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".createHitChestBarterAmount"));
                messageMap.put(type.toString() + "_initializeBarterAlt", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".initializeBarterAlt"));
            }
            messageMap.put(type.toString() + "_create", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".create"));
            messageMap.put(type.toString() + "_destroy", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".destroy"));
            messageMap.put(type.toString() + "_opDestroy", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".opDestroy"));
            messageMap.put(type.toString() + "_opOpen", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".opOpen"));

            messageMap.put(type.toString() + "_shopNoStock", chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".shopNoStock"));
            messageMap.put(type.toString() + "_ownerNoStock", chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".ownerNoStock"));
            messageMap.put(type.toString() + "_shopNoSpace", chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".shopNoSpace"));
            messageMap.put(type.toString() + "_ownerNoSpace", chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".ownerNoSpace"));
            messageMap.put(type.toString() + "_playerNoStock", chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".playerNoStock"));
            messageMap.put(type.toString() + "_playerNoSpace", chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".playerNoSpace"));

            if(type != ShopType.GAMBLE){
                messageMap.put(type.toString() + "_createHitChestAmount", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".createHitChestAmount"));
            }
            if(type != ShopType.BARTER){
                messageMap.put(type.toString() + "_createHitChestPrice", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".createHitChestPrice"));
            }
            if(type == ShopType.COMBO){
                messageMap.put(type.toString() + "_createHitChestPriceCombo", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".createHitChestPriceCombo"));
            }

            int count = 1;
            for(String s : chatConfig.getStringList("description."+type.toString().toUpperCase())){
                messageMap.put(type.toString() + "_description"+count, s);
                count++;
            }
        }
        messageMap.put("createHitChest", chatConfig.getString("interaction.createHitChest"));
        messageMap.put("adminCreateHitChest", chatConfig.getString("interaction.adminCreateHitChest"));

        messageMap.put("permission_use", chatConfig.getString("permission.use"));
        messageMap.put("permission_create", chatConfig.getString("permission.create"));
        messageMap.put("permission_destroy", chatConfig.getString("permission.destroy"));
        messageMap.put("permission_buildLimit", chatConfig.getString("permission.buildLimit"));

        messageMap.put("interactionIssue_line2", chatConfig.getString("interaction_issue.createLine2"));
        messageMap.put("interactionIssue_line3", chatConfig.getString("interaction_issue.createLine3"));
        messageMap.put("interactionIssue_noItem", chatConfig.getString("interaction_issue.createNoItem"));
        messageMap.put("interactionIssue_direction", chatConfig.getString("interaction_issue.createDirection"));
        messageMap.put("interactionIssue_sameItem", chatConfig.getString("interaction_issue.createSameItem"));
        messageMap.put("interactionIssue_displayRoom", chatConfig.getString("interaction_issue.createDisplayRoom"));
        messageMap.put("interactionIssue_signRoom", chatConfig.getString("interaction_issue.createSignRoom"));
        messageMap.put("interactionIssue_createOtherPlayer", chatConfig.getString("interaction_issue.createOtherShop"));
        messageMap.put("interactionIssue_createInsufficientFunds", chatConfig.getString("interaction_issue.createInsufficientFunds"));
        messageMap.put("interactionIssue_destroyInsufficientFunds", chatConfig.getString("interaction_issue.destroyInsufficientFunds"));
        messageMap.put("interactionIssue_teleportInsufficientFunds", chatConfig.getString("interaction_issue.teleportInsufficientFunds"));
        messageMap.put("interactionIssue_initialize", chatConfig.getString("interaction_issue.initializeOtherShop"));
        messageMap.put("interactionIssue_destroyChest", chatConfig.getString("interaction_issue.destroyChest"));
        messageMap.put("interactionIssue_useOwnShop", chatConfig.getString("interaction_issue.useOwnShop"));
        messageMap.put("interactionIssue_useShopAlreadyInUse", chatConfig.getString("interaction_issue.useShopAlreadyInUse"));
        messageMap.put("interactionIssue_adminOpen", chatConfig.getString("interaction_issue.adminOpen"));
        messageMap.put("interactionIssue_worldBlacklist", chatConfig.getString("interaction_issue.worldBlacklist"));
        messageMap.put("interactionIssue_regionRestriction", chatConfig.getString("interaction_issue.regionRestriction"));
        messageMap.put("interactionIssue_itemListDeny", chatConfig.getString("interaction_issue.itemListDeny"));
        messageMap.put("interactionIssue_createHitChestTimeout", chatConfig.getString("interaction_issue.createHitChestTimeout"));


        int count = 1;
        for(String s : chatConfig.getStringList("creativeSelection.enter")){
            messageMap.put("creativeSelection_enter"+count, s);
            count++;
        }
        count = 1;
        for(String s : chatConfig.getStringList("creativeSelection.prompt")){
            messageMap.put("creativeSelection_prompt"+count, s);
            count++;
        }

        count = 1;
        for(String s : chatConfig.getStringList("guiSearchSelection.enter")){
            messageMap.put("guiSearchSelection_enter"+count, s);
            count++;
        }
        count = 1;
        for(String s : chatConfig.getStringList("guiSearchSelection.prompt")){
            messageMap.put("guiSearchSelection_prompt"+count, s);
            count++;
        }

        messageMap.put("command_list", chatConfig.getString("command.list"));
        messageMap.put("command_list_output_total", chatConfig.getString("command.list_output_total"));
        messageMap.put("command_list_output_perms", chatConfig.getString("command.list_output_perms"));
        messageMap.put("command_list_output_noperms", chatConfig.getString("command.list_output_noperms"));
        messageMap.put("command_currency", chatConfig.getString("command.currency"));
        messageMap.put("command_currency_output", chatConfig.getString("command.currency_output"));
        messageMap.put("command_currency_output_tip", chatConfig.getString("command.currency_output_tip"));
        messageMap.put("command_setcurrency", chatConfig.getString("command.setcurrency"));
        messageMap.put("command_setcurrency_output", chatConfig.getString("command.setcurrency_output"));
        messageMap.put("command_setgamble", chatConfig.getString("command.setgamble"));
        messageMap.put("command_itemrefresh", chatConfig.getString("command.itemrefresh"));
        messageMap.put("command_itemrefresh_output", chatConfig.getString("command.itemrefresh_output"));
        messageMap.put("command_itemlist", chatConfig.getString("command.itemlist"));
        messageMap.put("command_itemlist_add", chatConfig.getString("command.itemlist_add"));
        messageMap.put("command_itemlist_remove", chatConfig.getString("command.itemlist_remove"));
        messageMap.put("command_reload", chatConfig.getString("command.reload"));
        messageMap.put("command_reload_output", chatConfig.getString("command.reload_output"));
        messageMap.put("command_error_novault", chatConfig.getString("command.error_novault"));
        messageMap.put("command_error_nohand", chatConfig.getString("command.error_nohand"));
        messageMap.put("command_not_authorized", chatConfig.getString("command.not_authorized"));
        messageMap.put("command_notify_user", chatConfig.getString("command.notify_user"));
        messageMap.put("command_notify_owner", chatConfig.getString("command.notify_owner"));
        messageMap.put("command_notify_stock", chatConfig.getString("command.notify_stock"));
        messageMap.put("command_notify_on", chatConfig.getString("command.notify_on"));
        messageMap.put("command_notify_off", chatConfig.getString("command.notify_off"));
    }

    private void loadSignTextFromConfig() {
        Set<String> allTypes = signConfig.getConfigurationSection("sign_text").getKeys(false);
        for (String typeString : allTypes) {

            ShopType type = null;
            try { type = ShopType.valueOf(typeString);}
            catch (IllegalArgumentException e){}

            if (type != null) {
                try {
                    Set<String> normalLineNumbers = signConfig.getConfigurationSection("sign_text." + typeString + ".normal").getKeys(false);
                    String[] normalLines = new String[4];

                    int i = 0;
                    for (String number : normalLineNumbers) {
                        String message = signConfig.getString("sign_text." + typeString + ".normal." + number);
                        if (message == null)
                            normalLines[i] = "";
                        else
                            normalLines[i] = message;
                        i++;
                    }

                    this.shopSignTextMap.put(type.toString() + "_normal", normalLines);
                } catch (NullPointerException e) {}

                try {
                    Set<String> adminLineNumbers = signConfig.getConfigurationSection("sign_text." + typeString + ".admin").getKeys(false);
                    String[] adminLines = new String[4];

                    int i = 0;
                    for (String number : adminLineNumbers) {
                        String message = signConfig.getString("sign_text." + typeString + ".admin." + number);
                        if (message == null)
                            adminLines[i] = "";
                        else
                            adminLines[i] = message;
                        i++;
                    }

                    this.shopSignTextMap.put(type.toString() + "_admin", adminLines);
                } catch (NullPointerException e) {}

                try {
                    Set<String> normalNoDisplayLineNumbers = signConfig.getConfigurationSection("sign_text." + typeString + ".normal_no_display").getKeys(false);
                    String[] normalNoDisplayLines = new String[4];

                    int i = 0;
                    for (String number : normalNoDisplayLineNumbers) {
                        String message = signConfig.getString("sign_text." + typeString + ".normal_no_display." + number);
                        if (message == null)
                            normalNoDisplayLines[i] = "";
                        else
                            normalNoDisplayLines[i] = message;
                        i++;
                    }

                    this.shopSignTextMap.put(type.toString() + "_normal_no_display", normalNoDisplayLines);
                } catch (NullPointerException e) {}

                try {
                    Set<String> adminNoDisplayLineNumbers = signConfig.getConfigurationSection("sign_text." + typeString + ".admin_no_display").getKeys(false);
                    String[] adminNoDisplayLines = new String[4];

                    int i = 0;
                    for (String number : adminNoDisplayLineNumbers) {
                        String message = signConfig.getString("sign_text." + typeString + ".admin_no_display." + number);
                        if (message == null)
                            adminNoDisplayLines[i] = "";
                        else
                            adminNoDisplayLines[i] = message;
                        i++;
                    }

                    this.shopSignTextMap.put(type.toString() + "_admin_no_display", adminNoDisplayLines);
                } catch (NullPointerException e) {}
            }
        }
        String[] timeoutLines = new String[4];

        for (int i=1; i<5; i++) {
            String message = signConfig.getString("sign_text.timeout." + i);
            if (message == null)
                timeoutLines[i] = "";
            else
                timeoutLines[i] = message;
            i++;
        }

        this.shopSignTextMap.put("timeout", timeoutLines);
    }

    private void loadDisplayTextFromConfig() {
        displayTextMap = new HashMap<>();
        Set<String> allTypes = displayConfig.getConfigurationSection("display_tag_text").getKeys(false);
        for (String typeString : allTypes) {

            ShopType type = null;
            try { type = ShopType.valueOf(typeString);}
            catch (IllegalArgumentException e){}

            if (type != null) {
                try {
                    List<String> normalLines = displayConfig.getStringList("display_tag_text." + typeString.toUpperCase() + ".normal");
                    this.displayTextMap.put(type.toString().toUpperCase() + "_normal", normalLines);
                } catch (NullPointerException e) {}
            }
        }
    }

    private void loadCreationWords(){
        String shopString = signConfig.getString("sign_creation.SHOP");
        if(shopString != null)
            creationWords.put("SHOP", shopString.toLowerCase());
        else
            creationWords.put("SHOP", "[shop]");

        String sellString = signConfig.getString("sign_creation.SELL");
        if(sellString != null)
            creationWords.put("SELL", sellString.toLowerCase());
        else
            creationWords.put("SELL", "sell");

        String buyString = signConfig.getString("sign_creation.BUY");
        if(buyString != null)
            creationWords.put("BUY", buyString.toLowerCase());
        else
            creationWords.put("BUY", "buy");

        String barterString = signConfig.getString("sign_creation.BARTER");
        if(barterString != null)
            creationWords.put("BARTER", barterString.toLowerCase());
        else
            creationWords.put("BARTER", "barter");

        String gambleString = signConfig.getString("sign_creation.GAMBLE");
        if(gambleString != null)
            creationWords.put("GAMBLE", gambleString.toLowerCase());
        else
            creationWords.put("BARTER", "barter");

        String adminString = signConfig.getString("sign_creation.ADMIN");
        if(adminString != null)
            creationWords.put("ADMIN", adminString.toLowerCase());
        else
            creationWords.put("ADMIN", "admin");

        String comboString = signConfig.getString("sign_creation.COMBO");
        if(comboString != null)
            creationWords.put("COMBO", comboString.toLowerCase());
        else
            creationWords.put("COMBO", "combo");
    }
}