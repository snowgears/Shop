package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ComboShop;
import com.snowgears.shop.shop.ShopType;
import net.md_5.bungee.api.chat.*;
import net.pl3x.bukkit.chatapi.ComponentSender;

import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.JsonParseException;


public class ShopMessage {

    private final static Shop plugin = Shop.getPlugin();

    private static final Map<String, Function<PlaceholderContext, TextComponent>> placeholders = new HashMap<>();
    // Regex pattern to identify placeholders within square brackets, e.g., [owner]
    private static final String COLOR_CODE_REGEX = "([&§][0-9A-FK-ORXa-fk-orx])";
    private static final String HEX_CODE_REGEX = "(#[0-9a-fA-F]{6})";
    // Modified placeholder regex to only match real placeholders
    private static final String PLACEHOLDER_REGEX = "(\\[([^&§#\\[\\]]+)\\])";
    private static final String TEXT_SEGMENT_REGEX = "([^&§\\[#]+)";
    // Add dedicated patterns for brackets to ensure they're matched individually
    private static final String OPEN_BRACKET_REGEX = "(\\[)";
    private static final String CLOSE_BRACKET_REGEX = "(\\])";
    private static final String MESSAGE_PARTS_REGEX = 
        COLOR_CODE_REGEX + "|" + 
        HEX_CODE_REGEX + "|" + 
        PLACEHOLDER_REGEX + "|" + 
        OPEN_BRACKET_REGEX + "|" +  // Match opening bracket
        CLOSE_BRACKET_REGEX + "|" +  // Match closing bracket
        TEXT_SEGMENT_REGEX + "|" +  // Match text segments
        "(.{1})";          // Match any single character as fallback

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
    private static int targetMaxLength;
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
        targetMaxLength = displayConfig.getInt("targetMaxLength", 40);

        // Load in our placeholders
        this.loadPlaceholders();
    }

    /**
     * Registers a placeholder with its corresponding value retrieval function.
     *
     * @param placeholder The placeholder string without brackets, e.g., "owner"
     * @param valueFunction A function that takes a PlaceholderContext instance and returns the replacement string
     */
    public static void registerPlaceholder(String placeholder, Function<PlaceholderContext, TextComponent> valueFunction) {
        placeholders.put(placeholder.toLowerCase(), valueFunction);
    }

    /**
     * Attempts to replace a single placeholder within a message.
     *
     * @param placeholder The placeholder string without brackets, e.g., "owner"
     * @param context     The PlaceholderContext instance containing Shop and Player
     * @return The replacement string or an empty string if replacement fails
     */
    public static TextComponent replacePlaceholder(String placeholder, PlaceholderContext context) {
        plugin.getShopLogger().spam("[ShopMessage.replacePlaceholder] Attempting to replace placeholder: " + placeholder + " " + context);
        Function<PlaceholderContext, TextComponent> valueFunction = placeholders.get(placeholder.toLowerCase());
        if (valueFunction != null) {
            try {
                plugin.getShopLogger().spam("[ShopMessage.replacePlaceholder]     Running placeholder function... " + placeholder);
                TextComponent message = valueFunction.apply(context);
                if (message != null) {
                    plugin.getShopLogger().trace("[ShopMessage.replacePlaceholder]  *** placeholder " + placeholder + "  value: " + message);
                    return message;
                }
            } catch (Exception e) {
                // Log the exception
                Bukkit.getLogger().warning("Error replacing placeholder " + placeholder + ": " + e.getMessage());
                e.printStackTrace();
            } catch (Error e) {
                Bukkit.getLogger().warning("Error replacing placeholder " + placeholder + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        // If placeholder not found, remove the placeholder and just return an empty string
        plugin.getShopLogger().spam("[ShopMessage.replacePlaceholder] *** returning empty string, unable to get function to replace placeholder: " + placeholder);
        return new TextComponent("");
    }

    // Gets the ChatColor from a string, supports Hex Colors.
    // Returns null if no color is found.
    public static ChatColor getColor(String color) {
        ChatColor newColor = null;
        if (color.matches(HEX_CODE_REGEX)) { 
            if (!MCVersion.atLeast("1.16")) {
                plugin.getShopLogger().warning("Hex Colors are only supported in 1.16+! Please remove any hex colors from your config files! Color with issue: " + color);
                return null;
            }
            newColor = ChatColor.of(color);
        }
        else if (color.matches(COLOR_CODE_REGEX)) {
            newColor = ChatColor.getByChar(color.charAt(1));
        }
        return newColor;
    }

    /**
     * Formats a message by replacing all placeholders with their respective values.
     *
     * @param message The message containing placeholders
     * @param context The PlaceholderContext instance containing Shop and Player
     * @return The formatted message with all placeholders replaced
     */
    public static TextComponent format(String message, PlaceholderContext context) {
        TextComponent formattedMessage = null;
        if (message == null) { return new TextComponent(""); }
        plugin.getShopLogger().spam("[ShopMessage] pre-format: " + ChatColor.translateAlternateColorCodes('&', message), true);

        // Define the regex pattern
        Matcher matcher = Pattern.compile(MESSAGE_PARTS_REGEX).matcher(message);
        List<String> parts = new ArrayList<>();
        while (matcher.find()) {
            parts.add(matcher.group());
        }

        ChatColor latestColor = null;
        boolean addedText = false;

        boolean isBold = false;
        boolean isItalic = false;
        boolean isStrikethrough = false;
        boolean isUnderlined = false;
        boolean isObfuscated = false;
        
        for (String part : parts) {
            plugin.getShopLogger().spam("\n\n");
            plugin.getShopLogger().trace("[ShopMessage.format] part: " + part);
            TextComponent partComponent = new TextComponent(part);

            // Check if we are a color code
            if (part.matches(COLOR_CODE_REGEX) || part.matches(HEX_CODE_REGEX)) {
                try {
                    ChatColor newColor = getColor(part);
                    if (newColor != null) {
                        if (newColor == ChatColor.BOLD) isBold = true;
                        else if (newColor == ChatColor.ITALIC) isItalic = true;
                        else if (newColor == ChatColor.STRIKETHROUGH) isStrikethrough = true;
                        else if (newColor == ChatColor.UNDERLINE) isUnderlined = true;
                        else if (newColor == ChatColor.MAGIC) isObfuscated = true;
                        else if (newColor == ChatColor.RESET) {
                            plugin.getShopLogger().hyper("[ShopMessage.format]     matched RESET color code: " + part);
                            latestColor = ChatColor.WHITE;
                            isBold = false;
                            isItalic = false;
                            isStrikethrough = false;
                            isUnderlined = false;
                            isObfuscated = false;
                            formattedMessage.addExtra("§r");
                        } else {
                            latestColor = newColor;
                        }
                    }
                    plugin.getShopLogger().hyper("[ShopMessage.format]     matched COLOR_CODE_REGEX: " + part);
                    plugin.getShopLogger().hyper("[ShopMessage.format]     newColor: " + newColor.toString());
                    plugin.getShopLogger().hyper("[ShopMessage.format] *** skipping to next part: " + newColor.getName().toUpperCase());
                    plugin.getShopLogger().hyper("[ShopMessage.format]     isBold: " + isBold + " isItalic: " + isItalic + " isStrikethrough: " + isStrikethrough + " isUnderlined: " + isUnderlined + " isObfuscated: " + isObfuscated);
                    continue; // Don't add this text to the message, just go to the next part
                } catch (Exception e) {
                    plugin.getShopLogger().hyper("[ShopMessage.format] XXX unknown color code! Going to add this as a normal string! " + part);
                }
            }

            // If we match to a placeholder, then we want to use it's TextComponent instead of the "normal" one
            if (part.matches(PLACEHOLDER_REGEX)) {
                plugin.getShopLogger().hyper("[ShopMessage.format]     matched PLACEHOLDER_REGEX: " + part);
                plugin.getShopLogger().hyper("[ShopMessage.format]     is part placeholder? " + (placeholders.get(part) != null));
                if (placeholders.get(part) != null) {
                    plugin.getShopLogger().hyper("[ShopMessage.format]     replacing placeholder... " + part);
                    partComponent = replacePlaceholder(part, context);
                    // Check if we set a color inside our part (for example [stock color])
                    if (partComponent.getColor() != latestColor && partComponent.getColor() != null && partComponent.getColor() != ChatColor.WHITE) { 
                        plugin.getShopLogger().hyper("[ShopMessage.format]     getting latestColor from partComponent: " + partComponent.getColor().getName().toUpperCase());
                        latestColor = partComponent.getColor(); 
                    }
                }
            }

            // Set the color
            if (latestColor != null) {
                plugin.getShopLogger().hyper("[ShopMessage.format]     setting part color to: " + latestColor.getName().toUpperCase());
                partComponent.setColor(latestColor);
            }
            partComponent.setBold(isBold);
            partComponent.setItalic(isItalic);
            partComponent.setStrikethrough(isStrikethrough);
            partComponent.setUnderlined(isUnderlined);
            partComponent.setObfuscated(isObfuscated);
            if (formattedMessage == null) {
                formattedMessage = partComponent;
            } else {
                // Add the part of the string to the
                formattedMessage.addExtra(partComponent);
            }
            addedText = true;
            plugin.getShopLogger().hyper("[ShopMessage.format] *** add part TextComponent to main message: " + partComponent);
        }

        // Handle if we are just a color code with an empty string
        if (formattedMessage == null) formattedMessage = new TextComponent("");
        if (!addedText && latestColor != null) formattedMessage.setColor(latestColor);

        plugin.getShopLogger().spam("[ShopMessage] postFormat: " + formattedMessage.toLegacyText(), true);
        return formattedMessage;
    }

    /**
     * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
     */
    public static void sendMessage(String message, Player player, PlaceholderContext context) {
        TextComponent fancyMessage = format(message, context);
        // Verify we are not trying to send an empty string or null
        // if(!ChatColor.stripColor(fancyMessage.toLegacyText()).trim().isEmpty())
        plugin.getShopLogger().debug("Sent msg to player " + player.getName() + ": " + fancyMessage.toLegacyText(), true);
        try {
            if (!MCVersion.atLeast("1.13")) {
                // Use ChatComponentAPI to send the message for legacy versions
                // https://github.com/OstlerDev/ChatComponentAPI
                ComponentSender.sendMessage(player, fancyMessage);
                return; 
            }
            player.spigot().sendMessage(fancyMessage);
            return;
        } catch (JsonParseException e) {
            plugin.getNBTAdapter().handleException("Possible NBTAPI error while sending message to player, Item Hover events will now be disabled! Details: " + e.getMessage());
        } catch (Error | Exception e) {
            plugin.getShopLogger().warning("Error sending message to player: " + e.getMessage());
        }

        // If we get here, we have an error, we should at least try to send it as legacy text
        try {
            player.sendMessage(fancyMessage.toLegacyText());
            plugin.getShopLogger().warning("Sent legacy text message to player as backup, removed hover/click events");
        } catch (Exception e) {} catch (Error e) {}
    }

    /**
     * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
     */
    public static void sendMessage(String message, Player player) {
        PlaceholderContext context = new PlaceholderContext();
        context.setPlayer(player);
        sendMessage(message, player, context);
    }

    /**
     * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
     */
    public static void sendMessage(String message, Player player, ItemStack item) {
        PlaceholderContext context = new PlaceholderContext();
        context.setPlayer(player);
        context.setItem(item);
        sendMessage(message, player, context);
    }

    /**
     * Loads message, swaps in placeholder values, sends fancy message with Click/Hover events to Player
     */
    public static void sendMessage(String key, String subkey, Player player, AbstractShop shop) {
        String message = getUnformattedMessage(key, subkey);
        if(message != null && !message.isEmpty())
            sendMessage(message, player, shop);
    }

    /**
     * Loads message, swaps in placeholder values, sends fancy message with Click/Hover events to Player
     */
    public static void sendMessage(String key, String subkey, ShopCreationProcess process, Player player) {
        PlaceholderContext context = new PlaceholderContext();
        context.setPlayer(player);
        context.setProcess(process);
        String message = getUnformattedMessage(key, subkey);
        if(message != null && !message.isEmpty())
            sendMessage(message, player, context);
    }

    /**
     * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
     */
    public static void sendMessage(String message, Player player, AbstractShop shop) {
        PlaceholderContext context = new PlaceholderContext();
        context.setPlayer(player);
        context.setShop(shop);
        sendMessage(message, player, context);
    }

    /**
     * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
     */
    public static void sendMessage(String message, Player player, Player user, AbstractShop shop) {
        PlaceholderContext context = new PlaceholderContext();
        context.setPlayer(user);
        context.setShop(shop);
        sendMessage(message, player, context);
    }

    /**
     * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
     */
    public static void sendMessage(String message, ShopCreationProcess process, Player player) {
        PlaceholderContext context = new PlaceholderContext();
        context.setPlayer(player);
        context.setProcess(process);
        sendMessage(message, player, context);
    }

    /**
     * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
     */
    public static void sendMessage(String message, Player player, OfflineTransactions offlineTxs) {
        PlaceholderContext context = new PlaceholderContext();
        context.setPlayer(player);
        context.setOfflineTransactions(offlineTxs);
        sendMessage(message, player, context);
    }

    /**
     * Loads all available placeholders into the map.
     * This method should be called during the plugin's initialization phase.
     */
    public static void loadPlaceholders() {
        registerPlaceholder("[plugin]", context -> new TextComponent(plugin.getCommandAlias()));
        registerPlaceholder("[server name]", context -> new TextComponent(ShopMessage.getServerDisplayName()));
        registerPlaceholder("[player]", context -> { Player player = context.getPlayer(); return new TextComponent((player != null) ? player.getName() : ""); });
        registerPlaceholder("[user]", context -> {
            if (context.getPlayer() != null) return new TextComponent(context.getPlayer().getName());
            if (context.getOfflinePlayer() != null) return new TextComponent(context.getOfflinePlayer().getName());
            return new TextComponent("Unknown Player");
        });
        registerPlaceholder("[shop type]", context -> {
            if (context.getProcess() != null && context.getProcess().getShopType() != null) return new TextComponent(context.getProcess().getShopType().toString());
            if (context.getShop() != null) return new TextComponent(ShopMessage.getCreationWord(context.getShop().getType().toString().toUpperCase()));
            return null;
        });
        registerPlaceholder("[shop types]", ShopMessage::getShopTypesPlaceholder);
        registerPlaceholder("[total shops]", context -> new TextComponent(String.valueOf(plugin.getShopHandler().getNumberOfShops())));

        // Player Info Placeholders
        registerPlaceholder("[owner]", context -> {
            if (context.getProcess() != null) return new TextComponent(String.valueOf(Bukkit.getOfflinePlayer(context.getProcess().getPlayerUUID())));
            else if (context.getShop() != null) return new TextComponent(context.getShop().isAdmin() ? ShopMessage.getServerDisplayName() : context.getShop().getOwnerName());
            return null;
        });
        registerPlaceholder("[user amount]", context -> {
            if (context.getPlayer() != null) return new TextComponent(String.valueOf(plugin.getShopHandler().getNumberOfShops(context.getPlayer())));
            else if (context.getShop().getOwner() != null) return new TextComponent(String.valueOf(plugin.getShopHandler().getNumberOfShops(context.getShop().getOwner().getUniqueId())));
            return new TextComponent("0"); // If they don't have any shops, it should be 0
        });
        registerPlaceholder("[build limit]", context -> new TextComponent(String.valueOf(plugin.getShopListener().getBuildLimit(context.getPlayer()))));
        registerPlaceholder("[tp time remaining]", context -> new TextComponent(String.valueOf(plugin.getShopListener().getTeleportCooldownRemaining(context.getPlayer()))));

        // Location Placeholders
        registerPlaceholder("[world]", context -> {
            if (context.getProcess() != null && context.getProcess().getClickedChest() != null) return new TextComponent(context.getProcess().getClickedChest().getWorld().getName());
            else if (context.getShop() != null) return new TextComponent(context.getShop().getSignLocation().getWorld().getName());
            return null;
        });
        registerPlaceholder("[location]", context -> {
            Location loc = null;
            if (context.getLocation() != null) loc = context.getLocation();
            else if (context.getProcess() != null && context.getProcess().getClickedChest() != null) loc = context.getProcess().getClickedChest().getLocation();
            else if (context.getShop() != null) loc = context.getShop().getSignLocation();
            if (loc == null) return null;
            TextComponent text = new TextComponent(UtilMethods.getCleanLocation(loc, false));
            if (context.getProcess() == null && context.getShop() == null) return text;
            text.setHoverEvent(getShopInfoHoverEvent(context));
            return text;
        });

        // Currency Placeholders
        registerPlaceholder("[currency name]", context -> new TextComponent(plugin.getCurrencyName()));
        registerPlaceholder("[currency item]", context -> embedItem(plugin.getItemNameUtil().getName(plugin.getItemCurrency()), plugin.getItemCurrency()));

        // Shop Item placeholders
        registerPlaceholder("[item]", ShopMessage::getItemPlaceholder);
        registerPlaceholder("[item amount]", context -> {
            if (context.getItem() != null) return new TextComponent(String.valueOf(context.getItem().getAmount()));
            else if (context.getProcess() != null) return new TextComponent(String.valueOf(context.getProcess().getItemAmount()));
            else if (context.getShop() != null && context.getShop().getItemStack() != null) return new TextComponent(String.valueOf(context.getShop().getItemStack().getAmount()));
            return null;
        });
        registerPlaceholder("[item enchants]", context -> { 
            if (context.getShop() != null) { return embedItem(UtilMethods.getEnchantmentsComponent(context.getShop().getItemStack()), context.getShop().getItemStack()); } 
            if (context.getProcess() != null) { return embedItem(UtilMethods.getEnchantmentsComponent(context.getProcess().getItemStack()), context.getProcess().getItemStack()); } 
            if (context.getItem() != null) { return embedItem(UtilMethods.getEnchantmentsComponent(context.getItem()), context.getItem()); }
            return null; });
        registerPlaceholder("[item lore]", context -> { 
            if (context.getShop() != null) { return embedItem(UtilMethods.getLoreString(context.getShop().getItemStack()), context.getShop().getItemStack()); } 
            if (context.getProcess() != null) { return embedItem(UtilMethods.getLoreString(context.getProcess().getItemStack()), context.getProcess().getItemStack()); } 
            if (context.getItem() != null) { return embedItem(UtilMethods.getLoreString(context.getItem()), context.getItem()); }
            return null; 
        });
        registerPlaceholder("[item durability]", context -> { if (context.getShop() != null) { return new TextComponent(String.valueOf(context.getShop().getItemDurabilityPercent())); } return null; });
        registerPlaceholder("[item type]", context -> { if (context.getShop() != null && context.getShop().getType() == ShopType.GAMBLE) { return new TextComponent("???"); } else { return new TextComponent(Shop.getPlugin().getItemNameUtil().getNameTranslatable(context.getShop().getItemStack().getType()).toLegacyText()); } });
        registerPlaceholder("[gamble item amount]", context -> { if (context.getShop() != null && context.getShop().getType() == ShopType.GAMBLE) { return new TextComponent(String.valueOf(context.getShop().getAmount())); } return null; });
        registerPlaceholder("[gamble item]", context -> { if (context.getShop() != null && context.getShop().getType() == ShopType.GAMBLE) { return embedItem(plugin.getItemNameUtil().getName(plugin.getGambleDisplayItem()), plugin.getGambleDisplayItem()); } return null; });

        // Shop Barter Item Placeholders
        registerPlaceholder("[barter item amount]", context -> {
            if (context.getBarterItem() != null) return new TextComponent(String.valueOf(context.getBarterItem().getAmount()));
            if (context.getShop() != null && context.getShop().getSecondaryItemStack() != null) return new TextComponent(String.valueOf(context.getShop().getSecondaryItemStack().getAmount()));
            if (context.getProcess() != null) return new TextComponent(String.valueOf(context.getProcess().getBarterItemAmount()));
            if (context.getItem() != null) return new TextComponent(String.valueOf(context.getItem().getAmount()));
            return null;
        });
        registerPlaceholder("[barter item]", ShopMessage::getBarterItemPlaceholder);
        registerPlaceholder("[barter item durability]", context -> { if (context.getShop() != null && context.getShop().getType() == ShopType.BARTER && context.getShop().getSecondaryItemStack() != null) { return new TextComponent(String.valueOf(context.getShop().getSecondaryItemDurabilityPercent())); } return null; });
        registerPlaceholder("[barter item type]", context -> { if (context.getShop() != null && context.getShop().getType() == ShopType.BARTER && context.getShop().getSecondaryItemStack() != null) { return new TextComponent(Shop.getPlugin().getItemNameUtil().getNameTranslatable(context.getShop().getSecondaryItemStack().getType())); } return null; });
        registerPlaceholder("[barter item enchants]", context -> { 
            if (context.getBarterItem() != null) { return embedItem(UtilMethods.getEnchantmentsComponent(context.getBarterItem()), context.getBarterItem()); }
            if (context.getShop() != null && context.getShop().getSecondaryItemStack() != null) { return embedItem(UtilMethods.getEnchantmentsComponent(context.getShop().getSecondaryItemStack()), context.getShop().getSecondaryItemStack()); } 
            if (context.getProcess() != null) { return embedItem(UtilMethods.getEnchantmentsComponent(context.getProcess().getBarterItemStack()), context.getProcess().getBarterItemStack()); } 
            if (context.getItem() != null) { return embedItem(UtilMethods.getEnchantmentsComponent(context.getItem()), context.getItem()); }
            return null; 
        });
        registerPlaceholder("[barter item lore]", context -> { 
            if (context.getBarterItem() != null) { return embedItem(UtilMethods.getLoreString(context.getBarterItem()), context.getBarterItem()); }
            if (context.getShop() != null && context.getShop().getType() == ShopType.BARTER && context.getShop().getSecondaryItemStack() != null) { return embedItem(UtilMethods.getLoreString(context.getShop().getSecondaryItemStack()), context.getShop().getSecondaryItemStack()); } 
            if (context.getProcess() != null) { return embedItem(UtilMethods.getLoreString(context.getProcess().getBarterItemStack()), context.getProcess().getBarterItemStack()); } 
            if (context.getItem() != null) { return embedItem(UtilMethods.getLoreString(context.getItem()), context.getItem()); }
            return null; 
        });

        // Shop Pricing Placeholders
        registerPlaceholder("[amount]", context -> { if (context.getShop() != null) { return new TextComponent(String.valueOf(context.getShop().getAmount())); } return null; });
        registerPlaceholder("[price sell]", context -> { if (context.getShop() != null && context.getShop().getType() == ShopType.COMBO) { return new TextComponent(((ComboShop) context.getShop()).getPriceSellString()); } return null; });
        registerPlaceholder("[price sell per item]", context -> { if (context.getShop() != null && context.getShop().getType() == ShopType.COMBO) { return new TextComponent(((ComboShop) context.getShop()).getPriceSellPerItemString()); } return null; });
        registerPlaceholder("[price combo]", context -> { if (context.getShop() != null && context.getShop().getType() == ShopType.COMBO) { return new TextComponent(((ComboShop) context.getShop()).getPriceComboString()); } return null; });
        registerPlaceholder("[price per item]", context -> { if (context.getShop() != null) { return new TextComponent(context.getShop().getPricePerItemString()); } return null; });
        registerPlaceholder("[price]", context -> { if (context.getShop() != null) { return new TextComponent(context.getShop().getPriceString()); } return null; });
        registerPlaceholder("[stock]", context -> {
            if (context.getShop() == null) { return null; }
            else if (context.getShop().isAdmin()) {
                return new TextComponent(String.valueOf(ShopMessage.getAdminStockWord()));
            } else {
                return new TextComponent(String.valueOf(context.getShop().getStock()));
            }
        });
        registerPlaceholder("[stock color]", context -> {
            if (context.getShop() == null) { return null; }
            if (context.getShop().getStock() < 1) {
                return format(getUnformattedMessage("signtext", "outofstockcolor"), context);
            }
            return format(getUnformattedMessage("signtext", "instockcolor"), context);
        });

        // Notify Placeholders
        registerPlaceholder("[notify user]", context -> {
            // @TODO: is this correct?
            String text_on = getUnformattedMessage("command", "notify_on");
            String text_off = getUnformattedMessage("command", "notify_off");

            ShopGuiHandler.GuiIcon guiIcon = plugin.getGuiHandler().getIconFromOption(context.getPlayer(), PlayerSettings.Option.NOTIFICATION_SALE_USER);
            return new TextComponent((guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_USER_ON) ? text_on : text_off);
        });

        registerPlaceholder("[notify owner]", context -> {
            String text_on = getUnformattedMessage("command", "notify_on");
            String text_off = getUnformattedMessage("command", "notify_off");

            ShopGuiHandler.GuiIcon guiIcon = plugin.getGuiHandler().getIconFromOption(context.getPlayer(), PlayerSettings.Option.NOTIFICATION_SALE_OWNER);
            return new TextComponent((guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_OWNER_ON) ? text_on : text_off);
        });

        registerPlaceholder("[notify stock]", context -> {
            String text_on = getUnformattedMessage("command", "notify_on");
            String text_off = getUnformattedMessage("command", "notify_off");

            ShopGuiHandler.GuiIcon guiIcon = plugin.getGuiHandler().getIconFromOption(context.getPlayer(), PlayerSettings.Option.NOTIFICATION_STOCK);
            return new TextComponent((guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_STOCK_ON) ? text_on : text_off);
        });

        // Offline Transaction Updates
        registerPlaceholder("[offline transactions]", context -> {
            TextComponent numOfTransactions = new TextComponent(String.valueOf(context.getOfflineTransactions().getNumTransactions()));
            numOfTransactions.setHoverEvent(getTransactionsHoverEvent(context));
            return numOfTransactions;
        });
        registerPlaceholder("[offline profit]", context -> {
            String boughtString = Shop.getPlugin().getPriceString(context.getOfflineTransactions().getTotalProfit(), false);
            if (boughtString.equals(freePriceWord)) { boughtString = "0"; }
            return new TextComponent(boughtString);
        });
        registerPlaceholder("[offline spent]", context -> {
            String spentString = Shop.getPlugin().getPriceString(context.getOfflineTransactions().getTotalSpent(), false);
            if (spentString.equals(freePriceWord)) { spentString = "0"; }
            return new TextComponent(spentString);
        });
        registerPlaceholder("[offline items sold]", context -> ShopMessage.getOfflineItemsPlaceholder(context, context.getOfflineTransactions().getItemsSold()));
        registerPlaceholder("[offline items bought]", context -> ShopMessage.getOfflineItemsPlaceholder(context, context.getOfflineTransactions().getItemsBought()));
        registerPlaceholder("[shops out of stock]", ShopMessage::getShopsOutOfStockPlaceholder);
    }

    private static HoverEvent getItemHoverEvent(ItemStack item) {
        if (item == null) { return null; }
        if (plugin.getNBTAdapter().haveErrorsOccured()) {
            plugin.getNBTAdapter().handleException("NBTAPI errors occurred, unable to show embedded Hover Text for item!");
            return null; 
        }
        try {
            ItemStack hoverItem = item.clone();
            hoverItem.setAmount(1);
            BaseComponent[] component = new BaseComponent[]{new TextComponent(plugin.getNBTAdapter().getNBTforItem(hoverItem))};
            return new HoverEvent(HoverEvent.Action.SHOW_ITEM, component);
            // 1.21.5 Paper - hover event is borked: https://github.com/PaperMC/Paper/issues/12289

            // In theory in the future we could remove the NBTAPI dependency and do:
            // String itemId = item.getType().getKey().toString();
            // String nbt = item.getItemMeta().getAsString(); // Special Bukkit function to get the NBT as a string built for HoverEvents!
            // ItemTag tag = ItemTag.ofNbt(nbt);
            // Item itemContent = new Item(itemId, item.getAmount(), tag);
            // return new HoverEvent(HoverEvent.Action.SHOW_ITEM, itemContent);
        } catch (Error | Exception e) {
            return null;
        }
    }

    private static TextComponent embedItem(String message, ItemStack item) {
        return embedItem(new TextComponent(message), item);
    }

    private static TextComponent embedItem(TextComponent message, ItemStack item) {
        // If we have any NBTAPI errors, don't try to embed the item hover text
        if (plugin.getNBTAdapter().haveErrorsOccured()) {
            return message;
        }
        try {
            if (item == null) { return null; }
            TextComponent msg;
            String legacyText = UtilMethods.removeColorsIfOnlyWhite(message.toLegacyText());
            if (legacyText.trim().isEmpty()) { return message; }
            
            msg = new TextComponent(TextComponent.fromLegacyText(legacyText));
            HoverEvent event = getItemHoverEvent(item);
            if (event != null) { msg.setHoverEvent(event); }
            return msg;
        } catch (Exception e) {
            plugin.getNBTAdapter().handleException("Error embedding item hover text: " + e.getMessage());
            return message;
        } catch (Error e) {
            plugin.getNBTAdapter().handleException("Error embedding item hover text: " + e.getMessage());
            return message;
        }
    }

    private static HoverEvent getTransactionsHoverEvent(PlaceholderContext context) {
        try {
            String transactionLore = context.getOfflineTransactions().getTransactionsLore();
            
            // Use the universally available TextComponent.fromLegacyText() method - no reflection needed
            BaseComponent[] components = TextComponent.fromLegacyText(transactionLore);
            if (components.length == 0) {
                components = new BaseComponent[]{new TextComponent(transactionLore)};
            }
            
            return new HoverEvent(HoverEvent.Action.SHOW_TEXT, components);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private static HoverEvent getShopInfoHoverEvent(PlaceholderContext context) {
        try {
            TextComponent hoverText = new TextComponent();
            List<String> hoverLines = getUnformattedMessageList("hover", "location");
            int i = 0;
            for (String line : hoverLines) {
                i++;
                // Add new lines between text
                hoverText.addExtra(format(line + (i == hoverLines.size() ? "" : "\n"), context));
            }
            // We need to remove any nested hover events, since legacy versions don't like them
            // When a hover event has no text, it will be replaced with "null", so we need to remove that
            // It's a bit of a hack, but it works well...
            String legacyText = hoverText.toLegacyText().replace("§fnull§f", "§f");
            BaseComponent[] components = TextComponent.fromLegacyText(legacyText);
            return new HoverEvent(HoverEvent.Action.SHOW_TEXT, components);
        } catch (Exception e) {}
        return null;
    }

    /**
     * Helper method to handle the [shop types] placeholder.
     *
     * @param context The PlaceholderContext instance.
     * @return A comma-separated list of shop types the player can create.
     */
    private static TextComponent getShopTypesPlaceholder(PlaceholderContext context) {
        List<ShopType> typeList = new ArrayList<>(Arrays.asList(ShopType.values()));
        Player player = context.getPlayer();

        if ((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
            typeList.remove(ShopType.GAMBLE);
        }

        if (plugin.usePerms()) {
            Iterator<ShopType> typeIterator = typeList.iterator();
            while (typeIterator.hasNext()) {
                ShopType type = typeIterator.next();
                if (
                    !player.hasPermission("shop.operator") 
                    && !player.hasPermission("shop.create." + type.toString()) 
                    && !player.hasPermission("shop.create")) {
                    typeIterator.remove();
                }
            }
        }

        StringBuilder types = new StringBuilder();
        for (int i = 0; i < typeList.size(); i++) {
            types.append(typeList.get(i).toCreationWord());
            if (i < typeList.size() - 1) {
                types.append(", ");
            }
        }
        return new TextComponent(types.toString());
    }

    /**
     * Helper method to handle the [item] placeholder with truncation for signs.
     *
     * @param context The PlaceholderContext instance.
     * @return The item name, potentially truncated to fit sign constraints.
     */
    private static TextComponent getItemPlaceholder(PlaceholderContext context) {
        ItemStack item = null;
        if (context.getItem() != null) {
            item = context.getItem();
        }
        else if (context.getProcess() != null) {
            item = context.getProcess().getItemStack(); 
        }
        else if (context.getShop() != null || context.getShop().getItemStack() != null) {
            item = context.getShop().getItemStack();
        }
        if (item == null) { return null; }

        TextComponent itemName = plugin.getItemNameUtil().getName(item);
        if (context.isForSign()) {
            return new TextComponent(UtilMethods.trimForSign(itemName.toPlainText()));
        }
        return embedItem(itemName, item);
    }

    /**
     * Helper method to handle the [barter item] placeholder with truncation for signs.
     *
     * @param context The PlaceholderContext instance.
     * @return The barter item name, potentially truncated to fit sign constraints.
     */
    private static TextComponent getBarterItemPlaceholder(PlaceholderContext context) {
        ItemStack item = null;
        if (context.getBarterItem() != null) {
            item = context.getBarterItem();
        }
        else if (context.getItem() != null) {
            item = context.getItem();
        }
        else if (context.getProcess() != null) {
            item = context.getProcess().getBarterItemStack();
        }
        else if (context.getShop() != null && context.getShop().getSecondaryItemStack() != null) {
            if (context.getShop().getType() != ShopType.BARTER) {
                return null;
            }
            item = context.getShop().getSecondaryItemStack();
        }
        if (item == null) { return null; }

        TextComponent itemName = plugin.getItemNameUtil().getName(item);
        if (context.isForSign()) {
            return new TextComponent(UtilMethods.trimForSign(itemName.toLegacyText()));
        }
        return embedItem(itemName, item);
    }

    private static TextComponent getOfflineItemsPlaceholder(PlaceholderContext context, Map<ItemStack, Integer> items) {
        TextComponent itemRowsText = new TextComponent("");
        String itemRow = getUnformattedMessage("offline", "itemRow");

        int i = 0;
        for (Map.Entry<ItemStack, Integer> entry : items.entrySet()) {
            i++;
            // Add a new line character between our rows, don't add it if we are the last item (since we don't want an extra line!
            String addNewLine = i < (items.size()) ? "\n" : "";
            ItemStack item = entry.getKey();
            item.setAmount(entry.getValue());

            PlaceholderContext itemContext = new PlaceholderContext();
            itemContext.setPlayer(context.getPlayer());
            itemContext.setItem(item);

            TextComponent currentRow = format(itemRow + addNewLine, itemContext);
            itemRowsText.addExtra(currentRow);
        }
        // If there were no lines added, just return null so that we don't log a blank line!
        if (i == 0) return null;

        return itemRowsText;
    }

    private static TextComponent getShopsOutOfStockPlaceholder(PlaceholderContext context) {
        TextComponent shopsOutOfStock = new TextComponent("");
        List<AbstractShop> playerShops = Shop.getPlugin().getShopHandler().getShops(context.getPlayer().getUniqueId());
        if (playerShops != null && !playerShops.isEmpty()) {
            // Collect all the out of stock shops
            List<AbstractShop> outOfStock = new ArrayList<>();
            for (AbstractShop shop : playerShops) {
                if (shop.getStock() == 0) {
                    outOfStock.add(shop);
                }
            }
            // No out of stock shops!
            if (outOfStock.isEmpty()) { return null; }

            // Add the lines for each
            int i = 0;
            int remainingOutOfStock = 0;
            List<String> remainingShopsMsgs = new ArrayList<>();
            for (AbstractShop shop : outOfStock) {
                i++;

                PlaceholderContext shopContext = new PlaceholderContext();
                shopContext.setPlayer(context.getPlayer());
                shopContext.setShop(shop);

                // For each item, generate a line based on the template line
                String addNewLine = (i < (outOfStock.size()) && i <= 3) ? "\n" : "";
                TextComponent currentRow = format(getUnformattedMessage("offline", "outOfStockShop") + addNewLine, shopContext);
                // Limit out of stock shops to 3
                if (i > 3) {
                    remainingShopsMsgs.add(currentRow.toLegacyText());
                } else {
                    shopsOutOfStock.addExtra(currentRow);
                }
            }

            if (!remainingShopsMsgs.isEmpty()) {
                String remainingMsg = getUnformattedMessage("offline", "moreOutOfStock");
                TextComponent remaining = format(remainingMsg.replace("[out of stock remaining]", "" + remainingShopsMsgs.size()), context);
                remaining.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(String.join("\n", remainingShopsMsgs)).create()));
                shopsOutOfStock.addExtra(remaining);
            }

            return shopsOutOfStock;
        }
        // No shops for player! don't add anything! p.s. should never get here.
        return null;
    }

    public static String getCreationWord(String type) {
        return creationWords.get(type);
    }

    public static String getFreePriceWord() {
        return freePriceWord;
    }

    public static String getAdminStockWord() {
        return adminStockWord;
    }

    public static String getServerDisplayName() {
        return serverDisplayName;
    }

    public static String getUnformattedMessage(String key, String subKey) {
        String message;
        if (subKey != null)
            message = messageMap.get(key + "_" + subKey);
        else
            message = messageMap.get(key);
        return message;
    }

    public static String formatMessage(String unformattedMessage, AbstractShop shop) {
        PlaceholderContext context = new PlaceholderContext();
        context.setShop(shop);
        TextComponent formattedMessage = format(unformattedMessage, context);
        // Return the legacy version since we are requesting the legacy formatter!
        return ChatColor.translateAlternateColorCodes('§', formattedMessage.toLegacyText());
    }

    public static String formatMessage(String unformattedMessage, AbstractShop shop, Player player, boolean forSign) {
        PlaceholderContext context = new PlaceholderContext();
        context.setPlayer(player);
        context.setShop(shop);
        context.setForSign(forSign);
        TextComponent formattedMessage = format(unformattedMessage, context);
        // Return the legacy version since we are requesting the legacy formatter!
        return ChatColor.translateAlternateColorCodes('§', formattedMessage.toLegacyText());
    }

    // Perform partial formatting to insert transaction purchase amounts since they might differ from shop amounts (partial sales)
    public static String getMessageFromOrders(ShopType transactionType, String subKey, double price, int amount){
        String message = ShopMessage.getUnformattedMessage(transactionType.toString(), subKey);
        String priceStr = Shop.getPlugin().getPriceString(price, false);
        message = message.replace("[price]", priceStr);
        message = message.replace("[item amount]", "" + amount);
        if(transactionType == ShopType.BARTER) {
            message = message.replace("[barter item amount]", "" + (int) price);
        }
        return message;
    }

    //      # [amount] : The amount of items the shop is selling/buying/bartering #
    //      # [price] : The price of the items the shop is selling (adjusted to match virtual or physical currency) #
    //      # [owner] : The name of the shop owner #
    //      # [server name] : The name of the server #
    public static String[] getSignLines(AbstractShop shop, ShopType shopType){

        DisplayType displayType = null;
        if (shop.getDisplay() != null)
            displayType = shop.getDisplay().getType();
        if (displayType == null)
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
            lines[i] = UtilMethods.trimForSign(lines[i]);
        }
        return lines;
    }

    public static String[] getTimeoutSignLines(AbstractShop shop){

        String[] lines = shopSignTextMap.get("timeout");

        for(int i=0; i<lines.length; i++) {
            lines[i] = formatMessage(lines[i], shop, null, true);
            lines[i] = ChatColor.translateAlternateColorCodes('&', lines[i]);
            lines[i] = UtilMethods.trimForSign(lines[i]);
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
//            formattedLine = ChatColor.translateAlternateColorCodes('&', formattedLine);

            Boolean splitLine = formattedLine.contains("[split]");
            formattedLine = formattedLine.replace("[split]", "");
            if(formattedLine != null && !formattedLine.isEmpty() && !ChatColor.stripColor(formattedLine).trim().isEmpty()) {
                if (splitLine) {
                    List<String> splitLines = UtilMethods.splitStringIntoLines(formattedLine, targetMaxLength);
                    formattedLines.addAll(splitLines);
                } else {
                    formattedLines.add(formattedLine);
                }
            }
        }
        return formattedLines;
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
        messageMap.put("initialCreateInstruction", chatConfig.getString("interaction.initialCreateInstruction"));
        messageMap.put("createHitChest", chatConfig.getString("interaction.createHitChest"));
        messageMap.put("adminCreateHitChest", chatConfig.getString("interaction.adminCreateHitChest"));

        messageMap.put("permission_use", chatConfig.getString("permission.use"));
        messageMap.put("permission_create", chatConfig.getString("permission.create"));
        messageMap.put("permission_destroy", chatConfig.getString("permission.destroy"));
        messageMap.put("permission_buildLimit", chatConfig.getString("permission.buildLimit"));

        messageMap.put("creativeSelection_disabled", chatConfig.getString("creativeSelection.disabled"));

        messageMap.put("interactionIssue_line2", chatConfig.getString("interaction_issue.createLine2"));
        messageMap.put("interactionIssue_line3", chatConfig.getString("interaction_issue.createLine3"));
        messageMap.put("interactionIssue_noItem", chatConfig.getString("interaction_issue.createNoItem"));
        messageMap.put("interactionIssue_direction", chatConfig.getString("interaction_issue.createDirection"));
        messageMap.put("interactionIssue_sameItem", chatConfig.getString("interaction_issue.createSameItem"));
        messageMap.put("interactionIssue_displayRoom", chatConfig.getString("interaction_issue.createDisplayRoom"));
        messageMap.put("interactionIssue_signRoom", chatConfig.getString("interaction_issue.createSignRoom"));
        messageMap.put("interactionIssue_createOtherPlayer", chatConfig.getString("interaction_issue.createOtherShop"));
        messageMap.put("interactionIssue_createInsufficientFunds", chatConfig.getString("interaction_issue.createInsufficientFunds"));
        messageMap.put("interactionIssue_createCooldown", chatConfig.getString("interaction_issue.createCooldown"));
        messageMap.put("interactionIssue_destroyInsufficientFunds", chatConfig.getString("interaction_issue.destroyInsufficientFunds"));
        messageMap.put("interactionIssue_createCancel", chatConfig.getString("interaction_issue.createCancel"));
        messageMap.put("interactionIssue_teleportInsufficientFunds", chatConfig.getString("interaction_issue.teleportInsufficientFunds"));
        messageMap.put("interactionIssue_teleportInsufficientCooldown", chatConfig.getString("interaction_issue.teleportInsufficientCooldown"));
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
        for(String s : chatConfig.getStringList("hover.location")){
            messageMap.put("hover_location"+count, s);
            count++;
        }
        count = 1;
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

        count = 1;
        for(String s : chatConfig.getStringList("transaction.OFFLINE_TRANSACTIONS_NOTIFICATION.summary")){
            messageMap.put("offline_summary"+count, s);
            count++;
        }
        messageMap.put("offline_itemRow", chatConfig.getString("transaction.OFFLINE_TRANSACTIONS_NOTIFICATION.itemRow"));
        messageMap.put("offline_outOfStockShop", chatConfig.getString("transaction.OFFLINE_TRANSACTIONS_NOTIFICATION.outOfStockShop"));
        messageMap.put("offline_moreOutOfStock", chatConfig.getString("transaction.OFFLINE_TRANSACTIONS_NOTIFICATION.moreOutOfStock"));

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
        messageMap.put("signtext_instockcolor", signConfig.getString("stock_color.in_stock"));
        messageMap.put("signtext_outofstockcolor", signConfig.getString("stock_color.out_of_stock"));
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

    public static int getTargetMaxLength() {
        return targetMaxLength;
    }
}